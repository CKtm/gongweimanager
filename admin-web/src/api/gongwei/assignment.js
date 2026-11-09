import request from '@/utils/request'

export function listAssignment(params) { return request({ url: '/gw/assignment/list', method: 'get', params }) }
export function listMyAssignment() { return request({ url: '/gw/assignment/my', method: 'get' }) }
export function assignFixed(data) { return request({ url: '/gw/assignment/fixed', method: 'post', data }) }
export function adjustAssignment(data) { return request({ url: '/gw/assignment/adjust', method: 'post', data }) }
export function recallAssignment(data) { return request({ url: '/gw/assignment/recall', method: 'post', data }) }
