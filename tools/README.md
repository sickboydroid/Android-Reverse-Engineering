# Scripts

This folder contains some utility scripts which prove to be very helpful while reverse engineering


## java2smali

Requires:
- d8 (download from Google)
- baksmali (download from Google)

Convert your java files directly into smali.

This tool comes handy when you quickly want to inject code but you don't want to write the lengthy smali lines.
Your java code can use android api as well.

NOTE: Please modify the script to use correct android.jar if you want to use android api

## adbwifi

Say goodbye to wires

This script lets you connect your device with adb over wifi. You can transfer files, install apps, do debugging and lots of other stuff as well.

Steps to setup:
1. Connect your android phone with your machine via USB cable (for first time)
2. Turn your phones **hotspot** on and connect your machines **wifi**
3. Open your terminal and simply execute this script
4. If there are no errors, simple unplug your device and now you can use adb without connecting your device

NOTE: The adb will work over wifi as long as you don't disconnect your wifi/hotspot.

## axml

Requires: xml2axml/xml2axml-1.1.0.jar

It helps you to convert compiled resources back to xml.

For example if you unzip any apk you will find **AndroidManifest.xml**. If you try to open this file in text editor you will see that it is binary.
Now, use `axml d AndroidManifest.xml Out.xml` command to decode binary into xml.
Once done you can easily modify **Out.xml**. After that you can use `axml e Out.xml AndroidManifest.xml` to encode xml back to binary.
What is cool about this tool is that now you can simply zip back the AndroidManifest.xml and it will work fine.

## java2smali

Requires:
- d8
- baksmali

Convert your java files directly into smali.

This tool comes handy when you quickly want to inject code but you don't want to write the lengthy smali lines.
Your java code can use android api as well.

NOTE: Please modify the script to use correct android.jar if you want to use android api
