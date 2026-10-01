# 第一阶段验证记录

最新的第五阶段闪避验证位于本文末尾；上方各阶段条目保留历史验证范围。

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

# 天气视觉冻结验证记录

验证日期：2026-09-28。沿用现有管理器、形态 API、R 键和 S2C，未改依赖或服务端暂停逻辑。
本轮需求取代第二阶段“雨滴继续运动”的视觉设计；上方第二阶段记录保留为历史结果。

## 构建、实际客户端和独立服务器

- `gradlew.bat build --console=plain --warning-mode=all`：`BUILD SUCCESSFUL`；3 个既有服务端 GameTest
  和新增 `runVisualClockTests` 均通过。箭/TNT 及变身、天气 Timer、生命周期回归保持通过。
- 完整构建复跑暴露旧计划更新测试的区块边界假设：相邻红石灯/水源并不总在同一区块。
  已改为分别序列化各自区块并保留全部剩余延迟断言，修正后重新运行完整构建。
- `src/visualtest` 验证雨雪实际 UV 时间公式、160 Tick 暂停、小数 Tick、恢复连续性、重复暂停、
  雨动画整数掩码边界与重置；不加载 Minecraft 客户端，不进入发布 JAR。
- 实际运行 `gradlew.bat --no-configuration-cache -I scripts/weather-visual-smoke.gradle runClient --console=plain`。
  客户端进入隔离存档 `build/run/weatherVisualSmoke/saves/WeatherSmoke`，通过原 C2S/S2C 开始和结束，
  两轮各等待 160 个服务端 Tick 自动恢复；日志有 `WEATHER VISUAL SMOKE PASSED`，退出任务
  `BUILD SUCCESSFUL in 58s`，三个维度保存完成。
- 客户端测试实际将周围 biome 改为 plains / snowy_plains，并断言客户端降水分别为 RAIN / SNOW。
  Fancy 云开启；冻结时钟、雨量/雷暴强度、天空角度、闪光计数、普通粒子 Tick/局部插值不变；
  原版渲染 Tick、粒子绘制调用和免疫粒子继续推进。旋转玩家视角/修改位置后断言仍通过。
  每轮恢复时有效动画时间相差小于 2 Tick，普通粒子恢复；断开后视觉偏移清理。
- `runServer` 真实 Dedicated Server 启动到 `Done`，随后 `stop`，三个维度正常保存，
  启动任务 `BUILD SUCCESSFUL`。新类全部位于 client 源集和 client-only Mixin 配置。
- 第一轮测试通过数值断言后，脚本直接调用原版 disconnect 等待未被请求停止的集成服务器，
  测试退出没有完成。已修正脚本先请求集成服务器停止并重新完整运行通过；业务代码无需改动。
- 日常存档没有改变；可选测试模组只在 init 脚本启动时加载，默认 `runClient` 不加载它。
  复测时先将一个可用的测试存档复制到上述 WeatherSmoke 目录，再执行可选命令。
  脚本会更改这个隔离存档的 biome、天气、时间和测试玩家形态，因此只使用测试存档。

这些客户端断言验证运行路径和数值，不能替代对最终画面与声音的人工判断。
此环境不能自动操作/观察 Minecraft 原生窗口，没有将以下画面项目伪称已验收。

## 12 项人工验收

| 项目 | 自动验证与仍需人工观察 |
|---|---|
| 1 雨滴定格 | 雨生物群系、渲染时钟与雨 UV 数学已通过；雨幕实际定格需观察 |
| 2 移动时雨仍静止 | 测试移动玩家后时钟仍固定；走动时世界空间雨幕需观察 |
| 3 转镜头不恢复雨动画 | 视角改变后断言通过；自由鼠标转镜头画面需观察 |
| 4 雪花停止 | 实际雪生物群系和雪 UV 数学通过；雪花悬停画面需观察 |
| 5 Fancy 云停止 | Fancy 模式与共享动画时钟通过；云层实际漂移需观察 |
| 6 恢复云不跳跃 | 两轮恢复时钟连续及重复暂停测试通过；云画面需观察 |
| 7 太阳/月亮/星空 | 客户端天空角度冻结通过；不同时间的天空需观察 |
| 8 Rain Gradient | 捕获强度、不同 delta 和暂停时修改被拒绝通过；过渡画面需观察 |
| 9 箭与雨同时悬停 | 服务端箭轨迹/速度回归和客户端雨时钟分别通过；同屏效果需观察 |
| 10 ActionBar/HUD 正常 | 未拦截 HUD、输入或全局渲染时间，渲染帧持续；菜单/聊天/背包需人工操作 |
| 11 恢复全部天气视觉 | 时钟及粒子恢复通过；雨雪云完整视觉与雨声恢复需观察 |
| 12 退出重进不残留 | 真正断开后清理断言、集成服务器保存退出通过；重进画面需观察 |

还需人工检查已有水花悬停、雷暴闪光和暂停期间不产生新的明显随机天气声。
Sodium/Iris 未安装参与本轮测试；未声称完成兼容验收。
Vanilla 天气各入口已冻结；Shader TIME、Shader 降水/体积云、世界纹理图集动画未接入，
已播放的雷声允许播完，没有实现声音采样暂停或静态环境雨声循环。

## 日志及发布范围

Java 编译没有新增 warning，真实客户端和 Dedicated Server 没有 Mixin 注入错误。
仍有 JDK 25 的 JNA/Unsafe/LWJGL 警告、原版 shader sampler/山羊音效提示、
Mojang 公钥请求超时及本地 offline-mode 提示；它们未阻止构建、测试或正常保存退出。
发布 JAR 只含业务代码/资源，测试类、测试模组资源、存档、日志、缓存均不提交或打包。

# 第三阶段：电影演出验证记录

验证日期：2026-09-29。沿用 Minecraft 1.21.1 / Yarn 1.21.1+build.3 / Fabric 0.116.17+1.21.1，
没有新增业务依赖。实现、协议和完整文件清单见 [CINEMATIC.md](CINEMATIC.md)。

## 构建与服务端测试

- 最终 `gradlew.bat build --console=plain`：`BUILD SUCCESSFUL`，4 个必需 GameTest 全部通过。
- `runCinematicTests`：轨道端点、半径/高度边界、easing 连续性、30/60/180 FPS 逐帧采样、
  重复/旧快照不倒退、晚加入、网络卡顿外推限制及有界校正通过。
- 新增服务器场景验证：稳定站立启动、残留速度清空、真实 move/jump/移动包均被锁定、
  100 Tick 只解除演出、160 Tick 才恢复世界、空中仍可暂停但不演出、落地不补播、
  外部位移仅取消演出、再次 R、死亡及实际跨维度传送清理。
- 40 Tick 时接入另一个真实 ServerPlayer 及内存网络连接，通过原有频道注册接收 S2C，
  验证相同 session/owner/origin 和 elapsedTicks=40，证明晚加入同步复用现有消息。
- 原变身、冷却、不同维度、箭/TNT/生物/掉落物、计划方块和流体更新、序列化、天气 Timer、
  原版生命周期与关闭 canary 均保留并通过。原暂停测试使用无演出的场景，保留原拥有者移动/伤害断言；
  新演出测试另外覆盖前 100 Tick 锁定及之后恢复，没有删除原来的断言。
- 新 S2C codec 往返包括新增会话、进度、原点和方向字段。

## 真实客户端与 Sodium

在隔离存档实际执行 `gradlew.bat --no-configuration-cache -I scripts/cinematic-smoke.gradle runClient`，
最终日志为 `CINEMATIC SMOKE PASSED`，结果文件内容 `PASSED`，Gradle `BUILD SUCCESSFUL`。
随后/另行通过同一套测试验证 Sodium，显式加 `-PcinematicSodium`，不进入默认依赖。
Sodium 版本为官方 Fabric 1.21.1 的 0.6.13，下载后核对 SHA-512，来源及哈希见实现文档。

