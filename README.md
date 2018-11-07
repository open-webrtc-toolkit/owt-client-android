# Open Media Streamer Android SDK

## Prerequisites

* Configure Android SDK

    If local.properties file with the following line doesn't exist in project root directory,
    please create local.properties file and add a line

    >`sdk.dir=/your/android/sdk/location`

    Otherwise, the environment variables ```ANDROID_HOME``` can be used instead of local.properties file.

    >`export ANDROID_HOME=/your/android/sdk/location`

* Configure dependency files

    This project relies on webrtc, by default dependencies in all submodules link to the files in /dependencies,
    which is left as empty files. So before building the project, webrtc library files need to be put into this
    folder.

    Getting information on how to build libwebrtc library, please refer to [oms-nativesdk](https://github.com/intel-webrtc/oms-nativesdk).

## Build Project

* To build the entire project including sdk and samples
    > ./gradlew assembleRelease

* To build the specific module, e.g. base
    > ./gradlew assembleRelease -p src/sdk/base

* To pack a whole release package
    > python tools/pack.py
