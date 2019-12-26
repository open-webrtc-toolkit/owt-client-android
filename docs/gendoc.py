#!/usr/bin/python


'''
Intel License
'''

import argparse
import os
import subprocess
import shutil

'''
For generating API document, this will copy java files located in webrtc stack to android client sdk repo
'''

THIS = os.path.dirname(os.path.abspath(__file__))
STACK_ANDROID_PATH='src/talk/ics/sdk/base/android/java/com/intel/webrtc/base'
STACK_AUDIO_PATH='src/third_party/webrtc/modules/audio_device/android/java/src/com/intel/webrtc/base'
ANDROID_REPO_PATH=os.path.join(THIS, '../src/sdk/base/src/main/java/com/intel/webrtc/base/')
DOXYGEN_PATH = os.path.join(THIS, 'doxygen')

files = ['FilterCallback.java', 'VideoFrameFilterInterface.java']
audio_files = ['IcsAudioRecord.java']

FOUND_STACK = False

def cleanEnv():
    os.chdir(os.path.join(THIS, '..'))
    cmd = ['./gradlew', 'clean']
    subprocess.call(cmd)

    if os.path.exists(os.path.join(THIS, 'html')):
        shutil.rmtree(os.path.join(THIS, 'html'))

def copyFiles(path):
    if not os.path.exists(os.path.join(path, STACK_ANDROID_PATH)):
        print 'Wrong location for webrtc stack.'
        return

    global FOUND_STACK
    FOUND_STACK = True
    for f in files:
        cmd = ['cp', os.path.join(path, STACK_ANDROID_PATH, f), os.path.join(ANDROID_REPO_PATH, f)]
        subprocess.call(cmd)
    for f in audio_files:
        cmd = ['cp', os.path.join(path, STACK_AUDIO_PATH, f), os.path.join(ANDROID_REPO_PATH, f)]
        subprocess.call(cmd)

def rmFiles():
    for f in files:
        cmd = ['rm', os.path.join(ANDROID_REPO_PATH, f)]
        subprocess.call(cmd)
    for f in audio_files:
        cmd = ['rm', os.path.join(ANDROID_REPO_PATH, f)]
        subprocess.call(cmd)

def genDoc():
    os.chdir(DOXYGEN_PATH)
    cmd = ['doxygen', 'doxygen_android.conf']
    subprocess.call(cmd)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-stack_location', default='.', help='webrtc stack location')
    args = parser.parse_args()
    cleanEnv()
    copyFiles(args.stack_location)
    genDoc()
    rmFiles()
    if not FOUND_STACK:
        print '\n ------------------------------'
        print ' | No webrtc stack found      |'
        print ' | Please use -stack_location |'
        print ' | to set webrtc stack path   |'
        print ' ------------------------------\n'
