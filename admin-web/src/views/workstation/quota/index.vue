<template>
  <div class="app-container">
    <el-alert title="工作室按整间独占分配；一个工作室只能归属一位导师，一位导师可以申请并拥有多间工作室。" type="info" :closable="false" class="mb20"/>

    <el-card v-if="isMentor" class="mb20">
      <template #header>申请工作室</template>
      <el-form ref="applyRef" :model="applyForm" :rules="applyRules" inline>
        <el-form-item label="可申请工作室" prop="roomId"><el-select v-model="applyForm.roomId" filterable style="width:260px"><el-option v-for="room in availableRooms" :key="room.roomId" :label="`${room.roomName} · ${room.location || '位置未填写'} · ${room.workstationCount || 0}个工位`" :value="room.roomId"/></el-select></el-form-item>
        <el-form-item label="申请理由" prop="reason"><el-input v-model="applyForm.reason" style="width:360px" maxlength="1000"/></el-form-item>
        <el-form-item><el-button type="primary" @click="submitApply" v-hasPermi="['gw:studio:apply']">提交申请</el-button></el-form-item>
      </el-form>
      <el-empty v-if="!availableRooms.length" description="暂无可申请工作室" :image-size="70"/>
    </el-card>

    <el-form :inline="true" class="mb8"><el-form-item label="状态"><el-select v-model="query.status" clearable style="width:160px"><el-option label="待审核" value="PENDING"/><el-option label="已通过" value="APPROVED"/><el-option label="已驳回" value="REJECTED"/><el-option label="已解除" value="RETURNED"/></el-select></el-form-item><el-form-item><el-button type="primary" @click="load">查询</el-button></el-form-item></el-form>
    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="studioApplicationId" label="申请ID" width="90"/>
      <el-table-column prop="mentorName" label="导师" width="120"/>
      <el-table-column prop="roomName" label="工作室" width="140"/>
      <el-table-column prop="location" label="位置" min-width="130"/>
      <el-table-column prop="workstationCount" label="实际工位数" width="105"/>
      <el-table-column prop="reason" label="申请理由" min-width="180" show-overflow-tooltip/>
      <el-table-column prop="adminComment" label="审批意见" min-width="180" show-overflow-tooltip/>
      <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
      <el-table-column v-if="isAdmin" label="操作" width="210" fixed="right"><template #default="{row}"><template v-if="row.status==='PENDING'"><el-button link type="success" @click="review(row,'APPROVED')" v-hasPermi="['gw:studio:review']">通过</el-button><el-button link type="danger" @click="review(row,'REJECTED')" v-hasPermi="['gw:studio:review']">驳回</el-button></template><el-button v-if="row.status==='APPROVED'" link type="warning" @click="release(row)" v-hasPermi="['gw:studio:release']">解除归属</el-button></template></el-table-column>
    </el-table>

    <el-divider content-position="left">当前工作室归属</el-divider>
    <el-table :data="ownedRooms" border>
      <el-table-column prop="roomName" label="工作室"/><el-table-column prop="location" label="位置"/><el-table-column prop="mentorName" label="归属导师"/><el-table-column prop="workstationCount" label="工位总数"/><el-table-column prop="availableCount" label="空闲"/><el-table-column prop="inUseCount" label="占用"/>
    </el-table>
  </div>
</template>

<script setup name="GwStudioApplication">
import useUserStore from '@/store/modules/user'
import { listRoom } from '@/api/gongwei/room'
import { listStudioApplication, submitStudioApplication, reviewStudioApplication, releaseStudio } from '@/api/gongwei/studio'
const {proxy}=getCurrentInstance();const userStore=useUserStore();const roles=computed(()=>userStore.roles||[])
const isAdmin=computed(()=>roles.value.includes('admin')||roles.value.includes('college_admin'));const isMentor=computed(()=>roles.value.includes('mentor')&&!isAdmin.value)
const loading=ref(false),list=ref([]),availableRooms=ref([]),ownedRooms=ref([]);const query=reactive({status:undefined});const applyForm=reactive({roomId:undefined,reason:''})
const applyRules={roomId:[{required:true,message:'请选择工作室',trigger:'change'}],reason:[{required:true,message:'请输入申请理由',trigger:'blur'}]}
function statusText(s){return({PENDING:'待审核',APPROVED:'已通过',REJECTED:'已驳回',RETURNED:'已解除'})[s]||s}function statusTag(s){return({PENDING:'warning',APPROVED:'success',REJECTED:'danger',RETURNED:'info'})[s]||'info'}
function load(){loading.value=true;Promise.all([listStudioApplication(query),listRoom({roomType:'STUDIO'})]).then(([apps,rooms])=>{list.value=apps.data||[];const all=rooms.data||rooms.rows||[];availableRooms.value=all.filter(x=>x.status==='NORMAL'&&!x.mentorId);ownedRooms.value=all.filter(x=>x.mentorId)}).finally(()=>loading.value=false)}
function submitApply(){proxy.$refs.applyRef.validate(valid=>{if(!valid)return;submitStudioApplication(applyForm).then(()=>{proxy.$modal.msgSuccess('工作室申请已提交');applyForm.roomId=undefined;applyForm.reason='';load()})})}
function review(row,status){const action=status==='APPROVED'?'通过':'驳回';proxy.$modal.prompt(`请输入${action}意见`,`工作室申请${action}`,{inputPattern:/\S+/,inputErrorMessage:'审批意见不能为空'}).then(({value})=>reviewStudioApplication(row.studioApplicationId,{status,comment:value})).then(()=>{proxy.$modal.msgSuccess('处理成功');load()}).catch(()=>{})}
function release(row){proxy.$modal.prompt(`解除“${row.roomName}”与“${row.mentorName}”的归属前，室内不能存在有效占用。请输入原因`,'解除工作室归属',{inputPattern:/\S+/,inputErrorMessage:'解除原因不能为空'}).then(({value})=>releaseStudio(row.roomId,{reason:value})).then(()=>{proxy.$modal.msgSuccess('工作室归属已解除');load()}).catch(()=>{})}
load()
</script>
