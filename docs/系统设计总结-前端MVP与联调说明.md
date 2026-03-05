# 《替你说话》系统设计总结（前后端一体化，MVP）

## 1. 项目定位与设计目标

本系统是面向职场沟通场景的 AI 回复助手。MVP 阶段聚焦“能登录、能生成、能对话、能配置”四个闭环：

- 认证闭环：登录、鉴权、当前用户信息
- 快速回复闭环：输入消息 -> 选择人格/场景 -> SSE 流式返回 3 条回复
- 对话闭环：会话管理 + STOMP 多轮流式返回
- 设置闭环：个人资料、API Key、模型、自定义人格

设计优先级：**先跑通业务价值，再做工程化增强**。

---

## 2. 总体架构（前后端）

## 2.1 架构风格

- 单体 Spring Boot 应用
- 前端资源放在 `src/main/resources/static`
- 后端统一暴露 REST + SSE + STOMP
- MySQL 持久化，Redis 用于限流

## 2.2 前端职责

- Vue3 CDN 页面渲染与状态管理
- API 调用封装（自动携带 JWT）
- SSE 数据流读取、拼装与展示
- STOMP 连接、订阅、发布与重连
- 业务交互（复制、会话切换、设置保存）

## 2.3 后端职责

- JWT 鉴权（HTTP）与 STOMP CONNECT 鉴权
- 人格、场景、会话、消息等业务编排
- 调用 AI 模型并以流式回调输出
- 限流控制与统一错误响应

---

## 3. 为什么这样设计

## 3.1 为什么前端用 Vue3 CDN 且保留在 static

- 与现有 Spring Boot 架构一致，部署最省事
- 减少 Vite/Node 构建链路对 MVP 的干扰
- 接口联调和跨域处理更简单（同域）

## 3.2 为什么快速回复用 SSE、对话用 STOMP

- 快速回复是“请求一次，流式返回一次”，天然匹配 SSE
- 对话模式是“长连接、多轮消息、订阅推送”，更适合 STOMP
- 两条链路职责分离，稳定性与排错效率更高

## 3.3 为什么坚持分层后端

- Controller 仅处理协议
- Service 聚焦业务规则
- Mapper 专注 SQL 映射

这样可以把协议变化（例如 STOMP）和业务变化（例如限流规则）分开演进。

---

## 4. 主要模块说明（前端）

## 4.1 页面与脚本组织

- `index.html`：登录页
- `app.html`：主应用壳（快速回复/对话/设置）
- `js/api.js`：请求封装与 token 管理
- `js/auth.js`：登录、登出、鉴权检查
- `js/app.js`：Vue3 主状态与业务动作
- `css/app.css`：中国红主题与通用组件样式

## 4.2 前端核心状态

- `user`：当前用户
- `personas/scenes`：基础数据
- `quick`：快速回复状态（输入、流文本、结果）
- `chat`：会话列表、当前会话、消息列表、连接状态
- `settings`：昵称、默认人格、apiKey、模型、自定义人格输入

## 4.3 前端交互策略

- 所有接口错误统一走 `normalizeError`
- STOMP 连接断开自动重连
- 流式消息采用“token 拼接 + complete 落定”策略
- 复制操作直接使用浏览器剪贴板 API

---

## 5. 主要模块说明（后端）

## 5.1 控制层（Controller）

- `AuthController`：`/api/auth/login|logout|me`
- `UserController`：用户资料、API Key、用量
- `PersonaController`：人格 CRUD
- `SceneController`：场景列表
- `QuickReplyController`：`/api/quick-reply/stream`（SSE）
- `ChatController`：会话 CRUD 与历史消息
- `ChatStompController`：`/app/chat.send` STOMP 消息入口

## 5.2 服务层（Service）

- `AuthService`：用户名密码校验、JWT 生成
- `QuickReplyService`：快速回复业务编排（限流 + 人格场景 + AI）
- `ChatService`：会话消息编排、存储、流式回调桥接
- `PersonaService/SceneService/UserService`：领域服务
- `RateLimitService`：Redis 每日限流
- `AiService`：模型调用与流式回调

## 5.3 鉴权与通信层

- `JwtAuthFilter`：HTTP Bearer Token 解析
- `StompAuthChannelInterceptor`：STOMP CONNECT 认证
- `WebSocketConfig`：STOMP endpoint 与 broker 配置

## 5.4 数据访问层

- MyBatis-Plus Mapper：
  - `UserMapper`
  - `PersonaMapper`
  - `SceneMapper`
  - `ChatSessionMapper`
  - `ChatMessageMapper`

---

## 6. 数据模型与存储设计

## 6.1 MySQL 核心表

- `sys_user`：用户、默认人格、API Key、模型
- `persona`：系统人格与用户人格
- `scene`：系统场景
- `chat_session`：会话
- `chat_message`：会话消息

## 6.2 Redis 设计

- 限流 Key：`rate_limit:{userId}:{date}`
- 作用：控制“未配置个人 Key 的用户”每日调用次数

## 6.3 SQL 初始化策略

- 使用 `schema.sql` 建表
- 避免重复创建索引导致启动失败（MVP 阶段采取幂等策略）

---

## 7. 业务流程（前后端联动）

## 7.1 登录流程

1. 前端提交用户名密码到 `/api/auth/login`
2. 后端校验成功后返回 JWT + 用户信息
3. 前端保存 token 并跳转 `app.html`
4. 页面初始化调用 `/api/auth/me`

## 7.2 快速回复流程（SSE）

1. 前端提交 `message + personaId + sceneId`
2. 后端进行鉴权与限流
3. 后端调用 AI 并通过 SSE 流式返回
4. 前端增量渲染，最终解析 3 条回复

## 7.3 对话模式流程（STOMP）

1. 前端 STOMP 连接 `/ws/chat`，携带 `Authorization`
2. 后端在 CONNECT 阶段鉴权
3. 前端订阅 `/user/queue/chat/{sessionId}`
4. 前端发布到 `/app/chat.send`
5. 后端推送 `token/complete/error`
6. 前端将 token 拼为完整回复并展示

## 7.4 设置与人格管理流程

- 更新个人设置 -> `/api/user/profile`
- 设置/清除 Key -> `/api/user/api-key`
- 人格增删改 -> `/api/personas`
- 用量查询 -> `/api/user/usage`

---

## 8. 使用到的设计模式

- **分层架构模式**：Controller / Service / Mapper
- **DTO 模式**：请求响应契约稳定，解耦内部模型
- **拦截器模式**：HTTP 与 STOMP 鉴权
- **发布订阅模式**：STOMP 的消息路由
- **回调模式**：AI 流式回调桥接到 SSE/STOMP
- **门面模式**：前端 `api.js` 统一请求入口

---

## 9. 关键设计取舍

## 9.1 做了什么

- 用最少依赖完成前后端闭环
- 协议按场景拆分（SSE + STOMP）
- UI 统一中国红主题并保持轻量

## 9.2 没做什么（有意取舍）

- 未引入前端工程化构建（Vite）
- 未做复杂权限、多角色、导出等非 MVP 能力
- 未做高复杂缓存/消息中间件架构

---

## 10. 风险与改进建议

- 增加后端接口集成测试（登录、人格、会话、流式）
- 增加前端错误埋点（接口失败率、STOMP 重连次数）
- 后续演进路径：Vue3 + Vite + TS 分离工程；后端补充 OpenAPI 与服务监控

---

## 11. 一句话结论

该方案本质是“**MVP 交付优先的前后端一体化设计**”：在保证业务闭环、联调效率和协议正确性的前提下，控制复杂度并预留后续工程化升级路径。
