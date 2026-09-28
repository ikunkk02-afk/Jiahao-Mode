# 第一阶段验证记录

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