实际覆盖：

| 场景 | 自动检查 |
|---|---|
| 第一人称、第三人称后视、第三人称前视 | 每种完整走原 C2S/S2C、Camera 更新和实体渲染，原 Perspective/FOV 设置保持；5 秒左右结束时暂停仍在 |
| 高帧率 | 限帧 180，最终 Vanilla 的完整轨道采样分别有 638/639/642 个移动帧；纯数学测试另覆盖 30/60/180 FPS |
| LookAt | 正式轨道每帧相机前向量与上半身目标向量点积 > 0.9999 |
| 碰撞 | 墙边场景每帧检测 target→camera 射线没有越过方块，实际输出墙边截图 |
| 雨景冻结 | Camera 移动期间天气视觉时钟每帧保持同一值；测试位置生物群系设为 plains，画面确实有雨 |
| 输入 | 实际按键状态注入 W/跳跃，检查输入清零和实体位置稳定；释放后不留锁 |
| ESC | 实际打开 GameMenuScreen，确认集成服务器真正暂停，暂停期间演出进度保持，关闭后继续 |
| 聊天 | 演出期间实际打开和关闭 ChatScreen，序列继续且不丢原视角 |
| 再次 R | 发送原 C2S，服务器结束暂停，客户端走短退场，无持续相机/输入覆盖 |
| 死亡 | 真正调用服务器死亡流程，客户端退出演出，之后请求原版重生 |
| 换维度 | 真实 Overworld→Nether，原维度暂停与客户端演出清理，之后返回继续测试 |
| 断开 | 演出中真实断开并保存退出，Camera/Input 活跃标志清零 |
| 远程 Pose | 客户端用远程玩家模型和晚到快照验证仅 Pose 生效、镜头/输入无覆盖，袖子/护甲复制变换、结束后原版姿势恢复 |
| Sodium | 0.6.13 完整重复上述七轮生命周期场景，通过；没有 Class/Mixin 注入错误 |

本轮还实际启动独立 Dedicated Server，监听本机临时端口，达到 `Done` 后发送 `stop`，
三个维度保存完成，任务 `BUILD SUCCESSFUL`。隔离目录为 `build/run/cinematicDedicated`。

截图使用游戏自身 ScreenshotRecorder 从 framebuffer 生成，已检查四段姿态和最终近景，
并据实际截图将最终手臂从遮脸位置下调到下巴附近。截图也证明人物可见、无准星、
上下黑边与物品栏共存。没有替换玩家皮肤，截图中的形象是测试账号的默认皮肤。

## 人工验收边界

- 数值每帧检查和静态截图已完成；完整动态镜头的观感、真实鼠标/键盘手感和 180 Hz 屏幕表现仍需人工体验。
- 已有真实服务器旁观者连接同步检查和真实客户端远程模型检查，但没有同时操作两个真实游戏窗口完成视觉验收。
- 16:9 的真实截图已检查；16:10、21:9 采用相同屏幕比例计算，仍需人工确认黑边/HUD 布局。
- 普通/纤细模型共用字段并保留原支点；各种装备与走路、游泳、骑乘、滑翔的全组合视觉矩阵未逐一人工观察。
- 极狭窄空间优先不穿墙，可能无法形成完整人物构图；没有宣称支持 Iris Shader Camera、ReplayMod 或 Flashback。
- 用户期望的雨滴、箭和怪物同屏定格场面：本轮雨景与 Camera 同屏截图、既有箭/怪物服务端测试均通过，完整场景观感仍需人工验收。

## 调试过程与日志

首轮新增 GameTest 的完成回调在测试 Tick 外执行，导致批次等待；改为在 GameTest Tick 内
报告完成后通过。首轮客户端测试误把“提前结束后的短退场”也断言为世界仍冻结；修正了该测试
条件，业务提前取消逻辑未因此更改。客户端测试脚本增加结果文件检查，不能用 Gradle 启动成功
掩盖业务断言失败。最终测试脚本也移除了执行阶段读取 Task.project 的 Gradle 弃用用法。

保留的环境提示包括 JDK 25 JNA/Unsafe、LWJGL、Mojang 公钥或账号资料连接失败，
以及原版资源警告；没有把外部认证服务连接问题写成业务测试失败。

主要本地证据（均在忽略的 build/ 内，不提交）：

- `build/cinematic-build.log`
- `build/cinematic-client.log`
- `build/cinematic-sodium.log` 与 `build/cinematic-sodium-result.txt`
- `build/run/cinematicSmoke/cinematic-smoke-result.txt`
- `build/run/cinematicSmoke/screenshots/cinematic-*-pose-*.png`
- `build/run/cinematicDedicated/logs/latest.log`

所有业务源码、测试源码和文档可提交；测试存档、截图、日志、第三方测试 JAR、build/run/.gradle
均不提交，测试类不加入发布 JAR。

## 最终天气回归

最终业务代码上重新运行原有 `scripts/weather-visual-smoke.gradle runClient`，日志为
`WEATHER VISUAL SMOKE PASSED`，任务 `BUILD SUCCESSFUL in 59s`。实际雨/雪生物群系、
Fancy 云时钟、天气强度、天空、普通/免疫粒子、8 秒自动恢复及断开清理全部通过。
日志为 `build/cinematic-weather-regression.log`。电影测试脚本最后的弃用 API 清理另经
`runClient --dry-run --warning-mode=all` 验证配置成功，无需重复业务演出断言。

# 第四阶段：嘉豪语录验证记录

日期：2026-09-29。实现与新增文件清单见 [QUOTES.md](QUOTES.md)。Minecraft、Fabric、Yarn、Java 编译目标与原配置一致。

## 已完成验证

| 项目 | 实际结果 |
| --- | --- |
| `gradlew.bat build --console=plain` | `BUILD SUCCESSFUL in 22s`；5 个服务器 GameTest 全通过，既有天气时钟、镜头数学测试通过 |
| 语录真实客户端 | `gradlew.bat -I scripts/quote-smoke.gradle runClient --console=plain`，`BUILD SUCCESSFUL in 51s`，结果文件 `PASSED`；实际进入隔离世界并保存退出 |
| Cinematic / Pose 回归 | 原有 cinematic-smoke 启动，`BUILD SUCCESSFUL in 1m 58s`、`CINEMATIC SMOKE PASSED`；全部视角、提前结束、死亡、维度切换与退出场景通过 |
| 天气回归 | 最终源码编译产物的真实客户端运行：`WEATHER VISUAL SMOKE PASSED`，退出码 0；雨雪、Fancy 云时钟、天气强度、天空、粒子、8 秒恢复、断线清理通过 |
| Dedicated Server | 最终公共代码实际到达 `Done (1.030s)`，发送 stop，三个维度保存退出，退出码 0 |
| 发布 JAR | 37 个中英文语录键对应一致；JAR 不含测试类和 quote-smoke 资源 |
| Git 内容检查 | 无 build、run、.gradle、日志、崩溃报告、截图和测试存档进入提交 |

## 用户 20 项测试的证据范围

