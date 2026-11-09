<template>
  <div class="dashboard" v-loading="loading">
    <div class="welcome">
      <div><h1>研究生工位管理后台</h1><p>集中处理房间、真实工位、工作室申请、分配、回收与卫生检查业务。</p></div>
      <el-button type="primary" @click="load">刷新数据</el-button>
    </div>

    <div class="metric-grid">
      <div class="metric"><span>工作室</span><strong>{{ rooms.length }}</strong><small>正常 {{ normalRoomCount }} 间</small></div>
      <div class="metric green"><span>工位总数</span><strong>{{ workstations.length }}</strong><small>空闲 {{ workstationSummary.available }}</small></div>
      <div class="metric orange"><span>使用中</span><strong>{{ workstationSummary.inUse }}</strong><small>待回收 {{ workstationSummary.pending }}</small></div>
      <div class="metric blue"><span>有效分配</span><strong>{{ activeAssignments.length }}</strong><small>历史记录 {{ assignments.length }}</small></div>
    </div>

    <el-row :gutter="18">
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel">
          <template #header><div class="panel-title"><span>待办事项</span><el-tag type="danger">{{ totalTodo }}</el-tag></div></template>
          <div class="todo" @click="go('/workstation/application')"><span>流动工位终审/待分配</span><b>{{ pending.mobile }}</b></div>
          <div class="todo" @click="go('/workstation/quota')"><span>导师工作室申请</span><b>{{ pending.studio }}</b></div>
          <div class="todo" @click="go('/workstation/return')"><span>退还审核/回收核验</span><b>{{ pending.returns }}</b></div>
          <div class="todo" @click="go('/inspection/record')"><span>待核实卫生记录</span><b>{{ pending.inspection }}</b></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel">
          <template #header><div class="panel-title"><span>快捷入口</span></div></template>
          <div class="quick-grid">
            <el-button @click="go('/workstation/room')">工作室管理</el-button>
            <el-button @click="go('/workstation/workstation')">工位台账与布局</el-button>
            <el-button @click="go('/workstation/assignment')">分配与调整</el-button>
            <el-button @click="go('/workstation/profile')">人员与师生关系</el-button>
            <el-button @click="go('/inspection/task')">卫生检查任务</el-button>
            <el-button @click="go('/system/user')">账号管理</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel table-panel">
      <template #header><div class="panel-title"><span>工作室工位概览</span><el-button link type="primary" @click="go('/workstation/workstation')">查看布局</el-button></div></template>
      <el-table :data="roomSummary" max-height="360">
        <el-table-column prop="roomName" label="工作室" min-width="160" />
        <el-table-column prop="location" label="位置" min-width="160" />
        <el-table-column prop="total" label="总数" width="90" />
        <el-table-column prop="available" label="空闲" width="90" />
        <el-table-column prop="inUse" label="使用中" width="90" />
        <el-table-column prop="pending" label="待回收" width="90" />
        <el-table-column label="使用率" width="190"><template #default="{ row }"><el-progress :percentage="row.rate" :stroke-width="10" /></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="Index">
import { computed, onMounted, reactive, ref } from 'vue'
import { listRoom } from '@/api/gongwei/room'
import { listWorkstation } from '@/api/gongwei/workstation'
import { listApplication } from '@/api/gongwei/application'
import { listReturn } from '@/api/gongwei/return'
import { listStudioApplication } from '@/api/gongwei/studio'
import { listInspectionRecord } from '@/api/gongwei/inspection'
import { listAssignment } from '@/api/gongwei/assignment'

const router = useRouter()
const loading = ref(false)
const rooms = ref([])
const workstations = ref([])
const assignments = ref([])
const pending = reactive({ mobile: 0, studio: 0, returns: 0, inspection: 0 })
const normalRoomCount = computed(() => rooms.value.filter(item => item.status === 'NORMAL').length)
const workstationSummary = computed(() => ({
  available: workstations.value.filter(item => item.status === 'AVAILABLE').length,
  inUse: workstations.value.filter(item => item.status === 'IN_USE').length,
  pending: workstations.value.filter(item => item.status === 'PENDING_RETURN').length
}))
const activeAssignments = computed(() => assignments.value.filter(item => !item.endedAt))
const totalTodo = computed(() => pending.mobile + pending.studio + pending.returns + pending.inspection)
const roomSummary = computed(() => rooms.value.map(room => {
  const rows = workstations.value.filter(item => item.roomId === room.roomId)
  const inUse = rows.filter(item => item.status === 'IN_USE').length
  return { roomName: room.roomName, location: room.location, total: rows.length, available: rows.filter(item => item.status === 'AVAILABLE').length, inUse, pending: rows.filter(item => item.status === 'PENDING_RETURN').length, rate: rows.length ? Math.round(inUse * 100 / rows.length) : 0 }
}))

function load() {
  loading.value = true
  Promise.all([listRoom({}), listWorkstation({}), listApplication({}), listReturn({}), listStudioApplication({}), listInspectionRecord({}), listAssignment({})]).then(([roomRes, stationRes, appRes, returnRes, studioRes, inspectionRes, assignmentRes]) => {
    rooms.value = roomRes.data || []
    workstations.value = stationRes.data || []
    assignments.value = assignmentRes.data || []
    pending.mobile = (appRes.data || []).filter(item => ['PENDING_ADMIN', 'WAIT_ALLOC'].includes(item.status)).length
    pending.studio = (studioRes.data || []).filter(item => item.status === 'PENDING').length
    pending.returns = (returnRes.data || []).filter(item => ['PENDING_ADMIN', 'WAIT_VERIFY'].includes(item.status)).length
    pending.inspection = (inspectionRes.data || []).filter(item => item.status === 'SUBMITTED').length
  }).finally(() => { loading.value = false })
}
function go(path) { router.push(path) }
onMounted(load)
</script>

<style scoped lang="scss">
.dashboard{min-height:calc(100vh - 84px);padding:22px;background:#f5f7fa}.welcome{display:flex;justify-content:space-between;align-items:center;padding:24px 28px;margin-bottom:18px;border-radius:12px;background:linear-gradient(135deg,#1d6fe9,#59a4ff);color:#fff}.welcome h1{margin:0 0 8px;font-size:26px}.welcome p{margin:0;opacity:.9}.metric-grid{display:grid;grid-template-columns:repeat(4,minmax(180px,1fr));gap:16px;margin-bottom:18px}.metric{padding:20px 22px;border-radius:10px;background:#fff;border-left:5px solid #8c8c8c;box-shadow:0 2px 10px rgba(0,0,0,.04)}.metric.green{border-color:#52c41a}.metric.orange{border-color:#fa8c16}.metric.blue{border-color:#1677ff}.metric span,.metric small{display:block;color:#8c8c8c}.metric strong{display:block;margin:8px 0;font-size:30px;color:#262626}.panel{margin-bottom:18px;border:0}.panel-title{display:flex;justify-content:space-between;align-items:center;font-weight:600}.todo{display:flex;justify-content:space-between;padding:16px 8px;border-bottom:1px solid #f0f0f0;cursor:pointer}.todo:hover{color:#1677ff}.todo:last-child{border-bottom:0}.todo b{min-width:28px;text-align:center;color:#f5222d}.quick-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}.quick-grid .el-button{height:54px;margin:0}.table-panel{margin-bottom:0}@media(max-width:900px){.metric-grid{grid-template-columns:repeat(2,1fr)}.quick-grid{grid-template-columns:repeat(2,1fr)}}
</style>
