# 第一阶段验证记录

验证日期：2026-09-28。Minecraft 1.21.1 / Yarn 1.21.1+build.3 /
Fabric Loader 0.19.5 / Fabric API 0.116.17+1.21.1。

## 构建和运行

- `gradlew.bat clean build --console=plain --warning-mode=all`：功能首次完整编译成功。
- `gradlew.bat build --console=plain --warning-mode=all`：最终业务代码及 GameTest 通过。
- 实际 Java 字节码版本为 65，即 Java 21；本机运行时使用原配置 JDK 25。
- `runClient`：模组加载、纹理图集与音效引擎初始化成功；日志记录单人测试世界载入、玩家登录及正常保存退出。
- `runServer`：独立 Dedicated Server 到达 `Done`，执行 `stop` 后三个维度正常保存并退出。
- 客户端和 Dedicated Server 使用临时启动配置，分别位于 `build/run/clientSmoke`
  与 `build/run/dedicatedSmoke`；没有改变日常运行世界。
- 检查发布 JAR：主类、客户端入口、中英文语言文件、模型、MIT 许可证均存在；
  不含测试类、示例 Mixin 或 Mixin 配置，`fabric.mod.json` 无版本占位符残留。

## 自动集成测试

`src/gametest` 使用当前版本的 Fabric GameTest API，运行在真实服务端世界，
以两个独立玩家和内存连接验证。测试模组只加入测试启动，不加入日常客户端、
Dedicated Server 或发布 JAR，也没有新增第三方测试依赖。

已通过的场景：

- 实际执行 `/give @s jiahao-mode:jiahao_transformer` 并确认物品进入背包。
- 通过物品服务端 `use` 首次开启、再次解除；返回值阻止副手继续处理同一次交互。
- 玩家状态相互独立，物品不消耗，无耐久。
- 开启、解除发送包含对应 translation key 的 Action Bar 消息；开启发送一次音效。
- 服务端向玩家自身发送 Fabric 状态同步消息。
- 开启立即设置雨天及 6000 Tick 天气时长，关闭雷暴；未新增闪电实体或造成伤害。
- 向当前维度玩家发送一次原版下雨开始、雨量和雷暴强度消息。
- 同值设置不重播提示、音效或天气；解除不修改天气、不发送清天气消息。
- 玩家 NBT 含 `fabric:attachments` 下的 `jiahao-mode:jiahao_state`；NBT 恢复不触发天气。
- 正常玩家保存产生磁盘 `.dat`，通过原版玩家加载流程恢复形态。
- 真正调用原版死亡重生流程后保留状态，不触发下雨。
- 下界往返保留状态；在下界重新开启不改变主世界天气。
- 调用末地返回使用的存活重生流程后保留状态。
- 服务端线程以外的修改被拒绝且原状态保持。

## 用户清单对应结果

| 检查项 | 结果与验证范围 |
|---|---|
| 游戏正常启动 | 通过：客户端与单人世界载入日志 |
| Fabric Loader 加载模组 | 通过：客户端、测试服务器、Dedicated Server |
| `/give` 获取变身器 | 通过：实际服务端命令与背包断言 |
| 物品名称正确 | 英文命令显示通过；中英文资源检查通过，中文画面待手动验证 |
| 首次右键进入、再次解除 | 服务端物品使用通过；实体客户端鼠标交互待手动验证 |
| 进入提示 | 服务端发送正确 Action Bar 文本通过，画面待手动验证 |
| 解除提示 | 服务端发送正确 Action Bar 文本通过，画面待手动验证 |
| 进入时立即下雨 | 服务端状态及当前维度天气消息通过，客户端降水画面待手动验证 |
| 没有有害闪电 | 通过：实体数量、血量断言及代码检查 |
| 解除不清天气 | 通过：天气状态、计时器和消息断言 |
| 切换维度状态正常 | 通过：真实下界往返、末地返回使用的存活重生流程 |
| 保存退出再进入仍保留 | NBT 与磁盘保存加载通过；完整客户端退出再进世界待手动验证 |
| Dedicated Server 类加载安全 | 通过：真实 Dedicated Server 启动、保存、退出及 common 引用检查 |
| `gradlew.bat build` | 通过：包括服务端 GameTest |
| 多人同步 | 玩家状态独立及自身状态消息通过；两个真实客户端的观察者同步待手动验证 |
| 创造栏、音效表现 | 注册及音效发送检查通过；画面、音量和听感待手动验证 |

未完成的画面、音效及真实客户端联机操作没有标记为通过。此环境可启动游戏，
但没有可操作 Minecraft 原生窗口的自动化接口。

## Warning 与环境日志

