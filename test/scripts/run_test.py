import argparse
import os
import shutil
import subprocess
import sys
import time

HOME_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))
TEST_PATH = os.path.join(HOME_PATH, 'test')
BASE_TEST_PATH = os.path.join(TEST_PATH, 'base')
CONF_TEST_PATH = os.path.join(TEST_PATH, 'conference/apiTest')
P2P_TEST_PATH = os.path.join(TEST_PATH, 'p2p/apiTest')
DEPS_PATH = os.path.join(HOME_PATH, 'dependencies')

CONF_TARGET_PACKAGE = 'oms.test.conference.apitest'
P2P_TARGET_PACKAGE = 'oms.test.p2p.apitest'
BASE_TARGET_PACKAGE = 'oms.test.base'

TEST_MODULES = ["':test:util'", "':test:base'", "':test:p2p:util'", "':test:p2p:apiTest'",
                "':test:conference:util'", "':test:conference:apiTest'"]
MODULES_LIST = ['base', 'conference', 'p2p']

LOGCAT_SUFFIX = str(int(time.time())) + '.log'


def analyse_result(result_file):
    start = False
    result_line = ""
    with open(result_file, 'r') as f:
        for line in f:
            if line.find('Test results for InstrumentationTestRunner') == 0:
                start = True
            elif line.find('Time:') == 0:
                break
            if start:
                result_line = result_line + line
    print result_line
    return (result_line != "" and result_line.find('F') == -1)


def run_cases(module, mode, log_dir, device):
    print '\n> running cases on device', device
    result_file = os.path.join(log_dir, module + '-result-' + LOGCAT_SUFFIX)
    logcat_file = os.path.join(log_dir, module + '-logcat-' + LOGCAT_SUFFIX)
    adb = ['adb'] if device == None else ['adb', '-s', device]
    if module == 'base':
        target_package = BASE_TARGET_PACKAGE
    elif module == 'conference':
        target_package = CONF_TARGET_PACKAGE
    elif module == 'p2p':
        target_package = P2P_TARGET_PACKAGE
    clean_logcat = ['logcat', '-c']
    subprocess.call(adb + clean_logcat)
    am_cmd = ['shell', 'am', 'instrument', '-w', '-e', 'debug', 'false', '-e',
              'size', mode, target_package + '.test/android.test.InstrumentationTestRunner']
    with open(result_file, 'a+') as rf:
        subprocess.call(adb + am_cmd, stdout=rf)
    logcat_cmd = ['logcat', '-d', target_package]
    with open(logcat_file, 'a+') as lf:
        subprocess.call(adb + logcat_cmd, stdout=lf)
    print '> done.'
    print '  Result file: <LOG_DIR>/' + module + '-result-' + LOGCAT_SUFFIX
    print '  Log file: <LOG_DIR>/' + module + '-logcat-' + LOGCAT_SUFFIX
    return analyse_result(result_file)


def install_test(module, device):
    print '\n> building and installing test module', module
    if module == 'base':
        test_path = BASE_TEST_PATH
    elif module == 'conference':
        test_path = CONF_TEST_PATH
    elif module == 'p2p':
        test_path = P2P_TEST_PATH
    cmd = [HOME_PATH + '/gradlew', '-q', 'assembleDebug']
    subprocess.call(cmd, cwd=test_path)
    cmd = [HOME_PATH + '/gradlew', '-q', 'assembleDebugAndroidTest']
    subprocess.call(cmd, cwd=test_path)

    cmd = 'ANDROID_SERIAL=' + device + ' ' + HOME_PATH + '/gradlew' + ' -q '
    subprocess.call(cmd + 'uninstallAll', cwd=test_path, shell=True)
    subprocess.call(cmd + 'installDebug', cwd=test_path, shell=True)
    subprocess.call(cmd + 'installDebugAndroidTest', cwd=test_path, shell=True)
    print '> done.'


def run_test(mode, log_dir, device):
    result = True
    for module in MODULES_LIST:
        install_test(module, device)
        result = run_cases(module, mode, log_dir, device) and result
    return result


def change_config():
    shutil.copyfile(os.path.join(HOME_PATH, 'settings.gradle'),
                    os.path.join(HOME_PATH, 'settings.gradle.bk'))
    modules_included = ''
    for module in TEST_MODULES:
        modules_included += '\ninclude ' + module
    with open(os.path.join(HOME_PATH, 'settings.gradle'), 'a') as settings_file:
        settings_file.write(modules_included)


def recover_config():
    shutil.move(os.path.join(HOME_PATH, 'settings.gradle.bk'),
                os.path.join(HOME_PATH, 'settings.gradle'))


def build_libs():
    print '> building sdk libraries...'
    cmd = ['python', HOME_PATH + '/tools/pack.py', '--skip-zip']
    if subprocess.call(cmd):
        sys.exit(1)


def copy_libs():
    print '> copying libs to dependency dirs...'
    testLibs = os.path.join(HOME_PATH, 'test/libs')
    if os.path.exists(testLibs):
        shutil.rmtree(testLibs)
    shutil.copytree(os.path.join(HOME_PATH, 'dist/libs'), testLibs)
    shutil.move(os.path.join(testLibs, 'webrtc/libwebrtc.jar'), testLibs)
    print '> done.'


def recover_deps():
    shutil.rmtree(os.path.join(DEPS_PATH, 'libwebrtc'))
    cmd = ['mv', os.path.join(DEPS_PATH, 'libwebrtc.bk'), os.path.join(DEPS_PATH, 'libwebrtc')]
    subprocess.call(cmd)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run android instrumentation tests.')
    parser.add_argument('--build-deps', dest='build', action='store_true', default=False,
                        help='Indicates if to build sdk libraries.')
    parser.add_argument("--mode", dest="mode", required=True, choices=('small', 'large', 'medium'),
                        help="Indicate which kind of test cases will be tested.")
    parser.add_argument('--device', dest='device',
                        help='Id of the android device on which the test will run.'
                             'If there are multiple devices on the test host machine,'
                             'please indicate the device using this parameter.')
    parser.add_argument('--log-dir', dest='log_dir', default=TEST_PATH,
                        help='Location of the directory where logs for this test will output to.')

    args = parser.parse_args()

    # generate sdk libraries.
    if args.build:
        build_libs()
        copy_libs()

    # change settings.gradle to include test modules.
    change_config()

    result = run_test(args.mode, args.log_dir, args.device)

    # recover the settings.gradle
    recover_config()

    # TODO: remove this
    recover_deps()

    # collect test results
    sys.exit(not result)
