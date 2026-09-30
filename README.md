# Jiahao Mode

Minecraft **1.21.1**, Fabric Loader **0.19.5**, Fabric API **0.116.17+1.21.1**,
Yarn **1.21.1+build.3**. Mod ID: `jiahao-mode`.

## 嘉豪变身器

在允许命令的世界中执行：

```mcfunction
/give @s jiahao-mode:jiahao_transformer
```

物品也加入了原版“工具与实用物品”创造栏。先穿齐嘉豪头盔、胸甲、护腿和靴子，
再手持右键切换嘉豪形态，不消耗
物品，没有耐久。进入时播放信标音效和 Action Bar 提示，并让当前支持天气的
维度立即下雨约 5 分钟。解除时播放较轻的音效和提示，不清除天气。

缺少任何一件时显示“你必须穿上整套嘉豪盔甲”。形态中脱掉任何一件，最迟下一服务器
Tick 自动解除，同时结束时间暂停、闪避、演出和字幕。重新穿齐需要再次使用变身器。
退出重进和切换维度穿齐装备时保留形态；死亡重生实际保留整套装备才保留形态。
恢复状态不会再次触发演出、台词或下雨。
无天空维度跳过天气修改；沙漠等生物群系遵循原版降水规则。

## 嘉豪时刻

进入嘉豪形态后按 **R** 暂停当前维度的自动模拟，再次按下提前恢复。
最多持续 **15 秒（300 个服务器 Tick）**，结束后冷却 **8 秒（160 Tick）**。
按键名称为“嘉豪时刻 / Jiahao Time”，可在“设置 → 控制 → 嘉豪模式”重新绑定。
发动时自动下车；同一维度只允许一个拥有者，不同维度可以各自发动。

稳定站立的拥有者先播放约 5 秒电影演出：镜头环绕并推近，服务器选定一套静态 Pose，
0.4 秒进入后保持定格。相邻两次时停不重复，奔跑回头保持固定视线。期间暂时锁定移动、跳跃、攻击、使用及鼠标转向；R 可提前结束，
ESC、聊天和基本 GUI 保留。三种原始视角及用户 FOV 设置均不改写。
演出结束后有约 10 秒自由时间，可移动、跳跃、闪避、转动视角并立即造成普通伤害。
空中、游泳、飞行等不稳定情况下仍能发动暂停，但跳过本次演出，落地不补播。
同维度其他玩家仅看到发动者 Pose，不会被抢夺镜头。
其他实体、投射物、TNT、掉落物、载具及同维度其他玩家被冻结；太阳、月亮、
计划方块/流体更新、随机更新、方块实体、活塞事件、自然生成及天气倒计时停止。
雨雪动画、云层漂移、天气强度、闪电闪光及普通环境粒子也定格；镜头、网络、输入
和 UI 继续运行。动画恢复时扣除暂停期间经过的时间，避免云层或雨雪跳跃。
暂停时停止产生新的随机天气声音和水花。箭的速度、TNT 引信等保留，恢复后继续。

普通玩家不能发动。死亡、退出、切换维度、解除形态、世界卸载及服务器关闭
均结束暂停。暂停状态和冷却不保存到存档；没有修改 daylight gamerule，也没有暂停线程。

服务端验证空内容 C2S 请求并发送 S2C 状态；技术接口、Mixin 和限制见
[TIME_STOP.md](TIME_STOP.md)，验证范围与待人工验收项见 [TESTING.md](TESTING.md)。
电影镜头、同步、输入保护及第三阶段验证见 [CINEMATIC.md](CINEMATIC.md)。
皮肤替换、语音、Shader 或延迟伤害仍未实现。

## 状态 API

基础包保持为 `com.shouyun.jiahaomode`。后续功能使用：

```java
boolean enabled = JiahaoStateManager.isJiahao(player);
JiahaoStateManager.setJiahao(serverPlayer, true);
boolean newState = JiahaoStateManager.toggleJiahao(serverPlayer);
```

`isJiahao(PlayerEntity)` 查询“保存的形态标记开启且穿齐整套盔甲”；客户端收到
更新前可能稍有延迟。`setJiahao` / `toggleJiahao` 只接受 `ServerPlayerEntity`，
必须在服务端线程调用。重复设置相同值不播放反馈，不修改天气。

