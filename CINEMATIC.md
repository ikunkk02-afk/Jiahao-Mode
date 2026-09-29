# 嘉豪时刻电影演出

第三阶段，Minecraft 1.21.1 / Fabric / Yarn 1.21.1+build.3。沿用原有时间暂停，
没有新增动画依赖、摄像机实体或另一种开始通知。

## 行为

服务器确认暂停成功且发动者稳定站立后，前 100 个服务器 Tick 播放演出；
暂停仍到 160 Tick 才自动结束。空中、游泳、飞行、滑翔、睡眠、蹲姿、不着地或
脚下没有支撑时跳过演出。落地不补播。正常 20 TPS 时约 5 秒和 8 秒；
服务器卡顿时按服务器进度运行，不承诺严格现实时间长度。

演出只锁定发动者移动、跳跃、攻击、使用、鼠标转向及 F5 切视角；ESC、聊天、
物品栏/菜单保留。R 仍发送原有请求。正常结束不结束时间暂停。
其他玩家只显示发动者 Pose，其摄像机和既有时间暂停规则不变。

## 同步与生命周期

`JiahaoTimeStatePayload` 保持 `jiahao-mode:time_state` 通道，在既有字段后增加
`session: UUID`、`elapsedTicks: int`、`cinematic: boolean`、`origin: Vec3d`、`yaw: float`。
会话 UUID 一次暂停只生成一次，20 Tick 周期刷新不会重播。首次收到晚到快照时直接进入
当前进度，发动者模型晚加载也使用同一时间线。100 Tick 后不补播。
这是协议格式变更，服务器与所有客户端必须安装本阶段的同一构建，不能混用旧版 JAR。

现有接收器先更新天气与世界状态，再调用 `JiahaoCinematicController.onStateSync`。
控制器拥有统一 `CinematicTimeline`，Camera 和 Pose 每帧读取同一个快照。
客户端逻辑 Tick 继续推进，渲染插值提供小数 Tick；与被冻结的 World Time、
FrozenRenderClock 完全分离。服务器更新做有界向前校正，不倒退；最多外推到
最近确认进度后 20 Tick，避免网络停顿时自行播完整段。

服务器开场只做一次位置确认并清空残留速度、停止物品使用。`Entity.move` 与跳跃入口
条件拦截，加上移动/交互网络包保护保持位置；不取消玩家整个 Tick，不改变重力设置，
不每 Tick 传送。脚下支撑、姿态失效或位置偏移超过 0.05 格时取消演出并同步，暂停继续。
演出窗口结束、死亡、断开、切维度、解除形态及世界卸载均有释放路径。

## Camera

`CameraCinematicMixin` 在原版 `Camera.update` 尾部调用 `setPos` / `setRotation`，
只改变本地拥有者的 Camera，并设置当帧第三人称标志使自身模型可见。
没有调用 `setCameraEntity`，没有更改玩家实体位置来实现轨道，没有替换整个 GameRenderer。

以起始 yaw 建立前向量 F、右向量 R，P 为服务器记录的玩家脚底中心：

```text
position = P + radius × (cos(angle) × F + sin(angle) × R) + height × Up
angle = lerp(135°, 360°, easeInOutCubic((elapsedTicks - 7) / 77))
radius = 4.5 → 4.0 → 3.2 → 2.2 格
height = 1.2 → 1.5 → 1.8 格
target = P + (0, 1.45, 0)
yaw = degrees(atan2(-look.x, look.z))
pitch = -degrees(atan2(look.y, hypot(look.x, look.z)))
look = target - cameraPosition
```

用户确认优先右后方起点和正面终点，实际单向环绕 225°。角度使用 easeInOutCubic，
半径、高度、入场和退场使用 smoothstep；LookAt 在整个正式轨道阶段对准上半身。
0–7 Tick 拉开，7–84 Tick 环绕并靠近，84–96 Tick 最终特写，96–100 Tick 返回原版。
过渡时同时平滑位置与朝向；最终 Pose 在 88 Tick 到位，保持约 0.6 秒再淡回原版。

碰撞使用目标到候选相机位置的 8 条偏移射线，按 VISUAL 形状忽略流体检测，
留 0.12 格采样体积及 0.15 格退让。受阻立即缩短，重新开阔时按帧时间指数平滑放远。
每帧最终距离仍受安全距离限制，过渡路径也参与检测，不设置会导致穿墙的最小半径。
极狭窄空间可能看不全人物，这是防穿墙优先的限制。

