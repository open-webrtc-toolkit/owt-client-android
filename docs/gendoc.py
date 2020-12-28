#!/usr/bin/python


'''
Intel License
'''

import os
import subprocess
import shutil

'''
For generating API document, this will copy java files located in webrtc stack to android client sdk repo
'''

THIS = os.path.dirname(os.path.abspath(__file__))
DOXYGEN_PATH = os.path.join(THIS, 'doxygen')


def cleanEnv():
    os.chdir(os.path.join(THIS, '..'))
    cmd = ['./gradlew', 'clean']
    subprocess.call(cmd)

    if os.path.exists(os.path.join(THIS, 'html')):
        shutil.rmtree(os.path.join(THIS, 'html'))


def genDoc():
    os.chdir(DOXYGEN_PATH)
    cmd = ['doxygen', 'doxygen_android.conf']
    subprocess.call(cmd)


if __name__ == '__main__':
    cleanEnv()
    genDoc()