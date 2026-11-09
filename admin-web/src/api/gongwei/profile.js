import request from '@/utils/request'

export function listProfile(params) {
  return request({ url: '/gw/profile/list', method: 'get', params })
}

export function saveProfile(data) {
  return request({ url: '/gw/profile', method: 'post', data })
}

export function listProfileOptions(roleKey) {
  return request({ url: '/gw/profile/options', method: 'get', params: { roleKey } })
}

export function listMentorTeam() {
  return request({ url: '/gw/profile/team', method: 'get' })
}

export function bindMentorRelation(data) {
  return request({ url: '/gw/profile/relation', method: 'post', data })
}

export function unbindMentorRelation(studentId) {
  return request({ url: `/gw/profile/relation/${studentId}`, method: 'delete' })
}
