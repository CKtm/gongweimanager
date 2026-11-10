function isAdmin(user) { return (user.roles || []).some(role => role === 'admin' || role === 'college_admin') }
function can(user, permission) { return (user.roles || []).includes('admin') || (user.permissions || []).includes('*:*:*') || (user.permissions || []).includes(permission) }
const statusNames={PENDING:'待审核',CANCELLED:'已取消',PENDING_MENTOR:'待导师审核',PENDING_ADMIN:'待管理员审核',WAIT_ALLOC:'待分配',COMPLETED:'已完成',REJECTED:'已驳回',WAIT_CLEAR:'等待清理照片',WAIT_VERIFY:'待核验',IN_PROGRESS:'检查中',SUBMITTED:'已上报',VERIFIED:'已核实',ARCHIVED:'已归档',CLOSED:'已关闭',AVAILABLE:'空闲',IN_USE:'使用中',PENDING_RETURN:'回收中',DISABLED:'停用',APPROVED:'已通过'}
module.exports={isAdmin,can,statusNames}
