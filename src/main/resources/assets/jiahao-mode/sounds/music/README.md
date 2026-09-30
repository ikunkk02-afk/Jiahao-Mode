# Bundled Jiahao March music

`jiahao_march.ogg` is tracked and included in both release and source JARs, following the project owner's request to bundle the music. Local `runClient` loads the same Minecraft resource.

Encoding: OGG container, Vorbis quality 5, 44100 Hz, stereo. Example local conversion:

```powershell
ffmpeg -i "<your local source.flac>" -vn -ar 44100 -ac 2 -c:a libvorbis -q:a 5 jiahao_march.ogg
```

Sound event: `jiahao-mode:music.jiahao_march`. Resource entry: `jiahao-mode:music/jiahao_march` with streaming enabled in `../../sounds.json`. No runtime FLAC or external filesystem fallback exists. Missing music does not stop the cinematic. The project code license does not assign a separate license to this recording.
