const request = require('../../utils/request')
const STATUS = { AVAILABLE: '空闲', IN_USE: '使用中', PENDING_RETURN: '待回收', DISABLED: '停用' }
Page({
  data: {
    search: '', loading: false, all: [], rooms: [], groups: [], summary: { total: 0, available: 0, inUse: 0 }, isStudent: false,
    roomOptions: [{ label: '全部房间', value: '' }], roomIndex: 0, roomLabel: '全部房间',
    typeOptions: [{ label: '全部类型', value: '' }, { label: '固定工位', value: 'FIXED' }, { label: '流动工位', value: 'MOBILE' }], typeIndex: 0, typeLabel: '全部类型',
    statusOptions: [{ label: '全部状态', value: '' }, { label: '空闲', value: 'AVAILABLE' }, { label: '使用中', value: 'IN_USE' }, { label: '待回收', value: 'PENDING_RETURN' }, { label: '停用', value: 'DISABLED' }], statusIndex: 0, statusLabel: '全部状态'
  },
  onShow() {
    const roles = (wx.getStorageSync('userInfo') || {}).roles || []
    this.setData({ isStudent: roles.indexOf('common') >= 0 })
    this.load()
  },
  load() {
    this.setData({ loading: true })
    Promise.all([request.get('/gw/room/list'), request.get('/gw/workstation/list')]).then(([rooms, stations]) => {
      const roomRows = rooms.data || []
      const roomOptions = [{ label: '全部房间', value: '' }].concat(roomRows.map(item => ({ label: item.roomName, value: item.roomId })))
      const all = (stations.data || []).map(item => Object.assign({}, item, { statusText: STATUS[item.status] || item.status, statusClass: String(item.status || '').toLowerCase() }))
      this.setData({ roomOptions, rooms: roomRows, all, roomIndex: 0, roomLabel: '全部房间' })
      this.applyFilters()
    }).finally(() => this.setData({ loading: false }))
  },
  onFilter(e) {
    const field = e.currentTarget.dataset.field
    const index = Number(e.detail.value)
    const optionField = field.replace('Index', 'Options')
    const labelField = field.replace('Index', 'Label')
    this.setData({ [field]: index, [labelField]: this.data[optionField][index].label })
    this.applyFilters()
  },
  onSearch(e) { this.setData({search:e.detail.value}); this.applyFilters() },
  clearSearch() { this.setData({search:''}); this.applyFilters() },
  applyFilters() {
    const roomId = this.data.roomOptions[this.data.roomIndex].value
    const type = this.data.typeOptions[this.data.typeIndex].value
    const status = this.data.statusOptions[this.data.statusIndex].value
    const search=this.data.search.trim().toLowerCase()
    const rows = this.data.all.filter(item => (!roomId || item.roomId === roomId) && (!type || item.workstationType === type) && (!status || item.status === status) && (!search || (String(item.roomName||'')+' '+String(item.workstationNo||'')).toLowerCase().includes(search)))
    const grouped = {}
    const roomMap = {}; this.data.rooms.forEach(room => { roomMap[room.roomId] = room })
    rows.forEach(item => { const room = roomMap[item.roomId] || {}; if (!grouped[item.roomId]) grouped[item.roomId] = { roomId: item.roomId, roomName: item.roomName, roomType: room.roomType || item.roomType, location: room.location || '', mentorName: room.mentorName || '', stations: [] }; grouped[item.roomId].stations.push(item) })
    const groups = Object.keys(grouped).map(key => grouped[key])
    this.setData({ groups, summary: { total: rows.length, available: rows.filter(x => x.status === 'AVAILABLE').length, inUse: rows.filter(x => x.status === 'IN_USE').length } })
  },
  openApplications() { wx.navigateTo({ url: '../applications/applications' }) }
})
