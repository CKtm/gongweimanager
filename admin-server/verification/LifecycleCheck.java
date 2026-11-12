import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;
import java.sql.Statement;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.gongwei.*;
import com.ruoyi.system.mapper.gongwei.*;
import com.ruoyi.system.service.gongwei.*;
import com.ruoyi.system.service.gongwei.impl.*;
import com.ruoyi.web.controller.gongwei.GwStudentOpsController;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

/** Real MySQL + Spring transaction checks; only UUID-scoped fixtures are created and removed. */
public class LifecycleCheck {
 static JdbcTemplate db;static DataSourceTransactionManager tm;static TransactionTemplate tx;
 static GwStudentOpsMapper ops;static GwWorkstationMapper seats;static GwAssignmentMapper assignments;
 static GwStudentOpsService lifecycle;static IGwWorkstationService seatApi;static IGwAssignmentService assignApi;static IGwInspectionService inspection;
 static List<Long> users=new ArrayList<>(),roomIds=new ArrayList<>(),taskIds=new ArrayList<>();
 static Long mentor,otherMentor,student,student2,student3,student4,inspector,room,room2,seat,seat2,seat3,seat4;
 static String prefix="__life_"+UUID.randomUUID().toString().substring(0,12);
 static int assertions;
 static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
 static void reject(Runnable action,String contains){try{action.run();throw new AssertionError("Expected rejection: "+contains);}catch(ServiceException e){check(e.getMessage().contains(contains),e.getMessage());}}
 static void set(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
 @SuppressWarnings("unchecked") static <T>T proxy(T target,boolean clazz){var interceptor=new TransactionInterceptor(tm,new AnnotationTransactionAttributeSource());interceptor.afterPropertiesSet();var factory=new ProxyFactory(target);factory.setProxyTargetClass(clazz);factory.addAdvice(interceptor);return (T)factory.getProxy();}
 static long insert(String sql,Object...args){var key=new GeneratedKeyHolder();db.update(c->{var p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p;},key);return key.getKey().longValue();}
 static Long user(String role,Long mentorId){long id=insert("insert into sys_user(user_name,nick_name,status,del_flag) values(?,?,'0','0')",prefix+users.size(),prefix+users.size());users.add(id);db.update("insert into sys_user_role(user_id,role_id) select ?,role_id from sys_role where role_key=? and status='0' limit 1",id,role);db.update("insert into gw_user_profile(user_id,identity_no,user_type,mentor_id) values(?,?,?,?)",id,prefix+id,role.equals("common")?"STUDENT":role.equals("mentor")?"MENTOR":"INSPECTOR",mentorId);return id;}
 static Long room(){var id=insert("insert into gw_room(room_name,room_type,status) values(?,'STUDIO','NORMAL')",prefix+roomIds.size());roomIds.add(id);return id;}
 static Long seat(Long rid,Long owner,int no){return insert("insert into gw_workstation(room_id,workstation_no,workstation_type,mentor_id,status) values(?,?,'FIXED',?,'AVAILABLE')",rid,String.valueOf(no),owner);}
 static GwFixedAllocation pair(Long s,Long w){var p=new GwFixedAllocation();p.studentId=s;p.workstationId=w;return p;}
 static String term="2026-11-12~2027-01-31";
 static Long penalty(Long sid){return db.queryForObject("select penalty_id from gw_penalty where workstation_id=? order by penalty_id desc limit 1",Long.class,sid);}
 static Long appeal(Long uid){return db.queryForObject("select appeal_id from gw_appeal where student_id=? order by appeal_id desc limit 1",Long.class,uid);}
 static void punish(Long sid){seatApi.changeWorkstationStatus(List.of(sid),"DISABLED","check",new Date(System.currentTimeMillis()+3600000),"违规电器测试",1L);}
 static void due(Long id){db.update("update gw_penalty set until_at=date_sub(now(),interval 2 second) where penalty_id=?",id);}
 static String state(Long sid){return seats.selectWorkstationById(sid).getStatus();}
 static void fixture(){tx.execute(st->{mentor=user("mentor",null);otherMentor=user("mentor",null);student=user("common",mentor);student2=user("common",mentor);student3=user("common",mentor);student4=user("common",otherMentor);inspector=user("inspector",null);room=room();room2=room();seat=seat(room,mentor,1);seat2=seat(room2,mentor,2);seat3=seat(room2,mentor,3);seat4=seat(room2,otherMentor,4);return null;});}
 public static void main(String[]args)throws Exception{
  var ds=new UnpooledDataSource("com.mysql.cj.jdbc.Driver","jdbc:mysql://127.0.0.1:3306/ry-vue?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai","root",System.getenv("MYSQL_PWD"));
  db=new JdbcTemplate(ds);tm=new DataSourceTransactionManager(ds);tx=new TransactionTemplate(tm);
  var cfg=new Configuration(new Environment("test",new SpringManagedTransactionFactory(),ds));cfg.getTypeAliasRegistry().registerAliases("com.ruoyi.system.domain.gongwei");
  var pagination=new com.github.pagehelper.PageInterceptor();var properties=new Properties();properties.setProperty("helperDialect","mysql");pagination.setProperties(properties);cfg.addInterceptor(pagination);
  // Parse every business mapper, with MyBatis DTD validation, to catch startup errors.
  for(String name:List.of("GwWorkstationMapper","GwRoomMapper","GwUserProfileMapper","GwAssignmentMapper","GwInspectionMapper","GwReturnMapper","GwApplicationMapper","GwStudentOpsMapper","GwStudioMapper","GwQuotaMapper")){
   String r="mapper/gongwei/"+name+".xml";try(var stream=LifecycleCheck.class.getClassLoader().getResourceAsStream(r)){if(stream!=null)new XMLMapperBuilder(stream,cfg,r,cfg.getSqlFragments()).parse();}
  }
  var session=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(cfg));ops=session.getMapper(GwStudentOpsMapper.class);seats=session.getMapper(GwWorkstationMapper.class);assignments=session.getMapper(GwAssignmentMapper.class);var profiles=session.getMapper(GwUserProfileMapper.class);var rooms=session.getMapper(GwRoomMapper.class);var returns=session.getMapper(GwReturnMapper.class);
  var life=new GwStudentOpsService();set(life,"mapper",ops);set(life,"profiles",profiles);set(life,"workstations",seats);lifecycle=proxy(life,true);
  var sw=new GwWorkstationServiceImpl();set(sw,"workstationMapper",seats);set(sw,"roomMapper",rooms);set(sw,"profileMapper",profiles);set(sw,"lifecycle",lifecycle);seatApi=proxy((IGwWorkstationService)sw,false);
  var as=new GwAssignmentServiceImpl();set(as,"assignmentMapper",assignments);set(as,"lifecycleMapper",ops);set(as,"lifecycle",lifecycle);set(as,"workstationMapper",seats);set(as,"profileMapper",profiles);set(as,"returnMapper",returns);assignApi=proxy((IGwAssignmentService)as,false);
  var ins=new GwInspectionServiceImpl();set(ins,"mapper",session.getMapper(GwInspectionMapper.class));set(ins,"lifecycleMapper",ops);set(ins,"lifecycle",lifecycle);set(ins,"profileMapper",profiles);set(ins,"roomMapper",rooms);set(ins,"workstationMapper",seats);inspection=proxy((IGwInspectionService)ins,false);
  System.out.println("PASS: all business Mapper XML files parsed with MyBatis validation");
  try{
   fixture();
   // The second student's seat belongs to another mentor: first insertion must be rolled back.
   reject(()->assignApi.batchAssignFixed(List.of(pair(student,seat),pair(student2,seat4)),mentor,term,false),"未划归");
   check(assignments.selectActiveByStudent(student)==null,"Partial batch rolled back first assignment");check("AVAILABLE".equals(state(seat)),"Partial batch rolled back first seat");check(ops.unread(student)==0,"Partial batch rolled back first message");
   reject(()->assignApi.batchAssignFixed(List.of(pair(student,seat),pair(student,seat2)),mentor,term,false),"重复");
   reject(()->assignApi.batchAssignFixed(List.of(pair(student4,seat4)),mentor,term,false),"自己的学生");
   check(assignApi.batchAssignFixed(List.of(pair(student,seat),pair(student2,seat2)),mentor,term,false)==2,"Batch across rooms succeeds");check(ops.unread(student)==1&&ops.unread(student2)==1,"Both students receive allocation messages");
   reject(()->assignApi.assignFixed(student,seat3,mentor,term,false),"已有有效工位");
   System.out.println("PASS: batch ownership, duplicate selection, multi-room allocation, actual rollback of assignments/seats/messages");

   reject(()->seatApi.changeWorkstationStatus(List.of(seat3),"DISABLED","check",new Date(System.currentTimeMillis()+100000),"reason",1L),"使用中工位");
   reject(()->seatApi.changeWorkstationStatus(List.of(seat),"DISABLED","check",new Date(System.currentTimeMillis()-10000),"reason",1L),"晚于");
   reject(()->seatApi.changeWorkstationStatus(List.of(seat),"DISABLED","check",new Date(System.currentTimeMillis()+100000)," ",1L),"停用原因");
   punish(seat);Long p1=penalty(seat);check("DISABLED".equals(state(seat)),"Penalty disables seat");check(assignments.selectActiveByStudent(student)!=null,"Penalty retains assignment");
   lifecycle.expirePenalty(p1);check("DISABLED".equals(state(seat)),"Future deadline not restored");due(p1);
   var executor=Executors.newFixedThreadPool(2);try{var a=executor.submit(()->lifecycle.expirePenalty(p1));var b=executor.submit(()->lifecycle.expirePenalty(p1));a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);}finally{executor.shutdownNow();}
   check("IN_USE".equals(state(seat)),"Due deadline restored original seat");check("EXPIRED".equals(ops.penalty(p1).get("status")),"Due penalty closed");check(db.queryForObject("select count(*) from gw_message where user_id=? and event_key=?",Integer.class,student,"PENALTY_END_"+p1)==1,"Concurrent expiry sends one notification");
   punish(seat);Long p2=penalty(seat);lifecycle.updateDeadline(p2,new Date(System.currentTimeMillis()+7200000));seatApi.changeWorkstationStatus(List.of(seat),"IN_USE","check");check("RESTORED".equals(ops.penalty(p2).get("status")),"Manual restore closes penalty");due(p2);lifecycle.expirePenalty(p2);check("IN_USE".equals(state(seat)),"Closed penalty expiry is idempotent");
   punish(seat);Long p3=penalty(seat);assignApi.initiateRecall(assignments.selectActiveByStudent(student).getAssignmentId(),"test recall",mentor,false);check("AVAILABLE".equals(state(seat)),"Fixed recall directly frees penalized seat");check("RECALLED".equals(ops.penalty(p3).get("status")),"Recall closes penalty");assignApi.assignFixed(student3,seat,mentor,term,false);due(p3);lifecycle.expirePenalty(p3);check(seats.selectWorkstationById(seat).getStudentId().equals(student3),"Old expiry never changes new occupant");
   punish(seat2);Long p4=penalty(seat2);due(p4);db.update("update gw_room set status='MAINTENANCE' where room_id=?",room2);lifecycle.expirePenalty(p4);check("DISABLED".equals(state(seat2)),"Unavailable room prevents restoration");db.update("update gw_room set status='NORMAL' where room_id=?",room2);lifecycle.expirePenalty(p4);check("IN_USE".equals(state(seat2)),"Restores after room usable again");
   System.out.println("PASS: deadlines, retained binding, concurrent/idempotent expiry, manual restore, recall/reassignment isolation, unavailable room safeguard");

   // Inspection captures assignment/student at scoring time, not whichever student later occupies a seat.
   var task=new GwInspectionTask();task.setTaskName(prefix);task.setInspectorId(inspector);task.setScheduledDate(new Date());task.setCreateBy("check");inspection.createTask(task);taskIds.add(task.getTaskId());check(ops.unread(inspector)==1,"Inspector gets task notification");
   var score=new GwInspectionRecord();score.setTaskId(task.getTaskId());score.setInspectorId(inspector);score.setWorkstationId(seat);score.setStudentId(student4);score.setScore(54);score.setExplanation("test");inspection.submitRecord(score);check(student3.equals(score.getStudentId()),"Server overrides supplied student snapshot");
   var overall=new GwInspectionRecord();overall.setTaskId(task.getTaskId());overall.setInspectorId(inspector);overall.setRoomId(room);overall.setStudentId(student3);overall.setScore(90);inspection.submitRecord(overall);check(overall.getStudentId()==null,"Room score not attributed to individual");
   check(ops.scores(student3).isEmpty(),"Unverified score hidden");inspection.completeTask(task.getTaskId(),inspector);inspection.verifyRecord(score.getRecordId(),"VERIFIED","checked",1L);inspection.verifyRecord(overall.getRecordId(),"VERIFIED","checked",1L);check(ops.scores(student3).size()==1,"Only own verified seat score visible");check(ops.scores(student4).isEmpty(),"Another student cannot see score");
   assignApi.initiateRecall(assignments.selectActiveByStudent(student3).getAssignmentId(),"snapshot test",mentor,false);assignApi.assignFixed(student,seat,mentor,term,false);check(ops.scores(student).isEmpty()&&ops.scores(student3).size()==1,"Reassignment doesn't transfer historical score");
   System.out.println("PASS: published score notifications, server-side student snapshots, room-score separation, historical scores isolated after reassignment");

   inspection.verifyRecord(score.getRecordId(),"ARCHIVED","archived",1L);inspection.verifyRecord(overall.getRecordId(),"ARCHIVED","archived",1L);
   check("CLOSED".equals(db.queryForObject("select status from gw_inspection_task where task_id=?",String.class,task.getTaskId())),"Task is closed before appeal submission/review");
   int baselinePending=ops.pendingAppeals(Map.of("status","PENDING_ADMIN"));
   reject(()->lifecycle.submitAppeal(student,"SCORE",score.getRecordId(),"not mine"),"本人");
   lifecycle.submitAppeal(student3,"SCORE",score.getRecordId(),"原评分有误");Long a1=appeal(student3);check(ops.pendingAppeals(Map.of("status","PENDING_MENTOR","mentorId",mentor))==1,"Mentor pending count scoped");check(ops.pendingAppeals(Map.of("status","PENDING_MENTOR","mentorId",otherMentor))==0,"Other mentor cannot count foreign appeals");reject(()->lifecycle.submitAppeal(student3,"SCORE",score.getRecordId(),"duplicate"),"正在处理");
   reject(()->lifecycle.reviewAppeal(a1,1L,true,"APPROVED","skip",80),"导师");reject(()->lifecycle.reviewAppeal(a1,otherMentor,false,"APPROVED","wrong",null),"您的学生");reject(()->lifecycle.reviewAppeal(a1,mentor,false,null,"missing",null),"结果不合法");
   lifecycle.reviewAppeal(a1,mentor,false,"APPROVED","建议复核",null);check("PENDING_ADMIN".equals(ops.appealForUpdate(a1).get("status")),"Mentor passes to admin");check(ops.pendingAppeals(Map.of("status","PENDING_ADMIN"))==baselinePending+1,"Closed-task appeal included in administrator pending count");reject(()->lifecycle.reviewAppeal(a1,1L,true,"APPROVED","invalid",101),"0至100");check(((Number)ops.scoreForUpdate(score.getRecordId()).get("score")).intValue()==54,"Rejected score edit leaves original");
   lifecycle.reviewAppeal(a1,1L,true,"APPROVED","复核通过",88);check(((Number)ops.scoreForUpdate(score.getRecordId()).get("score")).intValue()==88,"Admin score correction");check(ops.pendingAppeals(Map.of("status","PENDING_ADMIN"))==baselinePending,"Completed review removed from pending count");check(((Number)ops.appealForUpdate(a1).get("originalScore")).intValue()==54,"Appeal preserves original score");check(((Number)ops.appealForUpdate(a1).get("revisedScore")).intValue()==88,"Appeal records new score");reject(()->lifecycle.reviewAppeal(a1,1L,true,"REJECTED","again",null),"导师");
   punish(seat);Long p5=penalty(seat);lifecycle.submitAppeal(student,"PENALTY",p5,"处罚异议");Long a2=appeal(student);lifecycle.reviewAppeal(a2,mentor,false,"REJECTED","依据不足",null);check("DISABLED".equals(state(seat)),"Rejected appeal leaves punishment in force");
   lifecycle.submitAppeal(student,"PENALTY",p5,"补充申诉依据");Long a3=appeal(student);lifecycle.reviewAppeal(a3,mentor,false,"APPROVED","建议解除",null);lifecycle.reviewAppeal(a3,1L,true,"APPROVED","解除处罚",null);check("IN_USE".equals(state(seat)),"Approved penalty appeal restores seat");check("APPEAL_APPROVED".equals(ops.penalty(p5).get("status")),"Approved penalty record preserved");
   // An appeal on a historical penalty cannot restore a different current punishment.
   punish(seat);Long p6=penalty(seat);lifecycle.submitAppeal(student,"PENALTY",p5,"历史记录复核");Long a4=appeal(student);lifecycle.reviewAppeal(a4,mentor,false,"APPROVED","复核",null);lifecycle.reviewAppeal(a4,1L,true,"APPROVED","历史复核通过",null);check("DISABLED".equals(state(seat))&&"ACTIVE".equals(ops.penalty(p6).get("status")),"Old appeal never restores current punishment");
   check(ops.appeals(Map.of("studentId",student4)).isEmpty(),"Student appeal list scoped");check(ops.appeals(Map.of("mentorId",otherMentor)).isEmpty(),"Mentor appeal list scoped");check(ops.penalties(Map.of("studentId",student4)).isEmpty(),"Student penalty list scoped");
   System.out.println("PASS: appeal ownership, duplicate protection, mandatory mentor/admin stages, score correction audit, rejection unchanged, penalty解除 and historical appeal isolation");

   var messages=ops.messages(student);Long messageId=GwStudentOpsService.number(messages.get(0),"messageId");check(ops.readMessage(messageId,student4)==0,"Cannot read another user's message");int before=ops.unread(student);check(ops.readMessage(messageId,student)==1,"Own message read");ops.readMessage(messageId,student);check(ops.unread(student)==before-1,"Repeated read idempotent");ops.readAllMessages(student);check(ops.unread(student)==0,"Mark all own messages");check(ops.unread(student2)>0,"Reading own does not affect others");
   lifecycle.message(student,"dedup","test","","CHECK_"+prefix);lifecycle.message(student,"dedup","test","","CHECK_"+prefix);check(ops.unread(student)==1,"Duplicate notification event collapsed");
   System.out.println("PASS: per-user inbox/read/unread isolation and notification deduplication");
   controllerChecks(profiles);
   System.out.println("ALL PASS: "+assertions+" assertions");
  }finally{cleanup();}
 }
 static void login(Long id,String role){var u=new SysUser();u.setUserId(id);u.setUserName(prefix);var r=new SysRole();r.setRoleKey(role);u.setRoles(List.of(r));var principal=new LoginUser(id,null,u,Set.of());SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,List.of()));}
 static void deny(Runnable action){try{action.run();throw new AssertionError("Expected access denial");}catch(AccessDeniedException e){assertions++;}}
 static void controllerChecks(GwUserProfileMapper profiles)throws Exception{
  var c=new GwStudentOpsController();set(c,"mapper",ops);set(c,"service",lifecycle);set(c,"profiles",profiles);
  HttpServletRequest request=(HttpServletRequest)Proxy.newProxyInstance(LifecycleCheck.class.getClassLoader(),new Class<?>[]{HttpServletRequest.class},(p,m,a)->{if(m.getName().equals("getParameter"))return "pageNum".equals(a[0])?"1":"pageSize".equals(a[0])?"20":null;if(m.getName().equals("getAttributeNames"))return Collections.emptyEnumeration();if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;return null;});
  RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  try{
   login(student3,"common");check(c.scores(null).getTotal()==1,"Controller defaults to own scores with real pagination");deny(()->c.scores(student));
   var own=c.penalties();check(own.getRows().stream().allMatch(x->student3.equals(GwStudentOpsService.number((Map<String,Object>)x,"studentId"))),"Controller penalty scope cannot be supplied by caller");
   login(mentor,"mentor");check(c.scores(student3).getTotal()==1,"Mentor may see own student's score");deny(()->c.scores(student4));
   var deadline=new GwStudentOpsController.DeadlineBody();deadline.until=new Date(System.currentTimeMillis()+10000);deny(()->c.deadline(penalty(seat),deadline));
   login(inspector,"inspector");deny(()->c.penalties());deny(()->c.appeals());deny(()->c.scores(student3));
   Long foreign=GwStudentOpsService.number(ops.messages(student).get(0),"messageId");deny(()->c.read(foreign));
   check(c.messages().getRows().size()==1,"Inspector can read own task notification");
  }finally{RequestContextHolder.resetRequestAttributes();SecurityContextHolder.clearContext();com.github.pagehelper.PageHelper.clearPage();}
  System.out.println("PASS: real authenticated controller scope, PageHelper pagination, mentor/student/inspector boundaries and message-read authorization");
 }
 static void cleanup(){
  tx.execute(st->{
   for(Long uid:users){
    for(var a:db.queryForList("select appeal_id from gw_appeal where student_id=?",uid)){String id=a.get("appeal_id").toString();db.update("delete from gw_message where event_key in (?,?,?,?)","APPEAL_SUBMIT_"+id,"APPEAL_ADMIN_"+id,"APPEAL_REVIEW_"+id+"_ADMIN","APPEAL_REVIEW_"+id+"_MENTOR");}
    for(var p:db.queryForList("select penalty_id from gw_penalty where student_id=?",uid))db.update("delete from gw_message where event_key=?","PENALTY_BLOCKED_"+p.get("penalty_id"));
    db.update("delete from gw_appeal where student_id=?",uid);db.update("delete from gw_penalty where student_id=?",uid);db.update("delete from gw_message where user_id=?",uid);
    db.update("delete from gw_audit_log where business_type='ASSIGNMENT' and business_id in (select assignment_id from gw_assignment where student_id=?)",uid);db.update("delete from gw_assignment where student_id=?",uid);
   }
   for(Long tid:taskIds){db.update("delete from gw_message where event_key=?","INSPECTION_SUBMIT_"+tid);db.update("delete from gw_inspection_record where task_id=?",tid);db.update("delete from gw_inspection_task where task_id=?",tid);}
   for(Long rid:roomIds){check(db.queryForObject("select room_name from gw_room where room_id=?",String.class,rid).startsWith(prefix),"Cleanup fixture identity");db.update("delete from gw_workstation where room_id=?",rid);db.update("delete from gw_room where room_id=?",rid);}
   for(Long uid:users){db.update("delete from gw_user_profile where user_id=?",uid);db.update("delete from sys_user_role where user_id=?",uid);db.update("delete from sys_user where user_id=? and user_name like ?",uid,prefix+"%");}
   return null;
  });System.out.println("Temporary fixtures cleaned; pre-existing project data preserved.");
 }
}
