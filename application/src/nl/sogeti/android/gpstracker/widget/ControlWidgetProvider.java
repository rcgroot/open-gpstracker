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
import nl.sogeti.android.gpstracker.actions.ControlTracking;
import nl.sogeti.android.gpstracker.util.Constants;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
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
   private static final int BUTTON_TRACKINGCONTROL = 2;
   private static final int BUTTON_INSERTNOTE = 3;
   private static final String TAG = "OGT.ControlWidgetProvider";

   static final ComponentName THIS_APPWIDGET = new ComponentName("nl.sogeti.android.gpstracker", "nl.sogeti.android.gpstracker.widget.ControlWidgetProvider");
   private static int mState;

   @Override
   public void onEnabled(Context context)
   {
      Log.d(TAG, "onEnabled() ");
      super.onEnabled(context);

      PackageManager pm = context.getPackageManager();
      pm.setComponentEnabledSetting(THIS_APPWIDGET, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

      context.startService(new Intent(Constants.SERVICENAME));
   }

   @Override
   public void onDisabled(Context context)
   {
      Log.d(TAG, "onDisabled() ");
      PackageManager pm = context.getPackageManager();
      pm.setComponentEnabledSetting(THIS_APPWIDGET, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
   }

   @Override
   public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
   {
      // Update each requested appWidgetId
      RemoteViews view = buildUpdate(context, -1);

      for (int i = 0; i < appWidgetIds.length; i++)
      {
         appWidgetManager.updateAppWidget(appWidgetIds[i], view);
      }
   }

   /**
    * Load image for given widget and build {@link RemoteViews} for it.
    */
   static RemoteViews buildUpdate(Context context, int appWidgetId)
   {
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.control_appwidget);
      views.setOnClickPendingIntent(R.id.widget_insertnote_enabled, getLaunchPendingIntent(context, appWidgetId, BUTTON_INSERTNOTE));
      views.setOnClickPendingIntent(R.id.widget_trackingcontrol, getLaunchPendingIntent(context, appWidgetId, BUTTON_TRACKINGCONTROL));
      views.setBoolean(R.id.widget_insertnote_disabled, "setEnabled", false);
      views.setBoolean(R.id.widget_insertnote_enabled, "setEnabled", true);
      updateButtons(views, context);
      return views;
   }

   /**
    * Load image for given widget and build {@link RemoteViews} for it.
    */
   private static void updateButtons(RemoteViews views, Context context)
   {
      Log.d(TAG, "Updated the remote views to state " + mState);
      switch (mState)
      {
         case Constants.LOGGING:
            setEnableInsertNote(views, true);
            break;
         case Constants.PAUSED:
            setEnableInsertNote(views, false);
            break;
         case Constants.STOPPED:
            setEnableInsertNote(views, false);
            break;
         case Constants.UNKNOWN:
            setEnableInsertNote(views, false);
            break;
         default:
            Log.w(TAG, "Unknown logging state for widget: " + mState);
            break;
      }
   }
   
   private static void setEnableInsertNote( RemoteViews views, boolean enabled )
   {
      if( enabled )
      {
         views.setViewVisibility(R.id.widget_insertnote_enabled, View.VISIBLE);
         views.setViewVisibility(R.id.widget_insertnote_disabled, View.GONE);
      }
      else
      {
         views.setViewVisibility(R.id.widget_insertnote_enabled, View.GONE);
         views.setViewVisibility(R.id.widget_insertnote_disabled, View.VISIBLE);
      }
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
      Log.d(TAG, "Did recieve intent with action: " + intent.getAction());
      super.onReceive(context, intent);
      String action = intent.getAction();
      if (Constants.LOGGING_STATE_CHANGED_ACTION.equals(action))
      {
         mState = intent.getIntExtra(Constants.EXTRA_LOGGING_STATE, Constants.UNKNOWN);
         updateWidget(context);
         Log.d(TAG, "Changed state to " + mState);
      }
      else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE))
      {
         Uri data = intent.getData();
         int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
         if (buttonId == BUTTON_TRACKINGCONTROL)
         {
            Log.d(TAG, "Must launch tracking controll");
            Intent controlIntent = new Intent( context, ControlTracking.class );
            controlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(controlIntent);
         }
         else if (buttonId == BUTTON_INSERTNOTE)
         {
            Log.d(TAG, "Must launch note taking");
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

   /**
    * Updates the widget when something changes, or when a button is pushed.
    * 
    * @param context
    */
   public static void updateWidget(Context context)
   {
      RemoteViews views = buildUpdate(context, -1);
      // Update specific list of appWidgetIds if given, otherwise default to all
      final AppWidgetManager gm = AppWidgetManager.getInstance(context);
      gm.updateAppWidget(THIS_APPWIDGET, views);
      
      //5AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteView); 
   }

}