`state.JiahaoState` 封装 Fabric 数据附件注册，使用 `Codec.BOOL` 保存到玩家 NBT：
`fabric:attachments` → `jiahao-mode:jiahao_state`。使用 `copyOnDeath()` 复制重生状态，
再按重生后实际装备校验；装备缺失会清除标记。
Fabric 自动同步到玩家自己以及正在跟踪该玩家的客户端。形态保存本身无需直接操作
NBT、自定义网络协议或第三方状态库；时间暂停另外使用服务器权威网络包和条件 Mixin。

`item.ModItems` 负责注册，`item.JiahaoTransformerItem` 只转交服务端切换请求，
`effect.JiahaoTransformationEffects` 负责一次性提示、音效和天气。立即下雨同时发送
原版天气更新消息，避免直接设置雨量后客户端没有收到变化。

## 临时物品资源

模型 `src/main/resources/assets/jiahao-mode/models/item/jiahao_transformer.json`
暂时引用原版 `minecraft:item/echo_shard`，并标注为 **temporary placeholder**。
没有生成或复制 PNG。

有正式贴图后，将它放到：

```text
src/main/resources/assets/jiahao-mode/textures/item/jiahao_transformer.png
```

随后将模型里的 `textures.layer0` 改为 `jiahao-mode:item/jiahao_transformer`。
Minecraft 1.21.1 使用这里的 item model，不需要较新版本的 `assets/<modid>/items/` 定义。

## 构建与测试

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

构建同时运行 `src/gametest` 的服务端集成测试和 `src/visualtest` 的视觉时钟测试。测试使用已有 Fabric API，在
`build/run/gameTest` 中创建隔离世界，不写入日常 `run/saves`；测试类不包含在发布 JAR 中。
也可单独执行 `gradlew.bat runGameTest`。完整验证记录及手动测试项见 [TESTING.md](TESTING.md)。

可选客户端数值测试使用 `scripts/weather-visual-smoke.gradle`，详见测试记录；测试模组
只在显式使用此脚本时加载，不进入发布 JAR。Vanilla 天气渲染已接入冻结；Iris 自行
实现的 Shader 时间、降水及体积云动画尚未接入，也没有宣称完成 Sodium/Iris 实机验证。

编译目标为 Java 21。当前项目通过 `gradle.properties` 的 `org.gradle.java.home`
指定本机 JDK 25 路径，其他电脑需调整这个本地路径，或通过
`-Dorg.gradle.java.home=<JDK 路径>` 覆盖。版本与 mappings 保持原项目配置。

## License

Jiahao Mode is licensed under the MIT license; see [LICENSE](LICENSE).

The initial project was created with the Fabric Template Generator. The Fabric
template is distributed under CC0; its original license is preserved in
[LICENSE-FABRIC-TEMPLATE](LICENSE-FABRIC-TEMPLATE).

## 第六阶段：嘉豪盔甲

黑色/深灰的兜帽口罩服装甲，胸前与背后有白色 AW 风格标识，采用原版盔甲模型和正式
128×64 layer 贴图。四件物品 ID 为 `jiahao-mode:jiahao_helmet`、`jiahao-mode:jiahao_chestplate`、
`jiahao-mode:jiahao_leggings`、`jiahao-mode:jiahao_boots`。
锻造台使用黑色染料作为模板、对应钻石盔甲作为基础、回声碎片作为添加材料。
整套护甲 21、韧性 10、击退抗性 0.20，耐久较钻石增加约 12.1%，使用钻石修复。
获取命令、资产路径、穿戴清理和固定台词接入见 [ARMOR.md](ARMOR.md)。

## 第四阶段：嘉豪语录

嘉豪形态下按 **V** 随机说话（可改键，2.5 秒冷却）。成功变身立即显示固定台词
“10年前的仇难道不报了吗”；成功启动嘉豪时刻立即显示“注意时间并没有静止”。
这两句独立于随机池，优先级 50、持续 3 秒；开始与演出中段不会再叠加随机开场。
本人看电影字幕，附近玩家看头顶文字。文本支持中英文，无新增语音。

实现、扩展方式和测试入口见 [QUOTES.md](QUOTES.md)，实际验证记录见 [TESTING.md](TESTING.md)。

## 第五阶段：嘉豪闪避与完美闪避

