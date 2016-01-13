package nl.sogeti.android.gpstracker.service.logger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.IOException;

import nl.sogeti.android.gpstracker.integration.ExternalConstants;
import nl.sogeti.android.gpstracker.service.R;
import nl.sogeti.android.gpstracker.service.db.GPStracking;
import nl.sogeti.android.log.Log;

/**
 * Manages the different notification task needed when running the logger mService
 */
public class LoggerNotification {
    private static final int ID_DISABLED = R.string.service_connectiondisabled;
    private static final int ID_STATUS = R.string.service_gpsstatus;
    private static final int ID_GPS_PROBLEM = R.string.service_gpsproblem;
    private static final int SMALL_ICON = R.drawable.ic_maps_indicator_current_position;
    private final Service mService;

    int mSatellites = 0;

    private NotificationManager mNoticationManager;
    private boolean isShowingDisabled = false;

    public LoggerNotification(Service service) {
        mService = service;
        mNoticationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void startLogging(int mPrecision, int mLoggingState, boolean mStatusMonitor, long mTrackId) {
        mNoticationManager.cancel(ID_STATUS);

        Notification notification = buildLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);
        mService.startForeground(ID_STATUS, notification);
    }

    public void updateLogging(int mPrecision, int mLoggingState, boolean mStatusMonitor, long mTrackId) {
        Notification notification = buildLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);
        mNoticationManager.notify(ID_STATUS, notification);
    }

    public void stopLogging() {
        mService.stopForeground(true);
    }

    private Notification buildLogging(int precision, int state, boolean monitor, long trackId) {
        Resources resources = mService.getResources();
        CharSequence contentTitle = resources.getString(R.string.service_title);
        String precisionText = resources.getStringArray(R.array.precision_choices)[precision];
        String stateText = resources.getStringArray(R.array.state_choices)[state - 1];
        CharSequence contentText;
        switch (precision) {
            case (ExternalConstants.LOGGING_GLOBAL):
                contentText = resources.getString(R.string.service_networkstatus, stateText, precisionText);
                break;
            default:
                if (monitor) {
                    contentText = resources.getString(R.string.service_gpsstatus, stateText, precisionText,
                            mSatellites);
                } else {
                    contentText = resources.getString(R.string.service_gpsnostatus, stateText, precisionText);
                }
                break;
        }
        Uri uri = ContentUris.withAppendedId(GPStracking.Tracks.CONTENT_URI, trackId);
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW, uri);
        PendingIntent contentIntent = PendingIntent.getActivity(mService, 0, notificationIntent, 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mService)
                        .setSmallIcon(SMALL_ICON)
                        .setContentTitle(contentTitle)
                        .setContentText(contentText)
                        .setContentIntent(contentIntent)
                        .setOngoing(true);
        PendingIntent pendingIntent;
        if (state == ExternalConstants.STATE_LOGGING) {
            CharSequence pause = resources.getString(R.string.logcontrol_pause);
            Intent intent = new Intent(mService, GPSLoggerService.class);
            intent.putExtra(ExternalConstants.Commands.COMMAND, ExternalConstants.Commands.EXTRA_COMMAND_PAUSE);
            pendingIntent = PendingIntent.getService(mService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_pause_24dp, pause, pendingIntent);
        } else if (state == ExternalConstants.STATE_PAUSED) {
            CharSequence resume = resources.getString(R.string.logcontrol_resume);
            Intent intent = new Intent(mService, GPSLoggerService.class);
            intent.putExtra(ExternalConstants.Commands.COMMAND, ExternalConstants.Commands.EXTRA_COMMAND_RESUME);
            pendingIntent = PendingIntent.getService(mService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_play_arrow_24dp, resume, pendingIntent);
        }

        return builder.build();
    }

    void startPoorSignal(long trackId) {
        Resources resources = mService.getResources();
        CharSequence contentText = resources.getString(R.string.service_gpsproblem);
        CharSequence contentTitle = resources.getString(R.string.service_title);

        Uri uri = ContentUris.withAppendedId(GPStracking.Tracks.CONTENT_URI, trackId);
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW, uri);
        PendingIntent contentIntent = PendingIntent.getActivity(mService, 0, notificationIntent, 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mService)
                        .setSmallIcon(SMALL_ICON)
                        .setContentTitle(contentTitle)
                        .setContentText(contentText)
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true);

        mNoticationManager.notify(ID_GPS_PROBLEM, builder.build());
    }

    public void stopPoorSignal() {
        mNoticationManager.cancel(ID_GPS_PROBLEM);
    }

    void startDisabledProvider(int resId, long trackId) {
        isShowingDisabled = true;

        CharSequence contentTitle = mService.getResources().getString(R.string.service_title);
        CharSequence contentText = mService.getResources().getString(resId);
        CharSequence tickerText = mService.getResources().getString(resId);

        Uri uri = ContentUris.withAppendedId(GPStracking.Tracks.CONTENT_URI, trackId);
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW, uri);
        PendingIntent contentIntent = PendingIntent.getActivity(mService, 0, notificationIntent, 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mService)
                        .setAutoCancel(true)
                        .setTicker(tickerText)
                        .setContentTitle(contentTitle)
                        .setContentText(contentText)
                        .setContentIntent(contentIntent);

        mNoticationManager.notify(
                ID_DISABLED,
                mBuilder.build());
    }

    void stopDisabledProvider(int resId) {
        mNoticationManager.cancel(ID_DISABLED);
        isShowingDisabled = false;

        CharSequence text = mService.getString(resId);
        Toast toast = Toast.makeText(mService, text, Toast.LENGTH_LONG);
        toast.show();
    }

    public boolean isShowingDisabled() {
        return isShowingDisabled;
    }

    void soundGpsSignalAlarm() {
        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alert == null) {
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        MediaPlayer mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(mService, alert);
            final AudioManager audioManager = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                mMediaPlayer.setLooping(false);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (IllegalArgumentException e) {
            Log.e(this, "Problem setting data source for mediaplayer", e);
        } catch (SecurityException e) {
            Log.e(this, "Problem setting data source for mediaplayer", e);
        } catch (IllegalStateException e) {
            Log.e(this, "Problem with mediaplayer", e);
        } catch (IOException e) {
            Log.e(this, "Problem with mediaplayer", e);
        }
    }
}
