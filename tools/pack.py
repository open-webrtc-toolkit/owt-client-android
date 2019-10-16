import argparse
import os
import shutil
import subprocess
import sys
import zipfile
from xml.dom import minidom
import platform
import re

# path variables
HOME_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
DEPS_PATH = os.path.join(HOME_PATH, 'dependencies')
CODE_PATH = os.path.join(HOME_PATH, 'src')
SDK_PATH = os.path.join(CODE_PATH, 'sdk')
SAMPLE_PATH = os.path.join(CODE_PATH, 'sample')
OWT_DEBUG_PATH = os.path.join(SDK_PATH, 'base', 'src', 'main', 'java', 'owt', 'base')

# distribution path
DIST_PATH = os.path.join(HOME_PATH, 'dist')
DIST_LIB_PATH = os.path.join(DIST_PATH, 'libs')
DIST_SAMPLE_PATH = os.path.join(DIST_PATH, 'samples')
DIST_SAMPLE_CODE_PATH = os.path.join(DIST_SAMPLE_PATH, 'src', 'sample')
DIST_APK_PATH = os.path.join(DIST_PATH, 'apks')

# gradle wrapper
GRADLEW_EXECUTABLE = 'gradlew.bat' if platform.system() == 'Windows' else 'gradlew'
GRADLEW_PATH = os.path.join(HOME_PATH, GRADLEW_EXECUTABLE)


def recover_variable():
    # recover
    backup_path = os.path.join(OWT_DEBUG_PATH, 'CheckCondition.java.bk')
    origin_path = os.path.join(OWT_DEBUG_PATH, 'CheckCondition.java')
    if os.path.exists(origin_path):
        os.remove(origin_path)
    os.rename(backup_path, origin_path)


