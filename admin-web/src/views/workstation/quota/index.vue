<template>
  <div class="app-container">
    <el-alert title="固定工位由管理员在工位管理中直接划分给导师，导师再分配给自己的学生。工作室申请与审批流程已停用。" type="info" :closable="false" class="mb20"/>
    <el-divider content-position="left">固定工位所在房间</el-divider>
    <el-table :data="ownedRooms" border><el-table-column prop="roomName" label="房间"/><el-table-column prop="location" label="位置"/><el-table-column prop="mentorName" label="工位导师"/><el-table-column prop="workstationCount" label="工位数"/><el-table-column prop="availableCount" label="空闲"/><el-table-column prop="inUseCount" label="占用"/></el-table>
    <el-divider content-position="left">历史工作室申请记录</el-divider>
    <el-table v-loading="loading" :data="list" border><el-table-column prop="studioApplicationId" label="申请ID" width="90"/><el-table-column prop="mentorName" label="导师"/><el-table-column prop="roomName" label="工作室"/><el-table-column prop="reason" label="申请理由" show-overflow-tooltip/><el-table-column prop="adminComment" label="历史处理意见" show-overflow-tooltip/><el-table-column label="状态"><template #default="{row}">{{ statusText(row.status) }}</template></el-table-column></el-table>
  </div>
</template>
<script setup name="GwStudioHistory">
import useUserStore from '@/store/modules/user'
import { listRoom } from '@/api/gongwei/room'
import { listWorkstation } from '@/api/gongwei/workstation'
import { listStudioApplication } from '@/api/gongwei/studio'
const userStore=useUserStore(), loading=ref(false), list=ref([]), ownedRooms=ref([])
const isMentor=computed(()=>userStore.roles.includes('mentor')&&!userStore.roles.includes('admin')&&!userStore.roles.includes('college_admin'))
function statusText(s){return ({PENDING:'历史待处理（流程停用）',APPROVED:'已通过',REJECTED:'已驳回',RETURNED:'已解除',CANCELLED:'已取消'})[s]||s}
async function load(){loading.value=true;try{const [apps,roomRes,stationRes]=await Promise.all([listStudioApplication({}),listRoom({roomType:'STUDIO'}),listWorkstation({workstationType:'FIXED',...(isMentor.value?{mentorId:userStore.id}:{})})]);list.value=apps.data||[];const seats=stationRes.data||[];ownedRooms.value=(roomRes.data||[]).filter(r=>seats.some(s=>Number(s.roomId)===Number(r.roomId)&&s.mentorId!=null)).map(r=>{const owned=seats.filter(s=>Number(s.roomId)===Number(r.roomId)&&s.mentorId!=null);return {...r,workstationCount:owned.length,availableCount:owned.filter(s=>s.status==='AVAILABLE'&&s.roomStatus==='NORMAL').length,inUseCount:owned.filter(s=>['IN_USE','PENDING_RETURN'].includes(s.status)).length}})}finally{loading.value=false}}
load()
</script>
