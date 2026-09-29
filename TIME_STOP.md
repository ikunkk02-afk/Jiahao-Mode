# 嘉豪时刻实现说明

Minecraft 1.21.1，Yarn 1.21.1+build.3，Fabric API 0.116.17+1.21.1。
没有升级依赖。包为 `com.shouyun.jiahaomode`，Mod ID 为 `jiahao-mode`。

第三阶段已在原系统上接入约 5 秒 Camera/Pose 演出，完整说明见 [CINEMATIC.md](CINEMATIC.md)。
稳定站立的拥有者前 100 Tick 额外锁定位移与交互，之后恢复自由操作；暂停仍持续最多 300 Tick。
空中等不稳定情况下直接跳过演出，保留原暂停行为。下文文件表记录第二阶段基础实现。

## 文件变更

下列 Java 路径均位于对应源集的 `com/shouyun/jiahaomode/` 下。

| 源集 | 新文件 |
|---|---|
| main | `timestop/JiahaoTimeStopManager.java`、`timestop/JiahaoTimeView.java` |
| main | `network/JiahaoTimeTogglePayload.java`、`network/JiahaoTimeStatePayload.java`、`network/JiahaoTimeNetworking.java` |
| main | 下表的 8 个 common Mixin，位于 `mixin/` |
| client | `client/JiahaoTimeKeyBindings.java`、`client/JiahaoTimeClientNetworking.java`、`client/mixin/ClientWorldTimeStopMixin.java` |
| gametest | `test/JiahaoTimeStopTests.java`、`test/JiahaoTimeTestBlocks.java` |
| resources | `src/main/resources/jiahao-mode.mixins.json`、`src/client/resources/jiahao-mode.client.mixins.json` |
| 文档 | 本文件 |

修改了 `JiahaoMode.java`、`state/JiahaoStateManager.java`、客户端 `JiahaoModeClient.java`、
主 `fabric.mod.json`、`zh_cn.json`、`en_us.json`、原变身测试工具、测试 `fabric.mod.json`、
`README.md` 和 `TESTING.md`。没有删除原资源；mappings 和依赖版本保持原样。
后续天气视觉修改在 `build.gradle` 增加隔离的视觉时钟测试任务，未改依赖。
测试源码提交在隔离的 `src/gametest`；编译测试类不进入发布 JAR。

## 按键与网络

`JiahaoTimeKeyBindings.initialize()` 通过 `KeyBindingHelper.registerKeyBinding` 注册
KEYSYM 类型的 GLFW R 默认键。已有客户端入口调用它；`wasPressed()` 发送请求。
没有扫描键盘扫描码，控制设置可重新绑定。每客户端 Tick 合并一次按键请求。

`JiahaoTimeTogglePayload` 是空内容 C2S `jiahao-mode:time_toggle`。
`JiahaoTimeNetworking` 使用 `PayloadTypeRegistry` 和 typed `ServerPlayNetworking` 接收器，
从当前连接取得玩家；检查已有 `JiahaoStateManager.isJiahao(player)`、存活、拥有者和冷却。
拥有者再次请求会结束；其他玩家无法覆盖拥有者。

S2C `JiahaoTimeStatePayload` 的 ID 为 `jiahao-mode:time_state`，包含维度 ID、active、
拥有者 UUID、剩余 Tick、逻辑游戏时间及 Day Time。第三阶段在同一个包追加会话 UUID、
经过 Tick、演出启用标志、演出原点及朝向；两端必须使用相同构建。开始、结束、加入、频道注册及换维度
同步，暂停期间每 20 Tick 刷新。`JiahaoTimeClientNetworking` 只接受当前维度快照，
写入非持久化的世界附件 `time_stop_view` 并校准时间。共用查询不引用客户端类。

中英文提示和按键名称使用翻译键。开始播放原版紫水晶钟声，结束使用较轻信标关闭声。
原形态附件的保存、死亡复制、同步，以及变身器和雨天演出保持原实现。

## 管理器与时钟

`JiahaoTimeStopManager` 的弱键 Map 按 MinecraftServer 实例隔离数据，值不持有服务器、
玩家或世界引用。内部按 `RegistryKey<World>` 保存运行时状态：拥有者 UUID、start/end Tick、
游戏时间/Day Time 快照、逻辑时间偏移及其他玩家的坐标/旋转记录。
另有 UUID → 冷却截止 Tick。没有全局 active boolean，没有 NBT 存储 active。

`START_SERVER_TICK` 增长独立 `long` 时钟，300 Tick 自动结束，结束后冷却 160 Tick。
每 Tick 检查活动维度的拥有者有效性并锁定新加入的玩家；不遍历全部实体，不复制实体 NBT，
不扫描区块或保存全部方块实体状态。

