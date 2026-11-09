const request = require('../../utils/request')

Page({
  data: {
    username: '', password: '', loading: false, forceChange: false, resetToken: '', pwdChrtype: '0',
    newPassword: '', confirmPassword: '', resetLoading: false
  },
  onShow() {
    if (!this.data.loading && !this.data.forceChange && wx.getStorageSync('token') && wx.getStorageSync('userInfo')) {
      wx.reLaunch({ url: '/pages/index/index' })
    }
  },
  onInput(e) { this.setData({ [e.currentTarget.dataset.field]: e.detail.value }) },
  submit() {
    if (!this.data.username || !this.data.password) return wx.showToast({ title: '请输入账号和密码', icon: 'none' })
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    getApp().globalData.token = ''
    getApp().globalData.userInfo = null
    this.setData({ loading: true })
    request.post('/login', { username: this.data.username, password: this.data.password, code: '', uuid: '' }).then(res => {
      wx.setStorageSync('token', res.token)
      getApp().globalData.token = res.token
      return request.get('/getInfo')
    }).then(res => {
      const userInfo = Object.assign({}, res.user, { roles: res.roles || [], permissions: res.permissions || [] })
      wx.setStorageSync('userInfo', userInfo)
      getApp().globalData.userInfo = userInfo
      if (res.isDefaultModifyPwd) {
        wx.redirectTo({ url: '/pages/password/password' })
      } else {
        wx.reLaunch({ url: '../index/index' })
      }
    }).catch(error => {
      if (error && error.code === 602 && error.forcePasswordChange && error.resetToken) {
        this.setData({ forceChange: true, resetToken: error.resetToken, pwdChrtype: String(error.pwdChrtype || '0'), newPassword: '', confirmPassword: '' })
      }
    }).finally(() => this.setData({ loading: false }))
  },
  resetPassword() {
    const message = validatePassword(this.data.newPassword, this.data.pwdChrtype)
    if (message) return wx.showToast({ title: message, icon: 'none', duration: 2600 })
    if (this.data.newPassword === this.data.password) return wx.showToast({ title: '新密码不能与初始密码相同', icon: 'none' })
    if (this.data.newPassword !== this.data.confirmPassword) return wx.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    this.setData({ resetLoading: true })
    request.post('/forceResetPwd', { resetToken: this.data.resetToken, newPassword: this.data.newPassword }).then(() => {
      wx.showToast({ title: '修改成功，请重新登录', icon: 'success' })
      this.setData({ forceChange: false, password: '', resetToken: '', newPassword: '', confirmPassword: '' })
    }).catch(() => {}).finally(() => this.setData({ resetLoading: false }))
  },
  backToLogin() {
    this.setData({ forceChange: false, password: '', resetToken: '', newPassword: '', confirmPassword: '' })
  }
})

function validatePassword(password, type) {
  if (!password || password.length < 6 || password.length > 20) return '密码长度必须为6至20位'
  const rules = {
    '0': { pattern: /^[^<>"'|\\]+$/, message: '密码包含非法字符' },
    '1': { pattern: /^[0-9]+$/, message: '密码只能包含数字' },
    '2': { pattern: /^[A-Za-z]+$/, message: '密码只能包含英文字母' },
    '3': { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]+$/, message: '密码必须同时包含字母和数字' },
    '4': { pattern: /^(?=.*[A-Za-z])(?=.*\d)(?=.*[~!@#$%^&*()\-=_+])[A-Za-z\d~!@#$%^&*()\-=_+]+$/, message: '密码必须包含字母、数字和特殊字符' }
  }
  const rule = rules[type] || rules['0']
  return rule.pattern.test(password) ? '' : rule.message
}
