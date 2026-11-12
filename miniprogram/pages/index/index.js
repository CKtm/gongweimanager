const request = require('../../utils/request')
const { baseUrl } = require('../../utils/config')

const CATEGORY_LABELS = { all: '全部', space: '工位', review: '审批', inspection: '检查', account: '账号' }
const SERVICE_COVERS = { space: 'desk', review: 'review', inspection: 'inspection', account: 'desk' }
const SERVICE_DEFINITIONS = [
  { key: 'spaces', title: '空间总览', description: '房间与工位实时状态', category: 'space', handler: 'openWorkstations' },
  { key: 'student-apply', title: '流动工位申请', description: '申请资格与审批进度', category: 'review', handler: 'openApplications', role: 'isStudent' },
  { key: 'student-return', title: '退还与回收', description: '清理上传与进度跟踪', category: 'space', handler: 'openReturns', role: 'isStudent' },
  { key: 'mentor-seats', title: '我的固定工位', description: '管理员划分，导师分配', category: 'space', handler: 'openQuota', role: 'isMentor' },
  { key: 'mentor-team', title: '团队管理', description: '学生、工位与审批', category: 'review', handler: 'openMentorReview', role: 'isMentor' },
  { key: 'inspector-tasks', title: '现场检查', description: '座位评分与检查记录', category: 'inspection', handler: 'openInspection', role: 'isInspector' },
  { key: 'admin-appeals', title: '学生申诉终审', description: '导师初审通过后的申诉', category: 'review', handler: 'openAppeals', role: 'isAdmin' },
  { key: 'admin-reviews', title: '审批中心', description: '流动申请与退还审批', category: 'review', handler: 'openAdmin', tab: 'reviews', role: 'isAdmin' },
  { key: 'admin-seats', title: '座位管理', description: '分配、调整与主动收回', category: 'space', handler: 'openAdmin', tab: 'seats', role: 'isAdmin' },
  { key: 'admin-inspections', title: '卫生检查管理', description: '下发任务、核实与归档', category: 'inspection', handler: 'openAdmin', tab: 'inspections', role: 'isAdmin' },
  { key: 'admin-settings', title: '系统与基础配置', description: '账号、权限、档案与房间', category: 'account', handler: 'openAdmin', tab: 'settings', role: 'isAdmin' },
  { key: 'profile', title: '个人中心', description: '身份信息与账号安全', category: 'account', handler: 'openProfile' }
]

