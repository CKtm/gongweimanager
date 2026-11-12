<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="房间"><el-select v-model="queryParams.roomId" clearable style="width:180px" @change="getList"><el-option v-for="item in rooms" :key="item.roomId" :label="item.roomName" :value="item.roomId" /></el-select></el-form-item>
      <el-form-item label="类型"><el-select v-model="queryParams.workstationType" clearable style="width:130px"><el-option label="固定" value="FIXED"/><el-option label="流动" value="MOBILE"/></el-select></el-form-item>
      <el-form-item label="状态"><el-select v-model="queryParams.status" clearable style="width:140px"><el-option label="空闲" value="AVAILABLE"/><el-option label="使用中" value="IN_USE"/><el-option label="待回收" value="PENDING_RETURN"/><el-option label="停用" value="DISABLED"/></el-select></el-form-item>
      <el-form-item label="编号"><el-input v-model="queryParams.workstationNo" clearable @keyup.enter="getList" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="getList">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <div class="summary-row">
      <div class="summary"><b>{{ summary.total }}</b><span>工位总数</span></div><div class="summary available"><b>{{ summary.available }}</b><span>空闲</span></div><div class="summary using"><b>{{ summary.inUse }}</b><span>使用中</span></div><div class="summary pending"><b>{{ summary.pending }}</b><span>待回收</span></div>
    </div>
    <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['gw:workstation:add']">新增工位</el-button></el-col><el-col :span="1.5"><el-button type="info" plain icon="Upload" @click="handleImport" v-hasPermi="['gw:workstation:add']">批量导入</el-button></el-col><el-col :span="1.5"><el-radio-group v-model="viewMode"><el-radio-button value="table">台账</el-radio-button><el-radio-button value="grid" :disabled="!queryParams.roomId">房间布局</el-radio-button></el-radio-group></el-col><el-col v-if="!queryParams.roomId" :span="8"><el-text type="info">选择房间后可查看编号网格布局</el-text></el-col></el-row>

    <div v-if="viewMode === 'table'" class="batch-toolbar">
      <el-text>已选 {{ selectedRows.length }} 个工位（最多 200 个）</el-text>
      <el-button :disabled="!selectedRows.length || saving" @click="openBatchEdit" v-hasPermi="['gw:workstation:edit']">批量修改</el-button>
      <el-button type="warning" plain :disabled="!selectedRows.length || saving" @click="batchStatus('DISABLED')" v-hasPermi="['gw:workstation:edit']">批量停用</el-button>
      <el-button type="success" plain :disabled="!selectedRows.length || saving" @click="batchStatus('IN_USE')" v-hasPermi="['gw:workstation:edit']">批量恢复</el-button>
      <el-button type="danger" plain :disabled="!selectedRows.length || saving" @click="batchDelete" v-hasPermi="['gw:workstation:remove']">批量删除</el-button>
    </div>
    <el-table ref="tableRef" v-if="viewMode === 'table'" v-loading="loading || saving" :data="workstationList" row-key="workstationId" @selection-change="selectionChange">
      <el-table-column type="selection" width="48"/>
      <el-table-column label="编号" prop="workstationId" width="80"/><el-table-column label="房间" prop="roomName"/><el-table-column label="归属导师" prop="mentorName"/><el-table-column label="工位号" prop="workstationNo"/><el-table-column label="类型" width="90"><template #default="{row}">{{ row.workstationType === 'FIXED' ? '固定' : '流动' }}</template></el-table-column><el-table-column label="位置" width="110"><template #default="{row}">{{ row.layoutRow }} 行 / {{ row.layoutCol }} 列</template></el-table-column><el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column><el-table-column label="使用人" prop="studentName"/><el-table-column label="操作" width="230"><template #default="{row}"><el-button link type="primary" @click="handleEdit(row)" v-hasPermi="['gw:workstation:edit']">修改</el-button><el-button v-if="row.status === 'IN_USE' && row.studentId" link type="warning" @click="changeStatus(row,'DISABLED')" v-hasPermi="['gw:workstation:edit']">停用</el-button><el-button v-if="row.status === 'DISABLED'" link type="success" @click="changeStatus(row,'IN_USE')" v-hasPermi="['gw:workstation:edit']">恢复</el-button><el-button v-if="row.status === 'AVAILABLE' && !row.studentId" link type="danger" @click="handleDelete(row)" v-hasPermi="['gw:workstation:remove']">删除</el-button></template></el-table-column>
    </el-table>

    <div v-else v-loading="loading" class="layout-panel">
      <div class="legend"><span><i class="dot available-bg"/>空闲</span><span><i class="dot using-bg"/>使用中</span><span><i class="dot pending-bg"/>待回收</span><span><i class="dot disabled-bg"/>停用</span></div>
      <div class="station-grid" :style="gridStyle"><div v-for="item in workstationList" :key="item.workstationId" class="station-card" :class="item.status.toLowerCase()" :style="stationStyle(item)" @click="handleEdit(item)"><b>{{ item.workstationNo }}</b><small>{{ item.workstationType === 'FIXED' ? '固定' : '流动' }}</small><span>{{ statusText(item.status) }}</span><em v-if="item.studentName">{{ item.studentName }}</em></div></div>
    </div>

    <el-dialog v-model="open" :title="title" width="520px" append-to-body :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
      <el-form ref="formRef" :model="form" :rules="rules" :disabled="saving" label-width="100px">
        <el-alert v-if="occupied" title="已占用工位不能改变房间或导师归属。停用、恢复请使用列表操作。" type="warning" :closable="false" class="mb20"/>
        <el-form-item label="房间" prop="roomId"><el-select v-model="form.roomId" :disabled="occupied" style="width:100%" @change="syncType"><el-option v-for="item in rooms" :key="item.roomId" :label="`${item.roomName}（${item.roomType==='STUDIO'?'固定':'流动'}）`" :value="item.roomId" :disabled="item.status !== 'NORMAL'"/></el-select></el-form-item>
        <template v-if="!form.workstationId">
          <el-form-item label="新增数量" prop="count"><el-input-number v-model="form.count" :min="1" :max="200" :precision="0" :step="1" step-strictly style="width:100%"/></el-form-item>
          <el-alert title="按现有最大数字编号顺延，自动生成布局，初始状态为空闲。固定工位创建后可再划分导师。" type="info" :closable="false"/>
        </template>
        <template v-else>
          <el-form-item label="工位号" prop="workstationNo"><el-input v-model="form.workstationNo"/></el-form-item>
          <el-form-item label="工位类型"><el-input :model-value="form.workstationType==='FIXED'?'固定工位':'流动工位'" disabled/><div class="el-form-item-msg">工位类型由所属房间自动确定</div></el-form-item>
          <el-form-item v-if="form.workstationType==='FIXED'" label="归属导师"><el-select v-model="form.mentorId" clearable :disabled="occupied" style="width:100%"><el-option v-for="m in mentors" :key="m.userId" :label="m.nickName || m.userName" :value="m.userId"/></el-select></el-form-item>
          <el-form-item label="布局行"><el-input-number v-model="form.layoutRow" :min="0"/></el-form-item><el-form-item label="布局列"><el-input-number v-model="form.layoutCol" :min="0"/></el-form-item>
          <el-form-item label="状态"><el-input :model-value="statusText(form.status)" disabled/></el-form-item>
        </template>
      </el-form>
      <template #footer><el-button type="primary" :loading="saving" :disabled="saving" @click="submitForm">{{form.workstationId?'保存':'创建工位'}}</el-button><el-button :disabled="saving" @click="open=false">取消</el-button></template>
    </el-dialog>
    <el-dialog v-model="batchOpen" title="批量修改导师归属" width="480px" append-to-body :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
      <el-alert title="仅适用于未占用的固定工位。编号、房间和布局请在单个工位中修改。" type="info" :closable="false" class="mb20"/>
      <el-form label-width="90px" :disabled="saving"><el-form-item label="修改范围">已选 {{ selectedRows.length }} 个工位</el-form-item><el-form-item label="归属导师"><el-select v-model="batchMentorId" clearable placeholder="留空表示解除导师归属" style="width:100%"><el-option v-for="m in mentors" :key="m.userId" :label="m.nickName || m.userName" :value="m.userId"/></el-select></el-form-item></el-form>
      <template #footer><el-button :disabled="saving" @click="batchOpen=false">取消</el-button><el-button type="primary" :loading="saving" :disabled="saving" @click="saveBatchEdit">确认修改</el-button></template>
    </el-dialog>
    <el-dialog v-model="penaltyOpen" title="停用处罚" width="520px" :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving"><el-alert :title="`已选 ${penaltyIds.length} 个工位，保留学生占用，到期自动恢复`" type="info" :closable="false" class="mb20"/><el-form label-width="100px" :disabled="saving"><el-form-item label="截止时间" required><el-date-picker v-model="penaltyUntil" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="北京时间"/></el-form-item><el-form-item label="处罚原因" required><el-input v-model="penaltyReason" type="textarea" :rows="4" maxlength="1000" show-word-limit/></el-form-item></el-form><template #footer><el-button :disabled="saving" @click="penaltyOpen=false">取消</el-button><el-button type="primary" :loading="saving" :disabled="saving" @click="submitPenalty">确认停用</el-button></template></el-dialog>
    <excel-import-dialog ref="importRef" title="工位批量导入" action="/gw/workstation/importData" template-action="/gw/workstation/importTemplate" template-file-name="workstation_template" :show-update-support="false" @success="getList" />
  </div>
