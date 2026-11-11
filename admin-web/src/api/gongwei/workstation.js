import request from '@/utils/request'

export function listWorkstation(query) {
  return request({ url: '/gw/workstation/list', method: 'get', params: query })
}

export function getWorkstation(workstationId) {
  return request({ url: '/gw/workstation/' + workstationId, method: 'get' })
}

export function addWorkstation(data) {
  return request({ url: '/gw/workstation', method: 'post', data })
}

export function createWorkstations(data) {
  return request({ url: '/gw/workstation/batch', method: 'post', data })
}

export function updateWorkstation(data) {
  return request({ url: '/gw/workstation', method: 'put', data })
}

export function assignWorkstationMentor(data) {
  return request({ url: '/gw/workstation/mentor', method: 'put', data })
}

export function changeWorkstationStatus(data) {
  return request({ url: '/gw/workstation/status', method: 'put', data })
}

export function delWorkstation(workstationId) {
  return request({ url: '/gw/workstation/' + workstationId, method: 'delete' })
}