可扩展查询：`isTimeStopped(World/ServerWorld)`、`getOwner(World/ServerWorld)`、
`isOwner(ServerPlayerEntity)`、`shouldFreeze(Entity)`、`getRemainingTicks(ServerWorld)`、
`getServerTick(MinecraftServer)`；修改接口为 `startTimeStop` 和两个 `stopTimeStop` 重载，
只在服务器线程调用。

原版维度部分时间属性共享主世界底层时钟。底层继续推进，`World.getTime()` 和
`getTimeOfDay()` 返回该维度有效时间：暂停时返回快照，结束后使用运行时偏移继续增长。
其他维度正常推进，计划更新不集中补执行。客户端冻结本地时间 Tick 并用服务器快照校准天空。
未修改 `doDaylightCycle`。偏移只保留在运行时；重启后使用原版底层时间，天空可能向前校准，
但 active 不会恢复、计划更新的剩余延迟仍按存档记录加载。

## Mixin 作用

| Mixin | 目标与作用 |
|---|---|
| `ServerWorldEntityTimeStopMixin` | 条件取消 `tickEntity`、`tickPassenger`；还跳过 Yarn `method_31420` 实体列表回调里的 `checkDespawn`，防止冻结怪物因拥有者远离而消失 |
| `ServerWorldSimulationTimeStopMixin` | 仅跳过方块/流体调度器、天气、方块事件、袭击和龙战控制器；阻止睡眠跳夜及清雨 |
| `ServerChunkManagerTimeStopMixin` | 只包装 `tickChunks` 中 `TickManager.shouldTick()` 调用，跳过随机/降水方块更新和自然生成，保留区块加载、保存、广播 |
| `WorldTimeStopMixin` | 返回维度逻辑时间；跳过 `BlockEntityTickInvoker.tick()`，保留失效 ticker 清理；客户端方块实体也按同步状态冻结 |
| `ChunkSerializerTimeStopMixin` | 计划方块/流体更新保存使用维度逻辑时间，保持剩余延迟；在偏移存在时也生效 |
| `ServerPlayerTimeStopMixin` | 冻结非拥有者 `playerTick()`，补上网络处理器独立调用玩家模拟的路径 |
| `ServerPlayNetworkHandlerTimeStopMixin` | 在线程切换检查之后拒绝冻结玩家的移动、攻击、物品、方块和容器操作；保留传送确认、聊天和保活；清理悬空检测计数 |
| `EntityPushTimeStopMixin` | 任一实体被冻结时跳过碰撞推力，防止速度持续累加 |
| 客户端 `ClientWorldTimeStopMixin` | 冻结 `tickTime`、非拥有者 `tickEntity/tickPassenger`，整理前帧位置避免渲染插值漂移；天气视觉更新还禁止环境 display ticks 和闪电闪光倒计时 |

## 天气视觉冻结（后续修改）

原版 1.21.1 雨雪位于 `WorldRenderer.renderWeather`，云位于 `renderClouds`。
它们使用渲染器自己的 `ticks` 和传入的 `tickDelta`，不依赖被冻结的世界时间，也不是雨实体。
因此先前即使服务端天气和实体停止，雨雪与云仍会动画。
本轮检查本地 Yarn 源码及字节码后，只修改这些方法读取的动画时间和局部插值参数。

`client.visual.JiahaoTimeStopClientState` 是现有权威状态的视觉快照，不是第二套技能系统。
同维度 S2C 首次 active 时记录最近显示帧的动画时间、插值比例、雨量和原始雷暴强度；
每 20 Tick 的重复 active 同步不会重新拍快照。结束时释放；换 ClientWorld 或断线时清理。
天气快照的世界仅通过弱引用绑定。天气视觉阶段没有修改网络格式；第三阶段追加的演出字段
仍经由同一 S2C 接收器分发。R 键和形态 API 保持不变，换世界同时清理电影演出。

`FrozenRenderClock` 保存冻结时刻和累计时间偏移。暂停期间始终返回同一个整数 Tick 和
小数 Tick；雨雪的纹理滚动和随机列动画、云的世界空间漂移均使用这组参数。
相机坐标和视角仍由原版传入，所以玩家可以从其他位置观察同一片静止天气。
恢复时设置 `offset = 当前原版时间 - 冻结时间`，随后按原版速率推进；多次暂停累计偏移，
没有 300 Tick 的追赶跳跃。切换世界后重置，不持有上一存档的冻结状态。

