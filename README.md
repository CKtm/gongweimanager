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
