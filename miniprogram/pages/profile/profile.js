const request = require('../../utils/request')

Page({
  data: { profile: {}, userInfo: {}, roleText: '', avatar: '用' },
  onShow() {
    const userInfo = wx.getStorageSync('userInfo') || {}
    const roles = userInfo.roles || []
    const roleNames = { common: '研究生', mentor: '研究生导师', inspector: '卫生检查员', admin: '学院管理员', college_admin: '学院管理员' }
    this.setData({ userInfo, roleText: roles.map(role => roleNames[role] || role).join(' / '), avatar: String(userInfo.nickName || userInfo.userName || '用').charAt(0) })
    request.get('/gw/profile/me').then(res => { const profile = res.data || {}; this.setData({ profile, avatar: String(profile.nickName || profile.userName || this.data.avatar).charAt(0) }) }).catch(() => {})
  },
  logout() {
    wx.showModal({title:'退出登录',content:'确定退出当前账号吗？',success:res=>{
      if(!res.confirm)return
      request.post('/logout').catch(()=>{}).finally(()=>{wx.removeStorageSync('token');wx.removeStorageSync('userInfo');wx.reLaunch({url:'/pages/login/login'})})
    }})
  },
  editAccount() { wx.navigateTo({url:'/pages/account/account'}) },
  changePassword() { wx.navigateTo({ url: '../password/password' }) }
})
