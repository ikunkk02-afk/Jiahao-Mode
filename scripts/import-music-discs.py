# SPDX-License-Identifier: MIT
"""Convert the three supplied MP3s to positional jukebox audio and create disc resources."""
import json
from pathlib import Path
import struct
import subprocess
import sys
import zlib

ROOT = Path(__file__).resolve().parent.parent / 'src/main/resources'
SONGS = [
    ('jiahao_march', '†TAKEDISKRUSH!†（嘉豪进行曲）', '嘉豪进行曲', 'Jiahao March', 'N2UtheHartlocker', (224, 196, 82), 13),
    ('nevada', 'Nevada', 'Nevada', 'Nevada', 'Vicetone / Cozi Zuehlsdorff', (83, 204, 188), 14),
    ('spectre', 'Spectre', 'Spectre', 'Spectre', 'Alan Walker', (107, 141, 232), 15),
]


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def disc_texture(path, label):
    """Original 16px pixel-art disc with grooves, label and transparent center hole."""
    rows = bytearray()
    for y in range(16):
        rows.append(0)  # PNG row filter
        for x in range(16):
            r2 = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if r2 > 49 or r2 < 1:
                color = (0, 0, 0, 0)
            elif r2 <= 9:
                shade = 1 if y < 8 else .75
                color = tuple(round(c * shade) for c in label) + (255,)
            else:
                groove = 48 if 16 < r2 < 23 or 34 < r2 < 40 else 29
                color = (groove, groove, groove + 3, 255)
                if x + y < 9:
                    color = (66, 66, 70, 255)
            rows.extend(color)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0))
                     + chunk(b'IDAT', zlib.compress(bytes(rows))) + chunk(b'IEND', b''))


def main(source):
    assets = ROOT / 'assets/jiahao-mode'
    sounds = json.loads((assets / 'sounds.json').read_text(encoding='utf-8'))
    translations = {lang: json.loads((assets / f'lang/{lang}.json').read_text(encoding='utf-8')) for lang in ['zh_cn', 'en_us']}
    # Validate all sources before writing anything.
    for _, filename, *_ in SONGS:
        if not (source / (filename + '.mp3')).is_file():
            raise FileNotFoundError(source / (filename + '.mp3'))
    for song, filename, zh, en, artist, label, comparator in SONGS:
        audio = assets / f'sounds/music/discs/{song}.ogg'
        audio.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(['ffmpeg', '-nostdin', '-hide_banner', '-loglevel', 'error', '-y', '-i',
                        str(source / (filename + '.mp3')), '-map', '0:a:0', '-vn', '-map_metadata', '-1',
                        '-ac', '1', '-ar', '44100', '-c:a', 'libvorbis', '-q:a', '5', str(audio)], check=True)
        probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_entries', 'format=duration',
                                                   '-of', 'json', str(audio)], encoding='utf-8'))
        duration = float(probe['format']['duration'])
        sounds[f'music_disc.{song}'] = {'sounds': [{'name': f'jiahao-mode:music/discs/{song}', 'stream': True}]}
        write_json(ROOT / f'data/jiahao-mode/jukebox_song/{song}.json', {
            'sound_event': f'jiahao-mode:music_disc.{song}',
            'description': {'translate': f'jukebox_song.jiahao-mode.{song}'},
            'length_in_seconds': duration, 'comparator_output': comparator,
        })
        write_json(assets / f'models/item/music_disc_{song}.json', {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': f'jiahao-mode:item/music_disc_{song}'},
        })
        disc_texture(assets / f'textures/item/music_disc_{song}.png', label)
        for lang, title in [('zh_cn', zh), ('en_us', en)]:
            translations[lang][f'item.jiahao-mode.music_disc_{song}'] = ('音乐唱片 — ' if lang == 'zh_cn' else 'Music Disc — ') + title
            translations[lang][f'jukebox_song.jiahao-mode.{song}'] = artist + ' — ' + (filename if song == 'jiahao_march' else title)
        print(f'{song}: {duration:.3f}s, mono Vorbis')
    write_json(assets / 'sounds.json', sounds)
    for lang, lines in translations.items():
        write_json(assets / f'lang/{lang}.json', lines)
    write_json(ROOT / 'data/c/tags/item/music_discs.json', {
        'replace': False, 'values': [f'jiahao-mode:music_disc_{song[0]}' for song in SONGS],
    })


if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise SystemExit('Usage: python scripts/import-music-discs.py <folder containing the three MP3s>')
    main(Path(sys.argv[1]))