</template>

<script setup name="GwWorkstation">
import { computed, reactive, ref } from 'vue'
import ExcelImportDialog from '@/components/ExcelImportDialog'
import request from '@/utils/request'
import { listRoom } from '@/api/gongwei/room'
import { assignWorkstationMentor, changeWorkstationStatus, createWorkstations, delWorkstation, listWorkstation, updateWorkstation } from '@/api/gongwei/workstation'
const { proxy } = getCurrentInstance()
const route=useRoute()
const mentors=ref([])
request.get('/gw/profile/options',{params:{roleKey:'mentor'}}).then(res=>mentors.value=res.data||[])
const selectedRows=ref([]), tableRef=ref(null), batchOpen=ref(false), batchMentorId=ref(null)
function selectionChange(rows){selectedRows.value=rows}
function selectedIds(){if(!selectedRows.value.length || selectedRows.value.length>200){proxy.$modal.msgWarning('请选择1至200个工位');return null}return selectedRows.value.map(x=>x.workstationId)}
function openBatchEdit(){if(saving.value || !selectedIds())return;if(selectedRows.value.some(x=>x.workstationType!=='FIXED' || !['AVAILABLE','DISABLED'].includes(x.status) || x.studentId)){proxy.$modal.msgWarning('请选择未占用的固定工位');return}batchMentorId.value=null;batchOpen.value=true}
async function saveBatchEdit(){
  if(saving.value)return
  const ids=selectedIds();if(!ids)return
  saving.value=true
  try{await proxy.$modal.confirm(`确认修改所选 ${ids.length} 个工位的导师归属？${batchMentorId.value?'':'将解除所选工位的导师归属。'}`);await assignWorkstationMentor({workstationIds:ids,mentorId:batchMentorId.value||null});batchOpen.value=false;proxy.$modal.msgSuccess('批量修改成功');await getList()}catch(e){}finally{saving.value=false}
}
async function batchStatus(status){
  if(saving.value)return
  const ids=selectedIds();if(!ids)return
  if(selectedRows.value.some(x=>x.status!==(status==='DISABLED'?'IN_USE':'DISABLED') || !x.studentId)){proxy.$modal.msgWarning(status==='DISABLED'?'只能停用已绑定学生的使用中工位':'只能恢复已绑定学生的停用工位');return}
  if(status==='DISABLED')return openPenalty(ids)
  const action=status==='DISABLED'?'停用':'恢复';saving.value=true
  try{await proxy.$modal.confirm(`确认${action}所选 ${ids.length} 个工位？${status==='DISABLED'?'停用期间保留学生与座位绑定，禁止使用且不能分配给其他人。':'恢复后学生可继续使用原座位。'}`);await changeWorkstationStatus({workstationIds:ids,status});proxy.$modal.msgSuccess('批量'+action+'成功');await getList()}catch(e){}finally{saving.value=false}
}
async function batchDelete(){
  if(saving.value)return
  const ids=selectedIds();if(!ids)return
  if(selectedRows.value.some(x=>x.status!=='AVAILABLE' || x.studentId)){proxy.$modal.msgWarning('只能删除未占用的空闲工位；使用中或停用工位请先完成收回');return}
  saving.value=true
  try{await proxy.$modal.confirm(`确认删除所选 ${ids.length} 个工位？删除后不可恢复。删除后不再展示或分配，已有历史记录仍保留。有未完成业务时整批不执行。`);await delWorkstation(ids.join(','));proxy.$modal.msgSuccess('批量删除成功');await getList()}catch(e){}finally{saving.value=false}
}
const penaltyOpen=ref(false),penaltyIds=ref([]),penaltyUntil=ref(''),penaltyReason=ref('')
function openPenalty(ids){penaltyIds.value=ids.slice();penaltyUntil.value='';penaltyReason.value='';penaltyOpen.value=true}
async function submitPenalty(){
 if(saving.value)return
 if(!penaltyUntil.value||new Date(penaltyUntil.value.replace(/-/g,'/')).getTime()<=Date.now())return proxy.$modal.msgWarning('请选择晚于当前时间的截止时间')
 if(!penaltyReason.value.trim())return proxy.$modal.msgWarning('请填写处罚原因')
 saving.value=true;try{await changeWorkstationStatus({workstationIds:penaltyIds.value,status:'DISABLED',until:penaltyUntil.value,reason:penaltyReason.value.trim()});penaltyOpen.value=false;proxy.$modal.msgSuccess('已设置停用处罚');await getList()}catch(e){}finally{saving.value=false}
}
const saving=ref(false)
const loading=ref(false), workstationList=ref([]), rooms=ref([]), open=ref(false), title=ref(''), form=ref({}), viewMode=ref('table')
const queryParams=reactive({roomId:undefined,workstationNo:undefined,workstationType:undefined,status:undefined})
if(route.query.roomId)queryParams.roomId=Number(route.query.roomId)
const rules={roomId:[{required:true,message:'请选择房间',trigger:'change'}],workstationNo:[{required:true,message:'工位号不能为空',trigger:'blur'}],count:[{validator:(_,value,callback)=>Number.isInteger(value)&&value>=1&&value<=200?callback():callback(new Error('请输入1至200的整数')),trigger:'change'}]}
const summary=computed(()=>({total:workstationList.value.length,available:workstationList.value.filter(x=>x.status==='AVAILABLE').length,inUse:workstationList.value.filter(x=>x.status==='IN_USE').length,pending:workstationList.value.filter(x=>x.status==='PENDING_RETURN').length}))
const occupied=computed(()=>['IN_USE','DISABLED','PENDING_RETURN'].includes(form.value.status))
const gridStyle=computed(()=>({gridTemplateColumns:`repeat(${Math.max(1,...workstationList.value.map(x=>Number(x.layoutCol)||1))}, minmax(100px,1fr))`}))
function stationStyle(item){return item.layoutRow>0&&item.layoutCol>0?{gridRow:item.layoutRow,gridColumn:item.layoutCol}:{}}
function statusText(s){return({AVAILABLE:'空闲',IN_USE:'使用中',PENDING_RETURN:'待回收',DISABLED:'停用'})[s]||s} function statusTag(s){return({AVAILABLE:'success',IN_USE:'warning',PENDING_RETURN:'danger',DISABLED:'info'})[s]||'info'}
function getList(){loading.value=true;return listWorkstation(queryParams).then(res=>{workstationList.value=res.data||res.rows||[];selectedRows.value=[];tableRef.value?.clearSelection()}).finally(()=>loading.value=false)}
function resetQuery(){Object.assign(queryParams,{roomId:undefined,workstationNo:undefined,workstationType:undefined,status:undefined});viewMode.value='table';getList()}
function loadRooms(){listRoom({}).then(res=>{rooms.value=res.data||res.rows||[]})}
function syncType(){const room=rooms.value.find(x=>x.roomId===form.value.roomId);form.value.workstationType=room&&room.roomType==='MOBILE'?'MOBILE':'FIXED'}
function handleAdd(){if(saving.value)return;form.value={roomId:queryParams.roomId,count:10};syncType();title.value='按数量新增工位';open.value=true;proxy.$nextTick(()=>proxy.$refs.formRef?.clearValidate())}
function handleImport(){proxy.$refs.importRef.open()}
function handleEdit(row){if(saving.value)return;form.value={...row};title.value='修改工位';open.value=true;proxy.$nextTick(()=>proxy.$refs.formRef?.clearValidate())}
async function changeStatus(row,status){
  if(saving.value)return
  if(status==='DISABLED')return openPenalty([row.workstationId])
  saving.value=true
  try{await proxy.$modal.confirm(status==='DISABLED'?'确认暂时停用该学生的工位？停用期间保留绑定和占用，座位不能分配给其他人。':'确认恢复该学生使用原工位？');await changeWorkstationStatus({workstationIds:[row.workstationId],status});proxy.$modal.msgSuccess(status==='DISABLED'?'已停用':'已恢复使用');await getList()}catch(e){}finally{saving.value=false}
}
function handleDelete(row){proxy.$modal.confirm(`删除后不再展示或分配，历史记录仍保留。是否删除“${row.workstationNo}”？`).then(()=>delWorkstation(row.workstationId)).then(()=>{proxy.$modal.msgSuccess('删除成功');getList()}).catch(()=>{})}
async function submitForm(){
  if(saving.value)return
  try{await proxy.$refs.formRef.validate()}catch(e){return}
  if(saving.value)return
  const editing=!!form.value.workstationId
  const payload=editing?{...form.value,mentorId:form.value.workstationType==='FIXED'?(form.value.mentorId||null):null}:{roomId:form.value.roomId,count:form.value.count}
  saving.value=true
  try{await(editing?updateWorkstation:createWorkstations)(payload);proxy.$modal.msgSuccess(editing?'保存成功':'已新增 '+payload.count+' 个工位');open.value=false;await getList()}catch(e){}finally{saving.value=false}
}
loadRooms();getList()
</script>

