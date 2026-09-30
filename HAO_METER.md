# 第八阶段：豪气与自动爆发

豪气满值后自动爆发。战斗、低血量、敌人针对玩家都不阻止启动；96 豪气完成 Perfect Dodge 后，等当前闪避结束、站稳并关闭 GUI 即启动，无需脱战或回血。

## 使用和奖励

嘉豪形态显示底部中央豪气条，每秒增长 1。80% 提示聚集，90% 轻微呼吸，等待和爆发显示 MAX。遵循原版隐藏 HUD 设置，字幕位于豪气条上方。

OP 等级 2 可执行 `/jiahao hao set <0-100>`、`add <0-100>`、`burst`。推荐 `/jiahao hao set 99` 测试自然补满；`burst` 设置 MAX，走同一准备流程。管理命令清除增长锁，不能改写正在爆发的会话。

| 服务端确认事件 | 豪气 |
|---|---:|
| Perfect Dodge 实际取消一击 | +12 |
| 玩家击杀敌对 Monster | +6 |
| 成功启动手动时停 | +8 |
| 确认启动随机嘉豪瞬间 | +4 |
| 完成 Market BUY/SELL | +2 |
| 完成 Fake Command | +2 |

普通闪避、手动语录、爆发自身不奖励。Market 由服务端发放会话与单次令牌，等待至少 20 Tick；Code 为 24 Tick。两类独立冷却 200 Tick，BUY/SELL 共用冷却。验证实际形态、原手物品、所选栏、维度和时序；关闭界面、换手/换物品、换维度、死亡或断线失效。输入文本及奖励数额不进入道具协议。

## 状态、准备与排队

`hao.HaoMeterManager` 使用持久化 Fabric 附件 `jiahao-mode:hao_state`，NBT 在 `fabric:attachments` 内。`HaoState` 保存 0–10000 内部单位（100 单位 = 1 豪气）、剩余 0–100 Tick 增长锁和 `bursting` 标记，`copyOnDeath()` 保留普通死亡积累。

每服务器 Tick 增长 5 内部单位，独立于世界冻结。退出形态、普通死亡、重连、换维度保留积累；爆发时不增长。结束或异常中断清零并锁定增长 100 Tick。登录读到未完成爆发标记清零，避免崩溃/重启后立即重播。

MAX 设置等待序号，每 Tick 检查基本条件：已有时停/演出、闪避、姿态不稳定、睡眠、爬梯、使用物品、挖掘、传送未确认、容器未关闭会推迟启动。客户端反馈当前世界、GUI/加载遮罩和镜头就绪，服务器最终复查并启动。提案 UUID 有效 20 Tick，拒绝/过期后重试。同维度一个时停，按等待序号选第一个满足条件者；各维度独立。MAX 不启动新随机演出，已开始的随机演出正常完成。

## 12 秒演出和同步

复用 `JiahaoTimeStopManager` 的实体、模拟、世界时间和天气冻结，增加 `TimeStopReason.MANUAL/HAO_BURST`；爆发不受手动冷却阻止，不嵌套时停。服务器先冻结再发送完整爆发会话。

复用 `JiahaoCinematicController`、碰撞安全相机及模型 Pose 应用。优先级为爆发 > 手动时停演出 > 随机演出 > Perfect Dodge 镜头。从七个非 DEFAULT Pose 无放回选 3–5 个，最后一个为 Final Pose；切换四 Tick 平滑，主要阶段静止。奔跑侧面低机位、指天仰视、思考近景、回头后侧、拥雨远景，各阶段平滑衔接。

| Tick / 秒 | 行为 |
|---|---|
| 0 / 0 | 冻结、提交会话、提示“豪气已满” |
| 6 / 0.3 | 本人音乐开始 |
| 10 / 0.5 | 镜头进入、提示“嘉豪控制不住了” |
| 14–170 / 0.7–8.5 | 均分 Pose 阶段 |
| 170–200 / 8.5–10 | Final Pose/镜头 |
| 200–220 / 10–11 | 镜头恢复 |
| 220–240 / 11–12 | 音乐淡出，末尾恢复模型 |
| 240 / 12 | 解除时停/输入锁、归零 |

全部 240 Tick 禁止移动、跳跃、攻击、使用、闪避，R 不可取消；ESC、聊天、基本 GUI 和安全退出保留。音乐和字幕只给本人，旁观者看冻结、Pose 和头顶语录。

