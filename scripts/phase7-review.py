# SPDX-License-Identifier: MIT
"""Assemble local, read-only visual review gallery from actual client screenshots."""
from pathlib import Path
from html import escape

root = Path(__file__).resolve().parent.parent
out = root / 'build/verification/phase7-review.html'
shot = root / 'build/run/phase7Actor/screenshots'
resources = root / 'src/main/resources/assets/jiahao-mode/textures/item'
groups = {
    'Seven static poses': [
        ('Running Freeze', shot / 'pose-running_freeze.png'),
        ('Lean Back', shot / 'pose-lean_back.png'),
        ('Point Sky', shot / 'pose-point_sky.png'),
        ('Look Distance', shot / 'pose-look_distance.png'),
        ('Thinking Hao', shot / 'pose-thinking_hao.png'),
        ('Running Look Back', shot / 'pose-running_look_back.png'),
        ('Rain Embrace', shot / 'pose-rain_embrace.png')],
    'Actual performances': [
        ('Natural Jiahao Moment', shot / 'actor-natural-moment.png'),
        ('Time Stop camera', shot / 'actor-time-stop-priority.png')],
    'Entertainment screens': [
        ('Market Viewer', shot / 'market-hao.png'),
        ('Jiahao Code Editor', shot / 'code-hao.png')],
    'Final 16 by 16 item icons': [
        ('Jiahao Transformer', resources / 'jiahao_transformer.png'),
        ('Market Viewer', resources / 'market_viewer.png'),
        ('Jiahao Code Editor', resources / 'jiahao_code_editor.png')]
}
lines = [
    '<!doctype html><html lang="zh"><meta charset="utf-8"><title>Jiahao Mode · Stage 7 Visual Review</title>',
    '<style>body{background:#101720;color:#e8f3ee;font:16px system-ui;margin:3vw}h1{color:#87ebbe}h2{margin-top:2rem;border-bottom:1px solid #52655e;padding-bottom:.5rem}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:1rem}.card{background:#19242d;border:1px solid #395445;border-radius:8px;padding:.8rem}.card img{display:block;width:100%;object-fit:contain;background:#080d0e;image-rendering:pixelated}.card.icon img{height:160px;object-fit:contain}.card a{color:#8ce7b5}.card p{margin:.6rem 0 0}small{color:#aab6b0}</style>',
    '<h1>Jiahao Mode · Stage 7</h1><p>真实双客户端截图与发布用图标。点击可查看原图。Pose 和镜头构图请以你自己的游戏窗口为最终审美判断。</p>'
]
for group, entries in groups.items():
    lines.append('<h2>' + escape(group) + '</h2><div class="grid">')
    for title, path in entries:
        assert path.is_file(), 'Missing screenshot/icon: ' + str(path)
        rel = Path('..', '..') / path.relative_to(root) if path.is_relative_to(root / 'src') else Path('..') / path.relative_to(root / 'build')
        label, source = escape(title), escape(rel.as_posix())
        lines.append(f'<div class="card{" icon" if group.startswith("Final") else ""}"><a href="{source}" target="_blank"><img src="{source}" alt="{label}"></a><p>{label}</p></div>')
    lines.append('</div>')
lines.append('<small>Screenshot fixture: isolated build/run/phase7Actor. No image data is sent to a server.</small></html>')
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text('\n'.join(lines), encoding='utf-8')
print('Visual gallery:', out)
