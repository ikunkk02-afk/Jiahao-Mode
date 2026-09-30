# R 音乐与道具 Buff

## 时间暂停音乐

R 仍只发送现有 C2S 请求。服务器成功创建手动暂停以后，发动者客户端收到 `JiahaoTimeStatePayload`，由 `JiahaoMusicController` 启动现有 `music.jiahao_march` 资源。普通形态、冷却或其他玩家已暂停世界时，请求拒绝，不播放音乐；旁观客户端也不播放。

控制器与镜头生命周期分离：手动暂停的音乐覆盖完整 300 Tick / 15 秒，前 100 Tick 的镜头结束后继续播放。音量 0.75，使用 MUSIC 分类，不修改用户设置。正常结束、再次 R、解除形态、服务端强制结束淡出 15 个客户端 Tick / 0.75 秒；死亡、换世界/维度、退出和断线立即清理。资源重载停止当前会话且不重新从头播放。

Hao Burst 沿用服务器 `HaoBurstPayload`，保留 6 Tick 音乐起点、8 Tick 淡入和 220–240 Tick 淡出。两类会话 UUID 独立。控制器只持有一个声音实例，重复同步不重播，结束会话被缓存拒绝；合法 Manual → Burst 升级复用声音并升级来源，Burst 优先。零音量初始实例会被游戏声音引擎跳过，因此初次提交声音时使用 0.001 的最低音量，之后沿用时间轴。

歌曲为已存在的 Git 跟踪资源，继续随正式及源码 JAR 打包。项目所有者在本轮确认已购买版权并可使用；不将音频视为代码 MIT 授权的一部分。

## 服务端道具操作

普通形态仍可打开两个 GUI、交易和运行假终端，只在奖励阶段检查嘉豪形态。GUI 打开不会发 Buff。现有协议 `HaoGadgetPayload` 保留，只有 BUY、SELL、CODE、会话/操作 UUID 和阶段；输入文本、Buff、等级、时长、客户端时间戳都不进入 C2S。

服务端签发会话与单次操作令牌。Market 至少等待 20 Tick，Code 至少等待 24 Tick；Code 客户端也等待假终端结束，再提交 COMMIT。服务端校验存活、非旁观者、原物品引用/手位/快捷栏、维度、心跳、过期时间、动作及令牌，冻结/演出锁定期间拒绝。令牌消费后不能重放。服务器只能验证合法会话及操作时序，不能证明修改过的客户端实际绘制了 GUI。

`JiahaoBuffManager.tryGrantRandomBuff(player, source)` 仅在已验证操作完成后调用，在服务器线程验证形态和持有物品、检查冷却、随机、合并效果。新增 `JiahaoGadgetResultPayload` **只注册 S2C**，关联会话/操作并携带结果、效果、等级、时长、Override 标记。Market 显示结果，Code 向现有终端追加结果，均使用中英文翻译键。

Market 与 Code 的 Buff 冷却各为 600 Tick / 30 秒，按玩家 UUID 独立。BUY/SELL 共用 Market 冷却，两个物品之间互不阻塞。GUI 与假演出在冷却中照常运行。已拥有更强/更长效果时仍消耗奖励冷却，不能立即重新抽取。关闭 GUI、重登录、重生、换维度、解除形态不会刷新冷却；服务器重启清空运行时冷却。

原有豪气 +2 和 200 Tick / 10 秒冷却保持独立，只有嘉豪形态获得豪气。

## Buff 池

普通 Buff 等级均为 I，整数秒持续时间在含端点区间内均匀随机。

| 来源 | 效果 | 权重 | 秒数 |
|---|---|---:|---:|
| Market | 幸运 | 30% | 30–60 |
| Market | 急迫 | 25% | 30–45 |
| Market | 速度 | 20% | 20–40 |
| Market | 伤害吸收 | 15% | 20–30 |
| Market | 生命恢复 | 10% | 8–12 |
| Code | 急迫 | 25% | 30–45 |
| Code | 速度 | 20% | 20–35 |
| Code | 夜视 | 20% | 45–90 |
| Code | 抗性提升 | 15% | 15–25 |
| Code | 跳跃提升 | 10% | 20–30 |
| Code | 力量 | 10% | 15–20 |

Code 先抽取 5% `SYSTEM OVERRIDE`，命中后等概率选择速度 II 15 秒、急迫 II 20 秒、跳跃提升 II 15 秒之一，替代普通奖励。表中的 Code 权重为其余 95% 的普通结果条件权重。

更高级效果保持原样；同级效果保留更长时长及无限时长。新等级更高时沿用原版升级/隐藏效果机制，保留较弱的长时效果。只添加目标效果，不清理玩家其他效果。使用原版粒子与状态图标。

新增 Market 4 句、Code 5 句加入现有语录池，实际应用奖励后服务端以 25% 概率尝试现有 Quote 系统，遵守其优先级和冷却，不创建新的字幕渲染器。

## 验证入口

```powershell
.\gradlew.bat build
.\gradlew.bat --no-configuration-cache -I scripts/gadget-smoke.gradle runClient
.\gradlew.bat --no-configuration-cache -I scripts/hao-smoke.gradle runClient
.\gradlew.bat --no-configuration-cache -I scripts/cinematic-smoke.gradle runClient
python scripts/verify-hao-release.py
```

客户端脚本只用于隔离测试目录；Gadget 脚本需先将已有测试世界复制到 `build/run/gadgetSmoke/saves/CinematicSmoke`。所有测试入口不进入发布包。完整运行证据、专用服务器流程和 Warning 见 `TESTING.md`。
