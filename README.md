# Jiahao Mode

Minecraft **1.21.1**, Fabric Loader **0.19.5**, Fabric API **0.116.17+1.21.1**,
Yarn **1.21.1+build.3**. Mod ID: `jiahao-mode`.

## 嘉豪变身器

在允许命令的世界中执行：

```mcfunction
/give @s jiahao-mode:jiahao_transformer
```

物品也加入了原版“工具与实用物品”创造栏。手持右键切换嘉豪形态，不消耗
物品，没有耐久。进入时播放信标音效和 Action Bar 提示，并让当前支持天气的
维度立即下雨约 5 分钟。解除时播放较轻的音效和提示，不清除天气。

玩家退出重进、死亡重生和切换维度均保留形态。恢复状态不会再次触发演出或下雨。
无天空维度跳过天气修改；沙漠等生物群系遵循原版降水规则。

## 嘉豪时刻

进入嘉豪形态后按 **R** 暂停当前维度的自动模拟，再次按下提前恢复。
最多持续 **8 秒（160 个服务器 Tick）**，结束后冷却 **3 秒（60 Tick）**。
按键名称为“嘉豪时刻 / Jiahao Time”，可在“设置 → 控制 → 嘉豪模式”重新绑定。
发动时自动下车；同一维度只允许一个拥有者，不同维度可以各自发动。

稳定站立的拥有者先播放约 5 秒电影演出：镜头从右后方环绕到正面，人物依次低头、
摆手、侧身和托下巴。期间暂时锁定移动、跳跃、攻击、使用及鼠标转向；R 可提前结束，
ESC、聊天和基本 GUI 保留。三种原始视角及用户 FOV 设置均不改写。
演出结束后，剩余暂停期间可移动、跳跃、转动视角并立即造成普通伤害。
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

`isJiahao(PlayerEntity)` 可以查询服务端权威状态或客户端的同步副本；客户端收到
更新前可能稍有延迟。`setJiahao` / `toggleJiahao` 只接受 `ServerPlayerEntity`，
必须在服务端线程调用。重复设置相同值不播放反馈，不修改天气。

`state.JiahaoState` 封装 Fabric 数据附件注册，使用 `Codec.BOOL` 保存到玩家 NBT：
`fabric:attachments` → `jiahao-mode:jiahao_state`。使用 `copyOnDeath()` 保留重生状态，
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

## 第四阶段：嘉豪语录

嘉豪形态下按 **V** 随机说话（可改键，2.5 秒冷却）。进入形态约 1 秒后自动说话；嘉豪时刻正面特写在进度 0.74 出现“世界，安静一点。”。本人看电影字幕，附近玩家看头顶文字。文本支持中英文，本阶段无新增语音。

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
