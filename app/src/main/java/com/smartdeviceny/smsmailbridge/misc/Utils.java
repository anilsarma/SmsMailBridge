package com.smartdeviceny.smsmailbridge.misc;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.widget.EditText;

/**
 * Created by Mushtaq on 16-06-2016.
 */
public class Utils {

    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_ACCOUNT_PICKER_NO_MAIL = 1001;
    public static final int REQUEST_AUTHORIZATION = 1002;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1003;
    public static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1004;
    public static final int REQUEST_PERMISSION_SEND_SMS = 1005;
    public static final int REQUEST_PERMISSION_RECV_SMS = 1006;
    public static final  int REQUEST_PERMISSION_READ_PHONE_NUMBERS = 1007;

    public static boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().length() <=0;
    }

    @NonNull
    public static String getString(EditText editText) {
        return editText.getText().toString().trim();
    }

    public static boolean checkPermission(Context context, String permission) {
        if (isMarshmallow()) {
            int result = ContextCompat.checkSelfPermission(context, permission);
            return result == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    public static boolean isMarshmallow() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

}
