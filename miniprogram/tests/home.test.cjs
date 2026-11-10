const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert')
const root=path.resolve(__dirname,'..')
function pageFor(roles,answers={},permissions=['*:*:*']){
 let page,token='token',fail=false,stopped=0,requests=[],routes=[]
 const user={roles,permissions,nickName:'测试用户',userName:'test',avatar:'/profile/avatar.png'}
 const wx={getStorageSync:key=>key==='token'?token:user,redirectTo:x=>routes.push(x.url),navigateTo:x=>routes.push(x.url),reLaunch:x=>routes.push(x.url),stopPullDownRefresh:()=>stopped++,removeStorageSync(){}}
 const request={get:async(url,data)=>{requests.push({url,data});if(fail)throw new Error('offline');return {data:answers[url]||[]}},post:async()=>({code:200})}
 const admin={can:(u,p)=>u.roles.includes('admin')||u.permissions.includes('*:*:*')||u.permissions.includes(p)}
 vm.runInNewContext(fs.readFileSync(path.join(root,'pages/index/index.js'),'utf8'),{Page:x=>page=x,wx,require:name=>name.endsWith('/config')?{baseUrl:'http://localhost:9090'}:name.endsWith('/admin')?admin:request})
 page.data=JSON.parse(JSON.stringify(page.data));page.setData=function(value,cb){Object.assign(this.data,value);if(cb)cb()}
 return {page,requests,routes,setFail:x=>fail=x,clearToken:()=>token='',stopped:()=>stopped}
}
const keyEvent=key=>({currentTarget:{dataset:{key}}})
;(async()=>{
 const student=pageFor(['common'],{'/gw/assignment/my':[{roomName:'421',workstationNo:'A1',workstationType:'FIXED',termCode:'2026-11-10~2027-01-01'}],'/gw/application/list':[{status:'PENDING_MENTOR'},{status:'COMPLETED'}]})
 let p=student.page;await p.onShow();assert.equal(p.data.avatarUrl,'http://localhost:9090/profile/avatar.png');assert.equal(p.data.pendingCount,1);assert.equal(p.data.services.length,4)
 assert(!p.data.services.some(s=>s.role==='isAdmin'||s.role==='isMentor'));assert(p.data.nextCards[0].description.includes('2026-11-10 至 2027-01-01'))
 p.onAvatarError();assert.equal(p.data.avatarUrl,'');assert.equal(p.data.avatarInitial,'测')
 p.onCategory(keyEvent('review'));assert(p.data.visibleServices.every(s=>s.category==='review'));p.onCategory(keyEvent('not-a-category'));assert.equal(p.data.activeCategory,'review')
 p.resetServiceFilters();p.onSearch({detail:{value:'安全'}});assert.equal(p.data.visibleServices[0].key,'profile');p.clearSearch();assert.equal(p.data.visibleServices.length,4)
 p.onSearch({detail:{value:'没有此服务'}});assert.equal(p.data.visibleServices.length,0);p.resetServiceFilters()
 p.openAllServices();assert(p.data.allServicesOpen);p.openService(keyEvent('student-apply'));assert(student.routes.at(-1).includes('/applications/applications'));assert(!p.data.allServicesOpen)
 const n=student.routes.length;p.openService(keyEvent('admin-settings'));assert.equal(student.routes.length,n)
 p.openNextCard(keyEvent('student-workspace'));assert(student.routes.at(-1).includes('/myworkstation/myworkstation'))
 p.openNotifications();assert(p.data.notificationsOpen);p.openPendingItem(keyEvent('student-applications'));assert(!p.data.notificationsOpen)
 student.setFail(true);await p.onPullDownRefresh();assert(p.data.error);assert.equal(p.data.loading,false);assert.equal(student.stopped(),1)
 student.setFail(false);await p.refreshDashboard();assert.equal(p.data.error,'')
 const mentor=pageFor(['mentor'],{'/gw/application/list':[{},{}],'/gw/return/list':[{}]});await mentor.page.onShow();assert.equal(mentor.page.data.pendingCount,3);assert(mentor.page.data.services.some(s=>s.key==='mentor-seats'))
 mentor.page.openPendingItem(keyEvent('mentor-pending'));assert(mentor.routes.at(-1).endsWith('mentor?tab=review'))
 const inspector=pageFor(['inspector'],{'/gw/inspection/task/list':[{status:'PENDING'},{status:'IN_PROGRESS'},{status:'VERIFIED'}]});await inspector.page.onShow();assert.equal(inspector.page.data.pendingCount,2);assert(!inspector.requests.some(r=>r.url==='/gw/application/list'));inspector.page.openNextCard(keyEvent('inspector-pending'));assert(inspector.routes.at(-1).includes('/inspection/inspection'))
 const admin=pageFor(['college_admin'],{'/gw/application/list':[{status:'PENDING_ADMIN'},{status:'WAIT_ALLOC'}],'/gw/return/list':[{status:'PENDING_ADMIN'}],'/gw/inspection/record/list':[{status:'SUBMITTED',taskStatus:'SUBMITTED'}]});await admin.page.onShow();assert.equal(admin.page.data.pendingCount,4);admin.page.openService(keyEvent('admin-seats'));assert(admin.routes.at(-1).endsWith('admin?tab=seats'))
 assert(!admin.requests.some(r=>r.url==='/gw/studio/list'),'Cancelled studio approval must not be requested')
 const limited=pageFor(['college_admin'],{},[]);await limited.page.onShow();assert.equal(limited.requests.length,0)
 const multi=pageFor(['common','mentor','inspector','admin']);await multi.page.onShow();assert.equal(multi.page.data.nextCards.length,4)
 const out=pageFor(['common']);out.clearToken();await out.page.onShow();assert.equal(out.requests.length,0);assert(out.routes[0].includes('/login/login'));await out.page.onPullDownRefresh();assert.equal(out.stopped(),1)
 const empty=pageFor(['mentor']);await empty.page.onShow();assert.equal(empty.page.data.pendingItems.length,0);assert.equal(empty.page.data.nextCards.length,1)
 const wxml=fs.readFileSync(path.join(root,'pages/index/index.wxml'),'utf8');for(const m of wxml.matchAll(/(?:bind\w+|catch\w+)="(\w+)"/g))assert.equal(typeof p[m[1]],'function','Unbound handler '+m[1])
 assert(!wxml.includes('figma.com/api/mcp/asset/'));assert(wxml.includes('appearance="figma"'))
 const dimensions={placeholder:144,arrow:48,search:72,notification:60,calendar:72,'nav-home-active':72,'nav-spaces':72};for(const [name,size]of Object.entries(dimensions)){const image=fs.readFileSync(path.join(root,'assets/home',name+'.png'));assert.equal(image.subarray(1,4).toString(),'PNG');assert.equal(image.readUInt32BE(16),size);assert.equal(image.readUInt32BE(20),size)}
 console.log('HOME_OK: four roles and combined roles, original endpoint/permission behavior, search/categories, service/todo navigation, authentication, refresh failures/recovery, real avatar fallback, event bindings and PNG asset geometry.')
})().catch(error=>{console.error(error);process.exit(1)})
