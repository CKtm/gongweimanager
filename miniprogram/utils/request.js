const config = require('./config')

function request(method, url, data) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: config.baseUrl + url,
      method,
      data,
      header: { 'content-type': 'application/json', Authorization: wx.getStorageSync('token') ? 'Bearer ' + wx.getStorageSync('token') : '' },
      success(res) {
        const body = res.data || {}
        if (body.code === 200) return resolve(body)
        if (body.code === 602) return reject(body)
        if (body.code === 401) { wx.removeStorageSync('token'); wx.redirectTo({ url: '/pages/login/login' }) }
        if (body.code === 403 && String(body.msg || '').indexOf('初始密码') >= 0) wx.redirectTo({ url: '/pages/password/password' })
        wx.showToast({ title: body.msg || '请求失败', icon: 'none' })
        reject(body)
      },
      fail(err) { wx.showToast({ title: '网络连接失败，请稍后重试', icon: 'none' }); reject(err) }
    })
  })
}

function upload(url, filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: config.baseUrl + url,
      filePath,
      name: 'file',
      header: { Authorization: wx.getStorageSync('token') ? 'Bearer ' + wx.getStorageSync('token') : '' },
      success(res) {
        let body = {}
        try { body = JSON.parse(res.data || '{}') } catch (e) { return reject(e) }
        if (body.code === 200) return resolve(body)
        wx.showToast({ title: body.msg || '上传失败', icon: 'none' }); reject(body)
      },
      fail(err) { wx.showToast({ title: '照片上传失败，请检查网络', icon: 'none' }); reject(err) }
    })
  })
}

module.exports = { get: (url, data) => request('GET', url, data), post: (url, data) => request('POST', url, data), put: (url, data) => request('PUT', url, data), upload }