最终 Java 编译无弃用 API 或其他编译 warning。运行日志仍包括：

- JDK 25 对 Minecraft 自带 JNA 原生访问及 Guava `Unsafe` 的运行时警告。
- 删除空客户端 Mixin 资源后，Loom 对不存在的空 `build/resources/client` 目录给出开发类路径警告；
  实际客户端、测试服务器和 Dedicated Server 均成功运行，发布 JAR 验证通过。
- 客户端原版山羊音效缺失和原版 shader sampler 警告。
- Mojang 公钥/测试账号资料服务请求超时，不影响本地测试通过。
- 临时本机 Dedicated Server 绑定 `127.0.0.1` 且使用 offline mode，出现对应原版警告；
  初次平坦测试世界创建还记录了原版生成参数回退消息，服务器仍正常启动。
- Git 的 LF/CRLF 转换提示，未修改原有全局 Git 配置。

首次测试编写时发现命令执行接口在当前 mappings 返回 `void`，已按编译结果修正，
使用背包内容验证命令成功；没有通过删减功能规避错误。

## 文件变更

新增业务代码：`item/ModItems.java`、`item/JiahaoTransformerItem.java`、
`state/JiahaoState.java`、`state/JiahaoStateManager.java`、
`effect/JiahaoTransformationEffects.java`，基础包均为 `com.shouyun.jiahaomode`。

新增资源：`assets/jiahao-mode/lang/zh_cn.json`、`lang/en_us.json`、
`models/item/jiahao_transformer.json`。

新增测试：`src/gametest/java/com/shouyun/jiahaomode/test/JiahaoTransformationTests.java`
及 `src/gametest/resources/fabric.mod.json`；新增本记录 `TESTING.md`。

修改：`JiahaoMode.java`、主 `fabric.mod.json`、`build.gradle`、`README.md`。
模板导入提交还将许可证声明与远端 MIT 统一，并保留 `LICENSE-FABRIC-TEMPLATE`。

删除：模板中的 `ExampleMixin.java`、`ExampleClientMixin.java` 及两个对应 Mixin JSON。
保留主入口、客户端入口和数据生成入口；所有依赖版本、Minecraft、mappings、
Java 编译目标和原有 `gradle.properties` 保持不变。

# 第二阶段：嘉豪时刻验证记录

验证日期：2026-09-28。版本配置与第一阶段相同。实现说明及完整新增文件清单见
[TIME_STOP.md](TIME_STOP.md)。

## 构建与启动

- `gradlew.bat build --console=plain --warning-mode=all`：`BUILD SUCCESSFUL`，3 个必需 GameTest 通过。
- 原变身生命周期测试保留并通过：变身器命令获取、形态切换、保存加载、死亡复制、维度转换、雨天及同步。
- `runClient`：真实客户端加载模组，进入复制的单人测试世界并登录；日志记录三次嘉豪时刻开始与约 8 秒自动恢复，之后正常保存退出；启动任务成功结束。
- `runServer`：Dedicated Server 到达 `Done`，接受 `stop`，三个维度保存并正常退出，启动任务 `BUILD SUCCESSFUL`。
- 临时运行目录为 `build/run/timeStopClientSmoke` 与 `build/run/timeStopDedicatedSmoke`；没有改动日常存档。
- Java 编译目标仍为 21，本机仍用原配置 JDK 25；发布 JAR 含 common/client Mixin 配置和业务代码，不含 GameTest 类及测试方块。

## 自动行为验证

新 `jiahao_time_stop` 批次通过真实服务器世界、实体、方块系统和原版网络处理器执行断言；
连接使用内存 Netty channel。独立 `END_SERVER_TICK` 驱动断言，不使用被冻结的世界时间
作为唯一超时依据，另有 2000 个服务器 Tick 看门狗。
扩大后的测试区域清理地形并确保涉及区块可 Tick；仅在隔离测试世界开启原版随机 Tick
和天气循环，因为原版 TestServer 默认关闭它们。

已通过：

