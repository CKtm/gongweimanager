<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8" @submit.prevent>
      <el-form-item label="用户ID"><el-input v-model="query.userId" clearable placeholder="用户ID" style="width: 140px" /></el-form-item>
      <el-form-item label="身份类型"><el-select v-model="query.userType" clearable placeholder="全部" style="width: 160px"><el-option label="学生" value="student" /><el-option label="导师" value="mentor" /><el-option label="检查员" value="inspector" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button><el-button type="success" @click="openAdd" v-hasPermi="['gw:profile:edit']">新增档案</el-button><el-button type="warning" @click="openRelation()" v-hasPermi="['gw:profile:edit']">绑定师生关系</el-button><el-button type="info" icon="Upload" @click="openImport" v-hasPermi="['gw:profile:edit']">批量初始化账号</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column prop="userName" label="登录账号" width="140" />
      <el-table-column prop="nickName" label="姓名" width="120" />
      <el-table-column prop="identityNo" label="学号/工号" width="160" />
      <el-table-column prop="userType" label="身份类型" width="120"><template #default="{ row }">{{ typeName(row.userType) }}</template></el-table-column>
      <el-table-column label="导师" min-width="150"><template #default="{ row }"><span v-if="row.mentorId">{{ row.mentorName || '用户' + row.mentorId }}（{{ row.mentorId }}）</span><span v-else-if="String(row.userType).toLowerCase() === 'student'" class="text-muted">未绑定</span><span v-else>-</span></template></el-table-column>
      <el-table-column label="操作" width="220"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button v-if="String(row.userType).toLowerCase() === 'student'" link type="warning" @click="openRelation(row)">绑定导师</el-button><el-button v-if="row.mentorId" link type="danger" @click="unbind(row)">解绑</el-button></template></el-table-column>
    </el-table>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="460px">
      <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="100px">
        <el-form-item label="用户ID" prop="userId"><el-input v-model="dialog.form.userId" :disabled="dialog.edit" /></el-form-item>
        <el-form-item label="身份编号" prop="identityNo"><el-input v-model="dialog.form.identityNo" placeholder="学号或工号" /></el-form-item>
        <el-form-item label="身份类型" prop="userType"><el-select v-model="dialog.form.userType" style="width: 100%"><el-option label="学生" value="student" /><el-option label="导师" value="mentor" /><el-option label="检查员" value="inspector" /></el-select></el-form-item>
        <el-form-item v-if="String(dialog.form.userType || '').toLowerCase() === 'student'" label="导师"><el-select v-model="dialog.form.mentorId" clearable filterable placeholder="可暂不绑定" style="width: 100%"><el-option v-for="item in mentorOptions" :key="item.userId" :label="userLabel(item)" :value="item.userId" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog.visible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="relation.visible" title="绑定师生关系" width="480px">
      <el-alert v-if="!mentorOptions.length" title="当前没有可选导师，请先在用户管理中创建账号并分配“导师”角色。" type="warning" :closable="false" class="mb20" />
      <el-form ref="relationRef" :model="relation.form" :rules="relationRules" label-width="90px">
        <el-form-item label="学生" prop="studentId"><el-select v-model="relation.form.studentId" filterable placeholder="选择学生账号" style="width: 100%"><el-option v-for="item in studentOptions" :key="item.userId" :label="userLabel(item)" :value="item.userId" /></el-select></el-form-item>
        <el-form-item label="导师" prop="mentorId"><el-select v-model="relation.form.mentorId" filterable placeholder="选择导师账号" style="width: 100%"><el-option v-for="item in mentorOptions" :key="item.userId" :label="userLabel(item)" :value="item.userId" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="relation.visible = false">取消</el-button><el-button type="primary" :disabled="!mentorOptions.length" @click="bind">确认绑定</el-button></template>
    </el-dialog>
    <excel-import-dialog ref="importRef" title="人员账号与师生关系导入" action="/gw/profile/importData" template-action="/gw/profile/importTemplate" template-file-name="person_account_template" :show-update-support="false" @success="load" />
  </div>
</template>

<script setup>
import { getCurrentInstance, reactive, ref } from 'vue'
import ExcelImportDialog from '@/components/ExcelImportDialog'
import { bindMentorRelation, listProfile, listProfileOptions, saveProfile, unbindMentorRelation } from '@/api/gongwei/profile'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const list = ref([])
const studentOptions = ref([])
const mentorOptions = ref([])
const query = reactive({ userId: undefined, userType: undefined })
const dialog = reactive({ visible: false, title: '', edit: false, form: {} })
const relation = reactive({ visible: false, form: { studentId: undefined, mentorId: undefined } })
const rules = { userId: [{ required: true, message: '请输入用户ID' }], userType: [{ required: true, message: '请选择身份类型' }] }
const relationRules = { studentId: [{ required: true, message: '请选择学生' }], mentorId: [{ required: true, message: '请选择导师' }] }

function typeName(type) { return ({ student: '学生', mentor: '导师', inspector: '检查员' })[String(type || '').toLowerCase()] || type }
function userLabel(item) { return `${item.nickName || item.userName}（${item.userName} / ID ${item.userId}）` }
function loadOptions() { return Promise.all([listProfileOptions('common'), listProfileOptions('mentor')]).then(([students, mentors]) => { studentOptions.value = students.data || []; mentorOptions.value = mentors.data || [] }) }
function load() { loading.value = true; Promise.all([listProfile(query), loadOptions()]).then(([res]) => { list.value = res.data || [] }).finally(() => { loading.value = false }) }
function reset() { query.userId = undefined; query.userType = undefined; load() }
function openAdd() { dialog.edit = false; dialog.title = '新增师生档案'; dialog.form = { userId: '', identityNo: '', userType: 'student', mentorId: '' }; dialog.visible = true }
function openEdit(row) { dialog.edit = true; dialog.title = '编辑师生档案'; dialog.form = { userId: row.userId, identityNo: row.identityNo, userType: row.userType, mentorId: row.mentorId }; dialog.visible = true }
function save() { proxy.$refs.formRef.validate(valid => { if (!valid) return; saveProfile(dialog.form).then(() => { proxy.$modal.msgSuccess('保存成功'); dialog.visible = false; load() }) }) }
function openRelation(row) { relation.form = { studentId: row?.userId, mentorId: row?.mentorId }; relation.visible = true }
function openImport() { proxy.$refs.importRef.open() }
function bind() { proxy.$refs.relationRef.validate(valid => { if (!valid) return; bindMentorRelation(relation.form).then(() => { proxy.$modal.msgSuccess('师生关系绑定成功'); relation.visible = false; load() }) }) }
function unbind(row) { proxy.$modal.confirm(`确认解除 ${row.nickName || row.userName} 与当前导师的关系吗？`).then(() => unbindMentorRelation(row.userId)).then(() => { proxy.$modal.msgSuccess('已解除绑定'); load() }).catch(() => {}) }
load()
</script>

<style scoped>
.text-muted { color: #909399; }
</style>
