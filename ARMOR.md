# 第六阶段：嘉豪盔甲与固定台词

Minecraft 1.21.1 / Fabric，包名 `com.shouyun.jiahaomode`，mod id `jiahao-mode`。
继续使用原有状态附件、时间暂停管理器、闪避、语录、网络、字幕和头顶文字。

## 获取、升级与数值

```mcfunction
/give @s jiahao-mode:jiahao_helmet
/give @s jiahao-mode:jiahao_chestplate
/give @s jiahao-mode:jiahao_leggings
/give @s jiahao-mode:jiahao_boots
/give @s jiahao-mode:jiahao_transformer
```

四件盔甲加入原版战斗创造分类；英文依次为 Jiahao Hood、Jiahao Chestplate、
Jiahao Leggings、Jiahao Boots，中文为嘉豪头盔、嘉豪胸甲、嘉豪护腿、嘉豪靴子。

| 部件 | 护甲 | 耐久 |
|---|---:|---:|
| 头盔 | 3 | 407 |
| 胸甲 | 8 | 592 |
| 护腿 | 6 | 555 |
| 靴子 | 4 | 481 |

材质注册 ID 为 `jiahao-mode:jiahao`；耐久倍率 37，钻石为 33，提升约 12.1%。
每件韧性 2.5、击退抗性 0.05、附魔能力 12，钻石修复，皮革穿戴声音。
整套护甲 21、韧性 10、击退抗性 0.20；钻石为 20 / 8 / 0，下界合金为 20 / 12 / 0.40。
主要提升一格靴子护甲、耐久和少量韧性/击退抗性，保持原版伤害计算和减伤上限。

锻造台三个栏位：**黑色染料 + 对应钻石盔甲 + 回声碎片**，每次各消耗一个。
四份 `smithing_transform` 配方位于 `src/main/resources/data/jiahao-mode/recipe/`，
文件名为四件物品 ID（无命名空间）加 `_smithing.json`。使用原版升级逻辑，保留附魔、名称、
纹饰及绝对耐久损耗；最大耐久、默认属性和材质改为嘉豪装备。
例如已损耗 123 点的钻石胸甲升级后仍损耗 123 点，但最大耐久为 592。

## 外观与正式资产

