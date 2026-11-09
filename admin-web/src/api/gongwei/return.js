import request from '@/utils/request'

export function listReturn(params) { return request({ url: '/gw/return/list', method: 'get', params }) }
export function reviewReturn(returnId, data) { return request({ url: `/gw/return/${returnId}/review`, method: 'post', data }) }
export function verifyReturn(returnId, data) { return request({ url: `/gw/return/${returnId}/verify`, method: 'post', data }) }