- 实际空内容 C2S 交给已注册接收器：普通状态拒绝，嘉豪状态开始，同维度另一拥有者拒绝；再次请求提前结束。
- 160 个服务器 Tick 精确自动结束；结束后 59 Tick 仍冷却，60 Tick 可再次开始。
- 僵尸、箭、TNT、掉落物、经验球、船、矿车、盔甲架、雪球和三叉戟的位置/年龄冻结，恢复后重新 Tick。
- 箭在暂停时位置、速度及年龄不变，解除后的 Tick 沿原方向移动；TNT 引信保持 20，恢复先变 19，剩余 20 Tick 后爆炸。
- 物品位置与物品年龄不增长，碰撞调用不向冻结实体积累推力。
- 拥有者继续 Tick、跳跃产生向上速度、合法移动和视角包生效、切换物品，以及对冻结僵尸立即造成伤害。
- 发动时从船下车；其他玩家移动/跳跃包、攻击、使用变身器、丢物品、创造背包修改、容器点击及放方块被拒绝。
- Day Time 和有效游戏时间冻结，原 daylight gamerule 不变；恢复每 Tick 增长，无暂停时间补跑。
- 下雨状态与天气计时保持，恢复后天气倒计时继续。
- 熔炉燃料、漏斗物品、计划红石更新、水流、活塞事件和真实随机更新被冻结并恢复。
- 计划红石更新在恢复后等待剩余 20 Tick，再执行；没有在解除瞬间集中执行。
- 原版区块序列化/反序列化往返保留方块和流体计划更新剩余延迟，暂停 30 Tick 后再次验证。
- 主世界暂停时，下界和末地时间继续；下界独立开启/解除不影响主世界拥有者。
- 开始、20 Tick 剩余时间刷新、自动结束、暂停期间加入及频道注册发送 S2C；状态 codec 往返正确。
- 解除形态、实际死亡、实际连接断开、真实下界往返、世界卸载事件均释放暂停。
- `zz_shutdown_canary` 批次留下真实 active stop；服务器关闭日志确认在保存之前结束。玩家 NBT 不含暂停附件；新测试服务器不继承上次 active。

## 用户 15 项测试对应

| 测试 | 结果与范围 |
|---|---|
| 1 普通状态按 R | C2S 拒绝和翻译提示通过；实体键盘输入待人工 |
| 2 变身后发动 | 既有形态 API 与 C2S 开启通过；客户端日志记录实际开启 |
| 3 僵尸冻结/恢复 | 自动位置/年龄及恢复 Tick 通过；走动画面待人工 |
| 4 箭空中暂停/恢复 | 自动位置、速度保留与轨迹恢复通过；悬停画面待人工 |
| 5 TNT 引信 | 20 → 暂停保持 → 19 → 剩余 Tick 后爆炸，通过 |
| 6 掉落物 | 位置、年龄冻结及恢复，通过 |
| 7 黄昏太阳 | Day Time 在 12000 冻结与继续，通过；天空画面待人工 |
| 8 本人移动/跳跃/视角 | 服务端实际方法和移动包通过；手感待人工 |
| 9 8 秒上限 | 160 Tick 断言与客户端约 8 秒日志通过 |
| 10 再次 R | 第二次 C2S 提前结束，通过；实际键盘交互待人工 |
| 11 暂停期间死亡 | 原版死亡流程及事件释放，通过 |
| 12 退出/重进 | 实际断开和服务器停止清理、非持久化断言通过；active 时完整客户端退出再进待人工 |
| 13 换维度 | 原版 teleportTo 下界往返，原维度立即恢复，通过 |
| 14 雨天 | 雨天状态/计时与恢复通过；雨景待人工 |
| 15 无永久速度丢失 | 箭速度保留、所有测试实体恢复 Tick，通过 |

## 待人工验收与限制

尚未通过自动原生窗口操作确认：R 重绑定、中文控制菜单、箭悬停插值、太阳、雨滴、
音量听感，以及两个真实客户端的冻结操作和重连。客户端日志证明加载与开启/自动结束，
不代表这些画面或听感已验收。
区块测试覆盖原版序列化/反序列化，不宣称已完成暂停时磁盘自动卸载/重载全过程测试。

嘉豪造成的即时邻居更新、管理员命令/全局函数/定时事件、世界边界及其他模组直接执行的
逻辑继续；相应原因和后续完善方向见 `TIME_STOP.md`。原版伤害立即处理，没有延迟结算。

## Warning

最终 Java 编译没有弃用 API 或其他编译 warning，Mixin 在真实客户端及 Dedicated Server
成功加载。运行仍有以下环境或原版日志：

- JDK 25 的 JNA 原生访问和 Guava Unsafe 警告，客户端 LWJGL 的 JNI 版本提示。
- Mojang 公钥/测试账号资料请求超时或握手失败；不影响本地测试和世界启动。
- 客户端原版山羊音效缺失和 shader Sampler2 警告。
- 隔离 Dedicated Server 的 offline-mode 提示和初次平坦生成参数回退日志。
- Git LF/CRLF 转换提示，保留现有全局配置。

第一阶段记录的空 client resources 类路径提示已因新增客户端 Mixin 配置而消除。
开发测试遇到的重复传送确认、测试区域地形、TestServer 默认关闭随机更新，以及在
GameTest Tick 外直接完成导致批次监听器未通知等问题均修复；没有删除失败断言来伪造通过。
