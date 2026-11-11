<template>
  <div class="app-container">
    <el-alert title="固定工位由导师直接分配给自己的学生，系统按导师在所有房间名下的真实空闲固定工位校验，不再使用数字配额。" type="info" :closable="false" class="mb20"/>
    <el-form :inline="true" class="mb8"><el-form-item><el-checkbox v-model="activeOnly">只看有效占用</el-checkbox></el-form-item><el-form-item><el-button type="primary" @click="load">查询</el-button><el-button type="success" v-hasPermi="['gw:assignment:assign']" @click="openFixed">分配固定工位</el-button></el-form-item></el-form>
    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="assignmentId" label="记录ID" width="85"/><el-table-column prop="studentName" label="学生" min-width="110"/><el-table-column prop="roomName" label="房间" min-width="120"/><el-table-column label="工位" min-width="140"><template #default="{row}">{{row.workstationNo}} <el-tag v-if="row.workstationDeleted" type="info" size="small">已删除</el-tag></template></el-table-column><el-table-column prop="workstationType" label="类型" width="90"><template #default="{row}">{{ row.workstationType==='FIXED'?'固定':'流动' }}</template></el-table-column><el-table-column prop="termCode" label="学期" width="110"/><el-table-column label="来源" width="100"><template #default="{row}">{{ row.applicationId?'流动申请':'导师分配' }}</template></el-table-column><el-table-column prop="startedAt" label="开始时间" width="165"/><el-table-column prop="endedAt" label="结束时间" width="165"/>
      <el-table-column label="操作" width="190" fixed="right"><template #default="{row}"><template v-if="!row.endedAt"><el-button link type="primary" v-hasPermi="['gw:assignment:adjust']" @click="openAdjust(row)">调整</el-button><el-button link type="danger" v-hasPermi="['gw:assignment:recall']" @click="recall(row)">{{ recallButtonText(row) }}</el-button></template><span v-else>-</span></template></el-table-column>
    </el-table>

    <el-dialog v-model="fixedOpen" title="导师分配固定工位" width="520px">
      <el-form ref="fixedRef" :model="fixedForm" :rules="fixedRules" label-width="90px">
        <el-form-item label="学生" prop="studentId"><el-select v-model="fixedForm.studentId" filterable style="width:100%"><el-option v-for="item in students" :key="item.userId" :label="`${item.nickName||item.userName}（${item.userName}）`" :value="item.userId"/></el-select></el-form-item>
        <el-form-item label="学期" prop="termCode"><el-input v-model="fixedForm.termCode" placeholder="如 2026-秋"/></el-form-item>
        <el-form-item label="固定工位" prop="workstationId"><el-select v-model="fixedForm.workstationId" filterable style="width:100%"><el-option v-for="item in fixedStations" :key="item.workstationId" :label="`${item.roomName} / ${item.workstationNo}`" :value="item.workstationId"/></el-select></el-form-item>
      </el-form>
      <el-alert title="系统将校验师生关系、固定工位导师归属、学生全局单工位和工位实时状态。" type="info" :closable="false"/>
      <template #footer><el-button @click="fixedOpen=false">取消</el-button><el-button type="primary" @click="submitFixed">确认分配</el-button></template>
    </el-dialog>

    <el-dialog v-model="adjustOpen" title="调整工位" width="520px">
      <el-form ref="adjustRef" :model="adjustForm" :rules="adjustRules" label-width="90px"><el-form-item label="当前工位"><el-input :model-value="adjustCurrent" disabled/></el-form-item><el-form-item label="目标工位" prop="targetWorkstationId"><el-select v-model="adjustForm.targetWorkstationId" filterable style="width:100%"><el-option v-for="item in adjustStations" :key="item.workstationId" :label="`${item.roomName} / ${item.workstationNo}`" :value="item.workstationId"/></el-select></el-form-item><el-form-item label="调整原因" prop="reason"><el-input v-model="adjustForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit/></el-form-item></el-form>
      <template #footer><el-button @click="adjustOpen=false">取消</el-button><el-button type="primary" @click="submitAdjust">确认调整</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="GwAssignment">