def zip_package(package_name):
    print('\n> zipping up dist')
    current_path = os.getcwd()
    os.chdir(DIST_PATH)
    with zipfile.ZipFile(package_name, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, _, filenames in os.walk(os.getcwd()):
            for name in filenames:
                if name != package_name:
                    zf.write(os.path.relpath(os.path.join(root, name)))
    os.chdir(current_path)
    print('> done.')


def copy_build_file():
    print('\n> copying distribution files')
    # README.md
    shutil.copy2(os.path.join(SAMPLE_PATH, 'README.md'),
                 os.path.join(DIST_SAMPLE_PATH, 'README.md'))

    # top-level files
    shutil.copytree(os.path.join(HOME_PATH, 'gradle'), os.path.join(DIST_SAMPLE_PATH, 'gradle'))
    shutil.copy2(os.path.join(HOME_PATH, 'gradlew'), os.path.join(DIST_SAMPLE_PATH, 'gradlew'))
    shutil.copy2(os.path.join(HOME_PATH, 'gradlew.bat'),
                 os.path.join(DIST_SAMPLE_PATH, 'gradlew.bat'))
    shutil.copy2(os.path.join(HOME_PATH, 'build.gradle'),
                 os.path.join(DIST_SAMPLE_PATH, 'build.gradle'))
    # setting.gradle
    setting = open(os.path.join(DIST_SAMPLE_PATH, 'settings.gradle'), 'w')
    setting.writelines(['include \':src:sample:p2p\'\n',
                        'include \':src:sample:conference\'\n',
                        'include \':src:sample:utils\'\n'])

    print('> done.')


def pack_sample_source(sample):
    print('\n> packing sample source code')

    # copy source code
    if not os.path.exists(os.path.join(DIST_SAMPLE_CODE_PATH, sample)):
        os.makedirs(os.path.join(DIST_SAMPLE_CODE_PATH, sample))
    shutil.copytree(os.path.join(SAMPLE_PATH, sample, 'src'),
                    os.path.join(DIST_SAMPLE_CODE_PATH, sample, 'src'),
                    ignore=shutil.ignore_patterns('*.jar', '*.so'))
    shutil.copy2(os.path.join(SAMPLE_PATH, sample, 'build.gradle'),
                 os.path.join(DIST_SAMPLE_CODE_PATH, sample, 'build.gradle'))

    source_data = ""
    with open(os.path.join(DIST_SAMPLE_CODE_PATH, sample, 'build.gradle'), 'r+') as f:
        for line in f:
            source_data = source_data + line

    if sample == 'conference':
        source_data = re.sub('\s+implementation project\(\':src:sdk:conference\'\)', '', source_data)
    elif sample == 'p2p':
        source_data = re.sub('\s+implementation project\(\':src:sdk:p2p\'\)', '', source_data)
    elif sample == 'utils':
        source_data = re.sub('dependencies \{.*?\}', 'dependencies {\n}', source_data, flags=re.DOTALL)

    with open(os.path.join(DIST_SAMPLE_CODE_PATH, sample, 'build.gradle'), 'w') as wf:
        wf.write(source_data)
    print('> done.')


def pack_sample(sample):
    print('\n> packing owt_' + sample.lower() + '.apk')
    os.chdir(os.path.join(SAMPLE_PATH, sample))
    cmd = [GRADLEW_PATH, '-q', 'assembleDebug']
    if subprocess.call(cmd):
        print('\nFailed to build', sample, 'sample.')
        sys.exit(1)

    if not os.path.exists(DIST_APK_PATH):
        os.makedirs(DIST_APK_PATH)

    # copy apk files to dist/apks
    shutil.copy2(
        os.path.join(SAMPLE_PATH, sample, 'build/outputs/apk/debug/' + sample + '-debug.apk'),
        os.path.join(DIST_APK_PATH, 'owt_' + sample + '.apk'))

    print('> done.')


def pack_sdk(sdk):
    print('\n> packing owt_' + sdk.lower() + '.aar')
    os.chdir(os.path.join(SDK_PATH, sdk))
    cmd = [GRADLEW_PATH, '-q', 'assembleRelease']
    if subprocess.call(cmd):
        print('\nFailed to build', sdk, 'sdk.')
        sys.exit(1)

    if not os.path.exists(DIST_LIB_PATH):
        os.makedirs(DIST_LIB_PATH)

    # copy jar files to dist/libs
    shutil.copy2(os.path.join(SDK_PATH, sdk, 'build/outputs/aar', sdk + '-release.aar'),
                 os.path.join(DIST_LIB_PATH, 'owt_' + sdk.lower() + '.aar'))
    print('> done.')


def release_variable():
    # back up
    shutil.copy2(os.path.join(OWT_DEBUG_PATH, 'CheckCondition.java'),
                 os.path.join(OWT_DEBUG_PATH, 'CheckCondition.java.bk'))

    # change dependencies to release version
    with open(os.path.join(OWT_DEBUG_PATH, 'CheckCondition.java'), 'r+') as replace_file:
        file_content = replace_file.read()
        file_content = file_content.replace('OWT_DEBUG = true;',
                                            'OWT_DEBUG = false;')
    with open(os.path.join(OWT_DEBUG_PATH, 'CheckCondition.java'), 'w') as replace_file:
        replace_file.write(file_content)


def copy_deps():
    print('\n> coping dependency files to dist')
    if not os.path.exists(DIST_LIB_PATH):
        os.makedirs(DIST_LIB_PATH)
    # ThirdPartyLicenses.txt
    shutil.copy2(os.path.join(HOME_PATH, 'ThirdpartyLicenses.txt'),
                 os.path.join(DIST_PATH, 'ThirdpartyLicenses.txt'))
    # libjingle
    shutil.copytree(os.path.join(DEPS_PATH, 'libwebrtc'),
                    os.path.join(DIST_LIB_PATH, 'webrtc'))
    print('> done.')


def run_lint():
    cmd = [GRADLEW_PATH, '-p', SDK_PATH, 'lint']
    subprocess.call(cmd)

    has_error = False
    names = ['base', 'conference', 'p2p']
    for name in names:
        report_root = minidom.parse(
            SDK_PATH + '/' + name + '/build/reports/lint-results.xml').documentElement
        issue_nodes = report_root.getElementsByTagName('issue') if report_root else []
        for node in issue_nodes:
            severity = node.getAttribute('severity') if node else ''
            if severity == 'Warning' or severity == 'Error' or severity == 'Fatal':
                print('\nThere are some errors in ' + SDK_PATH + '/' + name + ',')
                print('Please go to ' + SDK_PATH + '/' + name \
                      + '/build/reports/lint-results.html to get more information.')
                has_error = True
                break

    if has_error:
        sys.exit(1)


def clean():
    print('\n> cleaning environment.')
    os.chdir(HOME_PATH)
    cmd = [GRADLEW_PATH, '-q', 'clean']
    subprocess.call(cmd)

    if os.path.exists(DIST_PATH):
        shutil.rmtree(DIST_PATH)
    print('> done.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Pack release package for android sdk.')
    parser.add_argument("--skip-lint", dest="skip_lint", action="store_true", default=False,
                        help="Indicates if to skip running lint.")
    parser.add_argument("--skip-samples", dest="skip_samples", action="store_true", default=False,
                        help="Indicates if to skip building and packing the samples.")
    parser.add_argument("--skip-zip", dest="skip_zip", action="store_true", default=False,
                        help="Indicates if to skip zipping up the package.")
    parser.add_argument("--package-name", dest="package_name", default="android-sdk.zip",
                        help="Set the release package name.")

    args = parser.parse_args()

    # clean environment
    clean()

    if not args.skip_lint:
        run_lint()

    # copy libwebrtc libraries into dist
    copy_deps()

    # OWT_DEBUG false
    release_variable()

    # compile sdk jars and copy to /dist

    for sdk in ['base', 'conference', 'p2p']:
        pack_sdk(sdk)

    # compile sample apks and source codes
    if not args.skip_samples:
        for sample in ['conference', 'p2p']:
            pack_sample(sample)
            pack_sample_source(sample)
        pack_sample_source('utils')

        # copy project files necessary for sample codes
        copy_build_file()

    # zip up
    if not args.skip_zip:
        zip_package(args.package_name)

    recover_variable()
