const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert')
const root=path.resolve(__dirname,'..')
const adminTools=require(path.join(root,'utils/admin'))
const event=(field,value)=>({currentTarget:{dataset:{field}},detail:{value}})
const idEvent=id=>({currentTarget:{dataset:{id}}})
function setup(name,roles=['common']){
 let page,fail=false,token='token',responses={},requests=[],routes=[],toasts=[],pending=false
 const user={roles,userId:10,permissions:['*:*:*']}
 const wx={getStorageSync:key=>key==='token'?token:user,redirectTo:x=>routes.push(x.url),reLaunch:x=>routes.push(x.url),navigateTo:x=>routes.push(x.url),showToast:x=>toasts.push(x.title),stopPullDownRefresh(){},setNavigationBarTitle(){},showModal:x=>x.success({confirm:true})}
 const request={};for(const m of ['get','post','put','delete'])request[m]=async(url,body)=>{requests.push({method:m,url,body});if(fail)throw new Error('offline');if(pending)await new Promise(resolve=>setImmediate(resolve));return responses[url]||{data:[]}}
 vm.runInNewContext(fs.readFileSync(path.join(root,`pages/${name}/${name}.js`),'utf8'),{Page:x=>page=x,wx,require:n=>n.endsWith('/request')?request:n.endsWith('/admin')?adminTools:n.endsWith('/admin-schema')?require(path.join(root,'utils/admin-schema')):{},console})
 page.data=JSON.parse(JSON.stringify(page.data));page.setData=function(x){Object.assign(this.data,x)}
 return {page,user,requests,routes,toasts,response:(url,r)=>responses[url]=r,fail:x=>fail=x,pending:x=>pending=x,logout:()=>token=''}
}
;(async()=>{
 const s=setup('student-records'),p=s.page;s.response('/gw/lifecycle/scores',{rows:[{recordId:42,score:54,status:'VERIFIED'}],total:1});p.onLoad({tab:'scores'});await p.onShow();assert.equal(p.data.tab,'scores');assert.equal(p.data.rows[0].key,42);assert(!s.requests.at(-1).body.studentId)
 p.openAppeal(idEvent(42));await p.submit();assert(s.toasts.includes('请填写申诉原因'));assert.equal(s.requests.length,1)
 p.input(event('reason','有异议'));s.pending(true);const submitting=p.submit();await p.submit();await submitting;assert.equal(s.requests.filter(x=>x.method==='post').length,1);assert.equal(s.requests.find(x=>x.method==='post').body.targetType,'SCORE');assert.equal(p.data.sheet,null)
 s.fail(true);await p.onPullDownRefresh();assert(p.data.error);s.fail(false);await p.load(1);assert(!p.data.error)
 s.response('/gw/messages',{rows:[{messageId:3,title:'提醒',content:'test',destination:'/pages/applications/applications'}],total:1});await p.switchTab({currentTarget:{dataset:{tab:'messages'}}});await p.readMessage(idEvent(3));assert.equal(s.routes.at(-1),'/pages/applications/applications');assert(s.requests.some(x=>x.url==='/gw/messages/3/read'))
 const inspector=setup('student-records',['inspector']);inspector.page.onLoad({tab:'penalties'});await inspector.page.onShow();assert.equal(inspector.page.data.tabs.length,1);assert.equal(inspector.page.data.tab,'messages')
 const mentor=setup('student-records',['mentor']);mentor.response('/gw/appeals',{rows:[{appealId:7,targetType:'SCORE',status:'PENDING_MENTOR'}],total:1});mentor.page.onLoad({tab:'appeals'});await mentor.page.onShow();mentor.page.openReview(idEvent(7));mentor.page.input(event('comment','同意复核'));await mentor.page.submit();const initial=mentor.requests.find(x=>x.url==='/gw/appeals/7/review');assert(initial);assert(!('revisedScore' in initial.body))
 const admin=setup('student-records',['college_admin']);admin.response('/gw/appeals',{rows:[{appealId:8,targetType:'SCORE',status:'PENDING_ADMIN'}],total:1});admin.page.onLoad({tab:'appeals'});await admin.page.onShow();admin.page.openReview(idEvent(8));admin.page.input(event('comment','同意调整'));await admin.page.submit();assert(!admin.requests.some(x=>x.method==='post'));admin.page.input(event('revisedScore','101'));await admin.page.submit();assert(!admin.requests.some(x=>x.method==='post'));admin.page.input(event('revisedScore','88'));await admin.page.submit();assert.equal(admin.requests.find(x=>x.method==='post').body.revisedScore,88)
 const out=setup('student-records');out.logout();out.page.onLoad({});await out.page.onShow();assert.equal(out.requests.length,0);assert.equal(out.routes[0],'/pages/login/login')

 const m=setup('mentor',['mentor']),mp=m.page
 m.response('/gw/profile/team',{data:[{userId:21,nickName:'甲'},{userId:22,nickName:'乙'}]});m.response('/gw/workstation/list',{data:[{workstationId:31,roomId:1,roomName:'421',workstationNo:'1',status:'AVAILABLE',roomStatus:'NORMAL',mentorId:10},{workstationId:32,roomId:2,roomName:'422',workstationNo:'2',status:'AVAILABLE',roomStatus:'NORMAL',mentorId:10}]});await mp.load();mp.batchStudents({detail:{value:['21','22']}});mp.batchStation({currentTarget:{dataset:{id:21}},detail:{value:'0'}});mp.batchStation({currentTarget:{dataset:{id:22}},detail:{value:'0'}});mp.setData({usageStart:'2026-11-12',usageEnd:'2027-01-31'});await mp.assignBatch();assert(m.toasts.includes('同一工位不能重复分配'));assert(!m.requests.some(x=>x.method==='post'))
 mp.batchStation({currentTarget:{dataset:{id:22}},detail:{value:'1'}});m.pending(true);const assigning=mp.assignBatch();await mp.assignBatch();await assigning;const batch=m.requests.filter(x=>x.url==='/gw/assignment/fixed/batch');assert.equal(batch.length,1);assert.equal(batch[0].body.allocations.length,2);assert.equal(batch[0].body.termCode,'2026-11-12~2027-01-31')
 await mp.usageHistory(idEvent(21));assert.equal(m.requests.at(-1).body.studentId,21);assert(!('activeOnly' in m.requests.at(-1).body));mp.closeHistory();assert.equal(mp.data.history,null)

 const a=setup('admin-manage',['admin']),ap=a.page;ap.onLoad({mode:'stations'});ap.user=a.user;ap.setData({authorized:true,cap:{edit:true}});ap.raw=[{workstationId:9,status:'IN_USE',studentId:21}];await ap.singleStationStatus({currentTarget:{dataset:{id:9,status:'DISABLED'}}});assert.equal(ap.data.sheet.kind,'penalty');assert(!a.requests.some(x=>x.method==='put'))
 ap.input(event('reason','违规电器'));await ap.save();assert(!a.requests.some(x=>x.method==='put'));ap.input(event('untilDate','2099-12-31'));ap.input(event('untilTime','23:59'));await ap.save();const penalty=a.requests.find(x=>x.method==='put');assert.equal(penalty.body.until,'2099-12-31 23:59:00');assert.equal(penalty.body.reason,'违规电器')
 for(const name of ['student-records','mentor','admin','admin-manage']){const t=setup(name);const wxml=fs.readFileSync(path.join(root,`pages/${name}/${name}.wxml`),'utf8');for(const match of wxml.matchAll(/(?:bind\w+|catch\w+)="(\w+)"/g))assert.equal(typeof t.page[match[1]],'function',name+': '+match[1])}
 const app=JSON.parse(fs.readFileSync(path.join(root,'app.json'),'utf8'));assert(app.pages.includes('pages/student-records/student-records'));assert(app.usingComponents['message-entry']);assert(JSON.parse(fs.readFileSync(path.join(root,'pages/student-records/student-records.json'),'utf8')).enablePullDownRefresh)
 console.log('LIFECYCLE_UI_OK: role-scoped records, appeals/reviews, score bounds, messages, error/retry, batch allocation/duplicate checks, history filters, date-time penalty forms, route and event bindings.')
})().catch(e=>{console.error(e);process.exit(1)})
