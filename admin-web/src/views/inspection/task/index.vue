<template>
  <div class="app-container">
    <el-alert title="管理员指定卫生检查员和检查日期，检查员自行选择工作室整体或学生当前工位评分，至少提交一条评分后完成任务。" type="info" :closable="false" class="mb20"/>
    <el-form :inline="true" class="mb8"><el-form-item label="状态"><el-select v-model="query.status" clearable style="width:150px"><el-option label="待检查" value="PENDING"/><el-option label="检查中" value="IN_PROGRESS"/><el-option label="已上报" value="SUBMITTED"/><el-option label="已关闭" value="CLOSED"/></el-select></el-form-item><el-form-item><el-button type="primary" @click="load">查询</el-button><el-button type="success" v-hasPermi="['gw:inspection:task:add']" @click="openAdd">创建任务</el-button></el-form-item></el-form>
    <el-table v-loading="loading" :data="list" border><el-table-column prop="taskId" label="任务ID" width="85"/><el-table-column prop="taskName" label="任务名称" min-width="160"/><el-table-column prop="inspectorName" label="检查员" width="120"/><el-table-column label="评分进度" min-width="210"><template #default="{row}"><div>工作室评分 {{(row.recordCount||0)-(row.workstationRecordCount||0)}} 条</div><div class="progress-detail">工位评分 {{row.workstationRecordCount||0}} 条 · 共 {{row.recordCount||0}} 条记录</div></template></el-table-column><el-table-column prop="scheduledDate" label="检查日期" width="120"/><el-table-column prop="remark" label="任务说明" show-overflow-tooltip/><el-table-column prop="status" label="状态" width="100"><template #default="{row}"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column></el-table>
    <el-dialog v-model="open" title="创建卫生检查任务" width="520px"><el-form ref="formRef" :model="form" :rules="rules" label-width="90px"><el-form-item label="任务名称" prop="taskName"><el-input v-model="form.taskName"/></el-form-item><el-form-item label="检查员" prop="inspectorId"><el-select v-model="form.inspectorId" filterable style="width:100%"><el-option v-for="item in inspectors" :key="item.userId" :label="`${item.nickName||item.userName}（${item.userName}）`" :value="item.userId"/></el-select></el-form-item><el-form-item label="检查日期" prop="scheduledDate"><el-date-picker v-model="form.scheduledDate" value-format="YYYY-MM-DD" type="date" style="width:100%"/></el-form-item><el-form-item label="任务说明"><el-input v-model="form.remark" type="textarea" :rows="3"/></el-form-item></el-form><template #footer><el-button @click="open=false">取消</el-button><el-button type="primary" @click="submit">创建</el-button></template></el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { addInspectionTask, listInspectionTask } from '@/api/gongwei/inspection'
import { listProfileOptions } from '@/api/gongwei/profile'
const {proxy}=getCurrentInstance(),list=ref([]),loading=ref(false),open=ref(false),inspectors=ref([]),rooms=ref([]),query=reactive({status:undefined}),form=reactive({taskName:'',inspectorId:undefined,roomId:undefined,scheduledDate:'',remark:''})
const rules={taskName:[{required:true,message:'请输入任务名称'}],inspectorId:[{required:true,message:'请选择检查员'}],scheduledDate:[{required:true,message:'请选择检查日期'}]}
function statusText(s){return({PENDING:'待检查',IN_PROGRESS:'检查中',SUBMITTED:'已上报',CLOSED:'已关闭'})[s]||s} function statusType(s){return({PENDING:'warning',IN_PROGRESS:'primary',SUBMITTED:'success',CLOSED:'info'})[s]||'info'}
function hasScore(score){return score!==null&&score!==undefined&&score!==''}
function load(){loading.value=true;listInspectionTask(query).then(res=>list.value=res.data||[]).finally(()=>loading.value=false)}
function openAdd(){Object.assign(form,{taskName:'',inspectorId:undefined,scheduledDate:'',remark:''});listProfileOptions('inspector').then(users=>{inspectors.value=users.data||[];open.value=true})}
function submit(){proxy.$refs.formRef.validate(valid=>{if(!valid)return;addInspectionTask(form).then(()=>{proxy.$modal.msgSuccess('任务创建成功');open.value=false;load()})})}
load()
</script>
<style scoped>.progress-detail{margin-top:5px;color:var(--el-text-color-secondary);font-size:12px}</style>