| 测试 | 证据 |
| --- | --- |
| 1 普通状态请求 | GameTest 与真实客户端发送空 C2S，服务器不发语录；V 注册及客户端形态过滤代码检查 |
| 2 延迟变身 | 20 Tick 前不发送、随后发送一次；真实客户端收到 TRANSFORM |
| 3 连续变身解除 | 服务端取消预约、客户端无残留；保留手动冷却 |
| 4 手动随机 | 真实 C2S/S2C 收到 MANUAL，并截图 |
| 5 狂按冷却 | 服务端同 Tick 100 次请求拒绝；客户端 30 次连发不重启字幕；49/50 Tick 边界 |
| 6 不连续重复 | 多类别确定性选择测试及真实客户端连续两次 Manual ID 不同 |
| 7 时间暂停 | 原有真实电影测试全通过 |
| 8 正面字幕 | 真正时间暂停中 Progress >= 0.74 后收到并显示固定招牌句，截图确认 |
| 9 一次演出一次 | 服务端 session 调度断言，客户端同一次显示开始时间不重置 |
| 10 结束冲突 | 服务器结束事件等待高优先级语录和间隔；实际客户端结束字幕到期清理 |
| 11 低血一次 | 同一次低血阶段只发一次 |
| 12 低血重置 | 恰好 50% 不重置，严格高于 50% 后再次低血可重发 |
| 13 敌对击杀 | 真实创建僵尸并以玩家伤害击杀；固定随机种子验证成功，失败概率分支另有断言 |
| 14 普通状态击杀 | 非嘉豪击杀回调不发语录；盔甲架/牛死亡事件不触发 |
| 15 附近头顶 | 服务端内存连接验证 64 格包含、65 格及其他维度排除；客户端渲染真实 OtherClientPlayerEntity 头顶文字并截图 |
| 16 旁观者无字幕 | 远端 UUID 消息不会进入 local subtitle；不同 UUID 分开保存 |
| 17 淡入淡出 | 真实渲染帧采样 0.15 秒淡入、最后 0.25 秒淡出，数学端点验证 |
| 18 冻结期间动画 | 字幕年龄、透明度在世界冻结时正常推进，电影语录实际在冻结期间绘制 |
| 19 换维度清理 | 服务器取消并保留冷却；客户端实际主世界到下界后旧显示清空；错误维度消息拒绝 |
| 20 退出清理 | 真正 disconnect 后客户端有效语录为空 |

另测：伤害阈值、18%/20% 成功失败概率分支、真实小伤害经过 Mixin 不误触发、30 秒 IDLE 边界、无效演出 session、重复回退、未知 Quote ID 与旧事件拒绝。概率分支采用预先找到并重置的随机种子，不靠重复运行碰概率。

## 视觉核对与人工边界

已查看最终 framebuffer 截图：中文电影字幕位于快捷栏上方、黑边之外；头顶文字带深色底、朝向摄像机，与玩家名分开。使用游戏自身 ScreenshotRecorder 在整帧结束采集。

多人网络断言使用真实服务器玩家和内存连接，头顶渲染使用真实客户端中的远端玩家实体。**未启动两个人工操作的独立客户端联机**，不声称完成该项人工验收。仍建议人工检查双客户端观感、改键、不同 GUI 缩放及资源包/Shader 组合；标准渲染的深度模式已实现，复杂遮挡场景未做逐像素自动判定。

## 本轮修正与环境限制

- 新增 GameTest 最初在独立 server tick 回调中直接结束，改为在 GameTest 自身 Tick 回报结果，避免批次等待。
- 伤害概率测试最初与结束语录占用重叠，修正测试时间安排，保留冷却规则。
- 客户端资源重载会暂停单人服务器；测试等待 overlay/暂停结束再计时。
- 测试截图最初早于 HUD 完整提交；改成仅测试使用的 GameRenderer 尾部截图 Mixin，并单独使用 mixin 子包。
- 内存连接跨维度后没有客户端传送确认，导致原版无敌；测试显式完成传送确认后真实伤害断言通过。
- 重跑测试存档可能保存于下界；固定初始化到主世界后，实际跨维度清理断言通过。
- 最后的天气 Gradle 启动和 Dedicated Gradle 复查遇到 Mojang 版本清单 TLS/下载失败，发生于游戏加载前。离线 Gradle 也因缺少版本清单失败，没有标记为通过。
- 因现有游戏、依赖和最终编译产物仍完整，最后两项运行使用成功构建生成的 classpath/启动参数，直接启动本地 JVM；天气和 Dedicated 真实运行均通过。没有变更项目依赖、关闭 TLS 校验或伪造 Mojang 版本清单。未来重新配置 Gradle 可能需要恢复到 Mojang 元数据服务的网络连接。
- 保留原有 JDK 25 JNA/Unsafe、原版资源及 Mojang 账号服务网络警告；它们与业务断言分开记录。

主要证据均在忽略的 build/ 内：

- `build/quote-build.log`
- `build/quote-client.log`
- `build/run/quoteSmoke/quote-smoke-result.txt`
- `build/run/quoteSmoke/screenshots/quote-cinematic.png`
- `build/run/quoteSmoke/screenshots/quote-speech-bubble.png`
- `build/quote-cinematic-regression.log`
- `build/quote-weather-regression.log`（Gradle 元数据连接失败）
- `build/quote-weather-cached-runtime.log`（最终实际天气测试通过）
- `build/quote-dedicated-cached-runtime.log`（最终实际服务器启动/退出通过）

# 第五阶段：闪避与完美闪避验证记录

日期：2026-09-29。实现、文件清单、参数和用户 30 项交付对应见 [DODGE.md](DODGE.md)。
Minecraft/Fabric/Yarn 及业务依赖版本没有改变；Java 编译目标仍为 21。

## 构建前置恢复

第四阶段末尾及本轮初始 `build --dry-run` 的失败发生于 Mojang 版本清单 TLS 下载阶段，未进入编译。
已通过本机已有代理与核实的 Mojang 官方服务地址恢复下载，将**未经修改的官方版本清单**放到
Loom 正常缓存位置。curl 保持证书校验，没有伪造版本元数据、关闭 TLS 或替换项目依赖。
之后显式 `gradlew.bat build` 和各 `runClient` 均能正常配置和运行，不需要离线替代启动。
Mojang 账号公钥/皮肤资料服务仍有网络警告，和已恢复的依赖元数据下载分别记录。

## 构建及服务端集成测试

- 最终 `gradlew.bat build --console=plain`：`BUILD SUCCESSFUL`；**14 个必需 GameTest 全通过**。
- 原有形态、天气/实体冻结、Cinematic、语录测试与视觉时钟/镜头数学检查保留。
- 新增 9 个闪避场景：方向/分类、运动/包/冷却/障碍、伤害/行政、空中窗口、真实伤害边界、
  演出与暂停拥有者/盾牌/死亡、跨维度/退出、冷却/语录/连完美边界、R 恢复后的真实箭命中。
- 真正运行 ServerPlayer 的 network handler Tick 和 travel，而非用 setPosition 模拟技能位移。

| 核验 | 实际覆盖 |
|---|---|
| 方向与速度 | Yaw 0/90/180/-90/33，前后左右、对角归一化、相反抵消、无输入后撤、六步总 2.8 格、递减且不超过 1.2 |
| 请求与位置 | typed C2S、非法位 255 拒绝、同动作 100 次连发拒绝、伪造坐标和落地声明丢弃、合法视角保留且方向不改变 |
| 冷却 | 结束后冷却、最后一个冷却 Tick 拒绝、恰好到期接受；变身切换不能绕过 |
| 碰撞 | 原版完整墙、关闭木门和墙角均裁剪位移；最终包围盒不嵌入墙 |
| 空中 | 保留下降、离地最多一次、冷却结束仍不能第二次、服务端落地重置；真实悬崖含潜行状态无抬升/悬崖安全补偿，fall 来源仍伤害 |
| 完美 | Tick 0 近战取消、Tick 3 箭取消、Tick 4 箭伤害正常、同动作第二击正常、金额 0 不消费；晚请求不能撤销已经发生的伤害 |
| 分类与扩展 | 环境/周期/虚空/行政排除；三叉戟和魔法投射物来源、未知模组近战/投射物来源允许；测试数据包 undodgeable 类型优先拒绝 |
| 行政 | 实际执行 `/damage @s 2 minecraft:arrow`，健康减少且机会不消费；genericKill 死亡不取消并清除动作 |
| 使用与暂停 | shield 使用被停止；演出中拒绝 C，独立 server Tick 104 时仍处于暂停但拥有者允许 C |
| 恢复投射物 | 真实箭在 R 中保持位置/速度，恢复后实际碰撞命中 Tick 0–3，正常消费一次完美机会且生命保持 20 |
| 清理 | 在动作已经 travel 后真实跨维度，清除旧水平技能速度并保留原版垂直速度；保留冷却，退出清除旧历史 |
| 语录与连完美 | 首次符合条件必播、字幕到期后仍受 80 Tick 冷却、恰好 80 Tick 可播；60 Tick 内连续成功累加 |

