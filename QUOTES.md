# 第四阶段：嘉豪语录 / 对话表现系统

Minecraft 1.21.1 / Fabric；沿用 `com.shouyun.jiahaomode`。本阶段仅文本，无 TTS、语音文件或音频依赖。

## 结构与新增文件

公共代码位于 `src/main/java/com/shouyun/jiahaomode/`：

- `quote/JiahaoQuote.java`：不可变语录 record。
- `quote/JiahaoQuoteCategory.java`：类别、优先级及初始条目数。
- `quote/JiahaoQuoteRegistry.java`：43 条语录定义（第五阶段补充 6 条 PERFECT_DODGE）、按类别检索和不重复选择。
- `quote/JiahaoQuoteManager.java`：服务器权威、预约、冷却、会话与接收者。
- `quote/JiahaoQuoteTrigger.java`：生命周期、攻击、死亡事件接入。
- `network/JiahaoQuoteNetworking.java`：频道及服务器接收器注册。
- `network/JiahaoQuoteRequestPayload.java`：空的手动请求。
- `network/JiahaoQuoteSyncPayload.java`：语录显示/取消消息。
- `network/JiahaoQuotePlaybackFailedPayload.java`：仅携带演出 session 的播放失败报告。
- `mixin/ServerPlayerQuoteDamageMixin.java`：在真实伤害调用前后读取生命和吸收值，取得结算伤害。
- `mixin/ServerPlayerQuoteActivityMixin.java`：空挥手也重置闲置时间。

客户端代码位于 `src/client/java/com/shouyun/jiahaomode/client/`：

- `JiahaoQuoteKeyBindings.java`：V 键。
- `JiahaoQuoteClientState.java`：按说话者 UUID 管理显示与结束语录等待。
- `JiahaoSubtitleRenderer.java`：本人字幕。
- `JiahaoSpeechBubbleRenderer.java`：其他玩家的头顶文字。

测试新增 `src/gametest/.../JiahaoQuoteTests.java`、`src/quotesmoke/` 中的 QuoteSmoke、截图 Mixin 和测试资源，以及 `scripts/quote-smoke.gradle`。测试代码不加入发布 JAR。

现有 main/client initializer、状态变更、时间暂停启动/停止/演出中止、Cinematic controller、Mixin 注册和中英文翻译文件增量接入；实体、云与天气冻结逻辑未重建。

## 数据与翻译

`JiahaoQuote(Identifier id, String translationKey, JiahaoQuoteCategory category, int priority, int displayTicks, Optional<Identifier> soundId, Optional<Identifier> animationId)`。

soundId 预留未来 SoundEvent 的注册 ID；当前所有条目为空，不加载或播放音频。animationId 同样仅预留。

| 类别 | 条目数 | 优先级 |
| --- | ---: | ---: |
| MANUAL | 8 | 10 |
| TRANSFORM | 4 | 20 |
| TIME_STOP_START | 5 | 30 |
| TIME_STOP_END | 4 | 30 |
| PERFECT_DODGE | 6 | 35 |
| CINEMATIC | 1 | 40 |
| LOW_HEALTH | 4 | 20 |
| TAKE_DAMAGE | 4 | 10 |
| KILL_ENTITY | 4 | 20 |
| IDLE | 3 | 10 |

文本全部在 `assets/jiahao-mode/lang/zh_cn.json`、`en_us.json`，键为 `jiahao.quote.<category>.<number>`。原文按需求收录，不声称是真实人物原话。CINEMATIC 固定“世界，安静一点。”，其他类别随机。未来加类别、条目和事件调用，不需要把文本写进 Item 或 Cinematic。

## 输入与联机权限

V 由独立 KeyBindingHelper 注册，主客户端入口初始化，复用嘉豪控制分类。名称为“嘉豪语录 / Jiahao Quote”，可在控制设置中改键。

V → 空 C2S → 从连接取得玩家 → 服务器验证存活、嘉豪形态、冷却与演出占用 → 服务器选句 → S2C。客户端不能上传 Quote ID、类别、任意文本或代替其他玩家说话。

S2C 含 UUID、维度、Quote ID、递增事件编号和可选演出 session；Quote ID 为空表示取消。只向同维度 64 格内支持频道的玩家发送，包括本人；客户端头顶绘制距离另限 32 格。旧接收者会在说话者状态失效时收到取消，防止离开范围或换维度后残留。

播放失败 C2S 只能引用当前拥有者的当前有效时间暂停 session，且只允许在本轮尚未发句时回退一次。它不能指定类别或语录。当前客户端在摄像机被其他实体占用、无法开始自己的演出时报告失败；服务器选择 TIME_STOP_START。

## 触发与时间线

