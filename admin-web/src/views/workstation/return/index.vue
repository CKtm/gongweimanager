<template>
  <div class="app-container">
    <el-alert title="流动工位需由学生上传清理照片，管理员确认后才会释放；导师固定工位可在分配台账中直接收回。" type="info" :closable="false" class="mb20"/>
    <el-form :inline="true" class="mb8">
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width:220px">
          <el-option label="待导师审核" value="PENDING_MENTOR"/>
          <el-option label="待管理员确认释放" value="PENDING_ADMIN"/>
          <el-option label="等待学生上传清理照片" value="WAIT_CLEAR"/>
          <el-option label="待管理员核验（旧流程）" value="WAIT_VERIFY"/>
          <el-option label="已释放" value="COMPLETED"/>
          <el-option label="已驳回" value="REJECTED"/>
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="load">查询</el-button></el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="returnId" label="申请ID" width="85"/>
      <el-table-column label="来源" width="105"><template #default="{row}"><el-tag :type="sourceTypeTag(row.sourceType)">{{ sourceTypeText(row.sourceType) }}</el-tag></template></el-table-column>
      <el-table-column prop="studentName" label="学生" width="110"/>
      <el-table-column prop="roomName" label="房间" min-width="120"/>
      <el-table-column prop="workstationNo" label="工位" width="90"/>
      <el-table-column label="类型" width="80"><template #default="{row}">{{ row.workstationType === 'FIXED' ? '固定' : row.workstationType === 'MOBILE' ? '流动' : '-' }}</template></el-table-column>
      <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip/>
      <el-table-column label="清理照片" width="150">
        <template #default="{row}">
          <div v-if="photoList(row).length" class="photo-list">
            <el-image v-for="url in photoList(row)" :key="url" :src="imageUrl(url)" :preview-src-list="photoList(row).map(imageUrl)" preview-teleported fit="cover" class="photo"/>
          </div>
          <span v-else class="empty-photo">未上传</span>
        </template>
      </el-table-column>
      <el-table-column prop="mentorComment" label="导师意见" min-width="140" show-overflow-tooltip/>
      <el-table-column prop="adminComment" label="管理员意见" min-width="160" show-overflow-tooltip/>
      <el-table-column prop="status" label="状态" width="190"><template #default="{row}"><el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{row}">
          <template v-if="row.status === 'PENDING_MENTOR' && isMentor">
            <el-button link type="primary" @click="review(row,'APPROVED')">导师同意</el-button>
            <el-button link type="danger" @click="review(row,'REJECTED')">驳回</el-button>
          </template>
          <template v-else-if="row.status === 'PENDING_ADMIN' && isAdmin">
            <el-button link type="success" @click="review(row,'APPROVED')">{{ row.sourceType ? '确认释放' : '同意清空（旧流程）' }}</el-button>
            <el-button link type="danger" @click="review(row,'REJECTED')">{{ row.sourceType ? '清理不合格' : '驳回' }}</el-button>
          </template>
          <template v-else-if="row.status === 'WAIT_VERIFY' && isAdmin">
            <el-button link type="success" @click="verify(row,true)">确认释放</el-button>
            <el-button link type="danger" @click="verify(row,false)">退回重传照片</el-button>
          </template>
          <el-text v-else-if="row.status === 'WAIT_CLEAR'" type="warning">{{ row.sourceType ? '等待学生上传' : '等待学生确认清空（旧流程）' }}</el-text>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import useUserStore from '@/store/modules/user'
import { listReturn, reviewReturn, verifyReturn } from '@/api/gongwei/return'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const isAdmin = computed(() => userStore.roles.includes('admin') || userStore.roles.includes('college_admin'))
const isMentor = computed(() => userStore.roles.includes('mentor') && !isAdmin.value)
const loading = ref(false)
const list = ref([])
const query = reactive({ status: undefined })

