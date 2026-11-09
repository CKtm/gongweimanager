import request from '@/utils/request'

export function listApplication(query) {
  return request({ url: '/gw/application/list', method: 'get', params: query })
}

export function submitApplication(data) {
  return request({ url: '/gw/application', method: 'post', data })
}

export function mentorReview(applicationId, data) {
  return request({ url: `/gw/application/${applicationId}/mentor-review`, method: 'post', data })
}

export function adminReview(applicationId, data) {
  return request({ url: `/gw/application/${applicationId}/admin-review`, method: 'post', data })
}

export function assignWorkstation(applicationId, workstationId) {
  return request({ url: `/gw/application/${applicationId}/assign`, method: 'post', data: { workstationId } })
}
