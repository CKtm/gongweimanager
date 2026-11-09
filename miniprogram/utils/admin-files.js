const request=require('./request')
const {baseUrl}=require('./config')
function exportFile(url,params={},name='export') {
 return new Promise((resolve,reject)=>wx.request({url:baseUrl+url,method:'POST',data:params,responseType:'arraybuffer',header:{Authorization:'Bearer '+wx.getStorageSync('token'),'content-type':'application/x-www-form-urlencoded'},success(res){
  try {
   const bytes=new Uint8Array(res.data),type=String(res.header['Content-Type']||res.header['content-type']||'')
   if(res.statusCode!==200||/json|text\//i.test(type)||bytes[0]===123){let message='文件导出失败';try{const text=decodeURIComponent(Array.from(bytes).map(b=>'%'+b.toString(16).padStart(2,'0')).join(''));message=JSON.parse(text).msg||message}catch(e){}throw new Error(message)}
   const filePath=wx.env.USER_DATA_PATH+'/'+name+'-'+Date.now()+'.xlsx';wx.getFileSystemManager().writeFile({filePath,data:res.data,success:()=>wx.openDocument({filePath,fileType:'xlsx',showMenu:true,success:()=>resolve(filePath),fail:reject}),fail:reject})
  }catch(e){reject(e)}
 },fail:reject})).catch(e=>{wx.showToast({title:e.message||'文件操作失败',icon:'none'});throw e})
}
function chooseExcel(){return new Promise((resolve,reject)=>wx.chooseMessageFile({count:1,type:'file',extension:['xls','xlsx'],success(res){const f=res.tempFiles[0];if(!f||! /\.(xls|xlsx)$/i.test(f.name))return reject(new Error('请选择 Excel 文件'));resolve(f)},fail:reject}))}
async function importFile(url,file,updateSupport=false){return request.upload(url,file.path,'file',{updateSupport:String(updateSupport)})}
function plainResult(msg){return String(msg||'导入完成').replace(/<br\s*\/?\s*>/gi,'\n').replace(/<[^>]*>/g,'')}
module.exports={exportFile,chooseExcel,importFile,plainResult}
