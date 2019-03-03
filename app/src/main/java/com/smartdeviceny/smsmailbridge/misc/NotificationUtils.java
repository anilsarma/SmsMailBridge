package com.smartdeviceny.smsmailbridge.misc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.smartdeviceny.smsmailbridge.MainActivity;
import com.smartdeviceny.smsmailbridge.R;

public class NotificationUtils {

    private static void createNotificationChannel(Context context, String CHANNEL_ID) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static NotificationCompat.Builder makeNotificationBuilder(Context context, String channelID, String groupName, String title, String msg) {
        createNotificationChannel(context, channelID);
        //int icon = R.mipmap.ic_launcher;
        //long when = System.currentTimeMillis();
//        Notification notification = new Notification(icon, getString(R.string.app_name), when);
//        notification.flags |= Notification.FLAG_NO_CLEAR; //Do not clear the notification
//        notification.defaults |= Notification.DEFAULT_LIGHTS; // LED
//        notification.defaults |= Notification.DEFAULT_VIBRATE; //Vibration
//        notification.defaults |= Notification.DEFAULT_SOUND; // Sound

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelID).setSmallIcon(R.mipmap.ic_sms_mail_bridge).setGroup(groupName).setContentTitle(title).setVisibility(
                NotificationCompat.VISIBILITY_PUBLIC).setContentText(msg);
        return mBuilder;
    }

    public static void notify(Context context, NotificationCompat.Builder builder, @Nullable Integer id) {
        final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = builder.build();
        //notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if (id == null) {
            id = new Integer(1);
        }
        mNotificationManager.notify(id, notification);
    }

    public static void notify_user(Context context, String channelID, String groupName, String title, String msg, @Nullable Integer id, boolean launch) {
        final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = makeNotificationBuilder(context, channelID, groupName, title, msg);


        if(launch) {
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(contentIntent);
        }
        Notification notification = mBuilder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        //notification.defaults |= Notification.DEFAULT_SOUND;

        Log.d("SVC", "notification sent " + msg);
        if (id == null) {
            id = new Integer(1);
        }
        mNotificationManager.notify(id, notification);
    }

    public static void notify_user(Context context, String groupName, String channelID, String title, String msg, @Nullable Integer id) {
        notify_user(context, channelID, groupName, title, msg, id, false);
    }
}
