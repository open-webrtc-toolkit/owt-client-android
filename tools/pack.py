#!/usr/bin/python

import re
import os
import sys
import getopt
import shutil
import commands
import subprocess
import zipfile
from xml.dom import minidom

#path variables
HOME_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
DEPS_PATH = os.path.join(HOME_PATH, 'dependencies')
CODE_PATH = os.path.join(HOME_PATH, 'src')
SDK_PATH = os.path.join(CODE_PATH, 'sdk')
SAMPLE_PATH = os.path.join(CODE_PATH, 'sample')
ICS_DEBUG_PATH = os.path.join(SDK_PATH, 'base/src/main/java/oms/base')

#distribution path
DIST_PATH = os.path.join(HOME_PATH, 'dist')
DIST_LIB_PATH = os.path.join(DIST_PATH, 'libs')
DIST_SAMPLE_PATH = os.path.join(DIST_PATH, 'samples')
DIST_SAMPLE_CODE_PATH = os.path.join(DIST_SAMPLE_PATH, 'src/sample')
DIST_APK_PATH = os.path.join(DIST_PATH, 'apks')

#gradle wrapper
GRADLEW = HOME_PATH + '/gradlew'

#nexus maven artifact info
SERVER_URL = 'http://webrtc-checkin.sh.intel.com:60000/nexus/content/repositories'
REPOSITORY = 'thirdparty'
GROUP_ID = 'woogeen'
ARTIFACT_URL =  SERVER_URL + '/' + REPOSITORY + '/' + GROUP_ID + '/'

#times to try downloading
DOWNLOAD_TRIAL = 3

def printusage():
       usage = '''Usage: python3 pack.py [OPTION]
     Packing Android SDK to dist/, and create a zip packages for SDK.
     dist/ will be removed if you run this script without --skip_clean.

     Options:
       --skip_samples     Do not include samples
       --skip_zip         Do not generate zip file
       --skip_clean       Do not clean
       --help, --usage    Display this help and exit.'''
       print(usage)

def clean():
    print '\n> cleaning environment.'
    os.chdir(HOME_PATH)
    cmd = [GRADLEW, '-q', 'clean']
    subprocess.call(cmd)

    if os.path.exists(DIST_PATH):
        shutil.rmtree(DIST_PATH)
    print '> done.'

def releaseVariable():
    #back up
    shutil.copy2(os.path.join(ICS_DEBUG_PATH, 'CheckCondition.java'),
                 os.path.join(ICS_DEBUG_PATH, 'CheckCondition.java.bk'))

    #change dependencies to release version
    with open(os.path.join(ICS_DEBUG_PATH, 'CheckCondition.java'), 'r+') as replace_file:
        file_content = replace_file.read()
        file_content = file_content.replace('ICS_DEBUG = true;',
                                            'ICS_DEBUG = false;')
    with open(os.path.join(ICS_DEBUG_PATH, 'CheckCondition.java'), 'w') as replace_file:
        replace_file.write(file_content)

def recoverVariable():
    #recover
    os.rename(os.path.join(ICS_DEBUG_PATH, 'CheckCondition.java.bk'),
              os.path.join(ICS_DEBUG_PATH, 'CheckCondition.java'))

def packSdk(sdk):
    print '\n> packing ics_' + sdk.lower() + '.aar'
    os.chdir(os.path.join(SDK_PATH, sdk))
    cmd = [GRADLEW, '-q', 'assembleRelease']
    subprocess.call(cmd)

    if not os.path.exists(DIST_LIB_PATH):
        os.makedirs(DIST_LIB_PATH)

    #copy jar files to dist/libs
    shutil.copy2(os.path.join(SDK_PATH, sdk, 'build/outputs/aar', sdk + '-release.aar'),
                 os.path.join(DIST_LIB_PATH, 'ics_' + sdk.lower() + '.aar'))
    print '> done.'

