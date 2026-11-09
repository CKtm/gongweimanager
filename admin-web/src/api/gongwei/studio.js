import request from '@/utils/request'

export function listStudioApplication(params) {
  return request({ url: '/gw/studio/list', method: 'get', params })
}

export function submitStudioApplication(data) {
  return request({ url: '/gw/studio', method: 'post', data })
}

export function reviewStudioApplication(studioApplicationId, data) {
  return request({ url: `/gw/studio/${studioApplicationId}/review`, method: 'post', data })
}

export function releaseStudio(roomId, data) {
  return request({ url: `/gw/studio/${roomId}/release`, method: 'post', data })
}
