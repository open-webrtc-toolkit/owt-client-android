# Open WebRTC Toolkit Client SDK for Android

Open WebRTC Toolkit Client SDK for Android builds on top of the W3C standard WebRTC APIs to accelerate development of real-time communications (RTC) for Android applications, including peer-to-peer, broadcasting, and conference mode communications.

# Documentation

See [README.md](docs/README.md) in *docs* directory.

# How to build
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

    Getting information on how to build libwebrtc library, please refer to [owt-nativesdk](https://github.com/open-webrtc-toolkit/owt-client-native).

## Build Project

* To build the entire project including sdk and samples
    > ./gradlew assembleRelease

* To build the specific module, e.g. base
    > ./gradlew assembleRelease -p src/sdk/base

* To pack a whole release package
    > python tools/pack.py

# How to contribute

We warmly welcome community contributions to owt-client-android repository. If you are willing to contribute your features and ideas to OWT, follow the process below:

* Make sure your patch will not break anything, including all the build and tests
* Submit a pull request onto [Pull Requests](https://github.com/open-webrtc-toolkit/owt-client-android/pulls)
* Watch your patch for review comments if any, until it is accepted and merged

OWT project is licensed under Apache License, Version 2.0. By contributing to the project, you agree to the license and copyright terms therein and release your contributions under these terms.

# How to report issues

Use the "Issues" tab on Github

# See Also

http://webrtc.intel.com
