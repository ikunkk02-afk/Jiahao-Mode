# 第五阶段：嘉豪闪避与完美闪避

Minecraft 1.21.1 / Fabric API 0.116.17+1.21.1 / Yarn 1.21.1+build.3，依赖版本保持原配置。
沿用形态、独立服务器 Tick、网络注册、语录、Pose 保存恢复及 Camera/FOV 接入点。

## 参数与接口

| 项目 | 实现 |
|---|---|
| 普通动作 | 6 次服务端 travel，平地总水平位移 2.8 格 |
| 曲线 | `2.8 × ((1-i/6)^3 - (1-(i+1)/6)^3)`，逐步减速；技能水平速度上限 1.2 |
| 冷却 | 动作结束或取消后 18 Tick |
| 完美窗口 | 接受请求的服务器 Tick 为 0，允许 0、1、2、3；4 起不再允许 |
| 伤害机会 | 每次动作只消费一次；不添加普通无敌帧 |
| 空中次数 | 每次离地最多一次；服务端确认落地且不在动作中才重置 |
| 镜头 | 6 个客户端 Tick，正常 20 TPS 下约 0.3 秒；正弦包络，FOV 峰值 +4°，左右 Roll 最大 ±4° |
| Pose | 普通倾斜峰值约 17°，完美约 25°，逐帧插值恢复 |
| 粒子 | 普通动作第 2/4/6 步各 2 个 cloud；完美另加 4 cloud + 4 crit，均标记暂停免疫 |
| 语录 | `PERFECT_DODGE(35, 6)`；首次符合条件必播；成功播放之间至少 80 Tick |
| 连续完美 | 两次成功相隔不超过 60 Tick 则累加，否则为 1；无 Combo UI |
| ActionBar | 非形态/冷却翻译键，每玩家至少间隔 10 Tick |

`dodge/JiahaoDodgeManager` 提供 `canDodge`、`startDodge`、`isDodging`、`isPerfectWindow`、
`getCooldownTicks`、`getPerfectDodgeCombo`、`state`、`clear`。权威查询与修改均要求服务端线程。
`JiahaoDodgeState` 保存 UUID、维度、动作编号、起始 Tick/位置、固定世界方向、局部方向、步数、
本 Tick 移动标记、一次性完美标记、外部击退和接收者。冷却、空中使用和连续完美放在独立 History。
这些运行数据不写 NBT；切形态和切维度保留冷却/空中记录，断线、死亡重生清除旧记录。

## 输入、网络与运动

C 为 Fabric 可改绑 KeyBinding，名称 `key.jiahao-mode.dodge`，中英文资源齐全。
原版 C 同时绑定保存快捷栏，`KeyBindingDodgeMixin` 在相同按键分派时补充闪避按压计数；
仍读取当前绑定键，不硬编码监听 GLFW，也不改动用户快捷栏按键。

| Payload | 权限与字段 |
|---|---|
| `JiahaoDodgeRequestPayload` | C2S，只带一个 WASD 四位标记：W=1、S=2、A=4、D=8 |
| `JiahaoDodgeStatePayload` | S2C，玩家 UUID、维度、动作编号、START/STEP/END/CANCEL、步数、位置/速度/落地及局部方向 |
| `JiahaoPerfectDodgePayload` | S2C，玩家、维度、动作编号、动作原点及可选攻击位置 |

服务端拒绝非四位值，以连接玩家而非客户端提供的 UUID 为身份。相反方向抵消、零输入后撤，
由服务器认可的 Yaw 旋转并归一化；对角不加速，启动后转动鼠标不改变世界方向。
客户端没有目标位置、完美声明或其他玩家身份的请求字段。

拒绝死亡、睡眠、骑乘、旁观、飞行、鞘翅滑翔、游泳/水中、攀爬、岩浆中、卡方块、noClip、
未完成传送、被时间暂停冻结及主演出锁定的玩家。演出结束后的暂停拥有者允许闪避。

`ServerPlayerDodgeMixin` 包装已有 `ServerPlayerEntity.travel`，只替换本步水平速度与移动输入。
继续运行 Vanilla travel、碰撞、重力、摩擦和摔落；不提高 Y 速度、不补偿撞墙损失。
`PlayerDodgeLedgeMixin` 仅取消本动作的潜行悬崖收缩，悬崖照常坠落。
外部水平冲量与技能位移分开保存，动作结束/取消清除技能水平余速而保留击退与垂直速度。

原版 network handler 在 `playerTick()` 后会恢复旧基准位置，因此在该调用后及时
`syncWithPlayerPosition()`。动作期间位置包只接受有限值且经过 wrap/clamp 的视角，
丢弃客户端位置和 onGround，不取消连接 Tick、KeepAlive、传送确认或消息序列确认。
结束时仅调用一次原版 `requestTeleport` 协调本人位置，动作期间没有逐 Tick 传送。

