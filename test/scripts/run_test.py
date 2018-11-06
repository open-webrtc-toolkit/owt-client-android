import argparse
import json
import shutil
import os
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

LOG_LIST = []


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
    cmd = [HOME_PATH + '/gradlew', '-q', 'uninstallAll']
    subprocess.call(cmd)
    cmd = [HOME_PATH + '/gradlew', '-q', 'installDebug']
    subprocess.call(cmd)
    cmd = [HOME_PATH + '/gradlew', '-q', 'installDebugAndroidTest']
    subprocess.call(cmd)
    print '> done.'


def replace_file(path):
    file_data = ""
    with open(path, "r") as f:
        for line in f:
            if "com.intel.webrtc.p2p.sample" in line:
                line = line.replace("com.intel.webrtc.p2p.sample", "com.intel.webrtc.test.p2p.util")
            if "MAX_RECONNECT_ATTEMPTS = 5" in line:
                line = line.replace("MAX_RECONNECT_ATTEMPTS = 5", "MAX_RECONNECT_ATTEMPTS = 1")
            if "com.intel.webrtc.sample.utils" in line:
                line = line.replace("com.intel.webrtc.sample.utils", "com.intel.webrtc.test.util")
            file_data += line
    with open(path, "w") as f:
        f.write(file_data)


def analyse_result():
    result_list = []
    for log in LOG_LIST:
        okNum = 0
        failNum = 0
        errorNum = 0
        testNum = log["testNum"]
        with open(log["path"], "a+") as f:
            for line in f:
                if "OK (1 test)" in line:
                    okNum = okNum + 1
                elif "InstrumentationTestRunner=.F" in line:
                    failNum = failNum + 1
                elif "InstrumentationTestRunner=.E" in line:
                    errorNum = errorNum + 1
            crashNum = testNum - okNum - failNum - errorNum
            resultMessage = "All: " + str(testNum) + ", OK: " + str(okNum) + ", Fail: " + str(
                failNum) + ", Error: " + str(
                errorNum) + ", Crashed: " + str(crashNum)
            f.write("Test Result:\n" + resultMessage + "\n")
            print log["module"] + " result:"
            print resultMessage
        if testNum == okNum:
            result_list.append(True)
        else:
            result_list.append(False)
    for result in result_list:
        if not result:
            return 1
    return 0


def prepare_test(module, device, log_dir, logcat_name):
    if not os.path.exists(log_dir):
        os.makedirs(log_dir)

    if module == 'p2p':
        print 'need to prepare some files for p2p.'
        target_package = BASE_TARGET_PACKAGE
        dst_signalingChannel_path = os.path.join(HOME_PATH,
                                                 "test/p2p/util/src/main/java/com/intel/webrtc/test/p2p/util/SocketSignalingChannel.java")
        shutil.copyfile(os.path.join(HOME_PATH,
                                     "src/sample/p2p/src/main/java/com/intel/webrtc/p2p/sample/SocketSignalingChannel.java"),
                        dst_signalingChannel_path)
        replace_file(dst_signalingChannel_path)
    elif module == 'conference':
        target_package = CONF_TARGET_PACKAGE
    elif module == 'base':
        target_package = BASE_TARGET_PACKAGE
    dst_icsFileVideoCapturer_path = os.path.join(HOME_PATH,
                                                 "test/util/src/main/java/com/intel/webrtc/test/util/IcsFileVideoCapturer.java")
    shutil.copyfile(os.path.join(HOME_PATH,
                                 "src/sample/utils/src/main/java/com/intel/webrtc/sample/utils/IcsFileVideoCapturer.java"),
                    dst_icsFileVideoCapturer_path)
    replace_file(dst_icsFileVideoCapturer_path)
    install_test(module, device)
    result_logcat = os.path.join(log_dir, logcat_name)
    adb = ['adb'] if device == None else ['adb', '-s', device]
    clean_cmd = ['logcat', '-c']
    record_cmd = ['logcat', target_package]
    subprocess.call(adb + clean_cmd)
    with open(result_logcat, 'a') as log_file:
        return subprocess.Popen(adb + record_cmd, stdout=log_file)


def recover_test():
    dst_signalingChannel_path = os.path.join(HOME_PATH,
                                            "test/p2p/util/src/main/java/com/intel/webrtc/test/p2p/util/SocketSignalingChannel.java")
    if os.path.exists(dst_signalingChannel_path):
        os.remove(dst_signalingChannel_path)

    dst_icsFileVideoCapturer_path = os.path.join(HOME_PATH,
                                                 "test/util/src/main/java/com/intel/webrtc/test/util/IcsFileVideoCapturer.java")
    if os.path.exists(dst_icsFileVideoCapturer_path):
        os.remove(dst_icsFileVideoCapturer_path)


def run_cases(case_list, device, log_dir):
    # load test cases.
    # [ {'module': '<module>', 'cases': ['case']} ]
    with open(case_list, 'r') as case_file:
        objs = json.loads(case_file.read())

    for obj in objs:
        # specific works for modules.
        time_stamp = str(int(time.time()))
        log_name = obj['module'] + '-test-result-' + time_stamp + '.log'
        logcat_name = obj['module'] + '-test-result-' + time_stamp + '-logcat.log'
        logcat_pid = prepare_test(obj['module'], device, log_dir, logcat_name)
        result_log = os.path.join(log_dir, log_name)
        LOG_LIST.append({"path": result_log, "testNum": len(obj['cases']), "module": obj['module']})
        for case in obj['cases']:
            adb = ['adb'] if device == None else ['adb', '-s', device]
            if obj['module'] == 'base':
                target_package = BASE_TARGET_PACKAGE
            elif obj['module'] == 'conference':
                target_package = CONF_TARGET_PACKAGE
            else:
                target_package = P2P_TARGET_PACKAGE
            shell_cmd = ['shell', 'am', 'instrument', '-w', '-r', '-e', 'debug', 'false', '-e',
                         'class', target_package + '.' + case,
                         target_package + '.test/android.test.InstrumentationTestRunner']
            print '> running cases on device', device
            with open(result_log, 'a') as log_file:
                subprocess.call(adb + shell_cmd, stdout=log_file)
            print '> done.'
        print 'Logcat path', os.path.abspath(result_log)
        logcat_pid.kill()


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
    subprocess.call(cmd)
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
    parser = argparse.ArgumentParser()
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

    #recover test script
    recover_test()

    # collect test results
    sys.exit(analyse_result())
