const request = require('../../utils/request')

Page({
  onPullDownRefresh() {
    return Promise.resolve().then(() => this.load()).catch(() => {}).finally(() => wx.stopPullDownRefresh())
  },

  data: {
    activeTab: 'team', loading: false, team: [], unassignedStudents: [], applications: [], returns: [], ownedRooms: [], fixedStations: [],
    studentIndex: 0, stationIndex: 0, selectedStudentName: '暂无未分配学生', selectedStationName: '暂无空闲固定工位',
    usageStart: '', usageEnd: '',
    submitting: false, capacity: { total: 0, available: 0, inUse: 0 }, availableFixedCount: 0
  },
  onLoad(options) { if (options.tab === 'review') this.setData({activeTab:'review'}) },
  onShow() { this.load() },
  switchTab(e) { this.setData({activeTab:e.currentTarget.dataset.tab}) },
  goAssign(e) {
    const studentIndex=this.data.unassignedStudents.findIndex(s=>Number(s.userId)===Number(e.currentTarget.dataset.id))
    if(studentIndex<0)return
    const student=this.data.unassignedStudents[studentIndex]
    this.setData({activeTab:'assign',studentIndex,selectedStudentName:student.nickName||student.userName})
  },
  load() {
    this.setData({ loading: true })
    const userInfo = wx.getStorageSync('userInfo') || {}
    return Promise.all([
      request.get('/gw/profile/team'), request.get('/gw/assignment/list', { activeOnly: true }),
      request.get('/gw/application/list', { status: 'PENDING_MENTOR' }), request.get('/gw/return/list', { status: 'PENDING_MENTOR' }),
      request.get('/gw/room/list', { roomType: 'STUDIO' }), request.get('/gw/workstation/list', { workstationType: 'FIXED', mentorId: userInfo.userId })
    ]).then(([teamRes, assignmentRes, appRes, returnRes, roomRes, stationRes]) => {
      const active = {}
      ;(assignmentRes.data || []).filter(item => !item.endedAt).forEach(item => { active[item.studentId] = item })
      const team = (teamRes.data || []).map(item => Object.assign({}, item, { assignment: active[item.userId] || null }))
      const unassignedStudents = team.filter(item => !item.assignment)
      const ownedStations = (stationRes.data || []).filter(item => Number(item.mentorId) === Number(userInfo.userId))
      const ownedRooms = (roomRes.data || []).filter(room => ownedStations.some(s => Number(s.roomId) === Number(room.roomId))).map(room => {
        const seats = ownedStations.filter(s => Number(s.roomId) === Number(room.roomId))
        return {...room, workstationCount: seats.length, availableCount: seats.filter(s => s.status === 'AVAILABLE' && s.roomStatus === 'NORMAL').length, inUseCount: seats.filter(s => ['IN_USE','PENDING_RETURN'].includes(s.status)).length}
      })
      const fixedStations = ownedStations.filter(item => item.status === 'AVAILABLE' && item.roomStatus === 'NORMAL' && Number(item.mentorId) === Number(userInfo.userId)).map(item => Object.assign({}, item, { label: item.roomName + ' · ' + item.workstationNo }))
      const availableFixedCount = fixedStations.length
      const capacity = ownedRooms.reduce((sum, room) => ({ total: sum.total + Number(room.workstationCount || 0), available: sum.available + Number(room.availableCount || 0), inUse: sum.inUse + Number(room.inUseCount || 0) }), { total: 0, available: 0, inUse: 0 })
      const studentIndex = Math.min(this.data.studentIndex, Math.max(0, unassignedStudents.length - 1))
      const stationIndex = Math.min(this.data.stationIndex, Math.max(0, fixedStations.length - 1))
      const returns = (returnRes.data || []).map(item => Object.assign({}, item, { photoList: item.photoUrls ? item.photoUrls.split(',').filter(Boolean) : [] }))
      this.setData({ team, unassignedStudents, applications: appRes.data || [], returns, ownedRooms, fixedStations, capacity, availableFixedCount, studentIndex, stationIndex,
        selectedStudentName: unassignedStudents[studentIndex] ? (unassignedStudents[studentIndex].nickName || unassignedStudents[studentIndex].userName) : '暂无未分配学生',
        selectedStationName: fixedStations[stationIndex] ? fixedStations[stationIndex].label : '暂无空闲固定工位' })
    }).finally(() => this.setData({ loading: false }))
  },
  reviewApplication(e) {
    const status = e.currentTarget.dataset.status
    if (status === 'APPROVED' && this.data.availableFixedCount > 0) return wx.showToast({ title: '仍有空闲固定工位，请先分配固定工位', icon: 'none', duration: 2600 })
    this.askReview('流动工位初审', '/gw/application/' + e.currentTarget.dataset.id + '/mentor-review', status)
  },
  reviewReturn(e) { this.askReview('工位退还确认', '/gw/return/' + e.currentTarget.dataset.id + '/review', e.currentTarget.dataset.status) },
  askReview(title, url, status) {
    wx.showModal({ title, content: status === 'APPROVED' ? '请填写同意意见' : '请填写驳回原因', editable: true, placeholderText: '审核意见不能为空', success: res => {
      if (!res.confirm) return
      const comment = (res.content || '').trim()
      if (!comment) return wx.showToast({ title: '审核意见不能为空', icon: 'none' })
      request.post(url, { status, comment }).then(() => { wx.showToast({ title: '已处理' }); this.load() })
    } })
  },
  onStudentChange(e) { const studentIndex = Number(e.detail.value); const item = this.data.unassignedStudents[studentIndex]; this.setData({ studentIndex, selectedStudentName: item ? (item.nickName || item.userName) : '暂无未分配学生' }) },
  onStationChange(e) { const stationIndex = Number(e.detail.value); const item = this.data.fixedStations[stationIndex]; this.setData({ stationIndex, selectedStationName: item ? item.label : '暂无空闲固定工位' }) },
  onUsageDate(e) {
    if (this.data.submitting) return
    const field = e.currentTarget.dataset.field
    if (!['usageStart', 'usageEnd'].includes(field)) return
    const value = e.detail.value
    const next = { [field]: value }
    if (field === 'usageStart' && this.data.usageEnd && this.data.usageEnd < value) next.usageEnd = ''
    this.setData(next)
  },
  assignFixed() {
    if (this.data.submitting) return
    const student = this.data.unassignedStudents[this.data.studentIndex]
    const station = this.data.fixedStations[this.data.stationIndex]
    const { usageStart, usageEnd } = this.data
    const termCode = usageStart + '~' + usageEnd
    if (!student) return wx.showToast({ title: '暂无未分配学生', icon: 'none' })
    if (!station) return wx.showToast({ title: '暂无空闲固定工位', icon: 'none' })
    if (!usageStart || !usageEnd) return wx.showToast({ title: '请选择开始和结束日期', icon: 'none' })
    if (usageEnd < usageStart) return wx.showToast({ title: '结束日期不能早于开始日期', icon: 'none' })
    this.setData({ submitting: true })
    request.post('/gw/assignment/fixed', { studentId: student.userId, workstationId: station.workstationId, termCode }).then(() => {
      wx.showToast({ title: '固定工位已分配' }); this.setData({ usageStart: '', usageEnd: '' }); this.load()
    }).finally(() => this.setData({ submitting: false }))
  },
  previewReturnPhoto(e) { const urls = e.currentTarget.dataset.urls || []; wx.previewImage({ current: e.currentTarget.dataset.url, urls }) },
  recall(e) {
    const type = e.currentTarget.dataset.type
    const fixed = type === 'FIXED'
    wx.showModal({ title: fixed ? '直接收回固定工位' : '发起收回流动工位', content: fixed ? '收回后将立即释放工位，无需学生和管理员确认。请输入收回原因。' : '发起后将通知学生清理工位并上传照片，管理员确认后释放。请输入收回原因。', editable: true, placeholderText: '原因不能为空', success: res => {
      if (!res.confirm) return
      const reason = (res.content || '').trim()
      if (!reason) return wx.showToast({ title: '原因不能为空', icon: 'none' })
      request.post('/gw/assignment/recall', { assignmentId: e.currentTarget.dataset.id, reason }).then(() => { wx.showToast({ title: fixed ? '已收回并释放' : '已发起收回' }); this.load() })
    } })
  }
})