FOV 只在 `getFov` 的世界渲染返回值减去最多 10°，用户设置不改写。
演出期间抑制手部和镜头摇晃。Perspective 从未写入，因此 FIRST_PERSON、
THIRD_PERSON_BACK、THIRD_PERSON_FRONT 自然恢复原值。

上下黑边各占屏高 6%，前后各 5 Tick 淡入淡出，按实际窗口比例绘制。
仅隐藏准星，其他 HUD 在黑边之上正常绘制。

## Pose、输入与恢复

`PlayerModelCinematicMixin` 在原版 `PlayerEntityModel.setAngles` 后应用姿态。
头、身体、左右臂、左右腿使用插值；四段为低头、神秘摆手、侧身、最终托下巴。
身体轻微侧倾，双臂不对称，腿基本保持直立。袖子、外套、帽子和裤腿复制对应变换，
原版护甲通过 `copyBipedStateTo` 跟随。手持物使用原版对应手臂变换。

模型变换在覆盖前保存，渲染 finally 中恢复，下一次 setAngles 前再作防御恢复，
防止共享模型污染普通玩家。适用普通/纤细手臂模型，不改皮肤。
侧身看向统一的虚拟轨道方向，不使用旁观者自己的摄像机方向。

`KeyboardCinematicMixin` 清零当帧移动、跳跃和潜行输入，`MouseCinematicMixin` 只拦截
玩家转向，让原版照常消费鼠标增量和处理 GUI。`ClientActionsCinematicMixin` 拦截
攻击、持续挖掘与使用；按住的攻击/使用键需要松开后才恢复，避免积压动作误触发。
服务器只对演出者增加对应行为限制，不将原有“其他冻结玩家”的 GUI 限制套到演出者身上。

正常结束关闭全部覆盖。再次 R 收到结束确认后立即释放 Pose/输入，Camera 最多用
3 个客户端 Tick 快速退回；死亡、切维度、断开和世界替换直接清理，不等待过渡。
清理可重复调用，不改永久 FOV、Perspective 或键位配置。
单人 ESC 暂停冻结演出时间；多人服务器上的菜单不暂停演出。

## 文件清单

新增业务文件：

- `src/client/java/com/shouyun/jiahaomode/client/cinematic/`：`CinematicTimeline.java`、`JiahaoCinematicController.java`、`JiahaoCinematicCamera.java`、`JiahaoPoseController.java`、`JiahaoCinematicInput.java`。
- `src/client/java/com/shouyun/jiahaomode/client/mixin/`：`CameraCinematicMixin.java`、`GameRendererCinematicMixin.java`、`PlayerModelCinematicMixin.java`、`PlayerRenderCinematicMixin.java`、`HudCinematicMixin.java`、`KeyboardCinematicMixin.java`、`MouseCinematicMixin.java`、`ClientActionsCinematicMixin.java`。
- `src/main/java/com/shouyun/jiahaomode/mixin/`：`EntityCinematicMovementMixin.java`、`LivingEntityCinematicMixin.java`。

新增测试和文档：`CINEMATIC.md`、`scripts/cinematic-smoke.gradle`、
`src/visualtest/java/com/shouyun/jiahaomode/test/CinematicTimelineTests.java`、
`src/gametest/java/com/shouyun/jiahaomode/test/JiahaoCinematicTests.java`、
`src/cinematicsmoke/java/com/shouyun/jiahaomode/test/CinematicSmoke.java`、
`src/cinematicsmoke/resources/fabric.mod.json`。

修改：`.gitignore`、`build.gradle`、`README.md`、`TIME_STOP.md`、`TESTING.md`；
`JiahaoTimeStopManager`、`JiahaoTimeView`、`JiahaoTimeStatePayload`、
`ServerPlayNetworkHandlerTimeStopMixin`；`JiahaoModeClient`、`JiahaoTimeClientNetworking`、
`JiahaoTimeStopClientState`；两份业务 Mixin JSON、GameTest metadata、`JiahaoTimeStopTests`。
原 R 键类、C2S、世界模拟冻结及天气冻结公式保持原实现。