def packSample(sample):
    print '\n> packing ics_' + sample.lower() + '.apk'
    os.chdir(os.path.join(SAMPLE_PATH, sample))

    #back up build.gradle
    shutil.copy2(os.path.join(SAMPLE_PATH, 'utils', 'build.gradle'),
                 os.path.join(SAMPLE_PATH, 'utils', 'build.gradle.bk'))

    #change dependencies to release version
    with open(os.path.join(SAMPLE_PATH, 'utils', 'build.gradle'), 'r+') as replace_file:
        file_content = replace_file.read()
        file_content = file_content.replace('libjingle-so-debug',
                                            'libjingle-so-release')
    with open(os.path.join(SAMPLE_PATH, 'utils', 'build.gradle'), 'w') as replace_file:
        replace_file.write(file_content)

    cmd = [GRADLEW, '-q', 'assembleDebug']
    subprocess.call(cmd)

    if not os.path.exists(DIST_APK_PATH):
        os.makedirs(DIST_APK_PATH)

    #copy apk files to dist/apks
    shutil.copy2(os.path.join(SAMPLE_PATH, sample, 'build/outputs/apk/debug/' + sample + '-debug.apk'),
                 os.path.join(DIST_APK_PATH, 'ics_' + sample + '.apk'))

    #recover build.gradle
    os.rename(os.path.join(SAMPLE_PATH, 'utils', 'build.gradle.bk'),
                 os.path.join(SAMPLE_PATH, 'utils', 'build.gradle'))
    print '> done.'

def copyDistFile():
    print '\n> copying distribution files'
    #README.md
    shutil.copy2(os.path.join(SAMPLE_PATH, 'README.md'), os.path.join(DIST_SAMPLE_PATH, 'README.md'))

    #top-level files
    shutil.copytree(os.path.join(HOME_PATH, 'gradle'), os.path.join(DIST_SAMPLE_PATH, 'gradle'))
    shutil.copy2(os.path.join(HOME_PATH, 'gradlew'), os.path.join(DIST_SAMPLE_PATH, 'gradlew'))
    shutil.copy2(os.path.join(HOME_PATH, 'gradlew.bat'), os.path.join(DIST_SAMPLE_PATH, 'gradlew.bat'))
    shutil.copy2(os.path.join(HOME_PATH, 'build.gradle'), os.path.join(DIST_SAMPLE_PATH, 'build.gradle'))
    #setting.gradle
    setting = open(os.path.join(DIST_SAMPLE_PATH, 'settings.gradle'), 'w')
    setting.writelines(['include \':src:sample:p2p\'\n',
                        'include \':src:sample:conference\'\n',
                        'include \':src:sample:utils\'\n'])
    #build.gradle
    with open(os.path.join(DIST_SAMPLE_PATH, 'build.gradle'), 'r+') as replace_file:
        file_content = replace_file.read()
        pattern = re.compile('maven \{[^\{\}]+\}')
        file_content = re.sub(pattern, '', file_content)
    with open(os.path.join(DIST_SAMPLE_PATH, 'build.gradle'), 'w') as replace_file:
        replace_file.write(file_content)
    print '> done.'

def packSampleSource(sample):
    print '\n> packing sample source code'
    print '         changing the dependencies in ' + sample
    #back up build.gradle
    shutil.copy2(os.path.join(SAMPLE_PATH, sample, 'build.gradle'),
                 os.path.join(SAMPLE_PATH, sample, 'build.gradle.bk'))

    #change the dependencies in build.gradle
    with open(os.path.join(SAMPLE_PATH, sample, 'build.gradle'), 'r+') as replace_file:
        file_content = replace_file.read()
        file_content = file_content.replace('api project(\':src:sdk:base\')', '')
        file_content = file_content.replace('implementation project(\':src:sdk:' + sample + '\')', '')
        file_content = file_content.replace('api \'woogeen:libjingle-so-debug:\' + rootProject.ext.stackVersion',
                                            '')
    with open(os.path.join(SAMPLE_PATH, sample, 'build.gradle'), 'w') as replace_file:
        replace_file.write(file_content)

    print '         copying ' + sample + 'source code to dist'
    if not os.path.exists(DIST_SAMPLE_PATH):
        os.makedirs(DIST_SAMPLE_PATH)

    #copy source code
    if not os.path.exists(os.path.join(DIST_SAMPLE_CODE_PATH, sample)):
        os.makedirs(os.path.join(DIST_SAMPLE_CODE_PATH, sample))
    shutil.copytree(os.path.join(SAMPLE_PATH, sample, 'src'),
                    os.path.join(DIST_SAMPLE_CODE_PATH, sample, 'src'),
                    ignore = shutil.ignore_patterns('*.jar', '*.so'))
    shutil.copy2(os.path.join(SAMPLE_PATH, sample, 'build.gradle'), os.path.join(DIST_SAMPLE_CODE_PATH, sample, 'build.gradle'))

    print '         recovering build.gradle'
    #recover build.gradle
    os.rename(os.path.join(SAMPLE_PATH, sample, 'build.gradle.bk'),
              os.path.join(SAMPLE_PATH, sample, 'build.gradle'))
    print '> done.'

