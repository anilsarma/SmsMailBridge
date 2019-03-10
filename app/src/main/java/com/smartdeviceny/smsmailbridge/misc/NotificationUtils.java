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

    private static void createNotificationChannel(Context context, NotificationChannels channelID) {
        // Create the NotificationChannels, but only on API 26+ because
        // the NotificationChannels class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID.getUniqueID(), channelID.getName(), channelID.getImportance());
            channel.setDescription(channelID.getDescription());
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private static NotificationCompat.Builder createNotificationGroup(Context context, NotificationGroup group) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, group.getChannel().getUniqueID());
        mBuilder.setSmallIcon(R.mipmap.ic_sms_mail_bridge);
        mBuilder.setGroup(group.getUniqueID());
        mBuilder.setContentTitle(group.getName()).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mBuilder.setContentText(group.getDescription());
        mBuilder.setGroupSummary(true);
        return mBuilder;
    }



    public static NotificationCompat.Builder makeNotificationBuilder(Context context, NotificationGroup group, String title, String msg) {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, group.getChannel().getUniqueID());
        mBuilder.setSmallIcon(R.mipmap.ic_sms_mail_bridge);
        mBuilder.setGroup(group.getUniqueID());
        mBuilder.setContentTitle(title).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mBuilder.setContentText(msg);
        return mBuilder;
    }




    public static void notify_user(Context context, NotificationGroup group, String msg, @Nullable Integer id) {
        createNotificationChannel(context, group.getChannel());

        final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = makeNotificationBuilder(context, group,  group.getDescription(), msg);
        mBuilder.setAutoCancel(true);
        Notification notification = mBuilder.build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |=Notification.DEFAULT_SOUND;

        Log.d("SVC", "notification sent " + msg);
        if (id == null) {
            id = group.getID() + 1;
        }
        mNotificationManager.notify(group.getID(), createNotificationGroup(context, group).build());
        mNotificationManager.notify(id, notification);
    }

    public static void notify_user_big_text(Context context, NotificationGroup group, String msg, @Nullable Integer id, boolean launch) {
        createNotificationChannel(context, group.getChannel());

        final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = makeNotificationBuilder(context, group,  group.getDescription(), msg);
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(msg));
        mBuilder.setAutoCancel(true);
        Notification notification = mBuilder.build();
        if(launch) {
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(contentIntent);
        }

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |=Notification.DEFAULT_SOUND;

        Log.d("SVC", "notification sent " + msg);
        if (id == null) {
            id = group.getID() + 1;
        }
        //mNotificationManager.notify(id, notification);
        mNotificationManager.notify(group.getID(), createNotificationGroup(context, group).build());
        mNotificationManager.notify(id, notification);
    }
}
