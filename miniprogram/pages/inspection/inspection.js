const request = require('../../utils/request')

const statusText = status => ({ PENDING: '待检查', IN_PROGRESS: '检查中', SUBMITTED: '已上报', VERIFIED: '已核实', ARCHIVED: '已归档', CLOSED: '已关闭' })[status] || status

Page({
  data: {
    tasks: [], taskIndex: -1, selectedTask: null,
    rooms: [], stations: [], selectedRoom: null, seats: [], boardWidth: 600, boardHeight: 200, selectedTarget: null, roomRecord: null,
    score: 80, explanation: '', issueDescription: '', photos: [], records: [],
    showHistory: false, scoredCount: 0,
    submitting: false, uploading: false, completing: false, ratingVisible: false
  },
  toggleHistory() { this.setData({showHistory:!this.data.showHistory}) },
  onShow() { if (!this.data.ratingVisible && !this.busy()) this.load() },
  load(preferredTaskId) {
    Promise.all([request.get('/gw/inspection/task/list'), request.get('/gw/inspection/record/list')]).then(([taskRes, recordRes]) => {
      const tasks = (taskRes.data || []).filter(item => ['PENDING', 'IN_PROGRESS'].indexOf(item.status) >= 0).map(item => ({ ...item, displayStatus: statusText(item.status), taskLabel: item.taskName + ' · ' + item.scheduledDate }))
      const records = (recordRes.data || []).map(item => ({
        ...item,
        displayStatus: statusText(item.status),
        scoreText: item.score == null ? (item.grade ? item.grade + '级' : '历史记录') : item.score + '分' + (item.grade ? ' · ' + item.grade + '级' : ''),
        photoList: item.photoUrls ? item.photoUrls.split(',') : []
      }))
      const taskIndex = preferredTaskId == null ? (tasks.length === 1 ? 0 : -1) : tasks.findIndex(item => Number(item.taskId) === Number(preferredTaskId))
      this.setData({ tasks, records }, () => {
        if (taskIndex >= 0) this.selectTask(taskIndex)
        else this.resetSelection()
      })
    })
  },
  onTask(e) {
    if (this.busy()) return
    this.selectTask(Number(e.detail.value))
  },
  selectTask(taskIndex) {
    const selectedTask = this.data.tasks[taskIndex]
    if (!selectedTask) return
    const selectionVersion = (this.selectionVersion || 0) + 1
    this.selectionVersion = selectionVersion
    const previousRoomId = this.data.selectedRoom && this.data.selectedRoom.roomId
    Promise.all([request.get('/gw/room/list'), request.get('/gw/workstation/list')]).then(([roomRes, stationRes]) => {
      if (selectionVersion !== this.selectionVersion) return
      const taskRecords = this.data.records.filter(item => Number(item.taskId) === Number(selectedTask.taskId))
      const stations = stationRes.data || []
      const rooms = (roomRes.data || []).map(room => ({...room,
        seatCount: stations.filter(s => Number(s.roomId) === Number(room.roomId)).length,
        scoreCount: taskRecords.filter(r => Number(r.roomId) === Number(room.roomId)).length
      }))
      this.setData({
        taskIndex, selectedTask, rooms, stations, selectedTaskRecords: taskRecords, selectedTarget: null,
        score: 80, explanation: '', issueDescription: '', photos: [], scoredCount: taskRecords.length
      })
      this.openRoomById(previousRoomId || (rooms[0] && rooms[0].roomId))
    })
  },
  busy() { return this.data.submitting || this.data.uploading || this.data.completing },
  openRoom(e) { if (!this.busy()) this.openRoomById(e.currentTarget.dataset.id) },
  openRoomById(roomId) {
    const selectedRoom = this.data.rooms.find(r => Number(r.roomId) === Number(roomId)) || null
    if (!selectedRoom) { this.setData({selectedRoom: null, selectedTarget: null, seats: []}); return }
    const records = this.data.selectedTaskRecords
    const occupied = new Set()
    let maxCol = 3, maxRow = 1
    const seats = this.data.stations.filter(s => Number(s.roomId) === Number(roomId)).map((s, index) => {
      let row = Number(s.layoutRow), col = Number(s.layoutCol)
      if (!Number.isInteger(row) || row < 1 || !Number.isInteger(col) || col < 1 || occupied.has(row + ':' + col)) {
        let pos = index
        do { row = Math.floor(pos / 3) + 1; col = pos % 3 + 1; pos++ } while (occupied.has(row + ':' + col))
      }
      occupied.add(row + ':' + col); maxCol = Math.max(maxCol, col); maxRow = Math.max(maxRow, row)
      const record = records.find(r => Number(r.workstationId) === Number(s.workstationId))
      const state = record ? 'scored' : s.studentId != null ? 'in-use' : s.status === 'DISABLED' ? 'disabled' : 'empty-seat'
      return {...s, record: record || null, state, left: (col - 1) * 194, top: (row - 1) * 164,
        stateText: record ? record.score + ' 分 · 已评分' : s.studentId != null ? '点击评分' : s.status === 'DISABLED' ? '已停用' : '暂无使用人'}
    })
    const roomRecord = records.find(r => r.workstationId == null && Number(r.roomId) === Number(roomId)) || null
    this.setData({selectedRoom, seats, roomRecord, boardWidth: maxCol * 194, boardHeight: maxRow * 164,
      selectedTarget: null, score: 80, explanation: '', issueDescription: '', photos: []})
  },
  chooseSeat(e) {
    if (this.busy()) return
    const seat = this.data.seats.find(s => Number(s.workstationId) === Number(e.currentTarget.dataset.id))
    if (!seat) return
    if (seat.record) return wx.showToast({title: '本任务中该座位已评分：' + seat.record.score + '分', icon: 'none'})
    if (seat.studentId == null) return wx.showToast({title: '该座位暂无使用人', icon: 'none'})
    this.chooseTarget({...seat, label: seat.studentName + ' · ' + this.data.selectedRoom.roomName + ' / ' + seat.workstationNo})
  },
  chooseRoomScore() {
    if (this.busy() || !this.data.selectedRoom || this.data.selectedRoom.roomType !== 'STUDIO') return
    if (this.data.roomRecord) return wx.showToast({title: '本任务中该工作室已评分', icon: 'none'})
    this.chooseTarget({roomId: this.data.selectedRoom.roomId, workstationId: null, label: this.data.selectedRoom.roomName + ' · 工作室整体'})
  },
  chooseTarget(selectedTarget) {
    this.setData({selectedTarget, ratingVisible: true, score: 80, explanation: '', issueDescription: '', photos: []})
  },
  closeRating() {
    if (this.busy()) return
    this.setData({ratingVisible: false, selectedTarget: null, score: 80, explanation: '', issueDescription: '', photos: []})
  },
  stopTouch() {},
  onInput(e) { this.setData({ [e.currentTarget.dataset.field]: e.detail.value }) },
  onScoreChange(e) {
    const raw = Number(e.detail.value)
    if (!Number.isFinite(raw)) return
    this.setData({ score: Math.max(0, Math.min(100, Math.round(raw))) })
  },
  choosePhoto() {
    if (this.busy()) return
    const remain = 3 - this.data.photos.length
    if (remain <= 0) return wx.showToast({ title: '最多上传3张照片', icon: 'none' })
    wx.chooseMedia({
      count: remain, mediaType: ['image'], sourceType: ['album', 'camera'],
      success: res => {
        this.setData({ uploading: true })
        Promise.all((res.tempFiles || []).map(file => request.upload('/common/upload', file.tempFilePath))).then(rows => {
          this.setData({ photos: this.data.photos.concat(rows.map(row => row.url)) })
          wx.showToast({ title: '照片已上传' })
        }).catch(() => {}).finally(() => this.setData({ uploading: false }))
      }
    })
  },
  previewPhoto(e) { wx.previewImage({ current: e.currentTarget.dataset.url, urls: this.data.photos }) },
  removePhoto(e) { const photos = this.data.photos.slice(); photos.splice(Number(e.currentTarget.dataset.index), 1); this.setData({ photos }) },
  previewRecordPhoto(e) { const urls = e.currentTarget.dataset.urls || []; wx.previewImage({ current: e.currentTarget.dataset.url, urls }) },
  submit() {
    if (this.busy()) return
    if (!this.data.selectedTask) return wx.showToast({ title: '请选择检查任务', icon: 'none' })
    if (!this.data.selectedTarget) return wx.showToast({ title: '请点击座位或选择工作室整体评分', icon: 'none' })
    const score = Number(this.data.score)
    if (!Number.isInteger(score) || score < 0 || score > 100) return wx.showToast({ title: '请输入0到100的整数评分', icon: 'none' })
    if (this.data.issueDescription.trim() && !this.data.photos.length) return wx.showToast({ title: '记录问题时请上传现场照片', icon: 'none' })
    const target = this.data.selectedTarget
    const taskId = this.data.selectedTask.taskId
    this.setData({ submitting: true })
    request.post('/gw/inspection/record', {
      taskId,
      roomId: target.roomId,
      workstationId: target.workstationId || null,
      score,
      explanation: this.data.explanation,
      issueDescription: this.data.issueDescription,
      photoUrls: this.data.photos.join(',')
    }).then(() => {
      wx.showToast({ title: '上报成功' })
      this.setData({ ratingVisible: false, selectedTarget: null, score: 80, explanation: '', issueDescription: '', photos: [] })
      this.load(taskId)
    }).catch(() => {}).finally(() => this.setData({ submitting: false }))
  },
  completeTask() {
    if (!this.data.selectedTask || !this.data.scoredCount) return wx.showToast({ title: '请至少提交一条评分', icon: 'none' })
    const content = '本任务已提交 ' + this.data.scoredCount + ' 条评分，确定完成检查并上报吗？'
    wx.showModal({ title: '完成检查任务', content, success: res => {
      if (!res.confirm) return
      this.setData({ completing: true })
      request.post('/gw/inspection/task/' + this.data.selectedTask.taskId + '/complete').then(() => {
        wx.showToast({ title: '任务已完成' })
        this.resetSelection()
        this.load()
      }).finally(() => this.setData({ completing: false }))
    } })
  },
  resetSelection() {
    this.setData({
      taskIndex: -1, selectedTask: null, rooms: [], stations: [], seats: [], selectedRoom: null, selectedTarget: null, roomRecord: null, selectedTaskRecords: [],
      score: 80, explanation: '', issueDescription: '', photos: [], scoredCount: 0, ratingVisible: false
    })
  }
})
