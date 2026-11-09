const request = require('../../utils/request')
const STATUS = { PENDING: '待管理员审核', APPROVED: '已通过', REJECTED: '已驳回', RETURNED: '已解除' }

Page({
  data: { rooms: [], availableRooms: [], ownedRooms: [], roomIndex: 0, selectedRoomLabel: '暂无可申请工作室', list: [], reason: '', submitting: false },
  onShow() { this.load() },
  load() {
    const userInfo = wx.getStorageSync('userInfo') || {}
    Promise.all([request.get('/gw/room/list', { roomType: 'STUDIO' }), request.get('/gw/studio/list')]).then(([roomRes, appRes]) => {
      const rooms = roomRes.data || []
      const availableRooms = rooms.filter(item => item.status === 'NORMAL' && !item.mentorId).map(item => Object.assign({}, item, { label: item.roomName + ' · ' + (item.location || '位置未填写') + ' · ' + Number(item.workstationCount || 0) + '个工位' }))
      const ownedRooms = rooms.filter(item => Number(item.mentorId) === Number(userInfo.userId))
      const list = (appRes.data || []).map(item => Object.assign({}, item, { statusText: STATUS[item.status] || item.status }))
      const roomIndex = Math.min(this.data.roomIndex, Math.max(0, availableRooms.length - 1))
      this.setData({ rooms, availableRooms, ownedRooms, list, roomIndex, selectedRoomLabel: availableRooms[roomIndex] ? availableRooms[roomIndex].label : '暂无可申请工作室' })
    })
  },
  onRoomChange(e) { const roomIndex = Number(e.detail.value); const room = this.data.availableRooms[roomIndex]; this.setData({ roomIndex, selectedRoomLabel: room ? room.label : '暂无可申请工作室' }) },
  onInput(e) { this.setData({ reason: e.detail.value }) },
  submit() {
    const room = this.data.availableRooms[this.data.roomIndex]
    const reason = (this.data.reason || '').trim()
    if (!room) return wx.showToast({ title: '暂无可申请工作室', icon: 'none' })
    if (!reason) return wx.showToast({ title: '请填写申请理由', icon: 'none' })
    this.setData({ submitting: true })
    request.post('/gw/studio', { roomId: room.roomId, reason }).then(() => {
      wx.showToast({ title: '申请已提交' })
      this.setData({ reason: '' })
      this.load()
    }).finally(() => this.setData({ submitting: false }))
  }
})
