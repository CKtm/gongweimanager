# gongweimanager

研究生工位管理系统，包含学生、导师、卫生检查员和学院管理员业务。

| 目录 | 内容 |
| --- | --- |
| `miniprogram/` | 微信小程序，多角色工作台及完整移动管理入口 |
| `admin-web/` | 保留的 PC 管理后台，供迁移验收和对照使用 |
| `admin-server/` | 若依 Java 后端及数据库初始化、增量升级 SQL |

## 版本

`v0.1.0` 是当前业务基线。此仓库汇总三个本地独立项目的已提交源码快照；原有独立仓库和若依历史保留在本地。

| 来源项目 | 基线提交 |
| --- | --- |
| 微信小程序 | `4648ba3934d27ecf0fc134a2d4fba4cb015f6004` |
| PC 管理前端 | `823fec2916a807dd10bba7133dfe3222e75fb3e1` |
| Java 后端 | `5456dc04ebea82cb06a55257450047b6595ef06c` |

后续对此远程仓库的变更，应在本仓库对应目录中提交，或从本地独立项目同步经过验证的修改后提交。不要直接推送三个独立项目覆盖此仓库的 `main` 分支。

## 当前更新（2026-11-12）

- 学生可查看自己的卫生评分和处罚记录，并提交申诉；导师初审、管理员终审，检查任务关闭后仍能完成申诉。
- 导师支持批量分配固定工位和查看学生使用历史；任何一项校验失败则整批撤销。
- 停用处罚支持设置原因与截止时间、提前恢复及调整期限；到期自动核对原占用并恢复，避免影响新使用人。
- 新增站内消息中心、未读提示和申诉终审待办；小程序底部“我的”显示未读数，角标可拖动隐藏。
- 首页服务卡片替换为四组本地校园主题配图，PC 退还回收菜单补齐图标。
- 现有数据库升级需执行 `admin-server/sql/gongwei_student_lifecycle_upgrade.sql`，无须清空已有数据。旧版本仍应先完成此前增量迁移。
- 本次仅上传源码，未发布线上。小程序当前默认连接本地 `http://127.0.0.1:9090`；运行环境配置、部署密钥和数据库数据不在仓库内。
- 详细业务与验收步骤见 `admin-server/docs/student-lifecycle.md`。验证：后端73项集成断言、6组小程序回归、原生页面编译及PC构建通过。

## 上一版更新（2026-11-11）

- 工位支持按数量新增、自动编号和排布，以及批量修改导师、停用、恢复和删除。
- 停用针对正在使用工位的学生。停用期间保留占用，恢复后继续使用原工位。
- 空闲工位和无现有工位、无未完成业务的房间支持逻辑删除。历史可查，同编号工位或同名房间重建后使用新的记录 ID。
- 已使用上一版的数据库，请在启动新后端前依次执行 `admin-server/sql/gongwei_workstation_soft_delete_upgrade.sql` 和 `admin-server/sql/gongwei_room_soft_delete_upgrade.sql`。迁移保留现有业务数据。

## 上一版更新（2026-11-10）

### 小程序界面统一

- 首页、空间总览、个人中心参考 Figma Home / Studios / Profile，以原生 WXML、WXSS、JS、JSON 实现，包含搜索、筛选、卡片、待办与账号操作。
- 其余11个业务页面统一浅灰背景、蓝色强调、白色卡片以及表单和弹窗样式，保留现有业务脚本、接口和权限流程。
- Figma图标已保存为本地PNG，运行时不依赖临时素材地址；头像使用真实账号资料。
- 验证通过：14个路由页面的官方WXML/WXSS编译、201个事件绑定检查，以及首页、空间、个人中心、管理和文件操作共5组回归测试。模拟器/真机视觉验收尚未完成。
- 此界面版本无需新增数据库迁移，已有环境更新小程序并重新编译即可。

### 业务功能

- 小程序业务页面支持下拉刷新；学生卫生评分新增闲置、游戏、吸烟、违规电器和其他多选项。
- 固定工位由管理员逐个或批量划给导师，导师再分配给自己的学生；同一工作室可共享，一个导师可跨房间拥有工位。
- 取消工作室申请和审批，历史申请保留；流动申请按导师所有房间内的可用固定工位校验。
- 小程序使用期限统一通过日历选择开始和结束日期，保存具体日期并校验区间；到期仍由导师主动收回。
- 现有数据库升级顺序：先备份，再执行 `admin-server/sql/gongwei_shared_fixed_seats_upgrade.sql`、`admin-server/sql/gongwei_admin_direct_fixed_seats_upgrade.sql`。详细规则见 `admin-server/doc/shared-fixed-seats.md`。
- 当前开发机已执行上述升级；其他环境按需执行迁移，不要重复初始化已有数据库。

## 开发与启动

- 后端：Java 17、Maven、MySQL、Redis；在 `admin-server/ruoyi-admin/src/main/resources/` 将两个 `*.example.yml` 模板复制为对应的 `application.yml` 和 `application-druid.yml`，填写本地配置并替换 `CHANGE_ME` 值，然后在 IDEA 启动 `RuoYiApplication`。详细步骤见 `admin-server/LOCAL_CONFIG.md`。
- PC 前端：进入 `admin-web/`，运行 `npm ci` 和 `npm run dev`。默认前端端口 5173，开发代理连接后端 9090。
- 微信小程序：使用微信开发者工具导入 `miniprogram/`，根据实际环境调整 AppID 和 `utils/config.js` 中的后端地址。
- 数据库脚本位于 `admin-server/sql/`；请先审阅初始化和升级脚本，再在目标数据库执行，不要对现有数据重复执行初始化或演示数据脚本。

本仓库不包含真实本地数据库密码、令牌密钥、Redis 快照、上传文件、依赖目录和构建产物。数据库内容和上传照片需要单独备份。

## 日常版本管理

```sh
git switch -c feature/your-feature
git add <files>
git diff --cached
git commit -m "feat: describe the change"
git push -u origin feature/your-feature
```

PC 前端与后端基于 RuoYi 开源项目开发，原许可证分别保留在相应子目录中。
