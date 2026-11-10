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

    <el-table v-if="viewMode === 'table'" v-loading="loading" :data="workstationList">
      <el-table-column label="编号" prop="workstationId" width="80"/><el-table-column label="房间" prop="roomName"/><el-table-column label="归属导师" prop="mentorName"/><el-table-column label="工位号" prop="workstationNo"/><el-table-column label="类型" width="90"><template #default="{row}">{{ row.workstationType === 'FIXED' ? '固定' : '流动' }}</template></el-table-column><el-table-column label="位置" width="110"><template #default="{row}">{{ row.layoutRow }} 行 / {{ row.layoutCol }} 列</template></el-table-column><el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column><el-table-column label="使用人" prop="studentName"/><el-table-column label="操作" width="230"><template #default="{row}"><el-button link type="primary" @click="handleEdit(row)" v-hasPermi="['gw:workstation:edit']">修改</el-button><el-button v-if="row.status === 'AVAILABLE'" link type="warning" @click="changeStatus(row,'DISABLED')" v-hasPermi="['gw:workstation:edit']">停用</el-button><el-button v-if="row.status === 'DISABLED'" link type="success" @click="changeStatus(row,'AVAILABLE')" v-hasPermi="['gw:workstation:edit']">恢复</el-button><el-button link type="danger" @click="handleDelete(row)" v-hasPermi="['gw:workstation:remove']">删除</el-button></template></el-table-column>
    </el-table>

    <div v-else v-loading="loading" class="layout-panel">
      <div class="legend"><span><i class="dot available-bg"/>空闲</span><span><i class="dot using-bg"/>使用中</span><span><i class="dot pending-bg"/>待回收</span><span><i class="dot disabled-bg"/>停用</span></div>
      <div class="station-grid" :style="gridStyle"><div v-for="item in workstationList" :key="item.workstationId" class="station-card" :class="item.status.toLowerCase()" :style="stationStyle(item)" @click="handleEdit(item)"><b>{{ item.workstationNo }}</b><small>{{ item.workstationType === 'FIXED' ? '固定' : '流动' }}</small><span>{{ statusText(item.status) }}</span><em v-if="item.studentName">{{ item.studentName }}</em></div></div>
    </div>

    <el-dialog v-model="open" :title="title" width="520px" append-to-body><el-form ref="formRef" :model="form" :rules="rules" label-width="100px"><el-alert v-if="occupied" title="使用中或待回收工位只能调整布局信息，不能直接改变房间或状态。" type="warning" :closable="false" class="mb20"/><el-form-item label="房间" prop="roomId"><el-select v-model="form.roomId" :disabled="occupied" style="width:100%" @change="syncType"><el-option v-for="item in rooms" :key="item.roomId" :label="`${item.roomName}（${item.roomType==='STUDIO'?'固定':'流动'}）`" :value="item.roomId"/></el-select></el-form-item><el-form-item label="工位号" prop="workstationNo"><el-input v-model="form.workstationNo"/></el-form-item><el-form-item label="工位类型"><el-input :model-value="form.workstationType==='FIXED'?'固定工位':'流动工位'" disabled/><div class="el-form-item-msg">工位类型由所属房间自动确定</div></el-form-item><el-form-item v-if="form.workstationType==='FIXED'" label="归属导师"><el-select v-model="form.mentorId" clearable :disabled="occupied" style="width:100%"><el-option v-for="m in mentors" :key="m.userId" :label="m.nickName || m.userName" :value="m.userId"/></el-select></el-form-item><el-form-item label="布局行"><el-input-number v-model="form.layoutRow" :min="0"/></el-form-item><el-form-item label="布局列"><el-input-number v-model="form.layoutCol" :min="0"/></el-form-item><el-form-item label="状态"><el-select v-model="form.status" :disabled="occupied" style="width:100%"><el-option label="可用" value="AVAILABLE"/><el-option label="停用" value="DISABLED"/></el-select></el-form-item></el-form><template #footer><el-button type="primary" @click="submitForm">确定</el-button><el-button @click="open=false">取消</el-button></template></el-dialog>
    <excel-import-dialog ref="importRef" title="工位批量导入" action="/gw/workstation/importData" template-action="/gw/workstation/importTemplate" template-file-name="workstation_template" :show-update-support="false" @success="getList" />
  </div>
</template>