const homeMethods = {
  refreshDashboard() {
    if (!wx.getStorageSync('token')) return Promise.resolve()
    if (this._refreshPromise) return this._refreshPromise
    this.setData({ loading: true, error: '' })
    this._refreshPromise = Promise.all([
      this.data.isStudent ? this.loadStudent() : Promise.resolve(),
      this.data.isMentor ? this.loadMentor() : Promise.resolve(),
      this.data.isInspector ? this.loadInspector() : Promise.resolve(),
      this.data.isAdmin ? this.loadAdmin() : Promise.resolve()
    ]).finally(() => {
      this.rebuildDashboard()
      this.setData({ loading: false })
      this._refreshPromise = null
    })
    return this._refreshPromise
  },
  reportLoadFailure() { this.setData({ error: '部分信息未更新，请下拉刷新或重试。' }) },
  onAvatarError() { this.setData({ avatarUrl: '' }) },
  rebuildDashboard() {
    const d = this.data
    const services = SERVICE_DEFINITIONS.filter(item => !item.role || d[item.role]).map(item => ({ ...item, cover: '/assets/home/covers/' + (item.key === 'spaces' ? 'space' : SERVICE_COVERS[item.category] || 'desk') + '.jpg', categoryLabel: CATEGORY_LABELS[item.category] }))
    const categories = Object.entries(CATEGORY_LABELS).filter(([key]) => key === 'all' || services.some(item => item.category === key)).map(([key, label]) => ({ key, label }))
    const activeCategory = categories.some(item => item.key === d.activeCategory) ? d.activeCategory : 'all'
    const nextCards = [], pendingItems = []
    if (d.isStudent) {
      const station = d.workstation
      nextCards.push({ key: 'student-workspace', title: station ? station.roomName + ' · ' + station.workstationNo : '尚未分配工位',
        description: station ? (station.workstationType === 'FIXED' ? '固定工位' : '流动工位') + (station.termCode ? ' · ' + station.termCode.replace('~', ' 至 ') : '') : '浏览空间，查看流动工位申请资格', buttonText: station ? '查看' : '看空间', handler: 'openMyWorkstation' })
      if (d.applicationCount) pendingItems.push({ key: 'student-applications', count: d.applicationCount, title: d.applicationCount + ' 项流动申请进行中', description: '查看导师和管理员审批进度', handler: 'openApplications' })
    }
    if (d.isMentor) {
      const count = d.mentorApplicationCount + d.mentorReturnCount
      const item = { key: 'mentor-pending', count, title: '导师工作台', description: '流动申请 ' + d.mentorApplicationCount + ' · 退还确认 ' + d.mentorReturnCount, buttonText: count ? '处理' : '查看', handler: 'openMentorReview', tab: count ? 'review' : '' }
      nextCards.push(item)
      if (count) pendingItems.push({ ...item, title: count + ' 项待导师审批' })
    }
    if (d.isInspector) {
      const item = { key: 'inspector-pending', count: d.inspectionTaskCount, title: '卫生检查任务', description: d.inspectionTaskCount ? d.inspectionTaskCount + ' 项待执行 · 进入座位图完成评分' : '暂无待执行任务，可查看检查记录', buttonText: '查看', handler: 'openInspection' }
      nextCards.push(item)
      if (item.count) pendingItems.push(item)
    }
    if (d.isAdmin) {
      const item = { key: 'admin-pending', count: d.adminPending + d.adminWaiting + d.adminInspection + d.adminAppeals, title: '管理员工作台', description: '待审批 ' + d.adminPending + ' · 待分配 ' + d.adminWaiting + ' · 待核实 ' + d.adminInspection + ' · 申诉终审 ' + d.adminAppeals, buttonText: '进入', handler: 'openAdmin', tab: 'reviews' }
      nextCards.push(item)
      if (item.count) pendingItems.push(item)
    }
    if (!nextCards.length) nextCards.push({ key: 'account', title: '个人资料', description: '查看身份信息与账号安全', buttonText: '查看', handler: 'openProfile' })
    this.setData({ services, categories, activeCategory, nextCards, pendingItems, pendingCount: pendingItems.reduce((sum, item) => sum + item.count, 0) })
    this.filterServices()
  },
  filterServices() {
    const query = this.data.search.trim().toLowerCase()
    const visibleServices = this.data.services.filter(item => (this.data.activeCategory === 'all' || item.category === this.data.activeCategory)
      && (!query || [item.title, item.description, item.categoryLabel].join(' ').toLowerCase().includes(query)))
    this.setData({ visibleServices })
  },
  onSearch(e) { this.setData({ search: e.detail.value }); this.filterServices() },
  clearSearch() { this.setData({ search: '' }); this.filterServices() },
  onCategory(e) {
    const key = e.currentTarget.dataset.key
    if (!this.data.categories.some(item => item.key === key)) return
    this.setData({ activeCategory: key }); this.filterServices()
  },
  resetServiceFilters() { this.setData({ activeCategory: 'all', search: '' }); this.filterServices() },
  openAllServices() { this.setData({ allServicesOpen: true, notificationsOpen: false }) },
  openNotifications() { this.setData({ notificationsOpen: true, allServicesOpen: false }) },
  closeSheet() { this.setData({ allServicesOpen: false, notificationsOpen: false }) },
  stopTouch() {},
  navigateItem(item) {
    const handlers = ['openWorkstations', 'openApplications', 'openReturns', 'openQuota', 'openMentorReview', 'openInspection', 'openAdmin', 'openAppeals', 'openProfile', 'openMyWorkstation']
    if (!item || !handlers.includes(item.handler)) return
    this.closeSheet()
    this[item.handler]({ currentTarget: { dataset: { tab: item.tab || '' } } })
  },
  openService(e) { this.navigateItem(this.data.services.find(item => item.key === e.currentTarget.dataset.key)) },
  openNextCard(e) { this.navigateItem(this.data.nextCards.find(item => item.key === e.currentTarget.dataset.key)) },
  openPendingItem(e) { this.navigateItem(this.data.pendingItems.find(item => item.key === e.currentTarget.dataset.key)) }
}


