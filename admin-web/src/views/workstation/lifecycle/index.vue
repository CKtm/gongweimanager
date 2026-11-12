<template>
  <div class="app-container">
    <el-alert title="学生申诉由导师初审、管理员终审。通过评分申诉时填写调整后分数；通过处罚申诉时解除对应处罚。" type="info" :closable="false" class="mb20"/>
    <el-tabs v-model="tab" @tab-change="changeTab"><el-tab-pane label="处罚记录" name="penalties"/><el-tab-pane label="学生申诉" name="appeals"/><el-tab-pane label="我的消息" name="messages"/></el-tabs>
    <div class="mb20"><el-button @click="load(1)" :disabled="loading">刷新</el-button><el-button v-if="tab==='messages'" :disabled="saving" @click="readAll">全部标为已读</el-button></div>
    <el-table v-loading="loading" :data="rows" border>
      <template v-if="tab==='penalties'">
        <el-table-column prop="studentName" label="学生"/><el-table-column label="工位"><template #default="{row}">{{row.roomName}} / {{row.workstationNo}}</template></el-table-column><el-table-column prop="reason" label="处罚原因" min-width="180"/><el-table-column prop="startedAt" label="开始时间" width="165"/><el-table-column label="截止时间" width="180"><template #default="{row}">{{row.untilAt || '历史记录，待补充期限'}}</template></el-table-column><el-table-column prop="endReason" label="结束说明" min-width="160"/>
      </template>
      <template v-else-if="tab==='appeals'">
        <el-table-column prop="studentName" label="学生"/><el-table-column label="申诉对象"><template #default="{row}">{{row.targetType==='SCORE'?'卫生评分':'停用处罚'}} #{{row.targetId}}</template></el-table-column><el-table-column prop="reason" label="申诉原因" min-width="170"/><el-table-column prop="originalScore" label="原评分" width="85"/><el-table-column prop="revisedScore" label="调整后" width="85"/><el-table-column prop="mentorComment" label="导师意见" min-width="140"/><el-table-column prop="adminComment" label="终审意见" min-width="140"/>
      </template>
      <template v-else><el-table-column prop="title" label="标题" min-width="170"/><el-table-column prop="content" label="内容" min-width="300"/><el-table-column label="阅读状态"><template #default="{row}">{{row.readAt?'已读':'未读'}}</template></el-table-column><el-table-column prop="createTime" label="时间" width="165"/></template>
      <el-table-column v-if="tab!=='messages'" label="状态" width="145"><template #default="{row}">{{labels[row.status] || row.status}}</template></el-table-column>
      <el-table-column label="操作" width="155"><template #default="{row}"><el-button v-if="tab==='appeals' && canReview(row)" link type="primary" :disabled="saving" @click="openReview(row)">{{admin?'终审处理':'初审处理'}}</el-button><el-button v-if="tab==='penalties' && admin && row.status==='ACTIVE'" link type="primary" :disabled="saving" @click="openDeadline(row)">调整截止时间</el-button><el-button v-if="tab==='messages'" link type="primary" :disabled="saving" @click="read(row)">查看消息</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" @pagination="load()"/>
    <el-dialog v-model="open" :title="kind==='review'?(admin?'管理员终审':'导师初审'):'调整处罚期限'" width="520px" :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
      <el-form label-width="100px" :disabled="saving"><template v-if="kind==='review'"><el-form-item label="申诉原因">{{target.reason}}</el-form-item><el-form-item label="审核结果"><el-radio-group v-model="decision"><el-radio value="APPROVED">通过</el-radio><el-radio value="REJECTED">驳回</el-radio></el-radio-group></el-form-item><el-form-item label="审核意见" required><el-input v-model="comment" type="textarea" :rows="4" maxlength="1000" show-word-limit/></el-form-item><el-form-item v-if="admin && target.targetType==='SCORE' && decision==='APPROVED'" label="调整后评分" required><el-input-number v-model="revisedScore" :min="0" :max="100" :precision="0"/></el-form-item></template><el-form-item v-else label="截止时间" required><el-date-picker v-model="until" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="北京时间"/></el-form-item></el-form>
      <template #footer><el-button :disabled="saving" @click="open=false">取消</el-button><el-button type="primary" :loading="saving" :disabled="saving" @click="submit">提交</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup name="GwLifecycle">