- 成功变身后延迟 20 个服务器 Tick；解除、死亡、换维度、退出取消。登录/重生只恢复监测，不重播变身。
- 实际伤害至少 2 点、18% 概率；盾挡、护甲、吸收后的变化由伤害 Mixin 读取。致死或非嘉豪不播。
- 死亡事件的伤害来源归属于嘉豪玩家，且受害者实现 Monster，才有 20% 击杀语录概率。盔甲架、动物、物品、投射物和玩家排除。
- 生命首次低于 30% 锁存本次阶段，严格超过 50% 才重置。被演出挡住时最多保留一条；脱离低血量区间便丢弃等待项。
- 连续闲置 600 Tick 后以 10% 概率尝试；每 600 Tick 最多再试一次。位置变化、攻击（含空挥）、伤害、手动说话与时间技能重置计时；冻结旁观者不说闲置语录。
- 时间暂停成功后使用原有 session。可演出时预约第 74 Tick，取消尚未播出的 TRANSFORM，并阻止普通台词插入。本人客户端等到实际 Timeline 至少 0.74 才开始字幕；其他人显示头顶文字。
- 无法演出或在发句前中止时回退 TIME_STOP_START；已发 CINEMATIC 则不回退。同一会话只发一次。
- 正常到时或主动 R 结束触发 TIME_STOP_END；高优先级文本未结束时延后到其结束后 5 Tick。死亡、退出、换维度、解除形态不播结束语录。
- 第五阶段由服务器 `ALLOW_DAMAGE` 成功消费完美机会后调用 `perfectDodge`。首次符合播放条件必播，随后成功播放至少相隔 80 Tick；不覆盖 CINEMATIC。受冷却或高优先级文本阻挡时不播语录，但音效、粒子、Pose 和本人镜头始终触发。

## 冷却、优先级和清理

手动间隔 50 Tick，普通自动间隔至少 60 Tick，闲置判定间隔 600 Tick。每个玩家一个服务器显示占用，普通语录不能顶掉正在显示的语录；特殊时间事件可越过普通冷却并替换较低优先级。只有低血量和结束事件有单条等待，其他受阻随机事件直接丢弃。新演出取消旧的待播结束事件。

保存最后实际选出的 ID 和每类别上次 ID，多条目的类别排除上次结果；单条目的 CINEMATIC 允许重复。失败请求不更新选择历史。显示状态和冷却历史分离，快速变身或换维度不能绕过手动冷却；断线移除历史。

服务端只监测已登记的嘉豪玩家，每次发句才遍历当前维度玩家选择接收者。客户端仅维护当前有效显示，结束删除；不是每 Tick 扫描所有玩家寻找台词。

## 渲染与独立计时

本人通过 HUD callback 显示白色原版字体、轻黑影、可换行的下方居中字幕，避开快捷栏和电影黑边，不占用 ActionBar。本人不重复显示自己的头顶文字。旁观者只见对应 UUID 的 billboard 文字，半透明黑底，位于名字上方，跟随玩家、面向实际摄像机，遵循深度遮挡。

默认 2.5 秒：0.15 秒淡入，最后 0.25 秒淡出。服务端使用原有独立 server tick；客户端用 `System.nanoTime()` 单调真实时间。冻结 World Time、天气或实体模拟不会冻结文字动画。服务器 Tick 在正常 20 TPS 下对应声明秒数；服务器卡顿时冷却按 Tick 延长。

Text、换行结果缓存，语言或宽度变化才重排。客户端同时校验事件编号、维度和注册 ID；低优先级消息不能覆盖仍有效的高优先级文本。换世界/断线清空，死亡或服务器取消移除对应 UUID。

所有渲染、输入和 MinecraftClient 引用留在 client source set。Dedicated Server 只加载数据、网络和服务端调度。

## 验证入口

- `gradlew.bat build`：包括旧测试与新增服务器专项 GameTest。
- `gradlew.bat -I scripts/quote-smoke.gradle runClient`：实际客户端与隔离 QuoteSmoke 世界、输入网络链路、电影触发、淡入淡出、头顶文字、生命周期及截图。
- 原有 `scripts/cinematic-smoke.gradle`、`scripts/weather-visual-smoke.gradle`：镜头/Pose 与天气冻结回归。
- 实际结果、限制及日志位置见 `TESTING.md` 第四阶段。

客户端测试首次使用需要在 `build/run/quoteSmoke/saves/QuoteSmoke` 准备可载入的测试世界；本次从既有隔离 CinematicSmoke 存档复制，不接触日常存档。测试会修改隔离世界并自动保存退出。原生双客户端联机观感、不同 GUI 缩放、极长自定义资源包文本和 Shader 兼容不以这些自动测试代替人工验收。
