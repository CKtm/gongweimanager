const request = require('../../utils/request')
Page({ data: { list: [], loading: false }, onShow() { this.load() }, load() { this.setData({ loading: true }); request.get('/gw/assignment/my').then(res => this.setData({ list: res.data || [] })).finally(() => this.setData({ loading: false })) } })
