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

本阶段不包含时间暂停、闪避、动画、镜头、人物模型、皮肤、语音、HUD、配置或按键。

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
Fabric 自动同步到玩家自己以及正在跟踪该玩家的客户端。无需直接操作 NBT、
自定义网络协议、第三方状态库或模组自定义 Mixin。

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

构建同时运行 `src/gametest` 的服务端集成测试。测试使用已有 Fabric API，在
`build/run/gameTest` 中创建隔离世界，不写入日常 `run/saves`；测试类不包含在发布 JAR 中。
也可单独执行 `gradlew.bat runGameTest`。完整验证记录及手动测试项见 [TESTING.md](TESTING.md)。

编译目标为 Java 21。当前项目通过 `gradle.properties` 的 `org.gradle.java.home`
指定本机 JDK 25 路径，其他电脑需调整这个本地路径，或通过
`-Dorg.gradle.java.home=<JDK 路径>` 覆盖。版本与 mappings 保持原项目配置。

## License

Jiahao Mode is licensed under the MIT license; see [LICENSE](LICENSE).

The initial project was created with the Fabric Template Generator. The Fabric
template is distributed under CC0; its original license is preserved in
[LICENSE-FABRIC-TEMPLATE](LICENSE-FABRIC-TEMPLATE).