额外测试类型/标签仅放在 gametest 资源中，没有进入发布 JAR。

## 真实单机客户端

显式执行 `gradlew.bat -I scripts/dodge-smoke.gradle runClient --console=plain`，结果文件
`build/run/dodgeSmoke/dodge-smoke-result.txt` 为 `PASSED`，Gradle `BUILD SUCCESSFUL`。
隔离 DodgeSmoke 存档通过原版 QuickPlay 加载，测试后正常保存三个维度并退出。

- 实际 KeyBinding 按压分派 → C2S → 服务器六步移动 → S2C → 客户端显示，验证默认 C 与原版共享按键仍有效。
- 非形态请求拒绝；Yaw 对应左闪在客户端真实可见，结束后恢复普通移动。
- 实际 Zombie `tryAttack`、Skeleton 发射的真实 ArrowEntity、Creeper 点燃/Fuse、TntEntity Fuse=0。
- 四种攻击都触发完美且本人生命保持 20；TNT/Creeper 对旁边猪造成伤害、破坏羊毛方块，
  本人额外爆炸击退仍然存在，证明没有取消整个爆炸。
- 真实渲染帧观察 Pose、完美字幕、FOV 峰值 >3.5°、左 Roll <−3.5°；纯后撤 Roll=0。
  效果结束 FOV/Roll 回零，用户 Options FOV 不改变。
- 截图使用原版 ScreenshotRecorder 在完整 framebuffer 渲染后采集，已检查正常侧身、完美效果、
  语录和雨景；默认测试账号皮肤，未增加皮肤替换。
- 普通 cloud 与完美 cloud/crit 使用现有暂停免疫接口。旧天气测试同时验证普通/免疫粒子时间行为。

## Dedicated Server 与两个独立客户端

`scripts/dodge-multiplayer-smoke.gradle` 从当前 Loom 配置导出三组真实启动参数，
`python scripts/dodge-multiplayer-smoke.py` 启动一个 Dedicated Server 和两个独立 JVM 客户端。
服务器仅绑定 127.0.0.1:25576，隔离世界/玩家/选项位于 build/run；没有访问日常存档。

- server、actor、observer 三个结果文件均 `PASSED`；协调器输出 `DODGE MULTIPLAYER ALL PASSED`。
- Dedicated Server 启动达到 `Done`；服务器验证六步、2.8 格端点、完美后健康 20。
- A 实际按 C；B 通过原版玩家实体跟踪收到多个连续位置样本并看到对应 Pose/完美状态。
- A 镜头有效且恢复；B 的 FOV 和 Roll 始终为零，各自 Options FOV 保持。
- 两客户端正常断开退出，随后发送原版 stop，服务器正常保存三个维度并以 0 退出。
- 已检查 B 的 framebuffer 截图，A 模型/名字、雨景和 B 自己的第一人称视角均存在。
  不是内存网络连接、同进程假客户端或单纯启动成功检查。

## 已有实机回归

| 入口 | 结果 |
|---|---|
| `scripts/cinematic-smoke.gradle runClient` | `CINEMATIC SMOKE PASSED`，`BUILD SUCCESSFUL in 1m 45s`；三个视角、镜头/模型、输入、ESC/聊天、再次 R、死亡、维度和断开 |
| `scripts/weather-visual-smoke.gradle runClient` | `WEATHER VISUAL SMOKE PASSED`，`BUILD SUCCESSFUL`；真实雨/雪、Fancy 云、天气强度、天空、普通/免疫粒子、暂停/恢复与断开 |
| `scripts/quote-smoke.gradle runClient` | 结果 `PASSED`，`BUILD SUCCESSFUL in 53s`；V、电影字幕、语录优先级、生命周期和截图 |

旧服务端测试另覆盖变身、下雨、冻结箭/TNT/生物、计划方块/流体、R 生命周期和恢复。
新增演出/闪避测试使用独立 server Tick，避免把被冻结的 GameTest 世界时钟当作演出计时。

## 发布检查、警告与边界

- 发布 `jiahao-mode-1.0.0.jar` 不含 test 类、smoke 入口、测试 Mixin 或测试伤害类型。
  main/client 入口和 Mixin 环境区分正确，Dedicated 实际加载成功；43 条中英文语录键完整，伤害标签存在。
- Git diff 检查无空白错误；提交只包含源码、资源、可重复测试脚本和文档。
  build、.gradle、存档、日志、截图、导出参数和第三方运行依赖均排除。
- 保留 JDK 25 的 JNA/native-access/Unsafe/LWJGL 提示、原版山羊音效/shader sampler 警告，
  Mojang 公钥超时/皮肤资料 TLS 失败及本地 offline-mode 的聊天验证提示。
  没有新增业务编译 warning 或 Mixin 注入错误，这些环境提示未阻止测试与正常保存退出。
- 测试客户端按键由原版 KeyBinding API 注入，实际走业务网络和渲染；真实硬件手感、完整动态观感仍需用户体验。
  自定义伤害类型验证兼容规则，未逐个安装第三方战斗模组；本轮没有新增 Iris/ReplayMod/Flashback 兼容验收。
  这些观察边界不被表述成已经完成的人工体验测试。

补充组合测试后发现并修正原有时间暂停的计划 Tick 时间基准：`WorldAccess.createOrderedTick`
的两个重载原本读取原始 properties 时间，恢复后的新计划 Tick 会多等待已暂停的时间。
新增 `WorldAccessTimeStopMixin` 明确覆盖两个重载，统一为服务器有效时间；客户端和非服务器
WorldAccess 保持原值。旧计划 Tick 序列化的 20 Tick 断言保留并重新通过。
空中测试还显式清空完整飞行通道，避免随机 GameTest 起点与原有随机 Tick 测试的大范围方块重叠。
闪避夹具显式保持所用区块运行，完成后释放自己新增的强加载；确保原版玩家 Tick、登录保护倒计时
和投射物模拟正常执行，避免模拟连接没有区块订阅造成的随机免伤。没有跳过原版登录保护或放宽伤害断言。

开发中捕获并修正：默认 C 的原版共享键冲突、网络 Tick 恢复旧位置、测试连接的传送确认，
时间暂停时 GameTest 时钟停止、重跑存档停在下界、行政伤害测试的 Vanilla PvP 限制、
跨维度保留的闪避余速、截图检查后校正张臂的旋转方向，以及联机参数导出任务捕获 Task 对象导致的 configuration-cache 错误。
导出脚本现保存普通值，最终配置缓存正常存储；没有用忽略失败的方式取得通过结果。

## 第六阶段：嘉豪盔甲、穿戴限制与固定台词（2026-09-29）

本轮基线工作区干净，原 14 项必需 GameTest 与 build 通过。实现后最终
`gradlew.bat build` 输出 **BUILD SUCCESSFUL in 18s**，**All 17 required tests passed**；
既有视觉时钟和镜头数学检查也属于 build 的 check 依赖。

新增三项 GameTest：

- `registrationAllEquipmentCombinationsAndSmithing`：真正执行四个 `/give`；验证注册、耐久、
  护甲、韧性、附魔能力及钻石修复；遍历全部 15 种不完整组合，拒绝变身且不下雨，
  Action Bar 使用准确翻译键；拒绝错误栏位和直接 setter 绕过。
  四个真实 SmithingScreenHandler 输出均正确，保留 protection 3、名称、石英 SENTRY 纹饰、
  损耗 123 点，最大耐久改为嘉豪数值，三个输入各消费一个。
