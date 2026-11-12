const request = require('../../utils/request')
Component({data:{unread:0},lifetimes:{attached(){this.refresh()}},pageLifetimes:{show(){this.refresh()}},methods:{
 refresh(){if(this.loading||!wx.getStorageSync('token'))return;this.loading=true;return request.get('/gw/messages/unread').then(r=>this.setData({unread:Number(r.data)||0})).catch(()=>{}).finally(()=>{this.loading=false})},
 open(){wx.navigateTo({url:'/pages/student-records/student-records?tab=messages'})}
}})
