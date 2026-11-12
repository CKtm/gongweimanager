const request = require('../../utils/request')
const { isAdmin } = require('../../utils/admin')

const LABELS = {
  ACTIVE: '处罚生效中', RESTORED: '已提前恢复', EXPIRED: '已到期恢复', RECALLED: '原工位已收回',
  APPEAL_APPROVED: '申诉通过，处罚解除', PENDING_MENTOR: '待导师初审', PENDING_ADMIN: '待管理员终审',
  APPROVED: '申诉通过', REJECTED: '申诉驳回', VERIFIED: '已核实', ARCHIVED: '已归档'
}
const ENDPOINTS = { scores: '/gw/lifecycle/scores', penalties: '/gw/lifecycle/penalties', appeals: '/gw/appeals', messages: '/gw/messages' }

Page({
  data: {
    tabs: [], tab: 'messages', rows: [], pageNum: 1, total: 0, pageSize: 20,
    loading: false, saving: false, error: '', admin: false, mentor: false, student: false,
    sheet: null, reason: '', comment: '', decision: 'APPROVED', revisedScore: '', deadlineDate: '', deadlineTime: '23:59'
  },
  onLoad(options) { this.requestedTab = options.tab },
  onShow() {
    if (!wx.getStorageSync('token')) return wx.redirectTo({ url: '/pages/login/login' })
    const user = wx.getStorageSync('userInfo') || {}
    const admin = isAdmin(user), mentor = (user.roles || []).includes('mentor'), student = (user.roles || []).includes('common')
    const tabs = []
    if (student) tabs.push({ key: 'scores', label: '卫生评分' })
    if (student || mentor || admin) tabs.push({ key: 'penalties', label: '处罚记录' }, { key: 'appeals', label: '学生申诉' })
    tabs.push({ key: 'messages', label: '消息中心' })
    const requested = this.requestedTab || this.data.tab
    this.setData({ admin, mentor, student, tabs, tab: tabs.some(t => t.key === requested) ? requested : tabs[0].key })
    this.requestedTab = null
    return this.load(1)
  },
  onPullDownRefresh() { return this.load(1).finally(() => wx.stopPullDownRefresh()) },
  switchTab(e) {
    if (this.data.loading || this.data.saving) return
    this.setData({ tab: e.currentTarget.dataset.tab, rows: [], total: 0, pageNum: 1 })
    return this.load(1)
  },
  load(page) {
    if (this.data.loading) return Promise.resolve()
    const pageNum = typeof page === 'number' ? page : this.data.pageNum
    this.setData({ loading: true, error: '' })
    return request.get(ENDPOINTS[this.data.tab], { pageNum, pageSize: this.data.pageSize }).then(res => {
      const rows = (res.rows || res.data || []).map(row => ({ ...row,
        key: row.recordId || row.penaltyId || row.appealId || row.messageId,
        statusText: LABELS[row.status] || row.status || '',
        photoList: (row.photoUrls || '').split(',').filter(Boolean),
        canReview: this.data.tab === 'appeals' && (this.data.admin ? row.status === 'PENDING_ADMIN' : this.data.mentor && row.status === 'PENDING_MENTOR')
      }))
      this.setData({ rows, pageNum, total: res.total == null ? rows.length : res.total })
    }).catch(() => this.setData({ error: '加载失败，请下拉刷新或重试' }))
      .finally(() => this.setData({ loading: false }))
  },
  previous() { if (this.data.pageNum > 1) return this.load(this.data.pageNum - 1) },
  next() { if (this.data.pageNum * this.data.pageSize < this.data.total) return this.load(this.data.pageNum + 1) },
  input(e) { this.setData({ [e.currentTarget.dataset.field]: e.detail.value }) },
  selectDecision(e) { this.setData({ decision: e.detail.value }) },
  openAppeal(e) {
    if (!this.data.student || this.data.saving) return
    const target = this.data.rows.find(row => String(row.key) === String(e.currentTarget.dataset.id))
    if (!target) return
    this.setData({ sheet: { kind: 'appeal', title: '提交申诉', target }, reason: '' })
  },
  openReview(e) {
    const target = this.data.rows.find(row => String(row.key) === String(e.currentTarget.dataset.id))
    if (!target || !target.canReview || this.data.saving) return
    this.setData({ sheet: { kind: 'review', title: this.data.admin ? '管理员终审' : '导师初审', target }, comment: '', decision: 'APPROVED', revisedScore: '' })
  },
  openDeadline(e) {
    if (!this.data.admin || this.data.saving) return
    const target = this.data.rows.find(row => String(row.key) === String(e.currentTarget.dataset.id))
    if (!target || target.status !== 'ACTIVE') return
    const value = String(target.untilAt || '')
    this.setData({ sheet: { kind: 'deadline', title: '调整处罚截止时间', target }, deadlineDate: value.slice(0, 10), deadlineTime: value.slice(11, 16) || '23:59' })
  },
  close() { if (!this.data.saving) this.setData({ sheet: null }) },
  stopTouch() {},
  async submit() {
    if (this.data.saving || !this.data.sheet) return
    const d = this.data, sheet = d.sheet
    let path, body
    if (sheet.kind === 'appeal') {
      if (!d.reason.trim()) return wx.showToast({ title: '请填写申诉原因', icon: 'none' })
      path = '/gw/appeals'
      body = { targetType: d.tab === 'scores' ? 'SCORE' : 'PENALTY', targetId: sheet.target.key, reason: d.reason.trim() }
    } else if (sheet.kind === 'review') {
      if (!d.comment.trim()) return wx.showToast({ title: '请填写审核意见', icon: 'none' })
      body = { decision: d.decision, comment: d.comment.trim() }
      if (d.admin && d.decision === 'APPROVED' && sheet.target.targetType === 'SCORE') {
        const score = Number(d.revisedScore)
        if (d.revisedScore === '' || !Number.isInteger(score) || score < 0 || score > 100) return wx.showToast({ title: '请输入0至100的整数分数', icon: 'none' })
        body.revisedScore = score
      }
      path = '/gw/appeals/' + sheet.target.appealId + '/review'
    } else {
      if (!d.deadlineDate || !d.deadlineTime) return wx.showToast({ title: '请选择截止日期和时间', icon: 'none' })
      path = '/gw/lifecycle/penalties/' + sheet.target.penaltyId + '/deadline'
      body = { until: d.deadlineDate + ' ' + d.deadlineTime + ':00' }
    }
    this.setData({ saving: true })
    try {
      await request.post(path, body)
      this.setData({ sheet: null })
      wx.showToast({ title: sheet.kind === 'appeal' ? '申诉已提交' : '处理成功' })
      await this.load(1)
    } catch (e) { /* Keep the form available for correction or retry. */ }
    finally { this.setData({ saving: false }) }
  },
  async readMessage(e) {
    if (this.data.saving) return
    const message = this.data.rows.find(row => String(row.key) === String(e.currentTarget.dataset.id))
    if (!message) return
    this.setData({ saving: true })
    try {
      await request.post('/gw/messages/' + message.messageId + '/read', {})
      const allowed = /^\/pages\/(student-records|applications|returns|mentor|admin|myworkstation|inspection)\//
      if (allowed.test(message.destination)) wx.navigateTo({ url: message.destination })
      else wx.showModal({ title: message.title, content: message.content, showCancel: false })
      await this.load(this.data.pageNum)
    } catch (e) { /* The request utility displays the API error. */ }
    finally { this.setData({ saving: false }) }
  },
  async readAll() {
    if (this.data.saving) return
    this.setData({ saving: true })
    try { await request.post('/gw/messages/read-all', {}); await this.load(this.data.pageNum) }
    catch (e) { /* Preserve the inbox on failure. */ }
    finally { this.setData({ saving: false }) }
  },
  preview(e) {
    const row = this.data.rows.find(row => String(row.key) === String(e.currentTarget.dataset.id))
    if (row && row.photoList.length) wx.previewImage({ current: row.photoList[0], urls: row.photoList })
  }
})
