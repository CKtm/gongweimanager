# 工位管理系统版本管理

当前基线：`v0.1.0`（2026-11-09）。三个项目独立管理，统一使用 `main` 主分支及同名发布标签。

| 项目 | 本地路径 |
| --- | --- |
| 微信小程序（学生、导师、检查员、管理员） | `D:/develop/wcProj/gongweiManager` |
| PC 管理前端 | `D:/develop/wcProj/gongweiManager-admin` |
| Java 后端和数据库升级脚本 | `D:/develop/wcProj/gongweiManager-admin-server` |

## 日常开发

在相应项目目录执行：

```powershell
git status
git switch -c feature/功能名称
# 修改并验证功能后，选择要提交的文件
git add 文件路径
git diff --cached
git commit -m "feat: 描述完成的功能"
# 功能完成后合并到主分支
git switch main
git merge feature/功能名称
```

修复使用 `fix:`，样式调整使用 `style:`，文档使用 `docs:`。
跨端功能需要分别提交对应仓库；完整发布时在涉及的仓库创建相同标签，例如 `git tag -a v0.2.0 -m "发布说明"`。
标签是固定快照，不要移动已有发布标签。

## 查看版本

```powershell
git log --oneline --decorate -10
git diff
git show v0.1.0 --stat
# 从基线创建独立修复分支，不覆盖当前开发分支
git switch -c fix/from-v0.1.0 v0.1.0
```

切换版本前先提交或暂存未提交修改。恢复历史业务数据需要数据库备份，Git 标签不能恢复 MySQL 数据、Redis 内容或上传照片。

## 本地配置

小程序私有开发者设置、日志、依赖、构建输出和后端真实连接配置不纳入版本管理。
PC 前端 `.env.development` 等公共接口路径配置可以提交，密码只能放在忽略的 `.env.local` 或环境变量中。
后端新环境启动前，复制资源目录下的 `application.example.yml` 为 `application.yml`、`application-druid.example.yml` 为 `application-druid.yml`，然后填写本机连接、密码和随机令牌密钥。
现有电脑的真实配置文件已保留，可以继续由 IDEA 启动。
演示账号数据脚本是开发数据，正式部署应修改演示账号密码。

## 远程仓库与备份

当前只建立本地版本，不向任何服务推送。
前后端原若依远程命名为 `upstream`，保留原历史方便查阅；它不是本项目的发布地址。
拥有自己的远程仓库后，在每个项目中添加对应的远程地址：

```powershell
git remote add origin 你的项目仓库地址
git push -u origin main
git push origin --tags
```

本地提交不能防止硬盘损坏，建议使用个人私有远程仓库备份。不要向若依官方仓库推送项目内容。
