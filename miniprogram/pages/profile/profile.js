const request = require('../../utils/request')
const { baseUrl } = require('../../utils/config')
const ROLE_NAMES = { common: '研究生', mentor: '研究生导师', inspector: '卫生检查员', admin: '学院管理员', college_admin: '学院管理员' }

Page({
  data: {
    profile: {}, userInfo: {}, roleText: '', avatar: '用', avatarUrl: '', displayName: '',
    loading: false, error: '', isStudent: false, roleCount: 0, completed: 0, totalFields: 4, missing: 4, progress: 0,
    statusBarHeight: 20, navigationHeight: 48, capsuleInset: 104,
    loggingOut: false, confirmingLogout: false
  },
  onLoad() {
    try {
      const info = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
      const capsule = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null
      const statusBarHeight = info.statusBarHeight || 20
      this.setData({ statusBarHeight, navigationHeight: capsule ? Math.max(48, capsule.height + Math.max(0, capsule.top - statusBarHeight) * 2) : 48,
        capsuleInset: capsule && capsule.left > 0 ? Math.max(96, info.windowWidth - capsule.left + 8) : 104 })
    } catch (e) { /* Older runtimes use the default safe header. */ }
  },
  onShow() {
    if (!wx.getStorageSync('token')) return wx.redirectTo({ url: '/pages/login/login' })
    this.setData({ userInfo: wx.getStorageSync('userInfo') || {}, profile: {} })
    this.deriveProfile()
    return this.load()
  },
  onPullDownRefresh() {
    if (!wx.getStorageSync('token')) { wx.stopPullDownRefresh(); return Promise.resolve() }
    return this.load().finally(() => wx.stopPullDownRefresh())
  },
  load() {
    if (this._loadPromise) return this._loadPromise
    this.setData({ loading: true, error: '' })
    this._loadPromise = request.get('/gw/profile/me').then(res => {
      this.setData({ profile: res.data || {} })
      this.deriveProfile()
    }).catch(() => this.setData({ error: '个人资料未能更新，请下拉刷新或重试。' }))
      .finally(() => { this._loadPromise = null; this.setData({ loading: false }) })
    return this._loadPromise
  },
  deriveProfile() {
    const p = this.data.profile, u = this.data.userInfo
    const roles = Array.from(new Set(u.roles || []))
    const roleText = Array.from(new Set(roles.map(role => ROLE_NAMES[role] || role))).join(' / ')
    const isStudent = String(p.userType || '').toUpperCase() === 'STUDENT' || roles.includes('common')
    const displayName = p.nickName || u.nickName || p.userName || u.userName || '用户'
    const rawAvatar = p.avatar || u.avatar || ''
    const avatarUrl = rawAvatar ? (/^https?:\/\//.test(rawAvatar) ? rawAvatar : baseUrl + rawAvatar) : ''
    const fields = [p.userName || u.userName, p.nickName || u.nickName, p.identityNo, p.userType || (roles.length ? roleText : '')]
    if (isStudent) fields.push(p.mentorId)
    const completed = fields.filter(value => value != null && String(value).trim() !== '').length
    this.setData({ displayName, avatar: Array.from(String(displayName))[0], avatarUrl, roleText: roleText || '身份待维护',
      isStudent, roleCount: roles.length, completed, totalFields: fields.length, missing: fields.length - completed, progress: Math.round(completed / fields.length * 100) })
  },
  onAvatarError() { this.setData({ avatarUrl: '' }) },
  editAccount() { wx.navigateTo({ url: '/pages/account/account' }) },
  changePassword() { wx.navigateTo({ url: '/pages/password/password' }) },
  openSettings() {
    if (wx.pageScrollTo) wx.pageScrollTo({ selector: '#account-settings', offsetTop: -(this.data.statusBarHeight + this.data.navigationHeight + 12), duration: 200 })
  },
  openIdentity() {
    const p = this.data.profile, u = this.data.userInfo
    const lines = ['登录账号：' + (p.userName || u.userName || '暂未维护'), '学号/工号：' + (p.identityNo || '暂未维护'), '业务身份：' + this.data.roleText]
    if (this.data.isStudent) lines.push('所属导师：' + (p.mentorName || '暂未绑定'))
    lines.push('学号、工号及导师关系由管理员维护。')
    if (this.data.error) lines.unshift('资料暂未更新，请刷新后核对。')
    wx.showModal({ title: '身份与导师信息', content: lines.join('\n'), showCancel: false, confirmColor: '#0f62fe' })
  },
  openBusiness() {
    const roles = this.data.userInfo.roles || [], entries = []
    if (roles.includes('common')) entries.push({ title: '我的工位', url: '/pages/myworkstation/myworkstation' })
    if (roles.includes('mentor')) entries.push({ title: '导师工作台', url: '/pages/mentor/mentor' })
    if (roles.includes('inspector')) entries.push({ title: '卫生检查', url: '/pages/inspection/inspection' })
    if (roles.includes('admin') || roles.includes('college_admin')) entries.push({ title: '管理员工作台', url: '/pages/admin/admin' })
    if (!entries.length) return wx.showToast({ title: '请联系管理员维护业务身份', icon: 'none' })
    if (entries.length === 1) return wx.navigateTo({ url: entries[0].url })
    wx.showActionSheet({ itemList: entries.map(entry => entry.title), success: res => { const entry = entries[res.tapIndex]; if (entry) wx.navigateTo({ url: entry.url }) } })
  },
  logout() {
    if (this.data.loggingOut || this.data.confirmingLogout) return
    this.setData({ confirmingLogout: true })
    wx.showModal({ title: '退出登录', content: '确定退出当前账号吗？', confirmColor: '#0f62fe', success: res => {
      this.setData({ confirmingLogout: false })
      if (!res.confirm) return
      this.setData({ loggingOut: true })
      request.post('/logout').catch(() => {}).finally(() => {
        wx.removeStorageSync('token'); wx.removeStorageSync('userInfo')
        wx.reLaunch({ url: '/pages/login/login' })
      })
    }, fail: () => this.setData({ confirmingLogout: false }) })
  }
})
