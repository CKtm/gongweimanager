import request from '@/utils/request'

export function listRoom(query) {
  return request({ url: '/gw/room/list', method: 'get', params: query })
}

export function getRoom(roomId) {
  return request({ url: '/gw/room/' + roomId, method: 'get' })
}

export function addRoom(data) {
  return request({ url: '/gw/room', method: 'post', data })
}

export function updateRoom(data) {
  return request({ url: '/gw/room', method: 'put', data })
}

export function delRoom(roomId) {
  return request({ url: '/gw/room/' + roomId, method: 'delete' })
}
