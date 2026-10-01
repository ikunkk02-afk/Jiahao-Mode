# 音乐唱片

三张音乐唱片在创造模式“工具与实用物品”栏获取。普通形态即可使用：手持唱片右键原版唱片机播放，再次右键取出；歌曲结束后唱片留在机器内。支持原版唱片提示、音符粒子、比较器和漏斗行为，物品不可堆叠。

| 唱片 | 物品 ID | 时长 | 比较器信号 |
|---|---|---|---|
| 嘉豪进行曲 | `jiahao-mode:music_disc_jiahao_march` | 2:42 | 13 |
| Nevada | `jiahao-mode:music_disc_nevada` | 3:28.562 | 14 |
| Spectre | `jiahao-mode:music_disc_spectre` | 3:50.635 | 15 |

也可使用 `/give @s jiahao-mode:music_disc_jiahao_march` 等原版命令获取。本轮没有添加合成配方或生物掉落。

唱片使用原版“唱片机／音符盒”音量和空间衰减，附近玩家可听到。此前独立的“嘉豪音乐音量”继续控制 R 和豪气爆发的进行曲。

## 资源和来源

音频由项目所有者指定的 `C:\Users\shouy\Downloads\音乐` 中三个 MP3 转为 44.1 kHz、单声道、Vorbis 质量 5 的 OGG。单声道用于唱片机的空间定位。完整歌曲随正式 JAR 与源码 JAR 打包；运行时只读取 Minecraft 资源，不访问 Downloads 文件夹。R 和豪气爆发继续使用原有歌曲资源。

- `†TAKEDISKRUSH!†（嘉豪进行曲）.mp3`：文件元数据作者 N2UtheHartlocker。
- `Nevada.mp3`：文件元数据作者 Vicetone / Cozi Zuehlsdorff。
- `Spectre.mp3`：文件元数据作者 Alan Walker。

音频保留其原作者权利，不属于项目代码的 MIT 授权。唱片图标为本项目新增的三色像素图。

`scripts/import-music-discs.py <MP3 文件夹>` 可重新导入这三个源文件。该脚本生成歌曲资源、时长、模型、纹理和中英翻译。Fabric 1.21.1 的 `JUKEBOX_PLAYABLE` 组件关联 `data/jiahao-mode/jukebox_song`；服务端仅载入歌曲元数据，不解码音频。`scripts/verify-music-disc-release.py` 检查打包歌曲、时长、单声道格式、翻译、模型与测试组件隔离。
