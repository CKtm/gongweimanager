const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert')
const root=path.resolve(__dirname,'..')
function setup(roles=['common'],profile={userName:'student',nickName:'测试学生',identityNo:'S001',userType:'STUDENT',mentorId:10,mentorName:'导师甲'}){
 let p,token='token',failure=false,modal,actionSheet,stopped=0,requests=[],routes=[],removed=[]
 const user={roles,userName:'student',nickName:'测试学生',avatar:'/profile/avatar.png'}
 const wx={getStorageSync:key=>key==='token'?token:user,getWindowInfo:()=>({statusBarHeight:44,windowWidth:393}),getMenuButtonBoundingClientRect:()=>({left:296,top:52,height:32}),
  redirectTo:x=>routes.push(x.url),navigateTo:x=>routes.push(x.url),reLaunch:x=>routes.push(x.url),removeStorageSync:key=>removed.push(key),stopPullDownRefresh:()=>stopped++,
  pageScrollTo:x=>routes.push(x.selector),showModal:x=>{modal=x},showActionSheet:x=>{actionSheet=x},showToast(){}}
 const request={get:async url=>{requests.push({method:'get',url});if(failure)throw new Error('offline');return {data:profile}},post:async url=>{requests.push({method:'post',url});if(failure)throw new Error('offline');return {code:200}}}
 vm.runInNewContext(fs.readFileSync(path.join(root,'pages/profile/profile.js'),'utf8'),{Page:x=>p=x,require:name=>name.endsWith('/config')?{baseUrl:'http://localhost:9090'}:request,wx})
 p.data=JSON.parse(JSON.stringify(p.data));p.setData=function(value,cb){Object.assign(this.data,value);if(cb)cb()}
 return {page:p,user,requests,routes,removed,modal:()=>modal,sheet:()=>actionSheet,stopped:()=>stopped,fail:value=>failure=value,clearToken:()=>token=''}
}
const tick=()=>new Promise(r=>setImmediate(r))
;(async()=>{
 const t=setup(),p=t.page;p.onLoad();assert.equal(p.data.statusBarHeight,44);assert.equal(p.data.capsuleInset,105)
 await p.onShow();assert.equal(p.data.completed,5);assert.equal(p.data.totalFields,5);assert.equal(p.data.progress,100);assert.equal(p.data.missing,0);assert.equal(p.data.roleCount,1)
 assert.equal(p.data.avatarUrl,'http://localhost:9090/profile/avatar.png');p.onAvatarError();assert.equal(p.data.avatarUrl,'');assert.equal(p.data.avatar,'测')
 p.editAccount();assert.equal(t.routes.at(-1),'/pages/account/account');p.changePassword();assert.equal(t.routes.at(-1),'/pages/password/password')
 p.openIdentity();assert(t.modal().content.includes('导师甲'));assert(t.modal().content.includes('管理员维护'))
 p.openBusiness();assert.equal(t.routes.at(-1),'/pages/myworkstation/myworkstation');p.openSettings();assert.equal(t.routes.at(-1),'#account-settings')
 t.fail(true);await p.onPullDownRefresh();assert(p.data.error);assert.equal(p.data.loading,false);assert.equal(t.stopped(),1)
 t.fail(false);await p.load();assert.equal(p.data.error,'')
 p.logout();p.logout();assert(p.data.confirmingLogout);t.modal().success({confirm:false});await tick();assert.equal(t.removed.length,0);assert(!t.requests.some(r=>r.method==='post'))
 p.logout();t.modal().fail();assert.equal(p.data.confirmingLogout,false)
 t.fail(true);p.logout();t.modal().success({confirm:true});await tick();assert(t.requests.some(r=>r.url==='/logout'));assert.deepEqual(t.removed,['token','userInfo']);assert.equal(t.routes.at(-1),'/pages/login/login')
 const unbound=setup(['common'],{userName:'student',nickName:'测试学生',identityNo:'S001',userType:'STUDENT'});await unbound.page.onShow();assert.equal(unbound.page.data.progress,80);assert.equal(unbound.page.data.missing,1)
 const mentor=setup(['mentor'],{userName:'mentor',nickName:'导师',identityNo:'M001',userType:'MENTOR'});await mentor.page.onShow();assert(!mentor.page.data.isStudent);assert.equal(mentor.page.data.totalFields,4);assert.equal(mentor.page.data.progress,100);mentor.page.openBusiness();assert.equal(mentor.routes.at(-1),'/pages/mentor/mentor')
 const inspector=setup(['inspector'],{userType:'INSPECTOR'});await inspector.page.onShow();inspector.page.openBusiness();assert.equal(inspector.routes.at(-1),'/pages/inspection/inspection')
 const admin=setup(['college_admin'],{});await admin.page.onShow();admin.page.openBusiness();assert.equal(admin.routes.at(-1),'/pages/admin/admin')
 const multi=setup(['common','mentor','inspector','admin']);await multi.page.onShow();multi.page.openBusiness();assert.equal(multi.sheet().itemList.length,4);multi.sheet().success({tapIndex:2});assert.equal(multi.routes.at(-1),'/pages/inspection/inspection')
 const out=setup();out.clearToken();await out.page.onShow();assert.equal(out.requests.length,0);assert.equal(out.routes[0],'/pages/login/login');await out.page.onPullDownRefresh();assert.equal(out.stopped(),1)
 const wxml=fs.readFileSync(path.join(root,'pages/profile/profile.wxml'),'utf8');for(const m of wxml.matchAll(/(?:bind\w+|catch\w+)="(\w+)"/g))assert.equal(typeof p[m[1]],'function','Missing handler: '+m[1])
 const config=JSON.parse(fs.readFileSync(path.join(root,'pages/profile/profile.json'),'utf8'));assert(config.enablePullDownRefresh);assert.equal(config.navigationStyle,'custom')
 assert(!wxml.includes('figma.com/api/mcp/asset/'));assert(!wxml.includes('Payment'));assert(wxml.includes('appearance="figma"'))
 const dimensions={settings:60,chevron:60,dot:18,'nav-profile-active':72,business:48,account:48,identity:48};for(const [name,size]of Object.entries(dimensions)){const image=fs.readFileSync(path.join(root,'assets/profile',name+'.png'));assert.equal(image.subarray(1,4).toString(),'PNG');assert.equal(image.readUInt32BE(16),size);assert.equal(image.readUInt32BE(20),size)}
 assert(t.requests.filter(r=>r.method==='get').every(r=>r.url==='/gw/profile/me'))
 console.log('PROFILE_OK: existing endpoint, accurate completion metrics, read-only identity/mentor, avatar fallback, four-role navigation, multi-role picker, refresh failures, logout confirmation/cancel/failure, safe header, event bindings and PNG assets.')
})().catch(error=>{console.error(error);process.exit(1)})
