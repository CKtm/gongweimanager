const request = require('../../utils/request')

Page({
  data: { oldPassword: '', newPassword: '', confirmPassword: '', saving: false },
  onInput(e) { this.setData({ [e.currentTarget.dataset.field]: e.detail.value }) },
  submit() {
    const { oldPassword, newPassword, confirmPassword } = this.data
    if (!oldPassword || !newPassword) return wx.showToast({ title: '请填写密码', icon: 'none' })
    if (newPassword.length < 5 || newPassword.length > 20) return wx.showToast({ title: '新密码长度为5-20位', icon: 'none' })
    if (newPassword !== confirmPassword) return wx.showToast({ title: '两次密码不一致', icon: 'none' })
    this.setData({ saving: true })
    request.put('/system/user/profile/updatePwd', { oldPassword, newPassword }).then(() => {
      wx.showToast({ title: '修改成功' })
      setTimeout(() => wx.reLaunch({ url: '/pages/index/index' }), 600)
    }).finally(() => this.setData({ saving: false }))
  }
})