本人接收 S2C 样本，停止重复本地 travel，在客户端 Tick 尾应用样本并使用原版渲染插值。
最终原版位置协调会清除待应用的旧样本。旁观者只用原版实体跟踪获得位置，S2C 只驱动姿态；
动作编号、维度、步数和超时限制防止旧状态复活。END/CANCEL 也发送给此前接收过状态的玩家。

启动服务端 `stopUsingItem()` 并取消已有挖掘裂纹。六 Tick 内输入和服务端处理器阻止攻击、挖掘、
新物品使用、方块/实体交互，拒绝后保留方块确认与物品同步；按住攻击/使用需要释放后再开始。

## 伤害规则

真正取消伤害使用现有 Fabric `ServerLivingEntityEvents.ALLOW_DAMAGE`，没有重写 `damage`。
检查正且有限的金额、服务器窗口、嘉豪形态、尚未消费的机会以及分类；成功先消费，再返回 false。
第二击、Tick 4 起及金额 0 均不提供保护，也不刷新 Vanilla 无敌计时。

按以下顺序判定：

1. 行政 `/damage` 调用范围、`jiahao-mode:undodgeable`、`BYPASSES_INVULNERABILITY` 优先排除。
2. 摔落、溺水、冻结标签优先排除。
3. 爆炸/投射物标签允许；真实 ProjectileEntity 来源允许。
4. 直接 LivingEntity 攻击者或没有直接实体但有 LivingEntity 攻击者允许，兼容使用此来源的模组近战。
5. 其他环境来源排除。

`data/jiahao-mode/tags/damage_type/undodgeable.json` 使用 1.21.1 的单数 `damage_type`。
包含摔落、窒息、饥饿、溺水、冻结、燃烧/岩浆、周期 magic/凋零、虚空、反伤、仙人掌、
掉落方块、边界和行政伤害等原版环境类型。数据包可用 `replace:false` 追加模组伤害 ID。
使用 `indirect_magic` 且具有投射物来源的魔法攻击可允许；周期 `magic` 不允许。

`DamageCommandDodgeMixin` 仅在原版 `/damage` 执行器调用 `Entity.damage` 时设置服务器行政范围，
try/finally 恢复；因此 `/damage @s 2 minecraft:arrow` 也不能闪避。它不改变伤害结算方法。
`/kill` 使用的不可绕过伤害保持原版。

投射物仍运行原版命中、落地/移除等处理。真正移开且没有 damage 调用只是普通闪避。
爆炸只取消本人该次 damage；原版爆炸仍产生击退、破坏方块、伤害其他实体。

## 视觉、语录与清理

`JiahaoDodgeClientController` 只管理同步视觉。Pose 在原有 PlayerModel 保存/恢复流程中
增加身体倾斜、张臂与平衡头部；有攻击位置时才额外转头，没有来源时保留普通头部朝向。
袖子/外层复制姿态；主 Cinematic 优先。Camera 在每帧新旋转上加 Roll，FOV 返回值加偏移，
不写 Options、不触发五秒演出、不修改 TPS。纯前后方向 Roll=0，远端玩家不影响本人的镜头。

每次成功播放原版紫水晶 chime 和少量粒子，语录失败也继续视觉反馈。六句中英文均使用现有字幕/
头顶文字：太慢了、你碰不到我、我已经看到了、差一点、这就是差距、你刚才……想碰我？
优先级 35 不能覆盖 40 的 CINEMATIC。

死亡、退出、换维度、解除形态、世界卸载及停服清除动作。客户端收到取消、死亡、换世界、
断开或样本超时后释放视觉与移动锁；相机/模型每帧使用原版基准并自动恢复。

## 文件清单

新增生产文件：

- `src/main/java/com/shouyun/jiahaomode/dodge/`：Manager、State、NetworkAccess、InteractionAccess。
- `src/main/java/com/shouyun/jiahaomode/network/`：DodgeNetworking、RequestPayload、StatePayload、PerfectDodgePayload。
- `src/main/java/com/shouyun/jiahaomode/mixin/`：ServerPlayerDodge、ServerPlayNetworkHandlerDodge、PlayerDodgeLedge、ServerPlayerInteractionDodge、DamageCommandDodge Mixin。
- `src/client/java/com/shouyun/jiahaomode/client/`：DodgeKeyBindings、DodgeClientController、DodgeKeyAccess。
- 客户端 Mixin：ClientPlayerDodge、ClientDodgeReconciliation、KeyBindingDodge。
- 公共数据资源：`data/jiahao-mode/tags/damage_type/undodgeable.json`。

修改生产文件：main/client 入口，JiahaoStateManager，QuoteCategory/QuoteManager，原有
LivingEntityCinematic 和 ServerPlayNetworkHandlerTimeStop Mixin，CinematicInput/PoseController，
Keyboard/Camera/GameRenderer Cinematic Mixin，两份 Mixin 配置，中英文语言文件。
测试：扩展 GameTest 注册、新增 JiahaoDodgeTests 与仅测试数据类型/标签；新增 dodgesmoke、
dodgemultiplayer 源集和三个启动脚本。文档更新 README、QUOTES、TESTING，新增本文。