# download artifact
def downloadArtifacts():
    #retrieve the stack version
    with open(os.path.join(HOME_PATH, 'build.gradle'), 'r+') as replace_file:
        file_content = replace_file.read()
        version = re.findall(r'stackVersion = \"([\.\d]+)\"', file_content)[0]

    #download .so dependency
    downloadArtifact('libjingle-so-release', version)
    cmd = ['jar', 'xvf', 'libjingle-so-release' + '-' + version + '.jar']
    subprocess.call(cmd)
    cmd = ['mv', 'lib', 'webrtc']
    subprocess.call(cmd)
    os.remove('libjingle-so-release' + '-' + version + '.jar')

    #download .jar dependency
    downloadArtifact('libjingle-jar-release', version)
    print 'libjingle-jar-release' + '-' + version + '.jar'
    cmd = ['cp', 'libjingle-jar-release' + '-' + version + '.jar', 'webrtc/libwebrtc.jar']
    subprocess.call(cmd)
    os.remove('libjingle-jar-release' + '-' + version + '.jar')

def downloadArtifact(name, version):
    jar_name = name + '-' + version + '.jar'
    print 'downloading ' + jar_name
    counter = 0
    while counter < DOWNLOAD_TRIAL:
        cmd = 'wget ' + ARTIFACT_URL + name + '/' + version + '/' + jar_name
        (status, output) = commands.getstatusoutput(cmd)
        if 'saved' in output:
            print 'succeeded in downloading artifact.'
            break
        else:
            counter += 1

    if counter == DOWNLOAD_TRIAL:
        print 'failed to download artifact. Please check nexus maven server.\n'
        sys.exit()

def copyDepFile():
    print '\n> coping dependency files to dist'
    if not os.path.exists(DIST_LIB_PATH):
        os.makedirs(DIST_LIB_PATH)
    #ThirdPartyLicenses.txt
    shutil.copy2(os.path.join(HOME_PATH, 'ThirdpartyLicenses.txt'),
                 os.path.join(DIST_PATH, 'ThirdpartyLicenses.txt'))
    #libjingle
    downloadArtifacts()
    shutil.copytree(os.path.join(HOME_PATH, 'webrtc'),
                    os.path.join(DIST_LIB_PATH, 'webrtc'))
    shutil.rmtree('webrtc')
    print '> done.'

def zipRelease():
    print '\n> zipping up dist'
    zip_process = subprocess.Popen(['zip', '-q', '-ry', 'android-sdk.zip', 'samples', 'libs', 'apks', 'ThirdpartyLicenses.txt'],
                                   cwd = DIST_PATH)
    zip_process.wait()
    print '> done.'

def checkVersion(what, version):
    global ANDROID_SDK_LOCATION
    cmd = 'ls ' + ANDROID_SDK_LOCATION + '/' + what
    result = os.popen(cmd)
    found = False
    for line in result:
        if(line.find(version) != -1):
            found = True
            break
    if not found:
        print 'Android ' + what + version + ' not installed.'
        sys.exit()

