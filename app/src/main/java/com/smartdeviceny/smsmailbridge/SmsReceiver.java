package com.smartdeviceny.smsmailbridge;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.smartdeviceny.smsmailbridge.misc.Constants;


public class SmsReceiver extends BroadcastReceiver {
    private String TAG = SmsReceiver.class.getSimpleName();

    public SmsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Activity.MODE_PRIVATE);
        Bundle myBundle = intent.getExtras();
        SmsMessage[] messages = null;
        String originatingAddress = "";
        String strMessage = "";
        String recvTimeStamp = "";
        String accountName = sharedPreferences.getString(Constants.ACCOUNT_NAME, "");

        if (myBundle != null) {
            Object[] pdus = (Object[]) myBundle.get("pdus");

            messages = new SmsMessage[pdus.length];

            for (int i = 0; i < messages.length; i++) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    String format = myBundle.getString("format");
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                } else {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }
                if (originatingAddress.equals("")) {
                    originatingAddress = "From: " + messages[i].getOriginatingAddress() ;
                    if(!accountName.isEmpty()) {
                        originatingAddress+= "\nGoogle Account: " + accountName + "\n";
                    }
                }
                if (recvTimeStamp.isEmpty()) {
                    recvTimeStamp = "RecTime: " + messages[i].getTimestampMillis() + "\n";
                    recvTimeStamp += "ServiceCenterAddress: " + messages[i].getServiceCenterAddress() + "\n";

                }
                //strMessage += messages[i].getStatus() + "\n";
                //strMessage += messages[i].getProtocolIdentifier() + "\n";


                strMessage += messages[i].getMessageBody();

            }
            try {
                TelephonyManager tMgr = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) {
                    String mPhoneNumber = tMgr.getLine1Number();
                    //String simSerialNumber = tMgr.getSimSerialNumber();
                    recvTimeStamp += "Recv Phone Number: " + mPhoneNumber + "\n";
                    //recvTimeStamp += "Recv SIM Number: " + simSerialNumber + "\n";
                }
            } catch (Exception e ) {
                e.printStackTrace();
            }

            Log.d("SMS", strMessage);

            Intent serviceIntent = new Intent(context, SendMailIntentService.class);
            serviceIntent.putExtra(Constants.ACCOUNT_NAME, accountName);
            serviceIntent.putExtra(Constants.EXTRA_SUBJECT, context.getString(R.string.mail_subject) + " " + originatingAddress);
            String header = originatingAddress + "\n" + recvTimeStamp;
            serviceIntent.putExtra(Constants.EXTRA_BODY, header + "\n--------- Message Body ---------\n" + strMessage);

            //NotificationUtils.notify_user(context.getApplicationContext(), Constants.NOTIFICATION_CHANNEL, Constants.NOTIFICATION_GROUP_MAIN, "SMS Bridge", originatingAddress + " " + strMessage, 5);
            context.startService(serviceIntent);
        }
    }
}
