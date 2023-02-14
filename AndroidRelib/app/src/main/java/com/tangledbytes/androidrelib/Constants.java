package com.tangledbytes.androidrelib;

import android.os.Environment;

import java.io.File;
import java.text.FieldPosition;

public class Constants {
    public static final String PACKAGE_NAME = "package_name";
    public static final String DEFAULT_TAG = "def_tag";
    public static final File LOG_DIR = new File(Environment.getExternalStorageDirectory(), "Android/data/"+PACKAGE_NAME+"/");
    public static final boolean LOG_ENABLED = true;
    public static final boolean SHOW_TEST_LOG = true;
}