<script setup name="GwWorkstation">
import { computed, reactive, ref } from 'vue'
import ExcelImportDialog from '@/components/ExcelImportDialog'
import request from '@/utils/request'
import { listRoom } from '@/api/gongwei/room'
import { addWorkstation, delWorkstation, listWorkstation, updateWorkstation } from '@/api/gongwei/workstation'
const { proxy } = getCurrentInstance()
const route=useRoute()
const mentors=ref([])
request.get('/gw/profile/options',{params:{roleKey:'mentor'}}).then(res=>mentors.value=res.data||[])
const loading=ref(false), workstationList=ref([]), rooms=ref([]), open=ref(false), title=ref(''), form=ref({}), viewMode=ref('table')
const queryParams=reactive({roomId:undefined,workstationNo:undefined,workstationType:undefined,status:undefined})
if(route.query.roomId)queryParams.roomId=Number(route.query.roomId)
const rules={roomId:[{required:true,message:'请选择房间',trigger:'change'}],workstationNo:[{required:true,message:'工位号不能为空',trigger:'blur'}]}
const summary=computed(()=>({total:workstationList.value.length,available:workstationList.value.filter(x=>x.status==='AVAILABLE').length,inUse:workstationList.value.filter(x=>x.status==='IN_USE').length,pending:workstationList.value.filter(x=>x.status==='PENDING_RETURN').length}))
const occupied=computed(()=>['IN_USE','PENDING_RETURN'].includes(form.value.status))
const gridStyle=computed(()=>({gridTemplateColumns:`repeat(${Math.max(1,...workstationList.value.map(x=>Number(x.layoutCol)||1))}, minmax(100px,1fr))`}))
function stationStyle(item){return item.layoutRow>0&&item.layoutCol>0?{gridRow:item.layoutRow,gridColumn:item.layoutCol}:{}}
function statusText(s){return({AVAILABLE:'空闲',IN_USE:'使用中',PENDING_RETURN:'待回收',DISABLED:'停用'})[s]||s} function statusTag(s){return({AVAILABLE:'success',IN_USE:'warning',PENDING_RETURN:'danger',DISABLED:'info'})[s]||'info'}
function getList(){loading.value=true;listWorkstation(queryParams).then(res=>{workstationList.value=res.data||res.rows||[]}).finally(()=>loading.value=false)}
function resetQuery(){Object.assign(queryParams,{roomId:undefined,workstationNo:undefined,workstationType:undefined,status:undefined});viewMode.value='table';getList()}
function loadRooms(){listRoom({}).then(res=>{rooms.value=res.data||res.rows||[]})}
function syncType(){const room=rooms.value.find(x=>x.roomId===form.value.roomId);form.value.workstationType=room&&room.roomType==='MOBILE'?'MOBILE':'FIXED'}
function handleAdd(){form.value={roomId:queryParams.roomId,status:'AVAILABLE',layoutRow:0,layoutCol:0};syncType();title.value='新增工位';open.value=true}
function handleImport(){proxy.$refs.importRef.open()}
function handleEdit(row){form.value={...row};title.value='修改工位';open.value=true}
function changeStatus(row,status){updateWorkstation({...row,status}).then(()=>{proxy.$modal.msgSuccess(status==='DISABLED'?'已停用':'已恢复');getList()})}
function handleDelete(row){proxy.$modal.confirm(`仅无占用、无未结业务的空闲工位可删除。是否删除“${row.workstationNo}”？`).then(()=>delWorkstation(row.workstationId)).then(()=>{proxy.$modal.msgSuccess('删除成功');getList()}).catch(()=>{})}
function submitForm(){form.value.mentorId=form.value.workstationType==='FIXED'?(form.value.mentorId||null):null;proxy.$refs.formRef.validate(valid=>{if(!valid)return;(form.value.workstationId?updateWorkstation:addWorkstation)(form.value).then(()=>{proxy.$modal.msgSuccess('保存成功');open.value=false;getList()})})}
loadRooms();getList()
</script>

<style scoped>
.summary-row{display:grid;grid-template-columns:repeat(4,minmax(120px,1fr));gap:14px;margin-bottom:16px}.summary{padding:16px 20px;border-radius:10px;background:#f2f3f5;display:flex;justify-content:space-between;align-items:end}.summary b{font-size:28px}.summary span{color:#606266}.summary.available{background:#f0f9eb}.summary.using{background:#fdf6ec}.summary.pending{background:#fef0f0}.layout-panel{min-height:300px;padding:18px;border:1px solid #ebeef5;border-radius:8px}.legend{display:flex;gap:20px;margin-bottom:18px}.legend span{display:flex;align-items:center;gap:6px}.dot{width:12px;height:12px;border-radius:50%}.available-bg{background:#67c23a}.using-bg{background:#e6a23c}.pending-bg{background:#f56c6c}.disabled-bg{background:#909399}.station-grid{display:grid;gap:12px;align-items:stretch}.station-card{min-height:92px;padding:12px;border-radius:8px;border:2px solid #dcdfe6;display:flex;flex-direction:column;cursor:pointer}.station-card b{font-size:17px}.station-card small,.station-card span,.station-card em{font-style:normal;font-size:12px;margin-top:4px}.station-card.available{border-color:#67c23a;background:#f0f9eb}.station-card.in_use{border-color:#e6a23c;background:#fdf6ec}.station-card.pending_return{border-color:#f56c6c;background:#fef0f0}.station-card.disabled{border-color:#909399;background:#f4f4f5;color:#909399}
</style>
