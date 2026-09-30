# Local optional music

Place a locally authorized copy at `jiahao_march.ogg` here. Git ignores it and all release/source JARs exclude it. Local `runClient` still loads it through Minecraft resources.

Encoding: OGG container, Vorbis quality 5, 44100 Hz, stereo. Example local conversion:

```powershell
ffmpeg -i "<your local source.flac>" -vn -ar 44100 -ac 2 -c:a libvorbis -q:a 5 jiahao_march.ogg
```

Sound event: `jiahao-mode:music.jiahao_march`. Resource entry: `jiahao-mode:music/jiahao_march` with streaming enabled in `../../sounds.json`. No runtime FLAC or external filesystem fallback exists. Missing music does not stop the cinematic. Obtain redistribution permission before changing packaging policy.
