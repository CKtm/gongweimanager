// app.js
App({
  onLaunch() {
    this.globalData.token = wx.getStorageSync('token') || ''
    this.globalData.userInfo = wx.getStorageSync('userInfo') || null
  },
  globalData: { token: '', userInfo: null }
})