<style scoped>
.batch-toolbar{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin:12px 0}.batch-toolbar .el-button{margin-left:0}.summary-row{display:grid;grid-template-columns:repeat(4,minmax(120px,1fr));gap:14px;margin-bottom:16px}.summary{padding:16px 20px;border-radius:10px;background:#f2f3f5;display:flex;justify-content:space-between;align-items:end}.summary b{font-size:28px}.summary span{color:#606266}.summary.available{background:#f0f9eb}.summary.using{background:#fdf6ec}.summary.pending{background:#fef0f0}.layout-panel{min-height:300px;padding:18px;border:1px solid #ebeef5;border-radius:8px}.legend{display:flex;gap:20px;margin-bottom:18px}.legend span{display:flex;align-items:center;gap:6px}.dot{width:12px;height:12px;border-radius:50%}.available-bg{background:#67c23a}.using-bg{background:#e6a23c}.pending-bg{background:#f56c6c}.disabled-bg{background:#909399}.station-grid{display:grid;gap:12px;align-items:stretch}.station-card{min-height:92px;padding:12px;border-radius:8px;border:2px solid #dcdfe6;display:flex;flex-direction:column;cursor:pointer}.station-card b{font-size:17px}.station-card small,.station-card span,.station-card em{font-style:normal;font-size:12px;margin-top:4px}.station-card.available{border-color:#67c23a;background:#f0f9eb}.station-card.in_use{border-color:#e6a23c;background:#fdf6ec}.station-card.pending_return{border-color:#f56c6c;background:#fef0f0}.station-card.disabled{border-color:#909399;background:#f4f4f5;color:#909399}
</style>
