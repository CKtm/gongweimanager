const request = require('../../utils/request')

Page({
  onPullDownRefresh() {
    if (!wx.getStorageSync('token')) { wx.stopPullDownRefresh(); return }
    return Promise.all([
      this.data.isStudent ? this.loadStudent() : Promise.resolve(),
      this.data.isMentor ? this.loadMentor() : Promise.resolve(),
      this.data.isInspector ? this.loadInspector() : Promise.resolve(),
      this.data.isAdmin ? this.loadAdmin() : Promise.resolve()
    ]).catch(() => {}).finally(() => wx.stopPullDownRefresh())
  },

  data: {
    userInfo: null,
    roleText: '',
    workstation: null,
    applicationCount: 0,
    mentorApplicationCount: 0,
    mentorReturnCount: 0,
    inspectionTaskCount: 0,
    isStudent: false,
    isMentor: false,
    isInspector: false,
    isAdmin: false,
    adminPending: 0, adminWaiting: 0, adminInspection: 0
  },
  onShow() {
    const token = wx.getStorageSync('token')
    if (!token) return wx.redirectTo({ url: '../login/login' })
    const userInfo = wx.getStorageSync('userInfo') || {}
    const roles = (userInfo && userInfo.roles) || []
    const isStudent = roles.indexOf('common') >= 0
    const isMentor = roles.indexOf('mentor') >= 0
    const isInspector = roles.indexOf('inspector') >= 0
    const isAdmin = roles.indexOf('admin') >= 0 || roles.indexOf('college_admin') >= 0
    const labels = []
    if (isStudent) labels.push('研究生')
    if (isMentor) labels.push('导师')
    if (isInspector) labels.push('卫生检查员')
    if (isAdmin) labels.push('学院管理员')
    this.setData({ userInfo, roleText: labels.join(' / ') || '普通用户', isStudent, isMentor, isInspector, isAdmin, workstation: null, adminPending: 0, adminWaiting: 0, adminInspection: 0 })
    if (isStudent) this.loadStudent()
    if (isMentor) this.loadMentor()
    if (isInspector) this.loadInspector()
    if (isAdmin) this.loadAdmin()
  },
  loadStudent() {
    return Promise.all([
      request.get('/gw/assignment/my').then(res => this.setData({ workstation: (res.data || [])[0] || null })),
      request.get('/gw/application/list').then(res => {
        const rows = res.data || []
        this.setData({ applicationCount: rows.filter(item => ['PENDING_MENTOR', 'PENDING_ADMIN', 'WAIT_ALLOC'].indexOf(item.status) >= 0).length })
      })
    ]).catch(() => {})
  },
  loadMentor() {
    return Promise.all([request.get('/gw/application/list', { status: 'PENDING_MENTOR' }), request.get('/gw/return/list', { status: 'PENDING_MENTOR' })]).then(([apps, returns]) => {
      this.setData({ mentorApplicationCount: (apps.data || []).length, mentorReturnCount: (returns.data || []).length })
    }).catch(() => {})
  },
  loadInspector() {
    return request.get('/gw/inspection/task/list').then(res => {
      const rows = res.data || []
      this.setData({ inspectionTaskCount: rows.filter(item => ['PENDING', 'IN_PROGRESS'].indexOf(item.status) >= 0).length })
    }).catch(() => {})
  },
  loadAdmin() {
    const {can}=require('../../utils/admin'),user=wx.getStorageSync('userInfo')||{}
    const read=(url,permission)=>can(user,permission)?request.get(url).then(r=>r.data||r.rows||[]):Promise.resolve([])
    return Promise.all([read('/gw/application/list','gw:application:list'),read('/gw/return/list','gw:return:query'),read('/gw/inspection/record/list','gw:inspection:record:list')]).then(([apps,returns,records])=>this.setData({adminPending:apps.filter(x=>x.status==='PENDING_ADMIN').length+returns.filter(x=>['PENDING_ADMIN','WAIT_VERIFY'].includes(x.status)).length,adminWaiting:apps.filter(x=>x.status==='WAIT_ALLOC').length,adminInspection:records.filter(x=>x.status==='SUBMITTED'&&x.taskStatus==='SUBMITTED').length})).catch(()=>{})
  },
  openAdmin(e) { const tab=e.currentTarget.dataset.tab || 'reviews';wx.navigateTo({url:'/pages/admin/admin?tab='+tab}) },
  openWorkstations() { wx.navigateTo({ url: '../workstations/workstations' }) },
  openProfile() { wx.navigateTo({ url: '../profile/profile' }) },
  openApplications() { wx.navigateTo({ url: '../applications/applications' }) },
  openMyWorkstation() { wx.navigateTo({ url: this.data.workstation ? '../myworkstation/myworkstation' : '../workstations/workstations' }) },
  openReturns() { wx.navigateTo({ url: '../returns/returns' }) },
  openQuota() { wx.navigateTo({ url: '../quota/quota' }) },
  openMentorReview(e) { const tab=e && e.currentTarget && e.currentTarget.dataset.tab; wx.navigateTo({ url: '../mentor/mentor' + (tab === 'review' ? '?tab=review' : '') }) },
  openInspection() { wx.navigateTo({ url: '../inspection/inspection' }) },
  logout() {
    const clear = () => { wx.removeStorageSync('token'); wx.removeStorageSync('userInfo'); wx.reLaunch({ url: '../login/login' }) }
    request.post('/logout').catch(() => {}).finally(clear)
  }
})