## 可复现验证

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat -I scripts/dodge-smoke.gradle runClient --console=plain
foreach ($role in @('server','actor','observer')) {
  .\gradlew.bat -I scripts/dodge-multiplayer-smoke.gradle "-PdodgeRole=$role" exportDodgeLaunch --console=plain
  if ($LASTEXITCODE -ne 0) { throw "export failed: $role" }
}
python scripts/dodge-multiplayer-smoke.py
```

单机测试需要把可用的**隔离测试世界**复制到 `build/run/dodgeSmoke/saves/DodgeSmoke`，
可参考既有 QuoteSmoke/CinematicSmoke。测试会初始化主世界高空平台、天气、实体及玩家状态，
修改并保存该隔离存档。不要使用日常存档。专用服务器测试自动在 build/run 下创建独立世界，
仅监听 127.0.0.1:25576，测试 offline-mode、两独立客户端 JVM，完成后正常 stop 保存。
脚本的 eula=true 仅用于用户授权的本地 Minecraft 测试。

所有结果、日志、启动参数、截图、存档在忽略的 build/，不提交。成功必须同时看 Gradle 结果与
业务 `PASSED` 文件；专用测试要求 server/actor/observer 三者通过。运行证据和警告见 TESTING。

## 用户 30 项交付对应

| 项目 | 实现/证据 |
|---|---|
| 1 新增文件 | 本文文件清单：公共状态/协议/5 个 Mixin、客户端控制器/输入/3 个 Mixin、标签与隔离测试 |
| 2 修改文件 | 本文修改清单及 Git diff；没有更换依赖版本 |
| 3 C KeyBinding | `client/JiahaoDodgeKeyBindings`，Fabric 注册、中英文、可改绑，兼容共享 C |
| 4 Request | KeyBinding → WASD 四位 → typed C2S → 连接玩家 |
| 5 方向验证 | 拒绝非法位、相反抵消、空输入后撤、服务器 Yaw 旋转归一化、启动固定方向 |
| 6 位移 | 原有 travel 中六步 ease-out 水平速度，保留 Vanilla 垂直结算 |
| 7 不穿墙 | 原版碰撞、无碰撞损失补偿；墙、关闭门、墙角真实 GameTest |
| 8 持续 | 6 次服务器 travel |
| 9 冷却 | 结束后 18 Tick；最后一个冷却 Tick 拒绝，恰好到期接受 |
| 10 完美窗口 | 服务器 Tick 0–3；真实伤害 Tick 3 取消、Tick 4 结算 |
| 11 Damage 接入 | `JiahaoDodgeManager.allowDamage`，伤害应用前消费机会 |
| 12 Event/Mixin | 取消用 ALLOW_DAMAGE；Mixin 仅运动、输入、同步与行政来源包装 |
| 13 可闪避 | 正伤害近战、投射物、爆炸；标签或实际攻击来源兼容模组 |
| 14 不可闪避 | undodgeable/行政/不可绕过优先；环境、周期伤害、虚空、反伤、零伤害等 |
| 15 Projectile | 命中流程原版；只有发生合格伤害调用才触发完美 |
| 16 Explosion | 仅本人一击伤害取消；方块/其他实体/本人击退在真实 TNT 和 Creeper 测试中仍有效 |
| 17 Air Dodge | 每次离地一次，服务端落地重置；保留重力与摔落 |
| 18 Pose | 现有保存/恢复及 PlayerModel 接入，方向倾斜、张臂、完美约 25°、插值退场 |
| 19 Camera | 本人短促 +4° FOV / 最大 ±4° Roll；纯前后无 Roll，不改 Options |
| 20 Quote | PERFECT_DODGE(35,6)，消费机会后触发，首次符合条件必播，成功播放间隔 80 Tick |
| 21 权威 | C2S 无目标/成功字段，状态与判定只在服务器线程；伪造位置被丢弃 |
| 22 Time Stop | 独立 Tick；演出后拥有者允许；粒子免疫；旧实体/天气暂停测试回归 |
| 23 Cinematic | 双侧输入保护及服务端稳定检查拒绝；主镜头与 Pose 优先 |
| 24 Dedicated | 真实 Dedicated + 两独立客户端；common 不引用 MinecraftClient，client Mixin 限客户端 |
| 25 build | 结果与日志见 TESTING 第五阶段；必须 BUILD SUCCESSFUL |
| 26 runClient | 显式 Gradle runClient，隔离世界、正常保存退出、结果与截图见 TESTING |
| 27 怪物/TNT | Zombie 实际 tryAttack、Skeleton 真实 Arrow、Creeper Ignite/Fuse、TNT Fuse=0 |
| 28 Warning | TESTING 单独记录环境/资源警告、未测兼容组合和人工观感边界 |
| 29 Commit SHA | 最终聊天交付报告完整 SHA；可由 `git log -1 --format=%H` 核对 |
| 30 Push | 最终聊天报告正常 origin/main 推送及远端 SHA 核对，不使用 force |
