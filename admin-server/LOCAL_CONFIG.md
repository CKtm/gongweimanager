# 本地启动配置

实际的 `ruoyi-admin/src/main/resources/application.yml` 和 `application-druid.yml` 保留在本机，并通过 `.gitignore` 排除。

首次获取此项目时：

1. 将 `application.example.yml` 复制为 `application.yml`。
2. 将 `application-druid.example.yml` 复制为 `application-druid.yml`。
3. 填写数据库、Redis、上传目录等本地配置；替换所有 `CHANGE_ME` 值，令牌密钥使用随机值。
4. 按数据库升级顺序准备数据库，然后在 IDEA 中使用 Java 17 启动。

提交前使用 `git diff --cached` 检查内容。`v0.1.0` 是源码基线，不包含本地数据库与上传文件备份。
