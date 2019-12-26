import argparse
import json
import os
import shutil
import subprocess
import sys
import time
import re
from enum import Enum
from xml.dom import minidom

HOME_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))
TEST_PATH = os.path.join(HOME_PATH, 'test')
DEPS_PATH = os.path.join(HOME_PATH, 'dependencies')

TEST_MODULES = ["':test:util'", "':test:base'", "':test:p2p:util'", "':test:p2p:apiTest'",
                "':test:conference:util'", "':test:conference:apiTest'"]

INST_TESTS = {'base': {'path': os.path.join(TEST_PATH, 'base'), 'target': 'owt.test.base'},
              'conference': {'path': os.path.join(TEST_PATH, 'conference/apiTest'),
                             'target': 'owt.test.conference.apitest'},
              'p2p': {'path': os.path.join(TEST_PATH, 'p2p/apiTest'),
                      'target': 'owt.test.p2p.apitest'}}

UNIT_TESTS = {'base': {'path': os.path.join(HOME_PATH, 'src/sdk/base'), 'target': 'owt.base'},
              'conference': {'path': os.path.join(HOME_PATH, 'src/sdk/conference'),
                             'target': 'owt.conference'},
              'p2p': {'path': os.path.join(HOME_PATH, 'src/sdk/p2p'), 'target': 'owt.p2p'}}

TIMESTAMP =  str(int(time.time()))
LOGCAT_SUFFIX = TIMESTAMP + '.log'


class TestMode(Enum):
    INSTRUMENTATION = 'instrumentation'
    UNIT = 'unit'


