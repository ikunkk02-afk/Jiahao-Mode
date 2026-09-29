# 第一阶段验证记录

最新的第三阶段电影演出验证位于本文末尾；上方第一、二阶段条目保留历史验证范围。

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
