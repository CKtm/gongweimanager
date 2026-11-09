const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert');
const source=fs.readFileSync(path.resolve(__dirname,'../utils/admin-files.js'),'utf8');
let response,writes=0,opened,uploaded,chosen;
const request={upload:async(...args)=>{uploaded=args;return {code:200,msg:'<br/>完成'}}};
const wx={env:{USER_DATA_PATH:'/user'},getStorageSync:()=> 'token',showToast(){},request:o=>o.success(response),getFileSystemManager:()=>({writeFile:o=>{writes++;o.success()}}),openDocument:o=>{opened=o;o.success()},chooseMessageFile:o=>o.success({tempFiles:[chosen]})};
const context={require:n=>n.includes('request')?request:{baseUrl:'http://localhost:9090'},module:{exports:{}},wx,Uint8Array,Date,decodeURIComponent};vm.runInNewContext(source,context);const f=context.module.exports;
(async()=>{
 response={statusCode:200,header:{'Content-Type':'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'},data:new Uint8Array([80,75,3,4]).buffer};await f.exportFile('/system/user/export',{},'users');assert.equal(writes,1);assert.equal(opened.fileType,'xlsx');assert(opened.showMenu);
 response={statusCode:200,header:{'Content-Type':'application/json'},data:Buffer.from(JSON.stringify({code:401,msg:'登录已过期'}))};await assert.rejects(()=>f.exportFile('/system/user/export'));assert.equal(writes,1);
 chosen={name:'users.xlsx',path:'/tmp/users.xlsx'};assert.equal((await f.chooseExcel()).name,'users.xlsx');await f.importFile('/system/user/importData',chosen,true);assert.equal(uploaded[2],'file');assert.equal(uploaded[3].updateSupport,'true');
 chosen={name:'oops.exe',path:'/tmp/oops.exe'};await assert.rejects(()=>f.chooseExcel());assert.equal(f.plainResult('成功<br/>两条<script>x</script>'),'成功\n两条x');
 console.log('ADMIN_FILES_OK: XLSX opens with share menu; JSON errors never saved as Excel; file type check; multipart import arguments; plain import report');
})().catch(e=>{console.error(e);process.exit(1)});
