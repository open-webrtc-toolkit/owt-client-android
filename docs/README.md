# Documentation

## 1. gendoc.sh

Description: Run this script to generate doxygen online document. This script will clean your project first, then generate document. Finally, it will output the document in a package named 'html' and open 'index.html' in that package to access the index.
Example Usage: ./gendoc.sh

Note: before running this script, you should install doxygen first.
See instruction at https://www.stack.nl/~dimitri/doxygen/manual/install.html
For Ubuntu, you can directly run the following commands to install doxygen under version 1.8.6.
    sudo apt-get update
    sudo apt-get install doxygen
You may need to configure ANDROID_HOME=your/android/sdk/path variable in `gendoc.sh`

## 2. index.md

Description: The index markdown file for android sdk.

## 3. doxygen

This package contains the doxygen related files.

- content_doxygen: style file of doxygen.
- doxygen_android.conf: the configure file to generate android document.
- oms_logo.png: log used when generate the document.