import {computed,ref,watch} from 'vue'
import request from '@/utils/request'
import useUserStore from '@/store/modules/user'
const {proxy}=getCurrentInstance(), user=useUserStore(), route=useRoute()
const admin=computed(()=>user.roles.some(r=>['admin','college_admin'].includes(r))), mentor=computed(()=>user.roles.includes('mentor'))
const tab=ref(['penalties','appeals','messages'].includes(route.query.tab)?route.query.tab:'appeals'),rows=ref([]),total=ref(0),pageNum=ref(1),pageSize=ref(20),loading=ref(false),saving=ref(false),open=ref(false),kind=ref('review'),target=ref({}),decision=ref('APPROVED'),comment=ref(''),revisedScore=ref(),until=ref('')
const labels={ACTIVE:'处罚生效中',RESTORED:'已提前恢复',EXPIRED:'已到期恢复',RECALLED:'原工位已收回',APPEAL_APPROVED:'申诉通过，处罚解除',PENDING_MENTOR:'待导师初审',PENDING_ADMIN:'待管理员终审',APPROVED:'申诉通过',REJECTED:'申诉驳回'}
const paths={penalties:'/gw/lifecycle/penalties',appeals:'/gw/appeals',messages:'/gw/messages'}
async function load(page){if(loading.value)return;if(typeof page==='number')pageNum.value=page;loading.value=true;try{const r=await request.get(paths[tab.value],{params:{pageNum:pageNum.value,pageSize:pageSize.value}});rows.value=r.rows||[];total.value=r.total||0}catch(e){rows.value=[];total.value=0}finally{loading.value=false}}
function changeTab(){rows.value=[];load(1)}
function canReview(row){return admin.value?row.status==='PENDING_ADMIN':mentor.value&&row.status==='PENDING_MENTOR'}
function openReview(row){target.value=row;kind.value='review';decision.value='APPROVED';comment.value='';revisedScore.value=undefined;open.value=true}
function openDeadline(row){target.value=row;kind.value='deadline';until.value=row.untilAt||'';open.value=true}
async function submit(){
 if(saving.value)return
 let path,body
 if(kind.value==='review'){
  if(!comment.value.trim())return proxy.$modal.msgWarning('请填写审核意见')
  body={decision:decision.value,comment:comment.value.trim()}
  if(admin.value&&target.value.targetType==='SCORE'&&decision.value==='APPROVED'){if(!Number.isInteger(revisedScore.value)||revisedScore.value<0||revisedScore.value>100)return proxy.$modal.msgWarning('请输入0至100的整数分数');body.revisedScore=revisedScore.value}
  path='/gw/appeals/'+target.value.appealId+'/review'
 }else{if(!until.value||new Date(until.value.replace(/-/g,'/')).getTime()<=Date.now())return proxy.$modal.msgWarning('请选择晚于当前时间的截止时间');path='/gw/lifecycle/penalties/'+target.value.penaltyId+'/deadline';body={until:until.value}}
 saving.value=true;try{await request.post(path,body);open.value=false;proxy.$modal.msgSuccess('处理成功');await load()}catch(e){}finally{saving.value=false}
}
async function read(row){if(saving.value)return;saving.value=true;try{await request.post('/gw/messages/'+row.messageId+'/read',{});await proxy.$modal.alert(row.content,row.title);await load()}catch(e){}finally{saving.value=false}}
async function readAll(){if(saving.value)return;saving.value=true;try{await request.post('/gw/messages/read-all',{});await load()}catch(e){}finally{saving.value=false}}
watch(()=>route.query.tab,value=>{if(['penalties','appeals','messages'].includes(value)&&value!==tab.value){tab.value=value;changeTab()}})
load()
</script>