- `everySlotClearsTimeStopDodgeAndQuotes`：分别移除头、胸、腿、脚，实际处于稳定地面演出
  和时间暂停；最迟下一服务器 Tick 清持久化标记、释放世界和服务端演出输入锁，
  旁观者每轮最新语录消息均为取消、最新时间消息均为 inactive。
  各栏位另外在闪避期间移除，动作与形态被清除。缺装备立即拒绝时间、闪避和手动语录。
  重穿全套不自动变身，完整装备实际属性为 21 护甲、10 韧性。
- `oldSaveWithoutArmorClearsFlagOnLogin`：写入真实磁盘玩家数据，确认其中仍有旧的开启附件
  但没有装备，再用相同身份执行真实登录加载。JOIN 清除持久化标记且不重播雨天；
  登录后穿齐也不会自动恢复。旧生命周期测试仍覆盖 NBT/磁盘、死亡丢装备、keepInventory
  保留装备、下界来回及活体重生。

EmbeddedChannel 不参加 ServerNetworkIo，清理夹具与既有闪避测试一样每 Tick 驱动真实
networkHandler.tick；没有绕过原版装备属性更新或人工直接添加护甲属性。
测试传送先确认旧请求再设置并同步位置，避免旧登录传送把演员移离旁观者。
重生校验安排在 Fabric 默认附件复制阶段之后，防止无装备的新实体被之后的复制重新开启标记。

固定语录测试改为两个明确 ID，并验证随机池不含它们、立即开场、保护期间的优先级，
第 74 Tick 不再发 CINEMATIC、失败回退不重复、正常结束间隔、V 冷却、取消与生命周期。
原伤害断言加入真实护甲减伤；generic/fall 等原版绕过护甲的伤害仍保留原期望。

### 真实客户端回归

| 入口 | 本轮实际结果 |
|---|---|
| `gradlew.bat -I scripts/quote-smoke.gradle runClient` | PASSED；最终 BUILD SUCCESSFUL in 55s，立即固定台词、3 秒淡入淡出、V、头顶、换维度、清理 |
| `gradlew.bat -I scripts/cinematic-smoke.gradle runClient` | CINEMATIC SMOKE PASSED；BUILD SUCCESSFUL in 1m 47s，三视角、镜头/Pose、盔甲复制动作、输入、死亡/维度/断开 |
| `gradlew.bat -I scripts/weather-visual-smoke.gradle runClient` | WEATHER VISUAL SMOKE PASSED；BUILD SUCCESSFUL in 56s，真实雨雪、云、粒子、恢复和断开 |
| `gradlew.bat -I scripts/dodge-smoke.gradle runClient` | PASSED；BUILD SUCCESSFUL in 54s，真实 C 键链路、盔甲演员、僵尸/箭/TNT/苦力怕、完美状态、Pose 和镜头恢复 |

均使用 build/run 下的独立存档，没有修改日常 run/saves。检查正式盔甲正面、背面、
兜帽口罩、裤腿、靴子和演出截图：没有紫黑缺失贴图，背部大白标、胸前小标和眼缝正常。
128×64 RGBA 资源由真实客户端加载，检查透明 UV 留白和白色图案像素。
初次过早截图尚未完成世界加载，随后延后首拍并补拍稳定正面；没有把天空截图作为外观验收。
发现立即固定字幕与原版 Action Bar 靠得太近后，上移字幕并重跑语录/联机，最终截图两行清楚分开。

### Dedicated Server、两个真实客户端与保存重进

```powershell
.\gradlew.bat -I scripts/armor-multiplayer-smoke.gradle -ParmorRole=server exportArmorLaunch
.\gradlew.bat -I scripts/armor-multiplayer-smoke.gradle -ParmorRole=actor exportArmorLaunch
.\gradlew.bat -I scripts/armor-multiplayer-smoke.gradle -ParmorRole=observer exportArmorLaunch
python scripts/armor-multiplayer-smoke.py
```

Dedicated 仅绑定 `127.0.0.1:25577`。导出时复制各角色独立 Loom 启动配置，避免其他 smoke
任务覆盖共享配置后改变启动行为。测试模组仅在显式使用脚本时加入，不进入发布 JAR。

最终协调器输出 **ARMOR MULTIPLAYER AND DISK RELOAD ALL PASSED**，first/reload 两轮
server、actor、observer 六个结果均 PASSED，所有客户端正常退出，服务器 stop 正常保存并以 0 退出。

- A/B 都是真正独立 JVM 客户端；B 原版玩家跟踪看到 A 的四件装备和完整外观。
- A 通过真实变身器 `interactItem` 成功变身、下雨、立即显示第一句；B 收到同一固定 ID，
  确认准确中文，并在实际 framebuffer 显示头顶文字。
- A 发送现有时间请求，自己看到立即字幕和电影镜头；B 看到第二句头顶文字，自己的镜头不被接管。
- 同一会话经过第 74 Tick，观察消息记录无随机 TRANSFORM、TIME_STOP_START 或 CINEMATIC 开场。
- 演出/冻结期间服务器移除 A 头盔，两客户端确认形态失效、冻结解除、输入与镜头恢复、
  台词取消，世界昼夜时间重新前进；重穿全套不自动变身。
- A 再次成功变身后正常断开并保存，服务器彻底退出。重新启动同一磁盘世界与相同两个身份，
  A 装备和形态恢复；没有重播固定台词，没有恢复时间暂停或演出。再脱装备、重穿仍安全清理。

服务器移除装备测试覆盖服务端权威清理；没有将其宣称为人工操作库存界面的手感测试。
真实眼缝宽度、标识线条和深色布料明暗仍可由用户按审美调整；当前版本已经可直接测试使用。
本轮未新增 Shader 或第三方战斗模组兼容验收。

### 发布与本地证据

发布 `build/libs/jiahao-mode-1.0.0.jar` 已检查六张 PNG、四份新模型、四份配方及现有变身器模型
全部包含；45 条语录键中英文一致，固定中文逐字正确。没有测试类、测试 Mixin、测试入口或测试伤害类型。
main/client 边界与既有业务 Mixin 配置保持；Dedicated 实际启动成功。
Git diff 无空白错误，build/run/.gradle/logs/crash-reports、截图和导出运行参数均被排除。

本地证据（不提交）：

- `build/armor-final-build.log`、`build/armor-release-verification.txt`
- `build/armor-{quote,cinematic,weather,dodge}-regression.log`
- `build/armor-export-{server,actor,observer}.log`
- `build/armor-multiplayer-coordinator.log`、`build/armor-multiplayer-{first,reload}-{server,actor,observer}.log`
- `build/run/armorDedicated/armor-server-result.txt`
- `build/run/armor{Actor,Observer}/armor-client-result.txt`
- `build/run/armorActor/screenshots/actor-armor-front.png`、`actor-armor-front-stable.png`、`actor-armor-back.png`
- `build/run/armorActor/screenshots/actor-transform.png`、`actor-time-notice.png`、`actor-restored-control.png`
- `build/run/armorObserver/screenshots/observer-transform-bubble.png`、`observer-time-bubble.png`、`observer-restored-world.png`

原有环境级 JDK/native-access/Unsafe/LWJGL、山羊音效、shader sampler、Mojang 公钥/TLS 及
offline-mode 聊天验证提示仍存在，未阻止本轮构建、资源显示、保存或正常退出。

本地运行证据（不提交）：