嘉豪形态下按 **C** 闪避，可在控制设置改键。方向取当前 WASD，相反输入抵消，没有方向时后撤。
服务端固定启动方向，按先快后慢的曲线移动 **6 Tick**，平地约 **2.8 格**；动作结束后冷却 **18 Tick**。
保持原版碰撞、重力和摔落，墙壁和关闭的门会缩短位移，悬崖不会提供额外保护。
空中每次离地最多一次，服务器确认落地才重置。

从服务器接受请求起 **Tick 0–3**，每次闪避最多取消一击符合规则的近战、投射物或爆炸伤害。
成功时播放紫水晶提示音、少量粒子和完美语录，本人获得约 **0.3 秒**的 FOV/轻微镜头侧倾。
伤害窗口之后及同动作第二击照常结算；环境伤害、反伤、`/kill`、行政 `/damage` 不可闪避。
爆炸依旧产生击退、破坏方块并伤害其他实体。六 Tick 动作期间不能攻击、挖掘或开始新的交互，启动会停止盾牌等物品使用。

电影演出期间 C 被拒绝，演出结束后时间暂停拥有者可以闪避；其他人的镜头不受影响。
普通闪避仅有少量 cloud 粒子，完整模型残影留待后续。没有新增耐力条、Combo UI、技能 HUD 或子弹时间。
技术接口、30 项交付对应和测试入口见 [DODGE.md](DODGE.md)，实际运行证据见 [TESTING.md](TESTING.md)。

## 随机嘉豪瞬间与娱乐道具

嘉豪形态每 45–120 秒出现一次随机触发窗口。条件不安全便取消本次，重新等待完整间隔；
最近 10 秒战斗、受伤、半血以下、GUI、飞行、游泳、骑乘、挖掘、使用物品或敌人正在针对
玩家时不启动。演出持续 3 秒，世界继续运行；附近玩家看到同一静态 Pose，镜头只影响本人。
GUI、受伤、传送、死亡、退出形态或断线都会取消。镜头优先级为豪气爆发 > 时停 > 随机演出 > 完美闪避。
个人开关在 `config/jiahao-mode-client.json`，默认 `enableRandomJiahaoMoments: true`；
改为 `false` 并重启客户端即可关闭本人的随机演出，仍能观看其他人的 Pose。

```mcfunction
/give @s jiahao-mode:market_viewer
/give @s jiahao-mode:jiahao_code_editor
```

两种物品都在工具创造栏，单件堆叠，任意形态可右键打开，豪言仅嘉豪形态触发。
股市查看器用铁锭、玻璃板、红石、纸无序合成；代码编辑器把纸换为书，各产出一个。
行情完全虚构，不联网；BUY/SELL 仅显示消息。终端输入最多 128 字符、历史 20 条、输出 80 行，
输入只作本地文本，Enter 播放假执行效果，不执行游戏命令、系统命令或脚本，也不发送输入文本。
两个 GUI 均不暂停世界，ESC 正常退出，终端历史在关闭后清除。

三个正式物品图标均为透明 16×16 PNG，由内置 Image Generation 生成后整理尺寸和像素。
第七阶段实现、资源来源和验收映射见 [STAGE7.md](STAGE7.md)。

## 第八阶段：豪气与强制爆发

嘉豪形态每秒积累 1 豪气，Perfect Dodge +12、敌对击杀 +6、手动时停 +8、随机瞬间 +4、经过服务端验证的 Market/假命令完成 +2。底部中央豪气条满值后自动爆发，无需按键；战斗、低血量和敌人针对玩家不会阻止启动。闪避、已有演出、GUI 或姿态不稳定则保留 MAX，条件满足后自动启动。

爆发复用现有时停与相机，12 秒冻结、3–5 个不重复静态 Pose、2–4 句新语录、本人的音乐和完整输入锁。旁观者看冻结、动作和头顶语录。结束清零，五秒后恢复积累；普通退出形态、重连、换维度和死亡保留积累。

OP 等级 2 用 `/jiahao hao set 99` 测试自然补满，也可 `add <0-100>` 或 `burst`。嘉豪进行曲 OGG 随发布及源码 JAR 打包，也用于本地 runClient；客户端只通过 Minecraft 资源系统播放。缺音乐仍可演出。

机制、完整语录、时间轴及资源说明见 [HAO_METER.md](HAO_METER.md)，实测和 TEST 1–25 对应见 [TESTING.md](TESTING.md)。
