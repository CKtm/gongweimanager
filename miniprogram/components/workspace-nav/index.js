const request = require('../../utils/request')
const DISMISS_DISTANCE = 48
Component({
  properties: { appearance: { type: String, value: 'default' }, active: { type: String, value: 'home' } },
  data: { unread: 0, badgeVisible: false, badgeX: 0, badgeY: 0, badgeDragging: false },
  lifetimes: { attached() { this.refreshUnread() } },
  pageLifetimes: { show() { this.refreshUnread() }, hide() { this.resetBadge() } },
  methods: {
    badgeStorageKey() {
      const user = wx.getStorageSync('userInfo') || {}
      return 'workspace-badge-dismissed:' + (user.userId || user.userName || 'current')
    },
    refreshUnread() {
      const token = wx.getStorageSync('token')
      if (!token) { this.setData({ unread: 0, badgeVisible: false }); return Promise.resolve() }
      if (this._unreadRequest) return this._unreadRequest
      this._unreadRequest = request.get('/gw/messages/unread').then(res => {
        if (wx.getStorageSync('token') !== token) return
        const raw = Number(res.data), unread = Number.isFinite(raw) ? Math.max(0, Math.floor(raw)) : 0
        const dismissed = wx.getStorageSync(this.badgeStorageKey())
        // A different unread count starts a new reminder, including zero followed by new messages.
        if (!unread || (dismissed && dismissed.count !== unread)) wx.removeStorageSync(this.badgeStorageKey())
        this.setData({ unread, badgeVisible: unread > 0 && !(dismissed && dismissed.count === unread), badgeX: 0, badgeY: 0, badgeDragging: false })
      }).catch(() => {}).finally(() => { this._unreadRequest = null })
      return this._unreadRequest
    },
    badgeStart(e) {
      const touch = e.touches && e.touches[0]
      if (!touch) return
      this._badgeOrigin = { x: touch.clientX, y: touch.clientY }
      this.setData({ badgeDragging: true, badgeX: 0, badgeY: 0 })
    },
    badgeMove(e) {
      const touch = e.touches && e.touches[0]
      if (!touch || !this._badgeOrigin) return
      this.setData({ badgeX: touch.clientX - this._badgeOrigin.x, badgeY: touch.clientY - this._badgeOrigin.y })
    },
    badgeEnd(e) {
      if (!this._badgeOrigin) return
      const touch = e.changedTouches && e.changedTouches[0]
      const x = touch ? touch.clientX - this._badgeOrigin.x : this.data.badgeX
      const y = touch ? touch.clientY - this._badgeOrigin.y : this.data.badgeY
      if (Math.sqrt(x * x + y * y) >= DISMISS_DISTANCE) {
        wx.setStorageSync(this.badgeStorageKey(), { count: this.data.unread })
        this.setData({ badgeVisible: false })
      }
      this.resetBadge()
    },
    resetBadge() { this._badgeOrigin = null; this.setData({ badgeX: 0, badgeY: 0, badgeDragging: false }) },
    stopBadgeTap() {},
    go(e) {
      const key = e.currentTarget.dataset.key
      if (key === this.data.active) return
      const routes = { home: '/pages/index/index', spaces: '/pages/workstations/workstations', profile: '/pages/profile/profile' }
      if (routes[key]) wx.reLaunch({ url: routes[key] })
    }
  }
})
