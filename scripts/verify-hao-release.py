# SPDX-License-Identifier: MIT
"""Check Stage 8 resources and bundled-audio packaging after gradlew build."""
import json
import pathlib
import subprocess
import zipfile

root = pathlib.Path(__file__).resolve().parent.parent
audio = 'src/main/resources/assets/jiahao-mode/sounds/music/jiahao_march.ogg'
packed_audio = 'assets/jiahao-mode/sounds/music/jiahao_march.ogg'
assert subprocess.run(['git', 'check-ignore', '--quiet', audio], cwd=root).returncode == 1
assert (root / audio).is_file(), 'Bundled music is required'

for name in ['jiahao-mode-1.0.0.jar', 'jiahao-mode-1.0.0-sources.jar']:
    with zipfile.ZipFile(root / 'build/libs' / name) as archive:
        entries = archive.namelist()
        assert packed_audio in entries, name + ' is missing bundled music'
        assert archive.read(packed_audio) == (root / audio).read_bytes(), name + ' contains different music'
        assert not any('/test/' in entry or 'HaoSmoke' in entry or 'HaoDedicated' in entry or 'hao-smoke' in entry for entry in entries)
        sounds = json.loads(archive.read('assets/jiahao-mode/sounds.json'))
        assert sounds['music.jiahao_march']['sounds'] == [
            {'name': 'jiahao-mode:music/jiahao_march', 'stream': True}]
        for language in ['zh_cn', 'en_us']:
            lines = json.loads(archive.read(f'assets/jiahao-mode/lang/{language}.json'))
            assert len([key for key in lines if key.startswith('jiahao.quote.hao_burst.')]) == 20
            assert all(lines[f'jiahao.quote.hao_burst.{i}'] for i in range(1, 21))
            assert all(lines[f'jiahao.quote.market.{i}'] for i in range(1, 15))
            assert all(lines[f'jiahao.quote.code.{i}'] for i in range(1, 18))
            assert all(lines[f'message.jiahao-mode.buff.{key}'] for key in ['form_required','cooldown','invalid','preserved','granted','override'])
        print(name + ': registration/translations/music present; test harness excluded')

for source in (root / 'src/main/java').rglob('*.java'):
    content = source.read_text(encoding='utf-8')
    assert 'net.minecraft.client.' not in content, str(source) + ' references client classes'
for directory in ['src/main/java', 'src/client/java']:
    for source in (root / directory).rglob('*.java'):
        content = source.read_text(encoding='utf-8')
        assert 'CloudMusic' not in content and '.flac' not in content.lower()

local = root / audio
if local.exists():
    data = json.loads(subprocess.check_output([
        'ffprobe', '-v', 'error', '-show_entries', 'stream=codec_name,sample_rate,channels',
        '-of', 'json', str(local)], encoding='utf-8'))
    stream = data['streams'][0]
    assert stream['codec_name'] == 'vorbis' and stream['sample_rate'] == '44100' and stream['channels'] == 2
    print('Local music: Vorbis, 44100 Hz, stereo; retained on disk')
else:
    print('Bundled music absent')
print('HAO RELEASE VERIFICATION PASSED')