import useUserStore from '@/store/modules/user'
import { adjustAssignment, assignFixed, listAssignment, recallAssignment } from '@/api/gongwei/assignment'
import { listProfileOptions, listMentorTeam } from '@/api/gongwei/profile'
import { listWorkstation } from '@/api/gongwei/workstation'
const {proxy}=getCurrentInstance();const userStore=useUserStore();const isMentor=computed(()=>userStore.roles.includes('mentor')&&!userStore.roles.includes('admin')&&!userStore.roles.includes('college_admin'))
const loading=ref(false),list=ref([]),activeOnly=ref(true),students=ref([]),fixedStations=ref([]),fixedOpen=ref(false)
const fixedForm=ref({studentId:undefined,workstationId:undefined,termCode:''});const fixedRules={studentId:[{required:true,message:'请选择学生'}],workstationId:[{required:true,message:'请选择固定工位'}],termCode:[{required:true,message:'请输入学期'}]}
const adjustOpen=ref(false),adjustForm=ref({assignmentId:undefined,targetWorkstationId:undefined,reason:''}),adjustStations=ref([]),adjustCurrent=ref('');const adjustRules={targetWorkstationId:[{required:true,message:'请选择目标工位'}],reason:[{required:true,message:'请输入调整原因'}]}
function load(){loading.value=true;listAssignment({activeOnly:activeOnly.value}).then(res=>list.value=res.data||[]).finally(()=>loading.value=false)}
function loadStudents(){return isMentor.value?listMentorTeam():listProfileOptions('common')}
function filterOwned(rows){const normalRows=rows.filter(x=>x.roomStatus==='NORMAL');return isMentor.value?normalRows.filter(x=>Number(x.mentorId)===Number(userStore.id)):normalRows}
function openFixed(){fixedForm.value={studentId:undefined,workstationId:undefined,termCode:''};Promise.all([loadStudents(),listWorkstation({workstationType:'FIXED',status:'AVAILABLE'})]).then(([users,stations])=>{const occupied=new Set(list.value.filter(x=>!x.endedAt).map(x=>Number(x.studentId)));students.value=(users.data||[]).filter(x=>!occupied.has(Number(x.userId)));fixedStations.value=filterOwned(stations.data||stations.rows||[]);fixedOpen.value=true})}
function submitFixed(){proxy.$refs.fixedRef.validate(valid=>{if(!valid)return;assignFixed(fixedForm.value).then(()=>{proxy.$modal.msgSuccess('固定工位分配成功');fixedOpen.value=false;load()})})}
function openAdjust(row){adjustForm.value={assignmentId:row.assignmentId,targetWorkstationId:undefined,reason:''};adjustCurrent.value=`${row.roomName} / ${row.workstationNo}`;listWorkstation({workstationType:row.workstationType,status:'AVAILABLE'}).then(res=>{adjustStations.value=filterOwned(res.data||res.rows||[]);adjustOpen.value=true})}
function submitAdjust(){proxy.$refs.adjustRef.validate(valid=>{if(!valid)return;adjustAssignment(adjustForm.value).then(()=>{proxy.$modal.msgSuccess('工位调整成功');adjustOpen.value=false;load()})})}
function isMentorFixedRecall(row){return isMentor.value&&row.workstationType==='FIXED'}
function recallButtonText(row){if(isMentorFixedRecall(row))return '直接收回';return isMentor.value?'发起收回':'主动收回'}
function recall(row){const mentorDirect=isMentorFixedRecall(row);const photoFlow=row.workstationType==='MOBILE';const message=mentorDirect?`确认直接收回 ${row.studentName} 的 ${row.roomName}/${row.workstationNo}？收回后将立即结束占用并释放工位，请填写收回原因`:photoFlow?`收回 ${row.studentName} 的 ${row.roomName}/${row.workstationNo} 后，学生需上传清理照片，管理员确认后才会释放工位。请输入收回原因`:`请输入收回 ${row.studentName} 的 ${row.roomName}/${row.workstationNo} 的原因`;const title=mentorDirect?'直接收回固定工位':photoFlow?'发起流动工位收回':'主动收回工位';proxy.$modal.prompt(message,title,{inputPattern:/\S+/,inputErrorMessage:'收回原因不能为空'}).then(({value})=>recallAssignment({assignmentId:row.assignmentId,reason:value})).then(()=>{const success=mentorDirect?'已直接收回并释放工位':photoFlow?'已发起收回，等待学生上传清理照片和管理员确认':'已发起收回，等待学生清空';proxy.$modal.msgSuccess(success);load()}).catch(()=>{})}
load()
</script>
