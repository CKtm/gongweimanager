<template>
  <div class="app-container">
    <el-alert title="房间数量和工位数量均由台账动态维护；工作室只能创建固定工位，流动房间只能创建流动工位。" type="info" :closable="false" class="mb20" />
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="房间名称"><el-input v-model="queryParams.roomName" clearable @keyup.enter="getList" /></el-form-item>
      <el-form-item label="类型"><el-select v-model="queryParams.roomType" clearable style="width:170px"><el-option label="工作室" value="STUDIO"/><el-option label="流动工位房间" value="MOBILE"/></el-select></el-form-item>
      <el-form-item label="状态"><el-select v-model="queryParams.status" clearable style="width:140px"><el-option label="正常" value="NORMAL"/><el-option label="维修中" value="MAINTENANCE"/><el-option label="停用" value="DISABLED"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="getList">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['gw:room:add']">新增房间</el-button></el-col></el-row>
    <el-table v-loading="loading" :data="roomList">
      <el-table-column label="编号" prop="roomId" width="75"/>
      <el-table-column label="房间名称" prop="roomName" min-width="120"/>
      <el-table-column label="类型" width="125"><template #default="{row}"><el-tag :type="row.roomType==='STUDIO'?'primary':'success'">{{ roomTypeText(row.roomType) }}</el-tag></template></el-table-column>
      <el-table-column label="位置" prop="location" min-width="130"/>
      <el-table-column label="归属导师" min-width="110"><template #default="{row}"><span v-if="row.roomType==='STUDIO'">{{ row.mentorName || '待申请' }}</span><span v-else>-</span></template></el-table-column>
      <el-table-column label="真实工位" width="210"><template #default="{row}"><span>总数 {{ row.workstationCount || 0 }}</span><el-divider direction="vertical"/><span class="available">空闲 {{ row.availableCount || 0 }}</span><el-divider direction="vertical"/><span>占用 {{ row.inUseCount || 0 }}</span></template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="备注" prop="remark" show-overflow-tooltip/>
      <el-table-column label="操作" width="220" fixed="right"><template #default="{row}"><el-button link type="success" @click="manageStations(row)">管理工位</el-button><el-button link type="primary" @click="handleEdit(row)" v-hasPermi="['gw:room:edit']">修改</el-button><el-button link type="danger" @click="handleDelete(row)" v-hasPermi="['gw:room:remove']">删除</el-button></template></el-table-column>
    </el-table>

    <el-dialog v-model="open" :title="title" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="房间名称" prop="roomName"><el-input v-model="form.roomName" maxlength="100"/></el-form-item>
        <el-form-item label="房间类型" prop="roomType"><el-radio-group v-model="form.roomType"><el-radio value="STUDIO">工作室（固定工位）</el-radio><el-radio value="MOBILE">流动工位房间</el-radio></el-radio-group></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location" maxlength="255" placeholder="楼栋、楼层、房间位置"/></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio value="NORMAL">正常</el-radio><el-radio value="MAINTENANCE">维修中</el-radio><el-radio value="DISABLED">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item v-if="form.mentorName" label="当前导师"><el-input :model-value="form.mentorName" disabled/></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3"/></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="submitForm">确定</el-button><el-button @click="open=false">取消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="GwRoom">
import { listRoom, addRoom, updateRoom, delRoom } from '@/api/gongwei/room'
const { proxy }=getCurrentInstance(); const loading=ref(false), roomList=ref([]), open=ref(false), title=ref(''), form=ref({})
const router=useRouter()
const queryParams=reactive({roomName:undefined,roomType:undefined,status:undefined})
const rules={roomName:[{required:true,message:'房间名称不能为空',trigger:'blur'}],roomType:[{required:true,message:'请选择房间类型',trigger:'change'}]}
function roomTypeText(v){return v==='MOBILE'?'流动工位房间':'工作室'}
function statusText(v){return({NORMAL:'正常',MAINTENANCE:'维修中',DISABLED:'停用'})[v]||v}
function statusTag(v){return({NORMAL:'success',MAINTENANCE:'warning',DISABLED:'info'})[v]||'info'}
function getList(){loading.value=true;listRoom(queryParams).then(res=>roomList.value=res.data||res.rows||[]).finally(()=>loading.value=false)}
function resetQuery(){Object.assign(queryParams,{roomName:undefined,roomType:undefined,status:undefined});getList()}
function handleAdd(){form.value={roomType:'STUDIO',status:'NORMAL'};title.value='新增房间';open.value=true}
function handleEdit(row){form.value={...row};title.value='修改房间';open.value=true}
function handleDelete(row){proxy.$modal.confirm(`只有无工位、无申请历史且未绑定导师的房间才能删除。是否删除“${row.roomName}”？`).then(()=>delRoom(row.roomId)).then(()=>{proxy.$modal.msgSuccess('删除成功');getList()}).catch(()=>{})}
function manageStations(row){router.push({path:'/workstation/workstation',query:{roomId:row.roomId}})}
function submitForm(){proxy.$refs.formRef.validate(valid=>{if(!valid)return;(form.value.roomId?updateRoom:addRoom)(form.value).then(()=>{proxy.$modal.msgSuccess('保存成功');open.value=false;getList()})})}
getList()
</script>

<style scoped>.available{color:var(--el-color-success)}</style>
