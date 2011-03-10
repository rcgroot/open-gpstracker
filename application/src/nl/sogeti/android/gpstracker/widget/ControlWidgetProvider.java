/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: grootren
 ** Copyright: (c) Mar 8, 2011 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.widget;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.util.Constants;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * An App Widget for on the home screen to control logging with a start, pause,
 * resume and stop
 * 
 * @version $Id:$
 * @author grootren (c) Mar 8, 2011, Sogeti B.V.
 */
public class ControlWidgetProvider extends AppWidgetProvider
{
   private static final int BUTTON_RECORDANDSTOP = 2;
   private static final int BUTTON_PAUSEANDRESUME = 3;
   private static final String TAG = "OGT.ControlWidgetProvider";
   
   static final ComponentName THIS_APPWIDGET =
      new ComponentName("nl.sogeti.android.gpstracker",
              "nl.sogeti.android.gpstracker.widget.ControlWidgetProvider");
   private static int mState;
   private GPSLoggerServiceManager mLoggerServiceManager;

   @Override
   public void onEnabled(Context context)
   {
      Log.d( TAG, "onEnabled() " );
      super.onEnabled(context);
      context.startService(new Intent(Constants.SERVICENAME));
   }

   @Override
   public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
   {
      Log.d( TAG, "onUpdate() " );
      
      // Update each requested appWidgetId
      RemoteViews view = buildUpdate(context, -1);

      for (int i = 0; i < appWidgetIds.length; i++)
      {
         appWidgetManager.updateAppWidget(appWidgetIds[i], view);
         view.setOnClickPendingIntent(R.id.recordAndStop, getLaunchPendingIntent(context, appWidgetIds[i], BUTTON_RECORDANDSTOP));
         view.setOnClickPendingIntent(R.id.pauseAndResume, getLaunchPendingIntent(context, appWidgetIds[i], BUTTON_PAUSEANDRESUME));
      }
   }

   /**
    * Load image for given widget and build {@link RemoteViews} for it.
    */
   static RemoteViews buildUpdate(Context context, int appWidgetId)
   {
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.control_appwidget);
      switch( mState )
      {
         case Constants.LOGGING:
            views.setImageViewResource(R.id.recordAndStop, R.drawable.stop);
            views.setImageViewResource(R.id.pauseAndResume, R.drawable.pause);
            views.setBoolean(R.id.pauseAndResume, "setEnabled", true);
            views.setBoolean(R.id.recordAndStop, "setEnabled", true);
            break;
         case Constants.PAUSED:
            views.setImageViewResource(R.id.recordAndStop, R.drawable.stop);
            views.setImageViewResource(R.id.pauseAndResume, R.drawable.resume);
            views.setBoolean(R.id.pauseAndResume, "setEnabled", true);
            views.setBoolean(R.id.recordAndStop, "setEnabled", true);
            break;
         case Constants.STOPPED:
            views.setImageViewResource(R.id.recordAndStop, R.drawable.record);
            views.setImageViewResource(R.id.pauseAndResume, R.drawable.pause_disabled);
            views.setBoolean(R.id.pauseAndResume, "setEnabled", false);
            views.setBoolean(R.id.recordAndStop, "setEnabled", true);
            break;
         case Constants.UNKNOWN:
            views.setImageViewResource(R.id.recordAndStop, R.drawable.record);
            views.setImageViewResource(R.id.pauseAndResume, R.drawable.pause_disabled);
            views.setBoolean(R.id.pauseAndResume, "setEnabled", false);
            views.setBoolean(R.id.recordAndStop, "setEnabled", false);
            break;
         default:
            Log.w( TAG, "Unknown logging state for widget: "+ mState);
            break;
      }
      Log.d( TAG, "Updated the remote views to state "+mState );
      return views;
   }

   /**
    * Creates PendingIntent to notify the widget of a button click.
    * 
    * @param context
    * @param appWidgetId
    * @return
    */
   private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId, int buttonId)
   {
      Intent launchIntent = new Intent();
      launchIntent.setClass(context, ControlWidgetProvider.class);
      launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
      launchIntent.setData(Uri.parse("custom:" + buttonId));
      PendingIntent pi = PendingIntent.getBroadcast(context, 0 /* no requestCode */, launchIntent, 0 /*
                                                                                                      * no
                                                                                                      * flags
                                                                                                      */);
      return pi;
   }

   /**
    * Receives and processes a button pressed intent or state change.
    * 
    * @param context
    * @param intent Indicates the pressed button.
    */
   @Override
   public void onReceive(Context context, Intent intent)
   {
      Log.d( TAG, "Did recieve intent"+ intent.getAction() );
      super.onReceive(context, intent);
      String action = intent.getAction();
      if( Constants.LOGGING_STATE_CHANGED_ACTION.equals(action) )
      {
         mState = intent.getIntExtra(Constants.EXTRA_LOGGING_STATE, Constants.UNKNOWN);
         Log.d( TAG, "Changed state to "+ mState );
      }
      else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE))
      {
         Uri data = intent.getData();
         int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
         if (buttonId == BUTTON_RECORDANDSTOP)
         {
            Log.d( TAG, "Must TOGGLE between record and stop" );
            //toggleRecordStop(context);
         }
         else if (buttonId == BUTTON_PAUSEANDRESUME)
         {
            Log.d( TAG, "Must TOGGLE between pause and resume" );
            //toggleRecordStop(context);
         }
      }
      else
      {
         // Don't fall-through to updating the widget.  The Intent
         // was something unrelated or that our super class took
         // care of.
         return;
      }
      // State changes fall through
      updateWidget(context);
   }

   public static void updateWidget(Context context) {
      RemoteViews views = buildUpdate(context, -1);
      // Update specific list of appWidgetIds if given, otherwise default to all
      final AppWidgetManager gm = AppWidgetManager.getInstance(context);
      gm.updateAppWidget(THIS_APPWIDGET, views);
  }

}