- `build/dodge-build.log`
- `build/dodge-client.log` 与 `build/run/dodgeSmoke/dodge-smoke-result.txt`
- `build/run/dodgeSmoke/screenshots/dodge-pose-0.png`、`dodge-perfect-0.png`、`dodge-quote.png` 等九张截图
- `build/dodge-cinematic-regression.log`、`build/dodge-weather-regression.log`、`build/dodge-quote-regression.log`
- `build/dodge-export-server.log`、`build/dodge-export-actor.log`、`build/dodge-export-observer.log`
- `build/dodge-multiplayer-server.log`、`build/dodge-multiplayer-actor.log`、`build/dodge-multiplayer-observer.log`
- `build/run/dodgeDedicated/dodge-server-result.txt`
- `build/run/dodgeActor/dodge-client-result.txt`、`build/run/dodgeObserver/dodge-client-result.txt`
- `build/run/dodgeActor/screenshots/actor-perfect.png`、`build/run/dodgeObserver/screenshots/observer-dodge-pose.png`


## 第七阶段（2026-09-30）

本节记录当前第七阶段版本；前文是既往阶段的历史验收记录。

- 每个主要模块完成后运行 `gradlew.bat build`，最终构建见 `build/phase7-final-build.log`：`BUILD SUCCESSFUL`。18 个必需服务器 GameTest 全通过，四组客户端纯数学/数据模型检查通过（冻结时钟、运镜、Pose、娱乐道具）。
- 模块构建记录：`build/phase7-{pose,duration,quotes,moment,textures,market,code}-build.log` 均 `BUILD SUCCESSFUL`。
- `gradlew.bat --no-configuration-cache -I scripts/cinematic-smoke.gradle runClient`：`CINEMATIC SMOKE PASSED` 和 `BUILD SUCCESSFUL`。三种原版视角、初始模型可见性、相机碰撞、八套静态模型恢复、护甲跟随、提前解除、死亡/换维度/断线清理通过。
- 既有 `quote-smoke.gradle`、`weather-visual-smoke.gradle`、`dodge-smoke.gradle` 的真实 `runClient` 回归均 `BUILD SUCCESSFUL`；天气 15 秒恢复、固定台词与完美闪避仍可用。
- `scripts/phase7-multiplayer-smoke.gradle` 导出配置、`scripts/phase7-multiplayer-smoke.py` 协调一个独立 Dedicated Server 和两个真实 JVM 客户端，三进程结果均 PASSED，正常断开及保存。仅连接 `127.0.0.1:25579`；隔离目录在 `build/run/phase7Dedicated`、`phase7Actor`、`phase7Observer`。测试客户端同时演出时各有自己的镜头，旁观者只看到演员 Pose；首次随机窗口按 900–2400 Tick 自然发生，世界继续运行，字幕按时出现。GUI 拒绝窗口、个人开关、时停接管镜头、时停演出结束后仍可闪避、受伤和打开 GUI 后清理会话均通过。
- 两个 GUI 在嘉豪/普通形态下均右键打开；真实 `/give` 三种物品成功。Market 截图可见轴、网格、80 点起步的变化折线、价格与涨跌；BUY/SELL 无数据副作用。Code Screen 实际键入 `test`、`dir`、`cmd`、`powershell`、`rm`、`bash`、`time.stop()` 都只生成字面文本和假进度；两种 GUI 的 ESC 和重新打开行为通过。
- `scripts/verify-phase7-release.py` 从发布 JAR 检查三张 16×16 RGBA 透明 PNG、模型引用、两份配方、42 句原文/英文对应、唯一语言键、客户端与 Dedicated Server 边界及无系统执行/文件/网络接口；输出 `PHASE7 RELEASE VERIFICATION PASSED`。
- 审美验收可打开本地 [视觉检查页](build/verification/phase7-review.html)：七套 Pose、真实时停/随机演出、两种 GUI、三张正式图标。游戏性能上纯行情模型十万次更新仍最多 128 点；终端最长 128 字、20 条历史、80 行输出。

本地证据（`build/` 已忽略，不进入仓库）：

- `build/phase7-final-build.log`、`build/phase7-cinematic-final.log`、`build/phase7-weather-regression.log`、`build/phase7-quote-regression.log`、`build/phase7-dodge-regression.log`
- `build/phase7-multiplayer-summary.log`、`build/phase7-multiplayer-first-{server,actor,observer}.log`、`build/phase7-release-verification.txt`
- `build/run/phase7{Dedicated,Actor,Observer}/*-result.txt`、`build/run/phase7Actor/screenshots/`

TEST 1–24 的对应关系和实现细节见 [STAGE7.md](STAGE7.md)。人工仍需以自己使用的角色皮肤和画面设置确认 Pose 观感、镜头构图、三个 16×16 图标及两个 GUI 的视觉偏好。


## 第八阶段（2026-09-30）：豪气与强制爆发

本节为本轮实际验证；前文保留历史验收结果。机制和完整中英语录见 [HAO_METER.md](HAO_METER.md)。

### 构建与服务端

`gradlew.bat build` 最终 **BUILD SUCCESSFUL**，23 个必需 GameTest 全通过，五组独立检查通过（冻结时钟、电影时间轴、Pose、道具模型、豪气时间轴）。新增测试覆盖：附件 NBT 编解码、普通死亡重生复制、加载未完成标记后的登录清零、普通切维度保留数值及增长锁、每秒增长、敌对死亡 +6、Perfect Dodge +12、手动时停 +8、随机瞬间 +4、道具时序/令牌/冷却/换手失效、满值 pending、容器推迟、随机/手动演出冲突、旁观者与晚加入同步、240 Tick 结束、100 Tick 增长锁、死亡清理、第二位玩家排队以及合法/伪造的异常取消。

关键验收在同一真实服务端场景中以 20% 血量、战斗标记和 targeting zombie 三个条件同时存在测试：`safe` 仍成立。96 豪气通过实际 `allowDamage` Perfect Dodge 事件补满，当前闪避结束后少于 25 Tick 自动进入爆发，无脱战/回血等待。普通随机窗口测试单独重置其测试玩家的豪气，避免长达 100 秒的旧随机测试被本阶段新增的必然爆发接管。

### 真实 runClient

```powershell
.\gradlew.bat --no-configuration-cache -I scripts/hao-smoke.gradle runClient
```

在 `build/run/haoSmoke` 的隔离存档实际通过 `/jiahao hao set 99` 启动完整演出，无按键触发。测试模组检查真实 renderer、网络和音频引擎；正常完整演出、解除形态、切维度、死亡四个周期全部 PASSED。实际右键 Market/Code、点击 BUY、输入假命令，两个完成事件均得到服务端 +2。R 无法取消；四件盔甲跟随 3–5 个 Pose，2–4 条字幕按时间出现，镜头运动、雨/云时钟冻结、原视角/FOV 不变，结束后声音/输入/镜头/冻结清理。重复/落后开始包不会重启镜头或音乐，已结束会话的包不会重播。

截图在 GameRenderer 完整帧末尾保存，已检查豪气条与字幕、原版状态栏分开显示。测试使用生存低血量、靶向僵尸，并从资源 OGG 启动真实 SoundManager 实例，不用系统文件回退。测试存档上次死亡后，测试入口先正常发送重生请求，再开始下一轮；避免将已死亡玩家当作准备完成。

### Dedicated Server + 两个独立客户端

```powershell
.\gradlew.bat --no-configuration-cache -I scripts/hao-multiplayer-smoke.gradle -PhaoRole=server exportHaoLaunch
.\gradlew.bat --no-configuration-cache -I scripts/hao-multiplayer-smoke.gradle -PhaoRole=actor exportHaoLaunch
.\gradlew.bat --no-configuration-cache -I scripts/hao-multiplayer-smoke.gradle -PhaoRole=observer exportHaoLaunch
python scripts/hao-multiplayer-smoke.py
```

协调器结果 **HAO DEDICATED + TWO CLIENTS ALL PASSED**，server/actor/observer 三结果均 PASSED，客户端正常退出，服务器正常 stop 保存。只绑定 `127.0.0.1:25581`，三个角色各有独立 JVM/目录/启动配置。

