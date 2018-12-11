import argparse
import json
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
DEPS_PATH = os.path.join(HOME_PATH, 'dependencies/libwebrtc')

CONF_TARGET_PACKAGE = 'oms.test.conference.apitest'
P2P_TARGET_PACKAGE = 'oms.test.p2p.apitest'
BASE_TARGET_PACKAGE = 'oms.test.base'

TEST_MODULES = ["':test:util'", "':test:base'", "':test:p2p:util'", "':test:p2p:apiTest'",
                "':test:conference:util'", "':test:conference:apiTest'"]

LOGCAT_SUFFIX = str(int(time.time())) + '.log'


def analyse_result(result):
    # Return numbers of succeed cases
    ok_num = 0
    with open(result, 'r') as f:
        for line in f:
            if 'OK (1 test)' in line:
                ok_num += 1
    return ok_num


def run_cases(module, cases, log_dir, device):
    print '\n> running cases on device', device
    result_file = os.path.join(log_dir, module + '-result-' + LOGCAT_SUFFIX)
    logcat_file = os.path.join(log_dir, module + '-logcat-' + LOGCAT_SUFFIX)
    for case in cases:
        adb = ['adb'] if device == None else ['adb', '-s', device]
        if module == 'base':
            target_package = BASE_TARGET_PACKAGE
        elif module == 'conference':
            target_package = CONF_TARGET_PACKAGE
        elif module == 'p2p':
            target_package = P2P_TARGET_PACKAGE
        clean_logcat = ['logcat', '-c']
        subprocess.call(adb + clean_logcat)
        am_cmd = ['shell', 'am', 'instrument', '-w', '-r', '-e', 'debug', 'false', '-e',
                  'class', target_package + '.' + case,
                  target_package + '.test/android.test.InstrumentationTestRunner']
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

    cmd = [HOME_PATH + '/gradlew', '-q', 'uninstallAll']
    subprocess.call(cmd, cwd=test_path)
    cmd = [HOME_PATH + '/gradlew', '-q', 'installDebug']
    subprocess.call(cmd, cwd=test_path)
    cmd = [HOME_PATH + '/gradlew', '-q', 'installDebugAndroidTest']
    subprocess.call(cmd, cwd=test_path)
    print '> done.'


def run_test(case_list, log_dir, device):
    # load test cases.
    # [ {'module': '<module>', 'cases': ['case']} ]
    with open(case_list, 'r') as case_file:
        objs = json.loads(case_file.read())

    result = True
    for obj in objs:
        install_test(obj['module'], device)
        succeed = run_cases(obj['module'], obj['cases'], log_dir, device)
        total = len(obj['cases'])
        result = result and (succeed == total)
        print '\n>', obj['module'] + ' result: All:', total, \
            'Succeed:', succeed, 'Failed:', total - succeed

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


def build_libs(dependencies_dir):
    print '> building sdk libraries...'
    if os.path.exists(DEPS_PATH):
        shutil.rmtree(DEPS_PATH)
    shutil.copytree(dependencies_dir, DEPS_PATH)
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


def validate_caselist(case_list):
    # check the existence of case list file.
    if not os.path.exists(case_list):
        print 'No case list file found:', case_list
        return False

    # check the format of case list file.
    try:
        with open(case_list, 'r') as case_file:
            json.load(case_file)
    except ValueError as e:
        print 'Failed to load json:', e
        return False
    return True


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run android instrumentation tests.')
    parser.add_argument('--build-deps', dest='build', action='store_true', default=False,
                        help='Indicates if to build sdk libraries.')
    parser.add_argument('--caselist', dest='caselist',
                        default=os.path.join(TEST_PATH, 'case_list.json'),
                        help='Location of the case list json file.')
    parser.add_argument('--device', dest='device',
                        help='Id of the android device on which the test will run.'
                             'If there are multiple devices on the test host machine,'
                             'please indicate the device using this parameter.')
    parser.add_argument('--log-dir', dest='log_dir', default=TEST_PATH,
                        help='Location of the directory where logs for this test will output to.')
    parser.add_argument('--dependencies-dir', dest='dependencies_dir', required=True,
                        help='Location of the dependency libraries.')

    args = parser.parse_args()

    if not validate_caselist(args.caselist):
        sys.exit(1)

    # generate sdk libraries.
    if args.build:
        build_libs(args.dependencies_dir)
        copy_libs()

    # change settings.gradle to include test modules.
    change_config()

    result = run_test(args.caselist, args.log_dir, args.device)

    # recover the settings.gradle
    recover_config()

    # collect test results
    sys.exit(not result)
