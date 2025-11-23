package com.example.bismillahberdetak.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;

import com.example.bismillahberdetak.R;
import com.example.bismillahberdetak.activities.MainActivity;
import com.example.bismillahberdetak.models.Reading;

public class NotificationHelper {

    private static final String CHANNEL_ID = "measurement_channel";
    private static final String CHANNEL_NAME = "Measurement Notifications";
    private static final int NOTIFICATION_ID_COMPLETE = 1001;
    private static final int NOTIFICATION_ID_ERROR = 1002;

    private Context context;
    private NotificationManager notificationManager;
    private Vibrator vibrator;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        createNotificationChannel();
    }

    /**
     * Create notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notif_channel_desc));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Show notification when measurement is complete
     */
    public void showMeasurementCompleteNotification(Reading reading) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        String contentText = String.format(
                context.getString(R.string.notif_measurement_complete_desc),
                reading.getHeartRate(),
                reading.getSpo2()
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_heart)
                .setContentTitle(context.getString(R.string.notif_measurement_complete))
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        notificationManager.notify(NOTIFICATION_ID_COMPLETE, builder.build());

        // Vibrate
        vibrateSuccess();
    }

    /**
     * Show notification for measurement error
     */
    public void showMeasurementErrorNotification(String errorMessage) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_error)
                .setContentTitle(context.getString(R.string.notif_measurement_error))
                .setContentText(errorMessage)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        notificationManager.notify(NOTIFICATION_ID_ERROR, builder.build());

        // Vibrate error pattern
        vibrateError();
    }

    /**
     * Vibrate for successful measurement
     */
    public void vibrateSuccess() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        }
    }

    /**
     * Vibrate for error
     */
    public void vibrateError() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long[] pattern = {0, 100, 100, 100, 100, 100};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                long[] pattern = {0, 100, 100, 100, 100, 100};
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    /**
     * Vibrate for button click
     */
    public void vibrateClick() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    /**
     * Cancel all notifications
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }
}