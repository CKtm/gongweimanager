const request = require('../../utils/request')
Page({
  onPullDownRefresh() {
    return Promise.resolve().then(() => this.load()).catch(() => {}).finally(() => wx.stopPullDownRefresh())
  },
 data: { list: [], loading: false }, onShow() { this.load() }, load() { this.setData({ loading: true }); return request.get('/gw/assignment/my').then(res => this.setData({ list: res.data || [] })).finally(() => this.setData({ loading: false })) } })
