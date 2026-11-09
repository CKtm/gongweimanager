const request=require('../../utils/request')
const {isAdmin,can,statusNames}=require('../../utils/admin')
const rows=res=>res.data||res.rows||[]
const same=(a,b)=>String(a)===String(b)
Page({
 data:{authorized:false,tab:'reviews',reviewTab:'mobile',inspectionTab:'tasks',loading:false,saving:false,error:'',applications:[],studios:[],returns:[],rooms:[],stations:[],assignments:[],students:[],inspectors:[],tasks:[],records:[],selectedRoom:null,roomSeats:[],search:'',filteredMobile:[],filteredStudio:[],filteredReturns:[],filteredTasks:[],filteredRecords:[],reviewCount:0,waitingCount:0,inspectionCount:0,sheet:null,comment:'',stationOptions:[],stationIndex:-1,studentOptions:[],studentIndex:-1,inspectorIndex:-1,taskName:'',scheduledDate:'',termCode:'',perm:{}},
 onLoad(options){if(['reviews','seats','inspections'].includes(options.tab))this.setData({tab:options.tab})},
 onShow(){const user=wx.getStorageSync('userInfo')||{};if(!wx.getStorageSync('token'))return wx.redirectTo({url:'/pages/login/login'});if(!isAdmin(user)){wx.showToast({title:'仅管理员可访问',icon:'none'});return wx.reLaunch({url:'/pages/index/index'})}this.user=user;const perm={};for(const [key,p] of Object.entries({mobile:'gw:application:adminReview',allocate:'gw:application:assign',studio:'gw:studio:review',returns:'gw:return:review',verifyReturn:'gw:return:verify',fixed:'gw:assignment:assign',adjust:'gw:assignment:adjust',recall:'gw:assignment:recall',createTask:'gw:inspection:task:add',verifyRecord:'gw:inspection:record:verify'}))perm[key]=can(user,p);this.setData({authorized:true,perm});this.load()},
 onPullDownRefresh(){if(this.data.authorized)this.load().finally(()=>wx.stopPullDownRefresh());else wx.stopPullDownRefresh()},
 async load(){
  if(this.data.loading)return
  this.setData({loading:true,error:''})
  const sources=[['applications','/gw/application/list','gw:application:list'],['studios','/gw/studio/list','gw:studio:list'],['returns','/gw/return/list','gw:return:query'],['rooms','/gw/room/list','gw:room:list'],['stations','/gw/workstation/list','gw:workstation:list'],['assignments','/gw/assignment/list','gw:assignment:list',{activeOnly:true}],['students','/gw/profile/options','gw:profile:list',{roleKey:'common'}],['inspectors','/gw/profile/options','gw:profile:list',{roleKey:'inspector'}],['tasks','/gw/inspection/task/list','gw:inspection:task:list'],['records','/gw/inspection/record/list','gw:inspection:record:list']]
  let failures=0;const next={}
  await Promise.all(sources.map(async([key,url,permission,params])=>{if(!can(this.user,permission)){next[key]=[];return}try{next[key]=rows(await request.get(url,params)).map(x=>({...x,statusText:statusNames[x.status]||x.status,photoList:x.photoUrls?x.photoUrls.split(',').filter(Boolean):[]}))}catch(e){failures++}}))
  this.setData({...next,loading:false,error:failures?'部分数据加载失败，请下拉刷新后再操作。':''})
  this.updateView()
 },
 updateView(){const d=this.data,q=d.search.trim().toLowerCase(),match=x=>!q||[x.studentName,x.mentorName,x.roomName,x.workstationNo,x.taskName,x.inspectorName].join(' ').toLowerCase().includes(q)
  const mobile=d.applications.filter(x=>['PENDING_ADMIN','WAIT_ALLOC'].includes(x.status)),studios=d.studios.filter(x=>x.status==='PENDING'),returns=d.returns.filter(x=>['PENDING_ADMIN','WAIT_VERIFY','WAIT_CLEAR'].includes(x.status))
  const selectedRoom=d.rooms.find(x=>d.selectedRoom&&same(x.roomId,d.selectedRoom.roomId))||d.rooms[0]||null
  const roomSeats=d.stations.filter(x=>selectedRoom&&same(x.roomId,selectedRoom.roomId)).map(x=>({...x,assignment:d.assignments.find(a=>same(a.workstationId,x.workstationId)&&!a.endedAt)||null,label:(x.roomName||'')+' / '+x.workstationNo}))
  this.setData({selectedRoom,roomSeats,filteredMobile:mobile.filter(match),filteredStudio:studios.filter(match),filteredReturns:returns.filter(match),filteredTasks:d.tasks.filter(match),filteredRecords:d.records.filter(match),reviewCount:mobile.filter(x=>x.status==='PENDING_ADMIN').length+studios.length+returns.filter(x=>['PENDING_ADMIN','WAIT_VERIFY'].includes(x.status)).length,waitingCount:mobile.filter(x=>x.status==='WAIT_ALLOC').length,inspectionCount:d.records.filter(x=>x.status==='SUBMITTED'&&x.taskStatus==='SUBMITTED').length})
 },
 switchTab(e){if(this.data.saving)return;this.setData({tab:e.currentTarget.dataset.tab,search:''});this.updateView()},
 switchReview(e){this.setData({reviewTab:e.currentTarget.dataset.tab})},switchInspection(e){this.setData({inspectionTab:e.currentTarget.dataset.tab})},
 onSearch(e){this.setData({search:e.detail.value});this.updateView()},openRoom(e){if(this.data.saving)return;this.setData({selectedRoom:this.data.rooms.find(x=>same(x.roomId,e.currentTarget.dataset.id))||null});this.updateView()},
 onInput(e){this.setData({[e.currentTarget.dataset.field]:e.detail.value})},onStation(e){this.setData({stationIndex:Number(e.detail.value)})},clearStation(){this.setData({stationIndex:-1})},onStudent(e){this.setData({studentIndex:Number(e.detail.value)})},onInspector(e){this.setData({inspectorIndex:Number(e.detail.value)})},
 previewPhotos(e){const urls=e.currentTarget.dataset.urls||[];if(urls.length)wx.previewImage({current:e.currentTarget.dataset.url||urls[0],urls})},
 closeSheet(){if(!this.data.saving)this.setData({sheet:null})},stopTouch(){},
 openSheet(sheet,extras={}){this.setData({sheet,comment:'',stationOptions:[],stationIndex:-1,studentOptions:[],studentIndex:-1,inspectorIndex:-1,taskName:'',scheduledDate:'',termCode:'',...extras})},
 allowed(key){if(this.data.authorized&&this.data.perm[key])return true;wx.showToast({title:'当前账号没有此操作权限',icon:'none'});return false},
 review(e){const {kind,id,decision}=e.currentTarget.dataset;const d=this.data;let item,sheet
  if(kind==='mobile'){if(!this.allowed('mobile'))return;item=d.applications.find(x=>same(x.applicationId,id)&&x.status==='PENDING_ADMIN');if(item)sheet={kind,id,title:decision==='APPROVED'?'通过流动工位申请':'驳回流动申请',description:item.studentName+' · '+item.reason,decision,selectStation:decision==='APPROVED',optionalStation:true}}
  if(kind==='studio'){if(!this.allowed('studio'))return;item=d.studios.find(x=>same(x.studioApplicationId,id)&&x.status==='PENDING');if(item)sheet={kind,id,title:decision==='APPROVED'?'通过工作室申请':'驳回工作室申请',description:item.mentorName+' · '+item.roomName,decision}}
  if(kind==='return'){if(!this.allowed('returns'))return;item=d.returns.find(x=>same(x.returnId,id)&&x.status==='PENDING_ADMIN');if(item)sheet={kind,id,title:item.sourceType?(decision==='APPROVED'?'确认释放工位':'清理不合格，退回补拍'):(decision==='APPROVED'?'同意清空（历史流程）':'驳回退还'),description:item.studentName+' · '+item.roomName+'/'+item.workstationNo,decision}}
  if(kind==='verifyReturn'){if(!this.allowed('verifyReturn'))return;item=d.returns.find(x=>same(x.returnId,id)&&x.status==='WAIT_VERIFY');if(item)sheet={kind,id,title:decision==='APPROVED'?'核验通过并释放':'核验不合格',description:item.studentName+' · '+item.workstationNo,decision}}
  if(!sheet)return
  this.openSheet(sheet,{stationOptions:this.available('MOBILE')})
 },
 available(type){return this.data.stations.filter(x=>x.status==='AVAILABLE'&&x.roomStatus==='NORMAL'&&x.workstationType===type).map(x=>({...x,label:x.roomName+' / '+x.workstationNo}))},
 allocate(e){if(!this.allowed('allocate'))return;const item=this.data.applications.find(x=>same(x.applicationId,e.currentTarget.dataset.id)&&x.status==='WAIT_ALLOC');if(!item)return;this.openSheet({kind:'allocate',id:item.applicationId,title:'分配流动工位',description:item.studentName+' · 请选择具体工位',selectStation:true,noComment:true},{stationOptions:this.available('MOBILE')})},
 seatAction(e){const seat=this.data.roomSeats.find(x=>same(x.workstationId,e.currentTarget.dataset.id));if(!seat)return
  const a=seat.assignment
  if(a){this.openSheet({kind:'seat',title:'座位使用信息',description:seat.label,seat,assignment:a,noComment:true});return}
  if(seat.status!=='AVAILABLE'){wx.showToast({title:'该工位暂不可分配',icon:'none'});return}
  if(seat.workstationType==='MOBILE'){this.setData({tab:'reviews',reviewTab:'mobile'});wx.showToast({title:'请从已审批申请中分配流动工位',icon:'none'});return}
  if(!this.allowed('fixed'))return
  const assigned=new Set(this.data.assignments.filter(x=>!x.endedAt).map(x=>String(x.studentId)))
  const studentOptions=this.data.students.filter(x=>x.mentorId!=null&&same(x.mentorId,seat.mentorId)&&!assigned.has(String(x.userId))).map(x=>({...x,label:(x.nickName||x.userName)+' · '+(x.identityNo||x.userName)}))
  this.openSheet({kind:'fixed',id:seat.workstationId,title:'分配固定工位',description:seat.label,selectStudent:true,term:true,noComment:true},{studentOptions})
 },
 adjust(){if(!this.allowed('adjust'))return;const a=this.data.sheet.assignment,student=this.data.students.find(x=>same(x.userId,a.studentId));let stationOptions=this.available(a.workstationType);if(a.workstationType==='FIXED'){const current=this.data.stations.find(x=>same(x.workstationId,a.workstationId));stationOptions=stationOptions.filter(x=>same(x.mentorId,student?student.mentorId:current&&current.mentorId))}this.openSheet({kind:'adjust',id:a.assignmentId,title:'调整工位',description:a.studentName+' · '+a.roomName+'/'+a.workstationNo,selectStation:true},{stationOptions})},
 recall(){if(!this.allowed('recall'))return;const a=this.data.sheet.assignment;this.openSheet({kind:'recall',id:a.assignmentId,title:'主动收回工位',description:a.studentName+' · '+a.roomName+'/'+a.workstationNo+'；学生清理后上传照片，管理员核验通过后释放。'})},
 createTask(){if(!this.allowed('createTask'))return;this.openSheet({kind:'task',title:'下发卫生检查任务',description:'指定检查员和日期，具体评分对象由检查员现场选择。',taskForm:true,optionalComment:true})},
 recordAction(e){if(!this.allowed('verifyRecord'))return;const item=this.data.records.find(x=>same(x.recordId,e.currentTarget.dataset.id));if(!item||item.taskStatus!=='SUBMITTED')return;const decision=item.status==='SUBMITTED'?'VERIFIED':item.status==='VERIFIED'?'ARCHIVED':null;if(decision)this.openSheet({kind:'record',id:item.recordId,decision,title:decision==='VERIFIED'?'核实检查评分':'归档检查评分',description:item.roomName+' · '+(item.workstationNo||'整体')+' · '+item.score+'分'})},
 async submitSheet(){
  if(this.data.saving||this.data.loading||!this.data.sheet)return
  if(!this.data.authorized||!isAdmin(wx.getStorageSync('userInfo')||{}))return
  if(this.data.error)return wx.showToast({title:'请先下拉刷新数据后重试',icon:'none'})
  const d=this.data,s=d.sheet,comment=d.comment.trim(),station=d.stationOptions[d.stationIndex],student=d.studentOptions[d.studentIndex]
  if(s.kind==='seat')return
  if(!s.noComment&&!s.optionalComment&&!comment)return wx.showToast({title:'请填写处理意见或原因',icon:'none'})
  if(s.selectStation&&!s.optionalStation&&!station)return wx.showToast({title:'请选择目标工位',icon:'none'})
  if(s.selectStudent&&!student)return wx.showToast({title:'请选择学生',icon:'none'})
  if(s.term&&!d.termCode.trim())return wx.showToast({title:'请填写分配学期',icon:'none'})
  if(s.taskForm&&(!d.taskName.trim()||!d.scheduledDate||!d.inspectors[d.inspectorIndex]))return wx.showToast({title:'请填写任务、检查员和日期',icon:'none'})
  let url,body
  if(s.kind==='mobile'){if(station&&!this.allowed('allocate'))return;url='/gw/application/'+s.id+'/admin-review';body={status:s.decision,comment,...(station?{workstationId:station.workstationId}:{})}}
  if(s.kind==='studio'){url='/gw/studio/'+s.id+'/review';body={status:s.decision,comment}}
  if(s.kind==='return'){url='/gw/return/'+s.id+'/review';body={status:s.decision,comment}}
  if(s.kind==='verifyReturn'){url='/gw/return/'+s.id+'/verify';body={approved:s.decision==='APPROVED',comment}}
  if(s.kind==='allocate'){url='/gw/application/'+s.id+'/assign';body={workstationId:station.workstationId}}
  if(s.kind==='fixed'){url='/gw/assignment/fixed';body={studentId:student.userId,workstationId:s.id,termCode:d.termCode.trim()}}
  if(s.kind==='adjust'){url='/gw/assignment/adjust';body={assignmentId:s.id,targetWorkstationId:station.workstationId,reason:comment}}
  if(s.kind==='recall'){url='/gw/assignment/recall';body={assignmentId:s.id,reason:comment}}
  if(s.kind==='task'){url='/gw/inspection/task';body={taskName:d.taskName.trim(),inspectorId:d.inspectors[d.inspectorIndex].userId,scheduledDate:d.scheduledDate,remark:comment}}
  if(s.kind==='record'){url='/gw/inspection/record/verify';body={recordId:String(s.id),status:s.decision,comment}}
  if(!url)return
  this.setData({saving:true});try{await request.post(url,body);this.setData({sheet:null});wx.showToast({title:s.kind==='recall'?'已发起收回':'处理成功'});await this.load()}catch(e){}finally{this.setData({saving:false})}
 }
})
