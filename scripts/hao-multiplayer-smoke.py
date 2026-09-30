# SPDX-License-Identifier: MIT
"""Stage 8 real dedicated server and two isolated clients."""
import json
import pathlib
import subprocess
import time

root = pathlib.Path(__file__).resolve().parent.parent
run = root / 'build/run/haoDedicated'
run.mkdir(parents=True, exist_ok=True)
(run / 'eula.txt').write_text('eula=true\n')
(run / 'server.properties').write_text(
    'online-mode=false\nserver-ip=127.0.0.1\nserver-port=25581\n'
    'level-name=HaoDedicated\nview-distance=6\nsimulation-distance=4\n'
    'spawn-protection=0\ndifficulty=normal\nmax-players=4\n'
    'enable-rcon=false\nenable-query=false\n')


def run_phase(phase):
    processes = []

    def launch(role):
        spec = json.loads((root / f'build/verification/hao-{role}-launch.json').read_text())
        directory = pathlib.Path(spec['directory'])
        directory.mkdir(parents=True, exist_ok=True)
        if role != 'server':
            options = directory / 'options.txt'
            lines = options.read_text().splitlines() if options.exists() else []
            lines = [line for line in lines if not line.startswith(('lang:', 'pauseOnLostFocus:', 'tutorialStep:'))]
            options.write_text('\n'.join(lines + ['lang:zh_cn', 'pauseOnLostFocus:false', 'tutorialStep:none']) + '\n')
        result = directory / ('hao-server-result.txt' if role == 'server' else 'hao-client-result.txt')
        result.unlink(missing_ok=True)
        arguments = spec['arguments']
        args = root / f'build/verification/hao-{role}-runtime.args'
        args.write_text('\n'.join(json.dumps(s) for s in arguments), encoding='utf-8')
        log = (root / f'build/hao-multiplayer-{phase}-{role}.log').open('w', encoding='utf-8')
        process = subprocess.Popen(
            [spec['executable'], '@' + str(args)], cwd=directory, stdin=subprocess.PIPE,
            stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        processes.append((role, process, result, log))
        return process

    try:
        server = launch('server')
        deadline = time.monotonic() + 100
        while time.monotonic() < deadline:
            log = (root / f'build/hao-multiplayer-{phase}-server.log').read_text(encoding='utf-8', errors='replace')
            if 'Done (' in log:
                break
            if server.poll() is not None:
                raise RuntimeError('Server failed to start')
            time.sleep(.5)
        else:
            raise RuntimeError('Server startup watchdog')
        actor = launch('actor')
        observer = launch('observer')
        deadline = time.monotonic() + 420
        while time.monotonic() < deadline and (actor.poll() is None or observer.poll() is None):
            for role, process, result, log in processes:
                if result.exists() and result.read_text().startswith('FAILED'):
                    raise RuntimeError(role + ': ' + result.read_text())
            time.sleep(.5)
        if actor.poll() is None or observer.poll() is None:
            raise RuntimeError('Client watchdog')
        for role, process, result, log in processes:
            result_text = result.read_text() if result.exists() else 'MISSING RESULT'
            print(phase, role, result_text, flush=True)
            if result_text != 'PASSED' or role != 'server' and process.returncode != 0:
                raise RuntimeError(role + ' ' + result_text)
        server.stdin.write(b'stop\n')
        server.stdin.flush()
        server.wait(timeout=40)
        if server.returncode:
            raise RuntimeError('Server did not exit cleanly')
    finally:
        for role, process, result, log in processes:
            if process.poll() is None:
                if role == 'server':
                    try:
                        process.stdin.write(b'stop\n')
                        process.stdin.flush()
                        process.wait(timeout=15)
                    except Exception:
                        process.terminate()
                else:
                    process.terminate()
                process.wait(timeout=15)
            log.close()


run_phase('first')

print('HAO DEDICATED + TWO CLIENTS ALL PASSED', flush=True)