发动者镜头、输入锁和本人音乐正常；旁观者看到同一多 Pose 和头顶语录，镜头/输入/音乐不被接管。两个客户端天气动画时钟固定，服务端世界时钟固定，12 秒后恢复。Dedicated 实际启动及完整演出证明公共初始化不会加载客户端声音/渲染类。测试模组和专用 Mixin 不进入发布 JAR。

### TEST 1–25 映射

| 附件 TEST | 实际证据 |
|---|---|
| 1–2 形态 HUD 显示/隐藏 | 真客户端截图确认显示；渲染按形态门控，解除形态周期通过；用户可复核 F1 和不同 GUI 比例 |
| 3 增长 | 服务端精确 Tick 检查；客户端 set 99 自然补满 |
| 4 Perfect Dodge | 实际伤害取消 +12、MAX、闪避结束自动启动 |
| 5–6 满值/无按键 | GameTest 和真实 set 99；战斗/低血量/敌人均无阻碍 |
| 7–8 世界/天气 | 服务端世界/实体固定、两个客户端天气时钟固定 |
| 9–10 音乐/资源 | 真 SoundManager isPlaying；资源流式 OGG，代码扫描无 FLAC/CloudMusic 读取 |
| 11–13 多 Pose/不重复/静止 | 服务器序列一致、千组随机种子无重复、四 Tick 后全权重静止，真实多模型渲染 |
| 14 环绕 | 真渲染帧相机坐标变化；继续复用已有碰撞保护 |
| 15 语录 | 完整客户端 2–4 句；时间轴 50 Tick 显示区间无重叠 |
| 16–19 正常结束 | 音量曲线端点与共享曲线测试；真客户端声音/镜头/时间恢复；服务端归零 |
| 20–21 死亡 | 真客户端周期及服务端死亡清理 |
| 22 手动时停 | 等完整 300 Tick，再跳过手动冷却启动爆发，无嵌套 |
| 23 Random Moment | 先完成 60 Tick 随机演出，再自动爆发；实际 +4 |
| 24 多人 | 两个独立客户端及 Dedicated，旁观者多 Pose/头顶、无相机/音乐 |
| 25 Dedicated 安全 | 独立服务器实际启动和整场演出；发布扫描公共代码无客户端引用 |

### 发布、音频及人工复核

`python scripts/verify-hao-release.py`：**HAO RELEASE VERIFICATION PASSED**。实际 ffprobe 为 Vorbis、44100 Hz、双声道；转换命令质量 5。正式 JAR 和 sources JAR 包含 sounds.json、注册和全部 20 条中英键，均没有 `jiahao_march.ogg` 或测试模组。精确本地音频路径已 Git 忽略、未跟踪；本地文件保留。Git diff 检查无空白错误。

人工仍需听感验收 0.75 相对音量、0.4 秒淡入/1 秒淡出与其他背景音乐的混合；不同皮肤、GUI 比例、狭窄墙角的镜头构图/碰撞、Pose 转换观感、Shader/Iris/Sodium 和其他镜头/战斗模组兼容。资源热重载/缺音频降级有实现和资源扫描，但本轮未额外做完整客户端热重载或移除音频实测；未进行断电崩溃再读磁盘的实机模拟（已验证加载的未完成附件标记清零）。其他维度同时两场爆发使用原有按维度时停隔离逻辑，本轮未启动两维度双客户端场景。

本地证据（build 已忽略）：

- `build/hao-build-final.log`、`build/hao-client-final.log`
- `build/hao-multiplayer-final.log`、`build/hao-multiplayer-first-{server,actor,observer}.log`
- `build/hao-release-verification.txt`
- `build/run/haoSmoke/hao-smoke-result.txt`、`build/run/haoSmoke/screenshots/hao-*.png`
- `build/run/haoDedicated/hao-server-result.txt`、`build/run/hao{Actor,Observer}/hao-client-result.txt`

原有环境级 JDK/native-access/Unsafe、Mojang TLS/公钥、offline-mode 和 shader sampler 提示仍存在，不阻碍本轮完成演出、保存或正常退出。音乐资源没有缺失/解码错误。


### 后续更新：音乐打包发布（2026-09-30）

按用户要求，取消 OGG 的 Git 忽略和 Jar 排除，歌曲加入仓库并随正式/源码 JAR 打包。上文“没有 OGG”的结论是首次交付时的历史策略，本次更新后不再适用。

`gradlew.bat build`：**BUILD SUCCESSFUL in 23s**，23 个必需 GameTest 和五组检查继续通过。`python scripts/verify-hao-release.py`：**HAO RELEASE VERIFICATION PASSED**；正式 JAR 和源码 JAR 内歌曲字节均与本地 OGG 完全一致，sounds.json 仍流式引用，编码为 Vorbis、44100 Hz、双声道，测试模组依旧排除。

本次只调整资源分发策略，没有修改播放、镜头或战斗逻辑；真实音乐播放沿用本节先前已通过的 runClient 实测。本次构建和资源证据为 `build/hao-music-build.log`、`build/hao-music-release-verification.txt`。仓库推送后发布 GitHub `v1.0.0`，附正式 JAR 与源码 JAR。

## 2026-09-30：R 进行曲与设备随机 Buff

实现说明、精确权重和持续时间见 [GADGET_BUFFS.md](GADGET_BUFFS.md)。本轮仍为 Fabric 1.21.1，编译目标 Java 21，本机以既有 JDK 25 运行 Gradle/Minecraft。歌曲沿用已跟踪的资源；项目所有者确认已购买版权并可使用。

### 最终执行证据

| 检查 | 结果与本地记录 |
|---|---|
| `gradlew.bat build` | `BUILD SUCCESSFUL in 38s`，**27 个必需 GameTest 全通过**，既有五组数学/模型检查通过；`build/gadget-final-build.log` |
| 设备与 R 专项真实客户端 | `scripts/gadget-smoke.gradle runClient`：`PASSED`、`BUILD SUCCESSFUL in 1m 50s`；`build/gadget-client-final.log`、`build/run/gadgetSmoke/gadget-smoke-result.txt` |
| Hao Burst 回归 | `scripts/hao-smoke.gradle runClient`：`PASSED`、`BUILD SUCCESSFUL in 1m 5s`；`build/gadget-hao-client.log` |
| 完整镜头回归 | `scripts/cinematic-smoke.gradle runClient`：`CINEMATIC SMOKE PASSED`、`BUILD SUCCESSFUL in 3m 8s`；`build/gadget-cinematic.log` |
| 专用服务器、发动者及旁观客户端 | `scripts/hao-multiplayer-smoke.py`：server/actor/observer 三个结果均 `PASSED`，`HAO DEDICATED + TWO CLIENTS ALL PASSED`；`build/gadget-dedicated.log`，各端详细日志仍在 `build/hao-multiplayer-first-*.log` |
| 天气冻结回归 | `scripts/weather-visual-smoke.gradle runClient`：`WEATHER VISUAL SMOKE PASSED`、`BUILD SUCCESSFUL in 1m 12s`；`build/gadget-weather.log` |
| 语录回归 | `scripts/quote-smoke.gradle runClient`：`PASSED`、`BUILD SUCCESSFUL in 1m 9s`；`build/gadget-quotes.log` |
| 闪避回归 | `scripts/dodge-smoke.gradle runClient`：`PASSED`、`BUILD SUCCESSFUL in 1m`；`build/gadget-dodge.log` |
| 发布包 | `python scripts/verify-hao-release.py`：`HAO RELEASE VERIFICATION PASSED`；`build/gadget-release-verification.txt` |

新增四个服务器测试覆盖精确累计权重边界、普通持续时间端点、5% Override 三种候选、等级/时长/无限效果保护、原版升级、不清理其他效果、旁观者拒绝、普通形态合法会话完成但无 Buff、BUY/SELL 共用冷却、Code 独立冷却、过早 COMMIT、100 次令牌重放与 BEGIN 请求、伪造会话、移动原物品失效，以及重新登录、重生、换维度后保留冷却和 599/600 Tick 边界。随机池使用可控随机序列，不靠概率碰运气。模拟连接显式 flush 后读取 OPEN/GRANT/结果包，避免误读上一操作的缓存响应。