本人豪气每秒同步；奖励、MAX、开始、结束立即同步。爆发 S2C 包含玩家、维度、会话 UUID、经过 Tick、完整 Pose 序列、原点和朝向，每 10 Tick 校正，登录和开始跟踪补发。客户端按会话去重，拒绝过期/结束会话；时停包增加原因，避免爆发另播普通时停镜头。

每场无重复选 2–4 句，各显示 50 Tick；两句在 70/170，三句在 32/104/176，四句在 22/76/130/184。期间抑制普通语录。20 条附件中文原文和自然英文翻译位于 `assets/jiahao-mode/lang/{zh_cn,en_us}.json`，键 `jiahao.quote.hao_burst.1` 至 `.20`。

## 音乐和清理

`jiahao-mode:music.jiahao_march` 在 `sound.ModSounds` 注册，`assets/jiahao-mode/sounds.json` 使用 `jiahao-mode:music/jiahao_march`、`stream: true`。仅通过 Minecraft 资源系统播放，不读取系统 FLAC；缺音频跳过音乐，演出继续。

客户端 `HaoMarchSound extends MovingSoundInstance` 使用原版 `TickableSoundInstance` 行为，MUSIC 分类、相对音量 0.75、非定位/无衰减，0.4 秒淡入、1 秒淡出。服务器确认后每个会话至多一实例，经过 6 Tick 才开始。

正常结束最后一秒淡出；死亡、换维度、退出世界、解除形态、相机异常取消立即停止。资源重载停止实例且本次不从头重播。服务端不引用客户端音频/渲染类。服务端结束路径清理冻结、锁、语录和状态，客户端断线/换世界另有兜底。

本地 OGG 为 44.1 kHz、双声道、Vorbis 质量 5。未取得公开分发授权，精确路径加入 `.gitignore`，所有 Jar 任务排除该 OGG，发布/源码包保留注册和说明；本地 `processResources` 与 `runClient` 仍可使用音乐，不删除测试文件。配置说明见 [music/README.md](src/main/resources/assets/jiahao-mode/sounds/music/README.md)。

验证范围、TEST 1–25 和待人工验收项见 [TESTING.md](TESTING.md)。

## 完整语录

| ID | 中文 | English |
|---|---|---|
| 1 | 豪气，不是计算出来的。 | Hao is not something you calculate. |
| 2 | 这一刻，甚至时间都需要考虑一下。 | Even time needs a moment to think about this. |
| 3 | 你以为我停下来了，其实是世界没跟上。 | You think I stopped. The world just could not keep up. |
| 4 | 有些动作，没有意义就是最大的意义。 | Sometimes having no meaning is the greatest meaning of all. |
| 5 | 今天的雨，只是背景。 | Today's rain is just the backdrop. |
| 6 | 别看时间，看我。 | Watch me, not the clock. |
| 7 | 我不是在摆动作，我是在等待世界理解。 | I am not striking a pose. I am waiting for the world to understand. |
| 8 | 速度达到一定程度，静止也是一种前进。 | At a certain speed, standing still is another way forward. |
| 9 | 如果你觉得奇怪，那说明你还没有进入状态。 | If this seems strange, you have not entered the right state yet. |
| 10 | 这一秒，比较长。 | This second is taking its time. |
| 11 | 我没有改变时间，只是时间改变了态度。 | I did not change time. Time changed its attitude. |
| 12 | 有人在赶路，我在让路自己过来。 | Some people hurry down the road. I wait for the road to come to me. |
| 13 | 你看到的是动作，我看到的是节点。 | You see a pose. I see a checkpoint. |
| 14 | 真正的豪气，不需要符合逻辑。 | True Hao does not have to make sense. |
| 15 | 我已经站在下一秒了。 | I am already standing in the next second. |
| 16 | 先别说话，让镜头走完。 | Hold that thought. Let the camera finish. |
| 17 | 时间不是停止，只是暂时不敢动。 | Time has not stopped. It just does not dare move yet. |
| 18 | 真正的问题不是我在做什么，是你为什么在看。 | The real question is not what I am doing. It is why you are watching. |
| 19 | 这不是姿势，这是状态。 | This is not a pose. It is a state of being. |
| 20 | 不要理解，感受就行。 | Do not try to understand. Just feel it. |