Page({
  ...homeMethods,
  onPullDownRefresh() {
    if (!wx.getStorageSync('token')) { wx.stopPullDownRefresh(); return Promise.resolve() }
    return this.refreshDashboard().finally(() => wx.stopPullDownRefresh())
  },

  data: {
    userInfo: null, loading: false, error: '', greeting: '欢迎回来', avatarUrl: '', avatarInitial: '用',
    services: [], visibleServices: [], categories: [], activeCategory: 'all', search: '',
    nextCards: [], pendingItems: [], pendingCount: 0, allServicesOpen: false, notificationsOpen: false,
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
    adminAppeals: 0, adminPending: 0, adminWaiting: 0, adminInspection: 0
  },
  onShow() {
    const token = wx.getStorageSync('token')
    if (!token) return wx.redirectTo({ url: '../login/login' })
    const userInfo = wx.getStorageSync('userInfo') || {}
    const roles = (userInfo && userInfo.roles) || []
    const hour = new Date().getHours()
    const greeting = hour < 6 ? '夜深了' : hour < 12 ? '上午好' : hour < 18 ? '下午好' : '晚上好'
    const avatar = userInfo.avatar || ''
    const avatarUrl = avatar ? (/^https?:\/\//.test(avatar) ? avatar : baseUrl + avatar) : ''
    const avatarInitial = Array.from(String(userInfo.nickName || userInfo.userName || '用'))[0]
    const isStudent = roles.indexOf('common') >= 0
    const isMentor = roles.indexOf('mentor') >= 0
    const isInspector = roles.indexOf('inspector') >= 0
    const isAdmin = roles.indexOf('admin') >= 0 || roles.indexOf('college_admin') >= 0
    const labels = []
    if (isStudent) labels.push('研究生')
    if (isMentor) labels.push('导师')
    if (isInspector) labels.push('卫生检查员')
    if (isAdmin) labels.push('学院管理员')
    this.setData({ greeting, avatarUrl, avatarInitial, userInfo, roleText: labels.join(' / ') || '普通用户', isStudent, isMentor, isInspector, isAdmin, workstation: null, adminAppeals: 0, adminPending: 0, adminWaiting: 0, adminInspection: 0 })
    this.rebuildDashboard()
    return this.refreshDashboard()
  },
  loadStudent() {
    return Promise.all([
      request.get('/gw/assignment/my').then(res => this.setData({ workstation: (res.data || [])[0] || null })),
      request.get('/gw/application/list').then(res => {
        const rows = res.data || []
        this.setData({ applicationCount: rows.filter(item => ['PENDING_MENTOR', 'PENDING_ADMIN', 'WAIT_ALLOC'].indexOf(item.status) >= 0).length })
      })
    ]).catch(() => this.reportLoadFailure())
  },
  loadMentor() {
    return Promise.all([request.get('/gw/application/list', { status: 'PENDING_MENTOR' }), request.get('/gw/return/list', { status: 'PENDING_MENTOR' })]).then(([apps, returns]) => {
      this.setData({ mentorApplicationCount: (apps.data || []).length, mentorReturnCount: (returns.data || []).length })
    }).catch(() => this.reportLoadFailure())
  },
  loadInspector() {
    return request.get('/gw/inspection/task/list').then(res => {
      const rows = res.data || []
      this.setData({ inspectionTaskCount: rows.filter(item => ['PENDING', 'IN_PROGRESS'].indexOf(item.status) >= 0).length })
    }).catch(() => this.reportLoadFailure())
  },
  loadAdmin() {
    const {can}=require('../../utils/admin'),user=wx.getStorageSync('userInfo')||{}
    const read=(url,permission)=>can(user,permission)?request.get(url).then(r=>r.data||r.rows||[]):Promise.resolve([])
    return Promise.all([read('/gw/application/list','gw:application:list'),read('/gw/return/list','gw:return:query'),read('/gw/inspection/record/list','gw:inspection:record:list'),request.get('/gw/appeals/pending-count')]).then(([apps,returns,records,appeals])=>this.setData({adminAppeals:Number(appeals.data)||0,adminPending:apps.filter(x=>x.status==='PENDING_ADMIN').length+returns.filter(x=>['PENDING_ADMIN','WAIT_VERIFY'].includes(x.status)).length,adminWaiting:apps.filter(x=>x.status==='WAIT_ALLOC').length,adminInspection:records.filter(x=>x.status==='SUBMITTED'&&x.taskStatus==='SUBMITTED').length})).catch(()=>this.reportLoadFailure())
  },
  openAppeals() { wx.navigateTo({url:'/pages/student-records/student-records?tab=appeals'}) },
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