设备专项实际使用物品右键与 GUI 按钮，Code 输入 `hello`、`cmd`、`powershell`、`rm -rf`、`shutdown`，仅显示假输出；普通形态 GUI 可用且无效果，嘉豪 BUY/SELL 和 Code 实际触发服务端状态效果，连续操作演出继续。R 使用真实按键绑定，验证普通形态拒绝、服务器确认前无声音、镜头结束后的原实例继续、再次 R 的原实例音量递减并停止、冷却请求不播放、解除形态、资源重载后重复同步不重播、强制结束和断线清理。

完整镜头测试另覆盖三个视角、单机暂停、重复同步、`cinematic=false` 的同步不停止音乐、15 秒自然结束、提前解除、死亡、维度变化及退出。测试夹具每轮归零豪气，避免积累到 MAX 后自动 Hao Burst 干扰手动暂停断言。Hao Burst 实际声音引擎播放、多 Pose、字幕、豪气、相机及异常结束均回归通过；修复了从零音量提交声音实例导致引擎跳过播放的问题。

双客户端专用服务器先执行完整手动暂停，再执行 Hao Burst。发动者手动音乐在镜头恢复后继续；旁观客户端两阶段声音实例始终为空，两端天气、冻结和恢复均通过。服务器初始化无客户端类引用，结果载荷只注册 S2C。

正式 JAR 与 sources JAR 内 `jiahao_march.ogg` 与本地歌曲字节一致，Vorbis / 44100 Hz / stereo；14 条 Market 与 17 条 Code 中英语录、全部奖励提示齐全。测试入口、测试 Mixin 与测试类未进入发布包。

### 音频证据与日志限制

已验证 Minecraft 声音引擎 `isPlaying`、声音实例唯一性及淡出音量，**未取得实际听音/音频录制证据，不能宣称人工实际听到歌曲**。

最终日志仍有 JDK 25/JNA native-access、Guava Unsafe 弃用提示，原版山羊角缺失音效与 Shader Sampler2 提示；部分测试假账号的 Mojang profile 查询失败。没有新的业务编译错误或 Mixin 注入失败。歌曲说明文件改为小写 `sounds/music/readme.md`，消除了原有非法资源路径 ERROR。

设备专项和天气回归在退出时各出现一次 OpenAL `Stop: Invalid name parameter` ERROR；声音播放、音乐清理断言和正常退出均通过，退出期该提示尚未消除，不报告为零 Warning/ERROR。运行证据保留原始日志。`git diff --check` 无空白错误；Git 对部分 Windows 文本提示后续 LF → CRLF 规范化，不影响构建。


## 2026-09-30 独立音乐音量与左上角豪气 HUD

- `gradlew.bat build`：`BUILD SUCCESSFUL in 29s`，27 个必需 GameTest 与既有五组数学/模型检查通过。
- `scripts/gadget-smoke.gradle runClient`：`PASSED`、`BUILD SUCCESSFUL in 1m 52s`，日志 `build/verification/volume-hud-client.log`。实际打开原版声音设置并操作“嘉豪音乐音量”滑块，40% 保存/重载后保持；原版音乐 0%、嘉豪音乐 0% 时仍创建声音引擎实例，嘉豪恢复至 40% 后实例音量为 0.3。原版音乐调至 80% 不改变此音量；嘉豪再次静音、调节原版音乐、恢复至 100% 后仍使用原实例，音量 0.75。已有 R 镜头后播放、提前结束淡出、形态解除、重载、强制结束、GUI 操作和 Buff 检查同时通过。测试结束恢复夹具原来的音量设置。
- `scripts/hao-smoke.gradle runClient`：`PASSED`、`BUILD SUCCESSFUL in 1m 10s`，日志 `build/verification/volume-hud-burst.log`；豪气爆发正常淡入淡出、重复同步、多个 Pose、语录及解除形态/切维度/死亡清理回归通过。
- 截图人工检查：`build/run/gadgetSmoke/screenshots/jiahao-volume-settings.png` 的滑块位于声音设置列表首行；`jiahao-hud-top-left.png` 的豪气文字与进度条位于左上角，电影黑边和状态效果未遮挡该截图中的豪气信息。聚集提示位于进度条上方，语录字幕仍保留原位置。
- `python scripts/verify-hao-release.py` 通过：中英语音量翻译、两个声音设置/声音引擎 Mixin 与原歌曲进入正式及源码包，测试组件未进入发布包；`build/verification/volume-hud-release.txt`。`git diff --check` 通过。

嘉豪音乐使用独立 0–100% 客户端偏好；原版“主音量”仍控制全部声音。声音实例可在嘉豪音量为零时开始，并在调节原版分类滑块时保留，以便恢复后继续当前位置。证据来自声音引擎播放状态、实例音量、真实设置界面与截图，未取得实际听音证据。

本轮两个客户端日志均无 ERROR；仍有已有的 JDK 25/JNA native-access、Guava Unsafe、原版山羊角音效缺失、Shader Sampler2 及离线测试账号的 profile 查询 Warning。此前记录的退出期 OpenAL 提示本轮未出现，不能据此宣称其根因已修复。


## 2026-10-01 三张音乐唱片

- `gradlew.bat build`：`BUILD SUCCESSFUL in 27s`，**28 个必需 GameTest 全通过**，既有五组数学/模型检查通过；`build/verification/music-discs-build.log`。
- 新增 `JiahaoMusicDiscTests.allDiscsInsertPlayStopAndEject`：三张唱片的动态歌曲注册、音频时长、不可堆叠、`c:music_discs` 标签、原版存储接受、普通玩家插入并转移唱片、占用拒绝、13/14/15 比较器输出、歌曲自然结束边界保留唱片、取出清空均通过。长曲结束采用原版管理器推进至结束边界，不等待数分钟。
- `gradlew.bat -I scripts/music-disc-smoke.gradle runClient`：`PASSED`、`BUILD SUCCESSFUL in 35s`，`build/verification/music-discs-client.log` 与 `build/run/musicDiscSmoke/music-discs-result.txt`。隔离世界中通过真实客户端右键、网络和服务端处理逐张插入/取出，三首歌均获得 `RECORDS` 原版声音实例且引擎 `isPlaying` 为真，取出后停止。截图 `build/run/musicDiscSmoke/screenshots/music-disc-{jiahao_march,nevada,spectre}.png`，已检查播放提示和唱片图标。
- `python scripts/verify-music-disc-release.py`：`MUSIC DISC RELEASE VERIFICATION PASSED`；`build/verification/music-discs-release.txt`。三首完整 OGG 在正式与源码包内字节一致；单声道 / Vorbis / 44100 Hz；时长与歌曲 JSON 相符；模型、三张原创图标、中英名称和描述齐全，测试组件未打包。
- 既有 `scripts/verify-hao-release.py` 仍通过，原 R / Hao Burst 的歌曲资源及独立音量组件保持完整；`build/verification/music-discs-hao-release.txt`。`git diff --check` 通过。

最终客户端日志无 ERROR；仍有 JDK 25/JNA native-access、Guava Unsafe、原版山羊音效和 Shader Sampler2 Warning。导入时采用当前 `c:music_discs` 标签，已消除初次测试发现的 legacy-tag Warning。测试夹具先修复声音引擎初始化前注册监听与退出时未停止集成服务器的问题，再完成上述成功运行；它们均属于隔离测试代码。

播放证据来自原版声音引擎、实际唱片机交互和截图；未取得实际听音或录音证据，不将其记为人工听音确认。测试仅使用 `build/run/musicDiscSmoke` 的独立世界，不修改日常存档。原 Downloads 文件夹中的三个 MP3 未修改。