function statusText(status) {
  return ({ PENDING_MENTOR: '待导师审核', PENDING_ADMIN: '待管理员确认释放', WAIT_CLEAR: '等待学生上传清理照片', WAIT_VERIFY: '待管理员核验（旧流程）', COMPLETED: '已释放', REJECTED: '已驳回' })[status] || status
}
function statusTag(status) { return ({ PENDING_MENTOR: 'warning', PENDING_ADMIN: 'warning', WAIT_CLEAR: 'info', WAIT_VERIFY: 'warning', COMPLETED: 'success', REJECTED: 'danger' })[status] || 'info' }
function sourceTypeText(sourceType) { return ({ STUDENT: '学生申请', MENTOR: '导师发起', ADMIN: '管理员发起' })[String(sourceType || '').toUpperCase()] || '历史记录' }
function sourceTypeTag(sourceType) { return ({ STUDENT: 'primary', MENTOR: 'success', ADMIN: 'warning' })[String(sourceType || '').toUpperCase()] || 'info' }
function photoList(row) {
  if (Array.isArray(row.photoUrls)) return row.photoUrls.filter(Boolean)
  const value = String(row.photoUrls || '').trim()
  if (!value) return []
  if (value.startsWith('[')) {
    try { const parsed = JSON.parse(value); if (Array.isArray(parsed)) return parsed.filter(Boolean) } catch (_) {}
  }
  return value.split(',').map(item => item.trim()).filter(Boolean)
}
function imageUrl(url) {
  if (/^(https?:|data:)/i.test(url)) return url
  const path = url.startsWith('/') ? url : `/${url}`
  return `${import.meta.env.VITE_APP_BASE_API}${path}`
}
function load() { loading.value = true; listReturn(query).then(res => { list.value = res.data || res.rows || [] }).finally(() => { loading.value = false }) }
function review(row, status) {
  const adminStep = row.status === 'PENDING_ADMIN' && isAdmin.value
  const legacy = !row.sourceType
  const approved = status === 'APPROVED'
  const title = adminStep ? (legacy ? (approved ? '旧流程：同意清空' : '旧流程：驳回退还') : (approved ? '确认释放工位' : '清理不合格')) : (approved ? '导师同意退还' : '导师驳回退还')
  const message = adminStep
    ? (legacy ? (approved ? '请输入同意意见，学生随后确认清空' : '请输入驳回原因') : (approved ? '请输入确认释放意见' : '请输入清理不合格原因，学生需重新上传清理照片'))
    : (approved ? '请输入导师审核意见' : '请输入驳回原因')
  proxy.$modal.prompt(message, title, { inputPattern: /\S+/, inputErrorMessage: '审核意见不能为空' })
    .then(({ value }) => reviewReturn(row.returnId, { status, comment: value }))
    .then(() => {
      const success = adminStep ? (legacy ? (approved ? '已通知学生清空工位' : '已驳回退还申请') : (approved ? '已确认释放工位' : '已退回，等待学生重新上传清理照片')) : (approved ? '导师审核通过，等待管理员确认' : '已驳回退还申请')
      proxy.$modal.msgSuccess(success)
      load()
    }).catch(() => {})
}
function verify(row, approved) {
  proxy.$modal.prompt(approved ? '请输入确认释放意见' : '请输入清理不合格原因，学生需重新清理并确认', approved ? '兼容旧流程：确认释放' : '兼容旧流程：退回重新清理', { inputPattern: /\S+/, inputErrorMessage: '核验意见不能为空' })
    .then(({ value }) => verifyReturn(row.returnId, { approved, comment: value }))
    .then(() => { proxy.$modal.msgSuccess(approved ? '已确认释放工位' : '已退回，等待学生重新上传清理照片'); load() })
    .catch(() => {})
}

load()
</script>

<style scoped>
.photo-list{display:flex;gap:5px;flex-wrap:wrap}.photo{width:44px;height:44px;border-radius:4px}.empty-photo{color:#909399}
</style>
