const request = require('../../utils/request')
const STATUS = { PENDING: '待管理员审核', APPROVED: '已通过', REJECTED: '已驳回', RETURNED: '已解除', CANCELLED: '已取消' }

Page({
  onPullDownRefresh() {
    return Promise.resolve().then(() => this.load()).catch(() => {}).finally(() => wx.stopPullDownRefresh())
  },

  data: { ownedRooms: [], list: [] },
  onShow() { this.load() },
  load() {
    const userInfo = wx.getStorageSync('userInfo') || {}
    return Promise.all([request.get('/gw/room/list', { roomType: 'STUDIO' }), request.get('/gw/studio/list'), request.get('/gw/workstation/list', { workstationType: 'FIXED' })]).then(([roomRes, appRes, stationRes]) => {
      const rooms = roomRes.data || []
      const ownedStations = (stationRes.data || []).filter(s => Number(s.mentorId) === Number(userInfo.userId))
      const ownedRooms = rooms.filter(room => ownedStations.some(s => Number(s.roomId) === Number(room.roomId))).map(room => {const seats=ownedStations.filter(s=>Number(s.roomId)===Number(room.roomId));return {...room,workstationCount:seats.length,availableCount:seats.filter(s=>s.status==='AVAILABLE'&&s.roomStatus==='NORMAL').length,inUseCount:seats.filter(s=>['IN_USE','PENDING_RETURN'].includes(s.status)).length}})
      const list = (appRes.data || []).map(item => Object.assign({}, item, { statusText: STATUS[item.status] || item.status }))
      this.setData({ ownedRooms, list })
    })
  }
})
