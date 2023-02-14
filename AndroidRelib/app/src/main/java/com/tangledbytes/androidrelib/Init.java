package com.tangledbytes.androidrelib;

import android.content.Context;

import com.bosphere.filelogger.FL;
import com.bosphere.filelogger.FLConfig;
import com.bosphere.filelogger.FLConst;

/**
 * invoke-static {p0}, Lcom/tangledbytes/androidrelib/Init;->init(Landroid/content/Context;)V
 * */
public class Init {
    public static final String TAG = Init.class.getSimpleName();

    public static void init(Context context) {
        initLog(context);
        if (Constants.SHOW_TEST_LOG) FL.d("Test Log");
        setExceptionHandler();
    }

    private static void setExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            FL.e("Something wrong happened in thread=" + paramThread, paramThrowable);
            System.exit(2);
        });
    }

    private static void initLog(Context context) {
        FL.init(new FLConfig.Builder(context)
                .defaultTag(Constants.DEFAULT_TAG)
                .minLevel(FLConst.Level.V)
                .logToFile(true)
                .dir(Constants.LOG_DIR)
                .retentionPolicy(FLConst.RetentionPolicy.FILE_COUNT)
                .maxFileCount(FLConst.DEFAULT_MAX_FILE_COUNT)
                .maxTotalSize(FLConst.DEFAULT_MAX_TOTAL_SIZE)
                .build());
        FL.setEnabled(Constants.LOG_ENABLED);
        FL.setEnabled(true);
    }
}
