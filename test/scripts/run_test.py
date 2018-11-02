import argparse
import json
import os
import shutil
import subprocess
import sys
import time

HOME_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))
TEST_PATH = os.path.join(HOME_PATH, 'test')
BASE_TEST_PATH = os.path.join(TEST_PATH, "base")
CONF_TEST_PATH = os.path.join(TEST_PATH, 'conference/apiTest')
P2P_TEST_PATH = os.path.join(TEST_PATH, 'p2p/apiTest')

CONF_TARGET_PACKAGE = "com.intel.webrtc.test.conference.apitest"
P2P_TARGET_PACKAGE = "com.intel.webrtc.test.p2p.apitest"
BASE_TARGET_PACKAGE = "com.intel.webrtc.test.base"


def install_test(module, device):
    print '> building test module', module
    if module == 'base':
        test_path = BASE_TEST_PATH
    elif module == 'conference':
        test_path = CONF_TEST_PATH
    else:
        test_path = P2P_TEST_PATH
    os.chdir(test_path)
    cmd = [HOME_PATH + '/gradlew', '-q', 'assembleDebug']
    subprocess.call(cmd)
    cmd = [HOME_PATH + '/gradlew', '-q', 'assembleDebugAndroidTest']
    subprocess.call(cmd)
    print '> done.'
    print '> installing test module', module, 'on device:', device

    os.chdir(test_path)
    cmd = [HOME_PATH + '/gradlew', '-q', 'installDebug']
    subprocess.call(cmd)
    cmd = [HOME_PATH + '/gradlew', '-q', 'installDebugAndroidTest']
    subprocess.call(cmd)
    print '> done.'


def analyse_result():
    # TODO: analyse the test result and return true if all cases succeed.
    return True


def prepare_test(module, device):
    # TODO: complete preparation works here
    # TODO: 1. copy source code from sdk; 2. start log.
    if module == 'p2p':
        print 'need to prepare some files for p2p.'

    install_test(module, device)


def run_cases(case_list, device, log_dir):
    # load test cases.
    # [ {'module': '<module>', 'cases': ['case']} ]
    with open(case_list, 'r') as case_file:
        objs = json.loads(case_file.read())

    for obj in objs:
        # specific works for modules.
        prepare_test(obj['module'], device)

        log_name = obj['module'] + '-test-result-' + str(int(time.time())) + '.log'
        for case in obj['cases']:
            adb = ['adb'] if device == None else ['adb', '-s', device]
            if obj['module'] == 'base':
                target_package = BASE_TARGET_PACKAGE
            elif obj['module'] == 'conference':
                target_package = CONF_TARGET_PACKAGE
            else:
                target_package = P2P_TARGET_PACKAGE
            shell_cmd = ['shell', 'am', 'instrument', '-w', '-r', '-e debug false', '-e class',
                         target_package + '.' + case,
                         target_package + '.test/android.test.InstrumentationTestRunner']

            print '> running cases on device', device
            result_log = os.path.join(log_dir, log_name)
            with open(result_log, 'a') as log_file:
                subprocess.call(adb + shell_cmd, stdout=log_file)
            print '> done.'


def change_config():
    shutil.copyfile(os.path.join(HOME_PATH, 'settings.gradle'),
                    os.path.join(HOME_PATH, 'settings.gradle.bk'))
    with open(os.path.join(HOME_PATH, 'settings.gradle'), "a") as settings_file:
        settings_file.write("\ninclude ':test:util',"
                            "':test:base',"
                            "':test:p2p:util', ':test:p2p:apiTest',"
                            "':test:conference:util', ':test:conference:apiTest'")


def recover_config():
    shutil.move(os.path.join(HOME_PATH, 'settings.gradle.bk'),
                os.path.join(HOME_PATH, 'settings.gradle'))


def build_libs():
    print '> building sdk libraries...'
    cmd = ['python', HOME_PATH + '/tools/pack.py', '--skip_zip']
    if not subprocess.call(cmd):
        sys.exit(1)
    print '> done.'


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
    if not os.path.exists(args.caselist):
        print 'No case list file found:', args.caselist
        return False

    # check the format of case list file.
    try:
        with open(args.caselist, 'r') as case_file:
            json.load(case_file)
    except ValueError as e:
        print 'Failed to load json:', e
        return False
    return True


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Run android instrumentation tests.')
    parser.add_argument("--build-deps", dest="build", action="store_true", default=False,
                        help="Indicates if to build sdk libraries.")
    parser.add_argument("--caselist", dest="caselist",
                        default=os.path.join(TEST_PATH, 'case_list.json'),
                        help="Location of the case list json file.")
    parser.add_argument("--device", dest="device",
                        help="Id of the android device on which the test will run."
                             "If there are multiple devices on the test host machine,"
                             "please indicate the device using this parameter.")
    parser.add_argument("--log-dir", dest="log_dir", default=TEST_PATH,
                        help="Location of the directory where logs for this test will output to.")

    args = parser.parse_args()

    if not validate_caselist(args.caselist):
        sys.exit(1)

    # generate sdk libraries.
    if args.build:
        build_libs()
        copy_libs()

    # change settings.gradle to include test modules.
    change_config()

    run_cases(args.caselist, args.device, args.log_dir)

    # recover the settings.gradle
    recover_config()

    # collect test results
    sys.exit(analyse_result())