参考 [Alan Walker 官方 Core Logo Hoodie](https://store.alanwalker.com/products/core-logo-hoodie-lv2)
的兜帽、黑色服装及明显背部标识。使用原版 `ArmorItem`、原版盔甲几何和 UV，
无需自定义渲染器、模型库或头盔遮罩。

正式文件均实际创建，位于 `src/main/resources/assets/jiahao-mode/`：

- `textures/models/armor/jiahao_layer_1.png`：128×64 RGBA，头盔、胸甲、袖子、靴子。
- `textures/models/armor/jiahao_layer_2.png`：128×64 RGBA，腰带、裤腿和膝部。
- `textures/item/jiahao_{helmet,chestplate,leggings,boots}.png`：四张 16×16 RGBA 图标。
- `models/item/jiahao_{helmet,chestplate,leggings,boots}.json`：四份 `minecraft:item/generated` 模型。

内置图像生成工具分别生成两张服装纹理原稿和四张物品原稿，要求深黑/深灰布料、
窄眼缝、口罩、口袋和袖口，白色 AW 风格交叠标识，透明背景。对照本机 1.21.1
原版 diamond/leather armor layer 布局，将生成的各面校准到原版 64×32 逻辑 UV，
以 2 倍分辨率导出；去掉生成稿留白，保留未使用区域的透明度。
没有把整张宣传图直接当成 UV，也没有使用纯色占位图。

头盔前部用深灰兜帽包围黑色下半脸口罩，眼部留窄透明开口露出玩家皮肤。
帽子的原版额外岛保持透明，避免重复遮挡。胸前小标识、背部大标识，裤腿有小标识，
袖口和鞋底少量灰白点缀。两条腿和两只袖子遵循原版镜像 UV。
原版装备跟踪和盔甲 FeatureRenderer 让其他玩家直接看到装备，额外网络包不负责贴图。

AI 原稿留在 Codex 本地生成图片目录，最终 PNG 随源码交付。校准中间产物、游戏截图、
生成缓存与存档留在被忽略的 build 目录。素材表现为方块服装，兜帽厚度仍受原版头盔模型限制。

## 穿戴校验与清理

`armor.JiahaoArmorUtil.isWearingFullJiahaoArmor(LivingEntity)` 精确检查 HEAD、CHEST、LEGS、FEET
四个栏位的对应物品；背包、手持、同材质替代品、错误栏位均不计入。
`isJiahaoArmorPiece(ItemStack)` 提供四件物品的判断。

`JiahaoStateManager.isJiahao` 返回“原始形态附件开启且穿齐整套”。内部 `storedForm`
单独读取持久化标记，幂等切换和清理不会被有效形态判断阻挡。
变身器继续调用既有 `toggleJiahao`，进入前缺装备就在此发送 Action Bar 翻译
`message.jiahao-mode.armor_required`：**你必须穿上整套嘉豪盔甲**。
失败没有变身、雨天、音效或语录；主动解除不要求穿齐。

服务端 START_SERVER_TICK 校验先于时间暂停时钟，不依赖被冻结的玩家/世界 Tick。
缺一件最迟下一 Tick 调用既有 `setJiahao(false)`：清附件、清闪避、结束自己拥有的时间暂停、
结束演出同步、取消本人字幕与旧接收者头顶文字。输入解除，镜头按既有最多 3 Tick 过渡恢复。
脱装备至下一 Tick 之间，所有使用 `isJiahao` 的时间暂停、闪避和语录请求也已无效。

JOIN 在读取存档后校验；AFTER_RESPAWN 在 Fabric 默认附件复制阶段之后校验并恢复语录监测。
穿齐的保存形态可保留，不重播台词/雨天；无装备旧存档、死亡丢装备都清标记。
keepInventory 保留整套时可保留形态。重新穿齐永远不会自动变身。

## 两句固定台词

| ID | 翻译键 | 中文 |
|---|---|---|
| `jiahao-mode:quote/special.transform_revenge` | `jiahao.quote.special.transform_revenge` | 10年前的仇难道不报了吗 |
| `jiahao-mode:quote/special.time_stop_notice` | `jiahao.quote.special.time_stop_notice` | 注意时间并没有静止 |

英文分别为 “Isn’t it time to settle the grudge from ten years ago?” 和
“Be careful — time has not truly stopped.”，中文逐字保留。

Registry 仅将两句放入 ID 索引，没有加入随机池。`emitFixed` 指定 ID 并复用既有 `publish`、
事件序号、接收者、优先级、取消和网络。固定台词优先级 50、60 Tick，绕过随机冷却；
普通语录不能打断，成功的新固定事件可以替换旧固定台词。

成功变身的 `formChanged` 立即指定第一句，旧 20 Tick 随机变身预约移除。
时间暂停成功的 `timeStarted` 立即指定第二句，并标记会话已发开场；旧随机开场、
第 74 Tick Cinematic 台词及失败回退不会再叠加。消息等待 session 为空，立即显示本人字幕。
附近支持频道的同维度玩家收到原有 S2C，头顶绘制距离仍为 32 格（发送范围 64 格）。
正常结束台词依旧按显示间隔执行；脱装备导致结束时不播放结束台词。
字幕上移一行区域，避免立即固定台词与原版 Action Bar 状态提示重叠。

## 文件清单与验收

新增生产文件：两个 `armor/` 工具/材质类；四份物品模型、四张图标、两张盔甲 layer、四份配方。
修改生产文件：`JiahaoMode`、`ModItems`、`JiahaoStateManager`、`JiahaoQuoteRegistry`、
`JiahaoQuoteManager`、`JiahaoQuoteSyncPayload` 注释、`JiahaoSubtitleRenderer`、两份语言文件。
没有新增业务 Mixin、能力系统或客户端盔甲渲染类。

新增测试：`JiahaoArmorTests`，`src/armorsmoke/`（测试入口、两份阶段/确认 payload、
截图和语录观察 Mixin、测试资源），`scripts/armor-multiplayer-smoke.gradle` 与 `.py`。
更新测试：GameTest 注册、Transformation/Quote/Dodge 夹具与断言，Cinematic/Weather/Quote/Dodge
客户端夹具及旧 DodgeMultiplayerServer。它们显式装备全套，并将伤害期望纳入原版护甲减伤。
更新说明：README、QUOTES、TESTING 和本文。

实际验收结果、可重复命令、日志和截图位置见 [TESTING.md](TESTING.md) 第六阶段。
外观已通过真实 framebuffer 检查；标识粗细、眼缝宽度及布料明暗属于用户可以继续调整的审美细节。
