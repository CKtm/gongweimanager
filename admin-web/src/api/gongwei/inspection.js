import request from '@/utils/request'

export function listInspectionTask(params) { return request({ url: '/gw/inspection/task/list', method: 'get', params }) }
export function addInspectionTask(data) { return request({ url: '/gw/inspection/task', method: 'post', data }) }
export function listInspectionRecord(params) { return request({ url: '/gw/inspection/record/list', method: 'get', params }) }
export function submitInspectionRecord(data) { return request({ url: '/gw/inspection/record', method: 'post', data }) }
export function verifyInspectionRecord(data) { return request({ url: '/gw/inspection/record/verify', method: 'post', data }) }
