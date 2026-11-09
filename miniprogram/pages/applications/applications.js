const request = require('../../utils/request')
const STATUS = { PENDING_MENTOR: '待导师审核', PENDING_ADMIN: '待管理员审核', WAIT_ALLOC: '待分配', COMPLETED: '已完成', REJECTED: '已驳回' }
const PENDING = ['PENDING_MENTOR', 'PENDING_ADMIN', 'WAIT_ALLOC']

Page({
  data: { list: [], reason: '', start: '', end: '', submitting: false, eligible: false, eligibilityText: '正在校验申请资格', fixedAvailable: 0 },
  onShow() { this.load() },
  load() {
    Promise.all([
      request.get('/gw/application/list'), request.get('/gw/assignment/my'),
      request.get('/gw/profile/me'), request.get('/gw/workstation/list', { workstationType: 'FIXED', status: 'AVAILABLE' })
    ]).then(([appRes, assignmentRes, profileRes, stationRes]) => {
      const list = (appRes.data || []).map(item => Object.assign({}, item, { statusText: STATUS[item.status] || item.status }))
      const assignments = assignmentRes.data || []
      const profile = profileRes.data || {}
      const fixedAvailable = (stationRes.data || []).filter(item => item.roomStatus === 'NORMAL' && Number(item.mentorId) === Number(profile.mentorId)).length
      const hasPending = list.some(item => PENDING.indexOf(item.status) >= 0)
      let eligible = true
      let eligibilityText = '导师名下没有空闲固定工位，可以提交流动工位申请；申请将先由导师审批，再由管理员终审。'
      if (!profile.mentorId) { eligible = false; eligibilityText = '尚未绑定导师，不能提交申请。' }
      else if (assignments.length) { eligible = false; eligibilityText = '您已经拥有有效工位，不能重复申请。' }
      else if (hasPending) { eligible = false; eligibilityText = '已有未办结的流动工位申请，请等待处理。' }
      else if (fixedAvailable > 0) { eligible = false; eligibilityText = '导师名下还有 ' + fixedAvailable + ' 个空闲固定工位，请联系导师分配固定工位。' }
      this.setData({ list, eligible, eligibilityText, fixedAvailable })
    })
  },
  onInput(e) { this.setData({ reason: e.detail.value }) },
  onDate(e) { this.setData({ [e.currentTarget.dataset.field]: e.detail.value }) },
  submit() {
    if (!this.data.eligible) return wx.showToast({ title: this.data.eligibilityText, icon: 'none', duration: 2600 })
    const reason = (this.data.reason || '').trim()
    if (!reason) return wx.showToast({ title: '请填写申请原因', icon: 'none' })
    if (!this.data.start || !this.data.end) return wx.showToast({ title: '请选择完整使用期限', icon: 'none' })
    if (this.data.end < this.data.start) return wx.showToast({ title: '结束日期不能早于开始日期', icon: 'none' })
    this.setData({ submitting: true })
    request.post('/gw/application', { reason, plannedStart: this.data.start, plannedEnd: this.data.end }).then(() => {
      wx.showToast({ title: '提交成功' }); this.setData({ reason: '', start: '', end: '' }); this.load()
    }).finally(() => this.setData({ submitting: false }))
  }
})
