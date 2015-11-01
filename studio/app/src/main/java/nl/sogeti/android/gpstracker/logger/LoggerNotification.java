package nl.sogeti.android.gpstracker.logger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;

/**
 * Manages the different notification task needed when running the logger mService
 */
public class LoggerNotification
{
   private static final int ID_DISABLED = R.string.service_connectiondisabled;
   private static final int ID_STATUS = R.layout.map;
   private static final int ID_GPS_PROBLEM = R.string.service_gpsproblem;
   private static final int SMALL_ICON = R.drawable.ic_maps_indicator_current_position;
   private final Service mService;

   int mSatellites = 0;

   private NotificationManager mNoticationManager;
   private boolean isShowingDisabled = false;

   LoggerNotification(Service service)
   {
      mService = service;
      mNoticationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
   }

   void startLogging(int mPrecision, int mLoggingState, boolean mStatusMonitor, long mTrackId)
   {
      mNoticationManager.cancel(ID_STATUS);

      Notification notification = buildLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);
      if (Build.VERSION.SDK_INT >= 5)
      {
         mService.startForeground(ID_STATUS, notification);
      }
      else
      {
         mNoticationManager.notify(ID_STATUS, notification);
      }
   }

   void updateLogging(int mPrecision, int mLoggingState, boolean mStatusMonitor, long mTrackId)
   {
      Notification notification = buildLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);
      mNoticationManager.notify(ID_STATUS, notification);
   }

   void stopLogging()
   {
      if (Build.VERSION.SDK_INT >= 5)
      {
         mService.stopForeground(true);
      }
      else
      {
         mNoticationManager.cancel(ID_STATUS);
      }
   }

   private Notification buildLogging(int precision, int state, boolean monitor, long trackId)
   {
      Resources resources = mService.getResources();
      CharSequence contentTitle = resources.getString(R.string.app_name);
      String precisionText = resources.getStringArray(R.array.precision_choices)[precision];
      String stateText = resources.getStringArray(R.array.state_choices)[state - 1];
      CharSequence contentText;
      switch (precision)
      {
         case (Constants.LOGGING_GLOBAL):
            contentText = resources.getString(R.string.service_networkstatus, stateText, precisionText);
            break;
         default:
            if (monitor)
            {
               contentText = resources.getString(R.string.service_gpsstatus, stateText, precisionText,
                     mSatellites);
            }
            else
            {
               contentText = resources.getString(R.string.service_gpsnostatus, stateText, precisionText);
            }
            break;
      }
      Intent notificationIntent = new Intent(mService, LoggerMap.class);
      notificationIntent.setData(ContentUris.withAppendedId(GPStracking.Tracks.CONTENT_URI, trackId));
      PendingIntent contentIntent = PendingIntent.getActivity(mService, 0, notificationIntent, 0);

      NotificationCompat.Builder builder =
            new NotificationCompat.Builder(mService)
                  .setSmallIcon(SMALL_ICON)
                  .setContentTitle(contentTitle)
                  .setContentText(contentText)
                  .setContentIntent(contentIntent)
                  .setOngoing(true);
      PendingIntent pendingIntent;
      if (state == Constants.STATE_LOGGING)
      {
         CharSequence pause = resources.getString(R.string.logcontrol_pause);
         Intent intent = new Intent(Constants.SERVICENAME);
         intent.putExtra(GPSLoggerService.COMMAND, GPSLoggerService.EXTRA_COMMAND_PAUSE);
         pendingIntent = PendingIntent.getService(mService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
         builder.addAction(R.drawable.ic_pause_24dp, pause, pendingIntent);
      }
      else if (state == Constants.STATE_PAUSED)
      {
         CharSequence resume = resources.getString(R.string.logcontrol_resume);
         Intent intent = new Intent(Constants.SERVICENAME);
         intent.putExtra(GPSLoggerService.COMMAND, GPSLoggerService.EXTRA_COMMAND_RESUME);
         pendingIntent = PendingIntent.getService(mService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
         builder.addAction(R.drawable.ic_play_arrow_24dp, resume, pendingIntent);
      }

      return builder.build();
   }

   void startPoorSignal()
   {
      Resources resources = mService.getResources();
      CharSequence contentText = resources.getString(R.string.service_gpsproblem);
      CharSequence contentTitle = resources.getString(R.string.app_name);
      Intent notificationIntent = new Intent(mService, LoggerMap.class);
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

   public void stopPoorSignal()
   {
      mNoticationManager.cancel(ID_GPS_PROBLEM);
   }

   void startDisabledProvider(int resId, long trackId)
   {
      isShowingDisabled = true;

      CharSequence contentTitle = mService.getResources().getString(R.string.app_name);
      CharSequence contentText = mService.getResources().getString(resId);
      CharSequence tickerText = mService.getResources().getString(resId);
      Intent notificationIntent = new Intent(mService, LoggerMap.class);
      notificationIntent.setData(ContentUris.withAppendedId(GPStracking.Tracks.CONTENT_URI, trackId));
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

   void stopDisabledProvider(int resId)
   {
      mNoticationManager.cancel(ID_DISABLED);
      isShowingDisabled = false;

      CharSequence text = mService.getString(resId);
      Toast toast = Toast.makeText(mService, text, Toast.LENGTH_LONG);
      toast.show();
   }

   public boolean isShowingDisabled()
   {
      return isShowingDisabled;
   }
}
