# SPDX-License-Identifier: MIT
"""Verify all jukebox songs, models, translations and positional audio in release artifacts."""
import json
from pathlib import Path
import subprocess
import zipfile

root = Path(__file__).resolve().parent.parent
songs = ['jiahao_march', 'nevada', 'spectre']
for name in ['jiahao-mode-1.0.0.jar', 'jiahao-mode-1.0.0-sources.jar']:
    with zipfile.ZipFile(root / 'build/libs' / name) as archive:
        sounds = json.loads(archive.read('assets/jiahao-mode/sounds.json'))
        tags = json.loads(archive.read('data/c/tags/item/music_discs.json'))
        assert tags['values'] == [f'jiahao-mode:music_disc_{song}' for song in songs]
        for i, song in enumerate(songs):
            base = 'assets/jiahao-mode'
            audio = f'{base}/sounds/music/discs/{song}.ogg'
            assert archive.read(audio) == (root / 'src/main/resources' / audio).read_bytes()
            assert sounds[f'music_disc.{song}']['sounds'] == [
                {'name': f'jiahao-mode:music/discs/{song}', 'stream': True}]
            data = json.loads(archive.read(f'data/jiahao-mode/jukebox_song/{song}.json'))
            assert data['sound_event'] == f'jiahao-mode:music_disc.{song}'
            assert data['description']['translate'] == f'jukebox_song.jiahao-mode.{song}'
            assert data['comparator_output'] == 13 + i
            local_audio = root / 'src/main/resources' / audio
            probe = json.loads(subprocess.check_output([
                'ffprobe', '-v', 'error', '-show_entries', 'stream=codec_name,sample_rate,channels:format=duration',
                '-of', 'json', str(local_audio)], encoding='utf-8'))
            stream = probe['streams'][0]
            assert stream['codec_name'] == 'vorbis' and stream['channels'] == 1 and stream['sample_rate'] == '44100'
            assert abs(data['length_in_seconds'] - float(probe['format']['duration'])) < .001
            model = json.loads(archive.read(f'{base}/models/item/music_disc_{song}.json'))
            assert model['textures']['layer0'] == f'jiahao-mode:item/music_disc_{song}'
            assert archive.read(f'{base}/textures/item/music_disc_{song}.png').startswith(b'\x89PNG')
            for lang in ['zh_cn', 'en_us']:
                lines = json.loads(archive.read(f'{base}/lang/{lang}.json'))
                assert lines[f'item.jiahao-mode.music_disc_{song}'] and lines[f'jukebox_song.jiahao-mode.{song}']
        assert not any('/test/' in entry or 'music-disc-smoke' in entry or 'MusicDiscSmoke' in entry
                       for entry in archive.namelist())
    print(name + ': three playable discs, audio, models and translations verified; tests excluded')
print('MUSIC DISC RELEASE VERIFICATION PASSED')
