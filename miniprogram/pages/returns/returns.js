const request = require('../../utils/request')

const STATUS = {
  PENDING_MENTOR: '待导师审核',
  PENDING_ADMIN: '待管理员确认释放',
  WAIT_CLEAR: '待上传清理照片',
  WAIT_VERIFY: '待管理员核验（历史流程）',
  COMPLETED: '已释放',
  REJECTED: '已驳回'
}
const SOURCE = { STUDENT: '学生主动退还', MENTOR: '导师主动收回', ADMIN: '管理员主动收回' }

Page({
  data: {
    list: [], assignment: null, hasPendingReturn: false, reason: '', photos: [], submitting: false, uploading: false,
    reuploadReturnId: null, reuploadPhotos: [], reuploading: false
  },
  onShow() { this.load() },
  load() {
    Promise.all([request.get('/gw/assignment/my'), request.get('/gw/return/list')]).then(([a, r]) => {
      const list = (r.data || []).map(item => Object.assign({}, item, {
        statusText: item.status === 'WAIT_CLEAR' && !item.sourceType ? '待确认已清空（历史流程）' : (STATUS[item.status] || item.status),
        sourceText: SOURCE[item.sourceType] || '退还申请',
        photoList: item.photoUrls ? item.photoUrls.split(',').filter(Boolean) : []
      }))
      const assignment = (a.data || [])[0] || null
      const pendingStatuses = ['PENDING_MENTOR', 'PENDING_ADMIN', 'WAIT_CLEAR', 'WAIT_VERIFY']
      const hasPendingReturn = !!assignment && list.some(item => Number(item.assignmentId) === Number(assignment.assignmentId) && pendingStatuses.indexOf(item.status) >= 0)
      this.setData({ assignment, list, hasPendingReturn })
    })
  },
  onInput(e) { this.setData({ reason: e.detail.value }) },
  choosePhoto(e) {
    const target = e.currentTarget.dataset.target === 'reupload' ? 'reuploadPhotos' : 'photos'
    const current = this.data[target] || []
    const remain = 3 - current.length
    if (remain <= 0) return wx.showToast({ title: '最多上传3张照片', icon: 'none' })
    wx.chooseMedia({
      count: remain, mediaType: ['image'], sourceType: ['album', 'camera'],
      success: res => {
        this.setData({ uploading: true })
        Promise.all((res.tempFiles || []).map(file => request.upload('/common/upload', file.tempFilePath))).then(rows => {
          this.setData({ [target]: current.concat(rows.map(row => row.url)) })
          wx.showToast({ title: '照片已上传' })
        }).finally(() => this.setData({ uploading: false }))
      }
    })
  },
  removePhoto(e) {
    const target = e.currentTarget.dataset.target === 'reupload' ? 'reuploadPhotos' : 'photos'
    const rows = (this.data[target] || []).slice()
    rows.splice(Number(e.currentTarget.dataset.index), 1)
    this.setData({ [target]: rows })
  },
  previewPhoto(e) {
    const urls = e.currentTarget.dataset.urls || []
    wx.previewImage({ current: e.currentTarget.dataset.url, urls })
  },
  submit() {
    if (!this.data.assignment) return wx.showToast({ title: '暂无可退还工位', icon: 'none' })
    if (this.data.assignment.workstationType !== 'MOBILE') return wx.showToast({ title: '固定工位由导师管理', icon: 'none' })
    if (this.data.hasPendingReturn) return wx.showToast({ title: '已有未完成的退还或收回流程', icon: 'none' })
    const reason = (this.data.reason || '').trim()
    if (!reason) return wx.showToast({ title: '请填写退还原因', icon: 'none' })
    if (!this.data.photos.length) return wx.showToast({ title: '请上传清理后的工位照片', icon: 'none' })
    this.setData({ submitting: true })
    request.post('/gw/return', {
      assignmentId: this.data.assignment.assignmentId,
      reason,
      photoUrls: this.data.photos.join(',')
    }).then(() => {
      wx.showToast({ title: '退还申请已提交' })
      this.setData({ reason: '', photos: [] })
      this.load()
    }).finally(() => this.setData({ submitting: false }))
  },
  startReupload(e) {
    this.setData({ reuploadReturnId: Number(e.currentTarget.dataset.id), reuploadPhotos: [] })
  },
  cancelReupload() { this.setData({ reuploadReturnId: null, reuploadPhotos: [] }) },
  legacyClear(e) {
    wx.showModal({ title: '确认已清空', content: '这是升级前的历史记录，请确认个人物品已经全部清空。', success: res => {
      if (res.confirm) request.post('/gw/return/' + e.currentTarget.dataset.id + '/clear').then(() => {
        wx.showToast({ title: '已确认清空' })
        this.load()
      })
    } })
  },
  submitReupload() {
    if (!this.data.reuploadReturnId) return
    if (!this.data.reuploadPhotos.length) return wx.showToast({ title: '请上传新的清理照片', icon: 'none' })
    this.setData({ reuploading: true })
    request.post('/gw/return/' + this.data.reuploadReturnId + '/photos', { photoUrls: this.data.reuploadPhotos.join(',') }).then(() => {
      wx.showToast({ title: '清理照片已提交' })
      this.setData({ reuploadReturnId: null, reuploadPhotos: [] })
      this.load()
    }).finally(() => this.setData({ reuploading: false }))
  }
})
