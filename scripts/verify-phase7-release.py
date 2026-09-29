# SPDX-License-Identifier: MIT
"""Read-only resource and side-boundary checks for the built release JAR."""
from pathlib import Path
from io import BytesIO
import hashlib
import json
import zipfile
from PIL import Image

root = Path(__file__).resolve().parent.parent
jar = root / 'build/libs/jiahao-mode-1.0.0.jar'

def unique(pairs):
    result = {}
    for key, value in pairs:
        assert key not in result, 'Duplicate JSON key: ' + key
        result[key] = value
    return result

expected = {
    'random_moment': [
        '有些事情，只有站得够高才能看清。', '你看到的是结果，我看到的是过程。',
        '雨不是为我下的，只是刚好遇见我。', '不是所有沉默，都代表没有答案。',
        '我只是停下来了，不代表世界也停了。', '有人在追时间，而我在等时间。',
        '真正重要的东西，从来不需要解释。', '有些路，看起来没人走，其实只是你看不到。',
        '风往哪里吹，不重要。', '我没有回头，只是世界从我身后经过。',
        '现在的你，还理解不了。', '别问为什么。', '这不是结束，这是开始之前的安静。',
        '时间会证明很多事情，但我不需要等。', '我只是在观察。', '你以为我停下来了？',
        '有时候速度越快，看起来越像静止。', '真正的变化，往往没有声音。',
        '如果你看懂了，就不需要我解释。', '我不是在装，我只是在保持状态。'],
    'market': ['市场不会骗人，只会筛选人。', '真正的机会，从来不会提醒你。',
        '红色和绿色，只是普通人眼里的颜色。', '我看的不是曲线，是人心。',
        '数字下跌，不代表我输了。', '真正的盈利，在图表之外。', '别人看风险，我看方向。',
        '如果所有人都看懂了，那就已经晚了。', '行情没有变，只是你开始害怕了。',
        '今天的价格，只是明天的回忆。'],
    'code': ['代码不会出错，出错的是理解代码的人。', '真正的程序，不需要运行。',
        '当你看到结果的时候，程序早就结束了。', 'Bug 只是系统没有理解我的意思。',
        '别人写代码，我修改规则。', '没有报错，不代表没有问题。',
        '我不是在调试，我是在观察系统自己修复。', '如果代码看起来很复杂，那说明你站得还不够高。',
        '编译成功只是最普通的结果。', '我输入的不是命令，是答案。', '权限？我不需要权限。',
        '系统没有拒绝，只是还没理解。']
}

with zipfile.ZipFile(jar) as package:
    names = package.namelist()
    assert not any('/test/' in n or 'smoke' in n.lower() or 'gametest' in n.lower() for n in names), 'Test content in release'
    zh = json.loads(package.read('assets/jiahao-mode/lang/zh_cn.json'), object_pairs_hook=unique)
    en = json.loads(package.read('assets/jiahao-mode/lang/en_us.json'), object_pairs_hook=unique)
    assert zh.keys() == en.keys(), 'Language key mismatch'
    for category, lines in expected.items():
        for number, line in enumerate(lines, 1):
            key = f'jiahao.quote.{category}.{number}'
            assert zh[key] == line and en[key].strip(), key
    print('42 new Chinese lines match exactly; English complete; language keys unique and matched')
    assert sum(k.startswith('jiahao.quote.') for k in zh) == 87
    for name in ['jiahao_transformer', 'market_viewer', 'jiahao_code_editor']:
        path = f'assets/jiahao-mode/textures/item/{name}.png'
        data = package.read(path)
        image = Image.open(BytesIO(data))
        assert image.mode == 'RGBA' and image.size == (16, 16), path
        assert set(image.getchannel('A').tobytes()) == {0, 255}, path
        model = json.loads(package.read(f'assets/jiahao-mode/models/item/{name}.json'))
        assert model['textures']['layer0'] == f'jiahao-mode:item/{name}'
        print(name, '16x16 RGBA, transparent pixels, model reference correct, SHA256', hashlib.sha256(data).hexdigest())
    for name, ingredient in [('market_viewer', 'paper'), ('jiahao_code_editor', 'book')]:
        recipe = json.loads(package.read(f'data/jiahao-mode/recipe/{name}.json'))
        assert recipe['type'] == 'minecraft:crafting_shapeless'
        assert recipe['result'] == {'id': 'jiahao-mode:' + name, 'count': 1}
        assert sorted(i['item'] for i in recipe['ingredients']) == sorted('minecraft:' + i for i in ['iron_ingot', 'glass_pane', 'redstone', ingredient])
    for name in names:
        if name.endswith('.class') and name.startswith('com/shouyun/jiahaomode/'):
            data = package.read(name)
            if '/client/' not in name:
                assert b'net/minecraft/client/' not in data, 'Common class references client: ' + name
            if '/client/gadget/' in name or name.endswith('JiahaoGadgetItem.class'):
                for forbidden in [b'java/lang/ProcessBuilder', b'java/lang/Runtime', b'java/net/', b'java/nio/file/', b'java/io/File']:
                    assert forbidden not in data, 'Gadget has external execution/network/file reference: ' + name
    print('Recipes packaged; common classes have no client references; gadgets have no process/network/file API')
print('PHASE7 RELEASE VERIFICATION PASSED', jar)
