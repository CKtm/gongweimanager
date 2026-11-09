<template>
  <div class="login">
    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">{{ title }}</h3>
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          size="large"
          auto-complete="off"
          placeholder="账号"
        >
          <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          size="large"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          size="large"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img"/>
        </div>
      </el-form-item>
      <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="large"
          type="primary"
          style="width:100%;"
          @click.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
    </el-form>

    <el-dialog
      v-model="forceDialog"
      title="首次登录必须修改密码"
      width="480px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <el-alert title="当前账号尚未修改初始密码，完成改密前不会登录系统。改密票据5分钟内有效。" type="warning" :closable="false" class="mb20" />
      <el-form ref="forcePwdRef" :model="forceForm" :rules="forceRules" label-width="90px">
        <el-form-item label="登录账号"><el-input :model-value="forceForm.username" disabled /></el-form-item>
        <el-form-item label="新密码" prop="newPassword" :rules="infoPwdValidator">
          <el-input v-model="forceForm.newPassword" type="password" show-password autocomplete="new-password" placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="forceForm.confirmPassword" type="password" show-password autocomplete="new-password" placeholder="请再次输入新密码" @keyup.enter="submitForcePassword" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelForceChange">返回登录</el-button>
        <el-button type="primary" :loading="resetLoading" @click="submitForcePassword">确认修改</el-button>
      </template>
    </el-dialog>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg, forceResetPassword } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import { usePasswordRule } from "@/utils/passwordRule"
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const forceDialog = ref(false)
const resetLoading = ref(false)
const forceForm = reactive({ username: '', resetToken: '', newPassword: '', confirmPassword: '' })
const { infoPwdValidator, pwdChrType } = usePasswordRule()
const equalToPassword = (rule, value, callback) => {
  if (forceForm.newPassword !== value) callback(new Error('两次输入的密码不一致'))
  else callback()
}
const forceRules = {
  confirmPassword: [
    { required: true, message: '确认密码不能为空', trigger: 'blur' },
    { validator: equalToPassword, trigger: 'blur' }
  ]
}

const codeUrl = ref("")
const loading = ref(false)
// 验证码开关
const captchaEnabled = ref(true)
// 注册开关
const register = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      // 调用action的登录方法
      userStore.login(loginForm.value).then(() => {
        saveRememberedCredentials()
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(error => {
        loading.value = false
        if (error && error.code === 602 && error.data && error.data.forcePasswordChange) {
          openForceChange(error.data)
          return
        }
        // 重新获取验证码
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function saveRememberedCredentials() {
  if (loginForm.value.rememberMe) {
    Cookies.set("username", loginForm.value.username, { expires: 30 })
    Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
    Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
  } else {
    Cookies.remove("username")
    Cookies.remove("password")
    Cookies.remove("rememberMe")
  }
}

function openForceChange(data) {
  Cookies.remove("password")
  Cookies.remove("rememberMe")
  pwdChrType.value = String(data.pwdChrtype ?? '0')
  Object.assign(forceForm, {
    username: loginForm.value.username,
    resetToken: data.resetToken,
    newPassword: '',
    confirmPassword: ''
  })
  forceDialog.value = true
}

function submitForcePassword() {
  proxy.$refs.forcePwdRef.validate(valid => {
    if (!valid) return
    if (forceForm.newPassword === loginForm.value.password) {
      return proxy.$modal.msgWarning('新密码不能与初始密码相同')
    }
    resetLoading.value = true
    forceResetPassword(forceForm.resetToken, forceForm.newPassword).then(() => {
      proxy.$modal.msgSuccess('密码修改成功，请使用新密码重新登录')
      forceDialog.value = false
      loginForm.value.password = ''
      Object.assign(forceForm, { resetToken: '', newPassword: '', confirmPassword: '' })
      getCode()
    }).finally(() => { resetLoading.value = false })
  })
}

function cancelForceChange() {
  forceDialog.value = false
  loginForm.value.password = ''
  Object.assign(forceForm, { resetToken: '', newPassword: '', confirmPassword: '' })
  getCode()
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()
</script>

<style lang='scss' scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
}
.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}

.login-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
  z-index: 1;
  .el-input {
    height: 40px;
    input {
      height: 40px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 0px;
  }
}
.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.login-code {
  width: 33%;
  height: 40px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.login-code-img {
  height: 40px;
  padding-left: 12px;
}

html.dark .login {
  background-image: linear-gradient(rgba(0, 0, 0, 0.55), rgba(0, 0, 0, 0.55)), url("../assets/images/login-background.jpg");
  .login-form {
    background: var(--el-bg-color-overlay) !important;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5);
  }
}
</style>
