const request = require('../../utils/request')
const STATUS = { AVAILABLE: '空闲', IN_USE: '使用中', PENDING_RETURN: '待回收', DISABLED: '停用' }
const same = (a, b) => String(a) === String(b)

Page({
  data: {
    loading: false, error: '', rooms: [], all: [], groups: [], selectedRoom: null,
    summary: { total: 0, available: 0, inUse: 0 }, isStudent: false,
    statusBarHeight: 20, navigationHeight: 48, capsuleInset: 104,
    search: '', searchOpen: false, filtersOpen: false, activeFilterCount: 0,
    roomOptions: [{ label: '全部房间', value: '' }], roomIndex: 0, roomLabel: '全部房间',
    typeOptions: [{ label: '全部类型', value: '' }, { label: '固定工位', value: 'FIXED' }, { label: '流动工位', value: 'MOBILE' }], typeIndex: 0, typeLabel: '全部类型',
    statusOptions: [{ label: '全部状态', value: '' }, { label: '空闲', value: 'AVAILABLE' }, { label: '使用中', value: 'IN_USE' }, { label: '待回收', value: 'PENDING_RETURN' }, { label: '停用', value: 'DISABLED' }], statusIndex: 0, statusLabel: '全部状态',
    draftRoomIndex: 0, draftTypeIndex: 0, draftStatusIndex: 0
  },
  onLoad() {
    try {
      const info = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
      const capsule = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null
      const statusBarHeight = info.statusBarHeight || 20
      this.setData({ statusBarHeight, navigationHeight: capsule ? Math.max(48, capsule.height + Math.max(0, capsule.top - statusBarHeight) * 2) : 48,
        capsuleInset: capsule && capsule.left > 0 ? Math.max(96, info.windowWidth - capsule.left + 8) : 104 })
    } catch (e) { /* Default dimensions cover older developer-tool runtimes. */ }
  },
  onShow() {
    const roles = (wx.getStorageSync('userInfo') || {}).roles || []
    this.setData({ isStudent: roles.includes('common') })
    return this.load()
  },
  onPullDownRefresh() { return this.load().finally(() => wx.stopPullDownRefresh()) },
  load() {
    if (this._loadPromise) return this._loadPromise
    this.setData({ loading: true, error: '' })
    const previousRoom = this.data.roomOptions[this.data.roomIndex] || this.data.roomOptions[0]
    this._loadPromise = Promise.all([request.get('/gw/room/list'), request.get('/gw/workstation/list')]).then(([roomRes, stationRes]) => {
      const rooms = roomRes.data || []
      const roomOptions = [{ label: '全部房间', value: '' }].concat(rooms.map(room => ({ label: room.roomName, value: room.roomId })))
      const roomIndex = Math.max(0, roomOptions.findIndex(room => same(room.value, previousRoom.value)))
      const all = (stationRes.data || []).map(station => ({ ...station, statusText: STATUS[station.status] || station.status, statusClass: String(station.status || '').toLowerCase() }))
      this.setData({ rooms, roomOptions, roomIndex, roomLabel: roomOptions[roomIndex].label, all })
      this.applyFilters()
    }).catch(() => this.setData({ error: '空间信息加载失败，请重试或下拉刷新。' }))
      .finally(() => { this._loadPromise = null; this.setData({ loading: false }) })
    return this._loadPromise
  },
  toggleSearch() {
    const searchOpen = !this.data.searchOpen
    this.setData({ searchOpen, search: searchOpen ? this.data.search : '' })
    if (!searchOpen) this.applyFilters()
  },
  onSearch(e) { this.setData({ search: e.detail.value }); this.applyFilters() },
  clearSearch() { this.setData({ search: '' }); this.applyFilters() },
  openFilters() { this.setData({ filtersOpen: true, draftRoomIndex: this.data.roomIndex, draftTypeIndex: this.data.typeIndex, draftStatusIndex: this.data.statusIndex }) },
  closeFilters() { this.setData({ filtersOpen: false }) },
  onFilter(e) {
    const field = e.currentTarget.dataset.field
    const options = { draftRoomIndex: 'roomOptions', draftTypeIndex: 'typeOptions', draftStatusIndex: 'statusOptions' }
    const index = Number(e.detail.value)
    if (!options[field] || !Number.isInteger(index) || !this.data[options[field]][index]) return
    this.setData({ [field]: index })
  },
  resetDraftFilters() { this.setData({ draftRoomIndex: 0, draftTypeIndex: 0, draftStatusIndex: 0 }) },
  applyDraftFilters() {
    const d = this.data
    const roomIndex = d.roomOptions[d.draftRoomIndex] ? d.draftRoomIndex : 0
    this.setData({ roomIndex, roomLabel: d.roomOptions[roomIndex].label,
      typeIndex: d.draftTypeIndex, typeLabel: d.typeOptions[d.draftTypeIndex].label,
      statusIndex: d.draftStatusIndex, statusLabel: d.statusOptions[d.draftStatusIndex].label, filtersOpen: false })
    this.applyFilters()
  },
  resetAll() {
    this.setData({ search: '', roomIndex: 0, roomLabel: '全部房间', typeIndex: 0, typeLabel: '全部类型', statusIndex: 0, statusLabel: '全部状态' })
    this.applyFilters()
  },
  applyFilters() {
    const d = this.data, roomId = (d.roomOptions[d.roomIndex] || d.roomOptions[0]).value
    const type = d.typeOptions[d.typeIndex].value, status = d.statusOptions[d.statusIndex].value
    const query = d.search.trim().toLowerCase()
    const rooms = d.rooms.slice()
    const roomIds = new Set(rooms.map(room => String(room.roomId)))
    d.all.forEach(station => {
      if (!roomIds.has(String(station.roomId))) { rooms.push({ roomId: station.roomId, roomName: station.roomName, roomType: station.roomType, status: station.roomStatus }); roomIds.add(String(station.roomId)) }
    })
    const groups = rooms.flatMap(room => {
      if (roomId !== '' && !same(room.roomId, roomId)) return []
      const roomType = room.roomType === 'STUDIO' ? 'FIXED' : 'MOBILE'
      if (type && type !== roomType) return []
      const roomMatches = [room.roomName, room.location, room.mentorName].filter(Boolean).join(' ').toLowerCase().includes(query)
      const original = d.all.filter(station => same(station.roomId, room.roomId))
      const stations = original.filter(station => (!type || station.workstationType === type) && (!status || station.status === status)
        && (!query || roomMatches || String(station.workstationNo || '').toLowerCase().includes(query)))
      if (!stations.length && (original.length || status || (query && !roomMatches))) return []
      const availableCount = stations.filter(station => station.status === 'AVAILABLE' && (!room.status || room.status === 'NORMAL')).length
      return [{ ...room, stations, availableCount, stationCount: stations.length,
        roomTypeText: room.roomType === 'STUDIO' ? '工作室' : '流动工位房间',
        roomStatusText: room.status === 'DISABLED' ? '已停用' : room.status === 'MAINTENANCE' ? '维修中' : '',
        inUseCount: stations.filter(station => ['IN_USE', 'PENDING_RETURN'].includes(station.status)).length }]
    })
    const visibleStations = groups.flatMap(group => group.stations)
    const selectedRoom = d.selectedRoom ? groups.find(group => same(group.roomId, d.selectedRoom.roomId)) || null : null
    this.setData({ groups, selectedRoom, activeFilterCount: [roomId !== '', !!type, !!status].filter(Boolean).length,
      summary: { total: visibleStations.length, available: groups.reduce((sum, room) => sum + room.availableCount, 0), inUse: visibleStations.filter(station => station.status === 'IN_USE').length } })
  },
  openRoom(e) {
    const room = this.data.groups.find(group => same(group.roomId, e.currentTarget.dataset.id))
    if (!room) return
    const selectedRoom = this.data.selectedRoom && same(this.data.selectedRoom.roomId, room.roomId) ? null : room
    this.setData({ selectedRoom }, () => {
      if (selectedRoom && wx.pageScrollTo) wx.pageScrollTo({ selector: '#room-detail', offsetTop: -(this.data.statusBarHeight + this.data.navigationHeight + 12), duration: 200 })
    })
  },
  closeRoom() { this.setData({ selectedRoom: null }) },
  openStation(e) {
    const room = this.data.selectedRoom
    const station = room && room.stations.find(item => same(item.workstationId, e.currentTarget.dataset.id))
    if (!station) return
    const lines = ['类型：' + (station.workstationType === 'FIXED' ? '固定工位' : '流动工位'), '状态：' + station.statusText]
    if (station.mentorName) lines.push('导师：' + station.mentorName)
    if (station.studentName) lines.push('使用人：' + station.studentName)
    if (station.layoutRow && station.layoutCol) lines.push('位置：' + station.layoutRow + '行 / ' + station.layoutCol + '列')
    wx.showModal({ title: room.roomName + ' · ' + station.workstationNo, content: lines.join('\n'), showCancel: false, confirmColor: '#0f62fe' })
  },
  stopTouch() {},
  openApplications() { wx.navigateTo({ url: '/pages/applications/applications' }) }
})