def exec_cmd(cmd, cmd_path, log_path):
    with open(log_path, 'a+') as f:
        proc = subprocess.Popen(cmd, cwd=cmd_path, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        for line in proc.stdout:
            if not isinstance(line, str):
                line = line.decode("utf-8")
            sys.stdout.write(line)
            f.write(line)
        proc.communicate()
    return proc.returncode


'''TODO: merge analyse_unit_test_result and analyse_instrumentation_test_result with
checking INSTRUMENTATION_STATUS_CODE of each test case.
'''
def analyse_instrumentation_test_result(result):
    # Return numbers of succeed cases
    ok_num = 0
    with open(result, 'r') as f:
        for line in f:
            if 'OK (1 test)' in line:
                ok_num += 1
    return ok_num


def analyse_unit_test_result(result):
    try:
        xml_doc = minidom.parse(result)
    except:
        print("Error occured while reading test result.")
        return 0, 0
    test_suite = xml_doc.documentElement
    total_num = int(test_suite.attributes["tests"].value)
    failed_num = int(test_suite.attributes["failures"].value)
    error_num = int(test_suite.attributes["errors"].value)
    success_num = total_num - failed_num - error_num
    return total_num, success_num


def exec_android_test(mode, cmd, device, target_package, result_file, logcat_file, cmd_path):
    if device is not None:
        os.environ['ANDROID_SERIAL'] = device
    if mode == TestMode.UNIT:
        subprocess.call([HOME_PATH + '/gradlew', '-q', 'clean'], cwd=cmd_path)
    adb = ['adb'] if device == None else ['adb', '-s', device]
    clean_logcat = ['logcat', '-c']
    subprocess.call(adb + clean_logcat)
    with open(result_file, 'a+') as rf:
        subprocess.call(cmd, cwd=cmd_path, stdout=rf ,stderr=rf)
    logcat_cmd = ['logcat', '-d', target_package]
    with open(logcat_file, 'a+') as lf:
        subprocess.call(adb + logcat_cmd, stdout=lf)


def run_cases(mode, module, target_package, log_dir, device, test_path, cases=None):
    print('\n> running ' + mode.value + ' cases on device', device)
    result_file = os.path.join(log_dir, module + '-' + mode.value + '-result-' + LOGCAT_SUFFIX)
    logcat_file = os.path.join(log_dir, module + '-' + mode.value + '-logcat-' + LOGCAT_SUFFIX)

    if mode == TestMode.INSTRUMENTATION:
        adb = ['adb'] if device == None else ['adb', '-s', device]
        for case in cases:
            am_cmd = ['shell', 'am', 'instrument', '-w', '-r', '-e', 'debug', 'false', '-e',
                      'class', target_package + '.' + case,
                      target_package + '.test/android.test.InstrumentationTestRunner']
            exec_android_test(mode, adb + am_cmd, device, target_package, result_file, logcat_file, test_path)
    elif mode == TestMode.UNIT:
        cmd = [HOME_PATH + '/gradlew', 'connectedAndroidTest']
        exec_android_test(mode, cmd, device, target_package, result_file, logcat_file, test_path)
        xml_file_name = module + '-' + mode.value + '-result-' + TIMESTAMP + '.xml'
        dst_xml_report_path = os.path.join(log_dir, xml_file_name)
        p = re.compile(".*?XML test result file generated at (.*?\.xml)")
        with open(result_file, 'r') as f:
            for line in f:
                m = re.match(p, line)
                if m is not None:
                    src_xml_report_path = m.group(1)
                    shutil.copyfile(src_xml_report_path, dst_xml_report_path)
                    result_file = dst_xml_report_path
    print('> done.')
    print('  Result file: <LOG_DIR>/' + module + '-' + mode.value + '-result-' + LOGCAT_SUFFIX)
    print('  Log file: <LOG_DIR>/' + module + '-' + mode.value + '-logcat-' + LOGCAT_SUFFIX)
    return result_file


def install_test(test_path, device, log_dir, mode, module):
    bulid_file = os.path.join(log_dir, module + '-' + mode.value + '-build-' + LOGCAT_SUFFIX)
    if device is not None:
        os.environ['ANDROID_SERIAL'] = device
    cmd = [HOME_PATH + '/gradlew', '-q', 'assembleDebug']
    exec_cmd(cmd, test_path, bulid_file)
    cmd = [HOME_PATH + '/gradlew', '-q', 'assembleDebugAndroidTest']
    exec_cmd(cmd, test_path, bulid_file)
    cmd = [HOME_PATH + '/gradlew', '-q', 'uninstallAll']
    exec_cmd(cmd, test_path, bulid_file)
    cmd = [HOME_PATH + '/gradlew', '-q', 'installDebug']
    exec_cmd(cmd, test_path, bulid_file)
    cmd = [HOME_PATH + '/gradlew', '-q', 'installDebugAndroidTest']
    exec_cmd(cmd, test_path, bulid_file)
    print('> done.')


def run_instrumentation_test(case_list, log_dir, device):
    # load test cases.
    # [ {'module': '<module>', 'cases': ['case']} ]
    with open(case_list, 'r') as case_file:
        objs = json.loads(case_file.read())

    result = True
    prepare_instrumentation_test()
    for obj in objs:
        install_test(INST_TESTS[obj['module']]['path'], device, log_dir, TestMode.INSTRUMENTATION, obj['module'])
        result_file = run_cases(TestMode.INSTRUMENTATION, obj['module'],
                                INST_TESTS[obj['module']]['target'], log_dir,
                                device, HOME_PATH, cases=obj['cases'])
        succeed = analyse_instrumentation_test_result(result_file)
        total = len(obj['cases'])
        result = result and (succeed == total)
        print('\n>', obj['module'] + ' result: All:', total, \
            'Succeed:', succeed, 'Failed:', total - succeed)
    return result


def run_unit_test(log_dir, device):
    result = True
    for module, unit_test in UNIT_TESTS.items():
        prepare_unit_test(unit_test['path'])
        result_file = run_cases(TestMode.UNIT, module, unit_test['target'], log_dir, device, unit_test['path'])
        total, succeed = analyse_unit_test_result(result_file)
        result = result and total != 0 and (succeed == total)
        print('\n>', module + ' result: All:', total, \
            'Succeed:', succeed, 'Failed:', total - succeed)
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


def build_libs(dependencies_dir, log_dir):
    print('> building sdk libraries...')
    build_file = os.path.join(log_dir, 'build-'+ LOGCAT_SUFFIX)
    cmd = ['mv', os.path.join(DEPS_PATH, 'libwebrtc'), os.path.join(DEPS_PATH, 'libwebrtc.bk')]
    subprocess.call(cmd)
    shutil.copytree(dependencies_dir, os.path.join(DEPS_PATH, 'libwebrtc'))
    cmd = ['python', 'tools/pack.py', '--skip-zip']
    result = exec_cmd(cmd, HOME_PATH, build_file)
    if not result and os.path.exists(build_file):
        os.remove(build_file)
    return result


def prepare_instrumentation_test():
    print('> copying libs to dependency dirs...')
    testLibs = os.path.join(HOME_PATH, 'test/libs')
    if os.path.exists(testLibs):
        shutil.rmtree(testLibs)
    shutil.copytree(os.path.join(HOME_PATH, 'dist/libs'), testLibs)
    shutil.move(os.path.join(testLibs, 'webrtc/libwebrtc.jar'), testLibs)
    print('> done.')


def prepare_unit_test(unit_path):
    print('> copying libwebrtc dependencies to androidTest')
    jni_path = os.path.join(unit_path, 'src/androidTest/jniLibs')
    if os.path.exists(jni_path):
        shutil.rmtree(jni_path)
    shutil.copytree(os.path.join(DEPS_PATH, 'libwebrtc'), jni_path)
    print('> done.')


def validate_caselist(case_list):
    # check the existence of case list file.
    if not os.path.exists(case_list):
        print('No case list file found:', case_list)
        return False

    # check the format of case list file.
    try:
        with open(case_list, 'r') as case_file:
            json.load(case_file)
    except ValueError as e:
        print('Failed to load json:', e)
        return False
    return True


def recover_deps():
    libwebrtc_bk = os.path.join(DEPS_PATH, 'libwebrtc.bk')
    libwebrtc = os.path.join(DEPS_PATH, 'libwebrtc')
    if os.path.exists(libwebrtc) and os.path.exists(libwebrtc_bk):
        shutil.rmtree(libwebrtc)
    if os.path.exists(libwebrtc_bk):
        cmd = ['mv', libwebrtc_bk, libwebrtc]
        subprocess.call(cmd)


def recover_unit_test_environment():
    for module, unit_test in UNIT_TESTS.items():
        shutil.rmtree(os.path.join(unit_test['path'], 'src/androidTest/jniLibs'))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run android instrumentation tests.')
    parser.add_argument('--build-deps', dest='build', action='store_true', default=False,
                        help='Indicates if to build sdk libraries.')
    parser.add_argument('--instrumentation', dest='instrumentation', default=None,
                        help='Run instrumentation test, parameter:location of the case list json file.')
    parser.add_argument('--device', dest='device', default=None,
                        help='Id of the android device on which the test will run.'
                             'If there are multiple devices on the test host machine,'
                             'please indicate the device using this parameter.')
    parser.add_argument('--log-dir', dest='log_dir', default=TEST_PATH,
                        help='Location of the directory where logs for this test will output to.')
    parser.add_argument('--time-stamp', dest='time_stamp', default=str(int(time.time())),
                        help='Test time.')
    parser.add_argument('--dependencies-dir', dest='dependencies_dir', required=True,
                        help='Location of the dependency libraries.')
    parser.add_argument('--unit', dest='unit', action='store_true', default=False,
                        help='Run unit test.')
    args = parser.parse_args()

    TIMESTAMP = args.time_stamp
    LOGCAT_SUFFIX = TIMESTAMP + '.log'

    # clean environment before test.
    recover_deps()

    validate_result = False if args.instrumentation is None else validate_caselist(args.instrumentation)
    if not (validate_result or args.unit):
        sys.exit(1)

    # generate sdk libraries.
    if args.build and build_libs(args.dependencies_dir, args.log_dir):
        recover_deps()
        sys.exit(1)

    instrumentation_result = True
    unit_result = True
    if validate_result:
        # change settings.gradle to include test modules.
        change_config()
        instrumentation_result = run_instrumentation_test(args.instrumentation, args.log_dir,
                                                          args.device)
        # recover the settings.gradle
        recover_config()

    if args.unit:
        unit_result = run_unit_test(args.log_dir, args.device)
        recover_unit_test_environment()

    # recover deps_path
    recover_deps()

    # collect test results
    sys.exit(0 if instrumentation_result and unit_result else 1)