## 复现测试

```powershell
.\gradlew.bat build
.\gradlew.bat --no-configuration-cache -I scripts/cinematic-smoke.gradle runClient
# 可选：只在测试脚本中加载已下载并校验的 Sodium JAR
.\gradlew.bat --no-configuration-cache -I scripts/cinematic-smoke.gradle -PcinematicSodium runClient
```

客户端测试使用 `build/run/cinematicSmoke/saves/CinematicSmoke`；首次运行前复制一个
可用测试存档到该目录。会修改此隔离存档的地形、生物群系、天气和玩家状态，不可放日常存档。
可选 Sodium 路径为 `build/test-mods/sodium-fabric-0.6.13+mc1.21.1.jar`；
来自 [Sodium 官方版本页](https://modrinth.com/mod/sodium/version/mc1.21.1-0.6.13-fabric)，
SHA-512 为 `13032e064c554fc8671573dadb07bc70e6ea2f68706c65c086c4feb1d2f664346a3414cbf9d1367b42b8d063a35e40f2f967ef9af31642e1f0093b852161fe91`。
测试类和可选 Sodium 不加入发布 JAR，日常 runClient 不加载测试模组。

运行结果必须同时满足 Gradle 成功、`CINEMATIC SMOKE PASSED` 和结果文件 `PASSED`。
脚本会在数值断言失败时将任务判为失败。截图由游戏自身 framebuffer 输出到测试目录。
验证结果与人工验收范围见 [TESTING.md](TESTING.md) 的第三阶段记录。

## 22 项交付核对

| 项目 | 结果 |
|---|---|
| 1. 新增文件 | 见上方文件清单：5 个控制/数学类、8 个客户端 Hook、2 个通用移动 Hook，以及隔离测试和本文档 |
| 2. 修改文件 | 见上方文件清单；复用既有管理器、S2C 接收器、客户端入口与 Mixin 配置 |
| 3. 启动位置 | `JiahaoTimeClientNetworking` 接收既有服务器确认后调用控制器 |
| 4. Camera Override | 原版更新后仅覆盖本地 Camera 的位置、旋转和当帧第三人称标志 |
| 5. Camera Mixin | 使用一个 `CameraCinematicMixin`，不创建摄像机实体 |
| 6. 轨道公式 | `P + r(cosθ F + sinθ R) + h Up`，右后方到正面 225° |
| 7. Easing | easeInOutCubic、smoothstep；碰撞放远用与帧时间有关的指数平滑 |
| 8. 高帧率 | 独立客户端 Tick + render delta，每帧采样；30/60/180 FPS 数学测试及实际数百移动帧通过 |
| 9. 碰撞 | 8 条偏移射线、安全退让、立即收近/平滑放远；实际墙边场景通过 |
| 10. LookAt | 根据上半身目标与相机的差向量求 atan2 yaw/pitch；逐帧方向断言通过 |
| 11. Pose Controller | 同一时间线四段插值，模型原版计算后覆盖，finally 恢复 |
| 12. 模型部位 | 头、身体、双臂、双腿；帽子、袖子、外套、裤腿、护甲同步 |
| 13. 远程 Pose | 已实现；服务器旁观者同步及客户端远程模型检查通过，双窗口观感仍待人工 |
| 14. 输入锁定 | Keyboard/Mouse/ClientActions Hook + 服务端移动/跳跃/交互保护 |
| 15. FOV | 渲染值临时缩小最多 10°，用户设置保持不变 |
| 16. Perspective | 从未改写设置，三种视角自动回到各自原版更新路径 |
| 17. 提前结束 | R 确认后释放 Pose/输入并短退场；死亡、换维度、断开立即清理 |
| 18. 时间暂停 | 原回归通过；100 Tick 结束演出，160 Tick 结束暂停 |
| 19. Weather Freeze | 原雨雪/云/粒子真实客户端回归再次通过 |
| 20. Build | `BUILD SUCCESSFUL`；4 个必需服务端 GameTest 与两套纯数学检查通过 |
| 21. runClient | Vanilla 和 Sodium 0.6.13 均真实进入隔离世界完成演出测试并正常退出 |
| 22. 人工观察 | 动态镜头手感、双窗口效果、不同宽高比和装备组合仍需人工体验，详见测试记录 |
