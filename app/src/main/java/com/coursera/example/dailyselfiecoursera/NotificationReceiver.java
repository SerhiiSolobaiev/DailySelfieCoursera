package com.coursera.example.dailyselfiecoursera;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class NotificationReceiver extends BroadcastReceiver{

    private static final int MY_NOTIFICATION_ID = 1;
    private static final String TAG = "TagNotificationReceiver";

    private Intent mNotificationIntent;
    private PendingIntent mContentIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG,"in onreceive method");
        // The Intent to be used when the user clicks on the Notification View
        mNotificationIntent = new Intent(context, MainActivity.class);

        // The PendingIntent that wraps the underlying Intent
        mContentIntent = PendingIntent.getActivity(context, 0,
                mNotificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Build the Notification
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.Builder notificationBuilder = new Notification.Builder(context)
                //.setTicker("Time for pic")
                .setSmallIcon(R.drawable.camera)
                .setAutoCancel(true)
                .setContentTitle("Daily selfie!")
                .setContentText("Time for another selfie")
                .setContentIntent(mContentIntent)
                .setSound(alarmSound);


        Notification notification = notificationBuilder.getNotification();
        //notification = notificationBuilder.build();
        // Get the NotificationManager
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        // Pass the Notification to the NotificationManager:
        mNotificationManager.notify(MY_NOTIFICATION_ID,
                notification);
    }
}
