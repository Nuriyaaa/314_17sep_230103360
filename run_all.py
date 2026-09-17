from pathlib import Path
import subprocess
import sys
import platform
import datetime

ROOT = Path(__file__).resolve().parent

def main():
    output = ROOT / 'results_230103360.txt'
    if output.exists():
        stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
        output.rename(ROOT / f'results_230103360_previous_{stamp}.txt')
    with output.open('w', encoding='utf-8', buffering=1) as log:
        def emit(message):
            print(message, flush=True)
            log.write(message + '\n')
        emit('Student: Sultanseitova Nuriya | ID: 230103360')
        emit('Local run: ' + datetime.datetime.now().astimezone().isoformat())
        emit('Python: ' + sys.version.replace('\n', ' '))
        emit('Executable: ' + sys.executable)
        emit('Platform: ' + platform.platform() + ' / ' + platform.machine())
        gil = getattr(sys, '_is_gil_enabled', None)
        emit('GIL enabled: ' + (str(gil()) if gil else 'not exposed by runtime'))
        try:
            import numpy
            emit('NumPy: ' + numpy.__version__)
        except ImportError:
            emit('ERROR: NumPy unavailable for this interpreter. Install it before running.')
            return 1
        hardware = ROOT / 'hardware_230103360.txt'
        if hardware.exists():
            emit('=== Previously collected hardware metadata ===')
            emit(hardware.read_text())
        if sys.platform == 'darwin':
            emit('=== Current core and cache check ===')
            check = subprocess.run(['sysctl', 'hw.physicalcpu', 'hw.logicalcpu', 'hw.cachelinesize'], capture_output=True, text=True)
            emit(check.stdout + check.stderr)
        tasks = ['task1_amdahl.py'] + ['task2_falsesharing.py'] * 3 + ['task3_sync.py', 'task4_roofline.py', 'bonus_scaling.py']
        failed = []
        for number, task in enumerate(tasks, 1):
            emit(f'\n=== RUN {number}/{len(tasks)}: {task} ===')
            with subprocess.Popen([sys.executable, '-u', str(ROOT/task)], cwd=ROOT,
                                  stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                  text=True, bufsize=1) as proc:
                for line in proc.stdout:
                    print(line, end='', flush=True)
                    log.write(line)
                code = proc.wait()
            emit(f'Exit code: {code}')
            if code: failed.append(task)
        emit('\nALL TASKS FINISHED' if not failed else '\nFAILED TASKS: ' + ', '.join(failed))
        emit('Send results_230103360.txt and bonus_results_230103360.csv for analysis.')
        return bool(failed)

if __name__ == '__main__':
    raise SystemExit(main())
