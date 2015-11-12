/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) 2011 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nl.sogeti.android.gpstracker.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.ControlTracking;
import nl.sogeti.android.gpstracker.actions.InsertNote;
import nl.sogeti.android.gpstracker.logger.GPSLoggerService;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.Log;

/**
 * An App Widget for on the home screen to control logging with a start, pause,
 * resume and stop
 *
 * @author grootren (c) Mar 8, 2011, Sogeti B.V.
 * @version $Id$
 */
public class ControlWidgetProvider extends AppWidgetProvider
{
   private static final int BUTTON_TRACKINGCONTROL = 2;
   private static final int BUTTON_INSERTNOTE = 3;
   static final ComponentName THIS_APPWIDGET = new ComponentName("nl.sogeti.android.gpstracker", "nl.sogeti.android" +
         ".gpstracker.widget.ControlWidgetProvider");
   private static int mState;

   public ControlWidgetProvider()
   {
      super();
   }

   @Override
   public void onEnabled(Context context)
   {
      Log.d(this, "onEnabled() ");
      super.onEnabled(context);

      context.startService(new Intent(context, GPSLoggerService.class));
   }

   @Override
   public void onDisabled(Context context)
   {
      Log.d(this, "onDisabled() ");
   }

   @Override
   public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
   {
      Log.d(this, "onDisabled() ");
      // Update each requested appWidgetId
      RemoteViews view = buildUpdate(context, -1);

      for (int i = 0; i < appWidgetIds.length; i++)
      {
         appWidgetManager.updateAppWidget(appWidgetIds[i], view);
      }
   }

   @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
   @Override
   public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle
         newOptions)
   {
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN)
      {
         Bundle bundle = appWidgetManager.getAppWidgetOptions(appWidgetId);
         int minwidth_dp = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
         int maxwidth_dp = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
         Log.d(this, "(%s ... %s)", Integer.toString(minwidth_dp), Integer.toString(maxwidth_dp));

      }
   }

   /**
    * Load image for given widget and build {@link RemoteViews} for it.
    */
   static RemoteViews buildUpdate(Context context, int appWidgetId)
   {
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.control_appwidget);
      views.setOnClickPendingIntent(R.id.widget_insertnote_enabled, getLaunchPendingIntent(context, appWidgetId,
            BUTTON_INSERTNOTE));
      views.setOnClickPendingIntent(R.id.widget_trackingcontrol, getLaunchPendingIntent(context, appWidgetId,
            BUTTON_TRACKINGCONTROL));
      updateButtons(views);
      return views;
   }

   /**
    * Load image for given widget and build {@link RemoteViews} for it.
    */
   private static void updateButtons(RemoteViews views)
   {
      Log.d(ControlWidgetProvider.class, "Updated the remote views to state " + mState);
      switch (mState)
      {
         case Constants.STATE_LOGGING:
            setEnableInsertNote(views, true);
            break;
         case Constants.STATE_PAUSED:
            setEnableInsertNote(views, false);
            break;
         case Constants.STATE_STOPPED:
            setEnableInsertNote(views, false);
            break;
         case Constants.STATE_UNKNOWN:
            setEnableInsertNote(views, false);
            break;
         default:
            Log.w(ControlWidgetProvider.class, "Unknown logging state for widget: " + mState);
            break;
      }
   }

   private static void setEnableInsertNote(RemoteViews views, boolean enabled)
   {
      if (enabled)
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
      PendingIntent pi = PendingIntent.getBroadcast(context, 0, launchIntent, 0);

      return pi;
   }

   /**
    * Receives and processes a button pressed intent or state change.
    *
    * @param context
    * @param intent  Indicates the pressed button.
    */
   @Override
   public void onReceive(Context context, Intent intent)
   {
      Log.d(this, "Did receive intent with action: " + intent.getAction());
      super.onReceive(context, intent);
      String action = intent.getAction();
      if (Constants.LOGGING_STATE_CHANGED_ACTION.equals(action))
      {
         mState = intent.getIntExtra(Constants.EXTRA_LOGGING_STATE, Constants.STATE_UNKNOWN);
         updateWidget(context);
      }
      else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE))
      {
         Uri data = intent.getData();
         int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
         if (buttonId == BUTTON_TRACKINGCONTROL)
         {
            Intent controlIntent = new Intent(context, ControlTracking.class);
            controlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            controlIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            context.startActivity(controlIntent);
         }
         else if (buttonId == BUTTON_INSERTNOTE)
         {
            Intent noteIntent = new Intent(context, InsertNote.class);
            noteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            noteIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            context.startActivity(noteIntent);
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
   }

}