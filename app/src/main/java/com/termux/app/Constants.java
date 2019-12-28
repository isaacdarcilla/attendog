package com.termux.app;

import android.annotation.SuppressLint;

public class Constants {
    /* API endpoints */
    public static final String VERIFY_URL = "http://localhost:3000/api/verify";
    public static final String TYPES_URL = "http://localhost:3000/api/types";
    public static final String SECTIONS_URL = "http://localhost:3000/api/sections";
    public static final String SUBJECTS_URL = "http://localhost:3000/api/subjects";
    /* Assets Files */
    public static final String AARCH64_ZIP = "/mnt/sdcard/.attendo/packages/php/arm64-v8a.zip";
    public static final String ZIP_DEST = "/data/data/com.termux/files/home/packages/";
    public static final String X86_64_ZIP = "/mnt/sdcard/.attendo/packages/php/x86_64.zip";
    public static final String X86_ZIP = "/mnt/sdcard/.attendo/packages/php/x86.zip";
    /* CPU Architecture*/
    public static final String AARCH64 = "arm64-v8a";
    public static final String X86_64 = "x86_64";
    public static final String I386 = "x86";
    public static final String ARM = "armeabi-v7a";
    /* Directories */
    @SuppressLint("SdCardPath")
    public static final String TERMUX_FAILSAFE_SESSION_ACTION = "com.termux.app.failsafe_session";
    @SuppressLint("SdCardPath")
    public static final String DESTINATION_ASSETS = "/mnt/sdcard/.attendo/packages/";
    public static final  String ASSETS_PATH = "php";
    @SuppressLint("SdCardPath")
    public static final String WWW_ASSETS = "/mnt/sdcard/.attendo/";
    public static final  String WWW_PATH = "www";
    public static final  String WWW_PHP_PATH = "/mnt/sdcard/.attendo/www";
    public static final  String WWW_PHP_ZIP = "/mnt/sdcard/.attendo/www/www.zip";
    @SuppressLint("SdCardPath")
    public static final String HOME_LOCATION = "/data/data/com.termux/files/home/";
    @SuppressLint("SdCardPath")
    public static final String PACKAGE_LOCATION = "/data/data/com.termux/files/home/packages";
    @SuppressLint("SdCardPath")
    public static final String ATTENDO_PATH = "/mnt/sdcard/.attendo/";
    @SuppressLint("SdCardPath")
    public static final String ATTENDO_PACKAGE_PATH = "/mnt/sdcard/.attendo/packages/";
    @SuppressLint("SdCardPath")
    public static final String ATTENDO_PHP_PATH = "/mnt/sdcard/.attendo/packages/php/";
    @SuppressLint("SdCardPath")
    public static final String DOC_ROOT = "/mnt/sdcard/.attendo/www/";
    /* Command */
    public static final String SERVER_SHELL = "/data/data/com.termux/files/usr/bin/sh";
    public static final String SERVER_START = "cd /data/data/com.termux/files/home/packages/php-bin ; ./php -S 0.0.0.0:3000 -t /mnt/sdcard/.attendo/www";
    public static final String MAKE_DIR_COMMAND = "/data/data/com.termux/files/usr/bin/mkdir";
    public static final String CHANGE_MOD = "chmod 777 -R /data/data/com.termux/files/home";
    public static final String COPY_LIB = "cd /data/data/com.termux/files/home/packages/libs ; cp * /data/data/com.termux/files/usr/lib";
    public static final String COPY_ASSETS = "cd /mnt/sdcard/.attendo/packages/php/www ; cp * /mnt/sdcard/.attendo/www";
}