def checkEnv():
    android_home_set = False
    android_dir_set = False

    android_home = None
    if not os.getenv('ANDROID_HOME') is None:
        android_home_set = True
        android_home = os.getenv('ANDROID_HOME')

    android_sdk_dir = None
    if os.path.exists(os.path.join(HOME_PATH, 'local.properties')):
        with open(os.path.join(HOME_PATH, 'local.properties'), 'r') as localProp:
            lines = localProp.readlines()
            for line in lines:
                if line.find('sdk.dir') == 0:
                    android_sdk_dir = line[line.find('=')+1 : line.find('\n')]
                    if android_sdk_dir != '':
                        android_dir_set = True

    if android_home_set and android_dir_set\
        and android_home != android_sdk_dir:
        print 'Ambiguous Android SDK location.'
        print '$ANDROID_HOME: ' + android_home
        print 'sdk.dir in local.properties: ' + android_sdk_dir
        print 'Please make sure variables above have the same value or unset one of them.'
        sys.exit()

    if not android_home_set and not android_dir_set:
        print 'Please set up android sdk location.'
        print 'You can use one of the ways below to set up android sdk location:'
        print '  1. export ANDROID_HOME=/path/to/android/sdk'
        print '  2. create `local.properties` file in project root folder'
        print '     and add `sdk.dir=/path/to/android/sdk` in it'
        print 'If you use both options above to set up the android sdk location, '\
                'please make sure they have the same value.'
        sys.exit()

    global ANDROID_SDK_LOCATION
    if android_home_set:
        ANDROID_SDK_LOCATION = android_home
    else:
        ANDROID_SDK_LOCATION = android_sdk_dir

    print '\nAndroid SDK location: ' + ANDROID_SDK_LOCATION

def runLint():
    cmd = [GRADLEW, '-p', SDK_PATH, 'lint']
    subprocess.call(cmd)

    has_error = False
    names=['base','conference','p2p']
    for name in names:
        report_root = minidom.parse(SDK_PATH+'/'+name+'/build/reports/lint-results.xml').documentElement
        issue_nodes = report_root.getElementsByTagName('issue') if report_root else []
        for node in issue_nodes:
            severity = node.getAttribute('severity') if node else ''
            if severity == 'Warning' or severity == 'Error' or severity == 'Fatal':
                print '\nThere are some errors in ' + SDK_PATH + '/' + name + ','
                print 'Please go to ' + SDK_PATH + '/' + name \
                       + '/build/reports/lint-results.html to get more information.'
                has_error = True
                break

    if has_error:
        sys.exit()

if __name__ == '__main__':
    runLint()

    checkEnv()

    #required android sdk version
    BUILD_TOOLS_VERSION = '28.0.0'
    SDK_VERSION = '26'

    print '\nDefault build_tools version is ' + BUILD_TOOLS_VERSION + ', sdk version is ' + SDK_VERSION
    print 'If you don\'t have this version installed or you would like to use different versions'
    print 'please modify the root build.gradle and change the version variables in pack.py'
    print 'at line 317 and line 318.'

    #handle arguments
    FLAG_SKIP_SAMPLES = False
    FLAG_SKIP_ZIP = False
    FLAG_SKIP_CLEAN = False
    args = getopt.getopt(sys.argv[1:], [], ['skip_samples', 'skip_zip', 'skip_clean', 'help'])[0]
    for arg in args:
        if arg[0] == '--skip_samples':
            print '\n '
            FLAG_SKIP_SAMPLES = True
        elif arg[0] == '--skip_zip':
            FLAG_SKIP_ZIP = True
        elif arg[0] == '--skip_clean':
            FLAG_SKIP_CLEAN = True
        else:
            printusage()
            sys.exit()

    #check build-tools version
    checkVersion('build-tools', BUILD_TOOLS_VERSION)

    #check platforms version
    checkVersion('platforms', SDK_VERSION)

    #clean the environment
    if FLAG_SKIP_CLEAN == False:
        clean()

    #copy dependencies
    copyDepFile()

    #ICS_DEBUG false
    releaseVariable()

    #compile sdk jars and copy to /dist
    for sdk in ['base', 'conference', 'p2p']:
        packSdk(sdk)

    #compile sample apks and source codes
    if FLAG_SKIP_SAMPLES == False:
        for sample in ['conference', 'p2p']:
            packSample(sample)
            packSampleSource(sample)
        packSampleSource('utils')

        #copy project files necessary for sample codes
        copyDistFile()

    #zip up
    if FLAG_SKIP_ZIP == False:
        zipRelease()

    recoverVariable()