| 客户端 Mixin | 作用 |
|---|---|
| `WorldRendererTimeAccess` | 只读原版渲染 Tick，绝不停止或改写真实计数器 |
| `WorldRendererWeatherTimeStopMixin` | 雨雪/云方法内替换时间读取和局部 delta；暂停时禁止 `tickRainSplashing` 的新水花与随机雨声；冻结非拥有者实体和方块实体渲染插值；换世界清理快照 |
| `WorldWeatherTimeStopMixin` | 锁定雨量/雷暴的显示值及原版渐变字段，暂停时拒绝更新，恢复从捕获值继续；虽目标为 World，但只列入 client 配置且只处理当前客户端世界 |
| `ParticleTimeStopMixin` | 为每个粒子提供默认 false 的 `timeStopImmune` 标志 |
| `ParticleManagerTimeStopMixin` | 暂停普通粒子及 emitter 的个别 Tick，渲染使用冻结的局部插值；保留管理器队列、清理和渲染以及免疫粒子 |
| `ClientWorldTimeStopMixin` | 原实体/世界时间冻结继续生效；新增禁止环境随机显示更新、锁定闪电闪光倒计时 |

普通粒子包含暂停前已经存在的水花；暂停后新普通粒子也默认冻结。未来演出使用
`JiahaoTimeStopClientState.markTimeStopImmune(particle)` 标记后可正常运动。
没有冻结整个 ParticleManager、Camera、鼠标、渲染循环、全局 tickDelta、GUI 或声音引擎。
`tickRainSplashing` 停止后不产生新的随机雨声，已有雷声/其他声音可播完；本轮未增加静态雨声循环。

太阳、月亮和星空继续使用现有维度 Day Time 冻结。1.21.1 `getSkyAngle` 使用
`getLunarTime()`，不需要第二套天空时钟。LightningEntity 的 Tick 已被原实体门控覆盖，
客户端额外锁定的闪光计数避免闪光提前结束。服务端 `tickWeather` 仍由既有 Mixin
跳过，clear/rain/thunder timer、raining/thundering 状态及自然天气转换均不推进。

所有新视觉类位于 client 源集。渲染器易被替换的调用点允许零匹配，避免其他渲染模组
移除这些调用后因强制匹配而启动崩溃；这不等于已验证 Sodium/Iris 兼容。
Vanilla 的雨雪、云、天空、天气强度、闪电和普通粒子已接入；实际视觉验收仍见测试清单。
Iris Shader 自行使用的 TIME uniform、Shader 雨和体积云可能继续动画，后续需要专门兼容层。

所有模拟取消均查询当前世界暂停状态；实体路径排除拥有者。时间 getter 和序列化在恢复后
继续处理偏移以保护调度延迟。没有取消整个 `ServerWorld.tick()`，没有暂停主线程或全局 TickManager。

跳过实体更新保留速度、位置、旋转、年龄、投射物内部状态和 TNT 引信，恢复后执行原版 Tick。
不会反复 `setVelocity(0)`。嘉豪攻击即时按原版伤害处理，原版受伤无敌时间也随敌人 Tick 冻结，
没有增加延迟伤害结算。

其他玩家移动包被服务器拒绝并按锁定坐标纠正；待确认传送不会重复发送，每 Tick 限制纠正次数。
被拒绝的交互仍确认序列、同步必要方块和背包，避免假方块或背包预测残留。
拥有者发动自动下车；暂停时拒绝对冻结实体的右键交互，包含重新骑乘。

## 生命周期及边界

Fabric 死亡、断线、换维度、世界卸载、服务器停止事件执行幂等清理；解除形态在现有 setter
立即通知管理器。服务器 Tick 检查作为兜底。服务器停止先结束，再保存；时间偏移保留到
`SERVER_STOPPED` 后才删除。客户端附件随当前 ClientWorld 销毁，不会污染重新加入的世界。

本版冻结自动模拟。以下仍按原版/其他模组逻辑执行：

- 嘉豪主动操作造成的即时邻居更新及即时伤害/爆炸；计划更新和活塞事件仍延后。
- 管理员命令、全局命令函数/底层定时事件、世界边界动画。
- 其他模组绕过这些入口直接执行的逻辑，Shader 自定义动画以及已经播放的声音。
- 原版纹理图集中的流动纹理等未在本轮拦截；它们与 GUI/物品共用资源，需另做有范围的渲染兼容。

这些路径不能通过取消整个世界 Tick 安全拦截。若后续需要更严格的画面静止，应针对具体
可见变化加有限拦截，并保留拥有者交互与网络响应。没有摄像机、滤镜、Shader、自定义音频。

构建、箭/TNT、服务端、客户端与 warning 的实际结果见 [TESTING.md](TESTING.md)。
最终提交 SHA 和 `origin/main` 推送结果在交付消息中报告。
