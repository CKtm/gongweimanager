const fs = require('fs'), path = require('path'), vm = require('vm'), assert = require('assert')
const root = path.resolve(__dirname, '..')
const rooms = [
  { roomId: 1, roomName: '421', roomType: 'STUDIO', status: 'NORMAL', location: '科研楼四层', mentorName: '导师甲、导师乙' },
  { roomId: 2, roomName: '422', roomType: 'MOBILE', status: 'NORMAL', location: '科研楼四层' },
  { roomId: 3, roomName: '空房间', roomType: 'STUDIO', status: 'NORMAL' }
]
const stations = [
  { workstationId: 11, roomId: 1, roomName: '421', workstationNo: 'A-01', workstationType: 'FIXED', status: 'AVAILABLE', mentorName: '导师甲' },
  { workstationId: 12, roomId: 1, roomName: '421', workstationNo: 'A-02', workstationType: 'FIXED', status: 'IN_USE', mentorName: '导师乙' },
  { workstationId: 21, roomId: 2, roomName: '422', workstationNo: 'M-01', workstationType: 'MOBILE', status: 'PENDING_RETURN' },
  { workstationId: 22, roomId: 2, roomName: '422', workstationNo: 'M-02', workstationType: 'MOBILE', status: 'DISABLED' }
]
const event = (field, value) => ({ currentTarget: { dataset: { field } }, detail: { value: String(value) } })
function setup() {
  let page, failure = false, requests = [], stopped = 0, navigation = [], modal
  const wx = { getStorageSync: () => ({ roles: ['common'] }), getWindowInfo: () => ({ statusBarHeight: 44, windowWidth: 393 }),
    getMenuButtonBoundingClientRect: () => ({ left: 296, top: 52, height: 32 }), stopPullDownRefresh: () => stopped++,
    pageScrollTo() {}, navigateTo: value => navigation.push(value.url), showModal: value => { modal = value } }
  const request = { get: async url => { requests.push(url); if (failure) throw new Error('offline'); return { data: JSON.parse(JSON.stringify(url === '/gw/room/list' ? rooms : stations)) } } }
  vm.runInNewContext(fs.readFileSync(path.join(root, 'pages/workstations/workstations.js'), 'utf8'), { Page: value => { page = value }, require: () => request, wx })
  page.data = JSON.parse(JSON.stringify(page.data))
  page.setData = function (value, callback) { Object.assign(this.data, value); if (callback) callback() }
  return { page, requests, navigation, wx, modal: () => modal, stopped: () => stopped, fail: value => { failure = value } }
}
;(async () => {
  const test = setup(), p = test.page
  p.onLoad(); assert.equal(p.data.statusBarHeight, 44); assert.equal(p.data.capsuleInset, 105)
  await p.onShow(); assert(p.data.isStudent); assert.equal(p.data.groups.length, 3); assert.equal(p.data.summary.total, 4)
  assert.equal(p.data.summary.available, 1); assert.equal(p.data.groups.find(r => r.roomId === 3).stationCount, 0)
  p.onSearch({ detail: { value: 'a-02' } }); assert.equal(p.data.groups.length, 1); assert.equal(p.data.summary.total, 1)
  p.onSearch({ detail: { value: '导师甲' } }); assert.equal(p.data.groups.length, 1); assert.equal(p.data.summary.total, 2)
  p.clearSearch(); p.openFilters(); p.onFilter(event('draftStatusIndex', 3)); p.closeFilters(); assert.equal(p.data.statusIndex, 0)
  p.openFilters(); p.onFilter(event('draftTypeIndex', 2)); p.applyDraftFilters(); assert.equal(p.data.groups.length, 1); assert.equal(p.data.groups[0].roomId, 2)
  assert.equal(p.data.activeFilterCount, 1); p.openFilters(); p.resetDraftFilters(); p.applyDraftFilters(); assert.equal(p.data.groups.length, 3)
  p.openFilters(); p.onFilter(event('draftRoomIndex', 1)); p.applyDraftFilters(); await p.load(); assert.equal(p.data.roomOptions[p.data.roomIndex].value, 1)
  p.openRoom({ currentTarget: { dataset: { id: '1' } } }); assert.equal(p.data.selectedRoom.roomId, 1)
  p.openStation({ currentTarget: { dataset: { id: '11' } } }); assert(test.modal().content.includes('导师甲')); assert(test.modal().content.includes('空闲'))
  p.openRoom({ currentTarget: { dataset: { id: '1' } } }); assert.equal(p.data.selectedRoom, null)
  p.openApplications(); assert.equal(test.navigation[0], '/pages/applications/applications')
  p.resetAll(); p.onSearch({ detail: { value: '不存在' } }); assert.equal(p.data.groups.length, 0); p.resetAll()
  const count = p.data.groups.length; test.fail(true); await p.onPullDownRefresh(); assert(p.data.error); assert.equal(test.stopped(), 1); assert.equal(p.data.groups.length, count)
  test.fail(false); await p.load(); assert.equal(p.data.error, '')
  const originalIndex = p.data.draftTypeIndex; p.onFilter(event('draftTypeIndex', 99)); assert.equal(p.data.draftTypeIndex, originalIndex)
  const originalData = JSON.stringify(stations); await p.load(); assert.equal(JSON.stringify(stations), originalData)
  const wxml = fs.readFileSync(path.join(root, 'pages/workstations/workstations.wxml'), 'utf8')
  for (const match of wxml.matchAll(/(?:bind\w+|catch\w+)="(\w+)"/g)) assert.equal(typeof p[match[1]], 'function', 'Missing handler: ' + match[1])
  const config = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8')); assert(config.pages.includes('pages/workstations/workstations'))
  for (const file of ['workstations.wxml', 'workstations.wxss']) {
    const source = fs.readFileSync(path.join(root, 'pages/workstations', file), 'utf8')
    assert(!source.includes('figma.com/api/mcp/asset/'), 'Temporary asset URL leaked into source')
  }
  for (const file of fs.readdirSync(path.join(root, 'assets/studios'))) {
    const data = fs.readFileSync(path.join(root, 'assets/studios', file))
    assert.equal(data.subarray(1, 4).toString(), 'PNG'); assert(data.readUInt32BE(16) > 0 && data.readUInt32BE(20) > 0)
  }
  let component
  vm.runInNewContext(fs.readFileSync(path.join(root, 'components/workspace-nav/index.js'), 'utf8'), { Component: value => { component = value }, require: () => ({ get: async () => ({ data: 0 }) }), wx: { reLaunch: value => test.navigation.push(value.url) } })
  assert.equal(component.properties.appearance.value, 'default')
  const nav = { data: { active: 'spaces' } }; component.methods.go.call(nav, { currentTarget: { dataset: { key: 'home' } } })
  assert.equal(test.navigation.at(-1), '/pages/index/index')
  console.log('WORKSTATIONS_OK: real endpoint mapping, filters/cancel/reset, search, empty rooms, refresh persistence/failure, room and seat details, safe header, navigation, event bindings, local PNG assets.')
})().catch(error => { console.error(error); process.exit(1) })
