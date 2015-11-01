/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.actions;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.Calendar;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;

/**
 * Empty Activity that pops up the dialog to name the track
 *
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 * @version $Id$
 */
public class NameTrack extends Activity
{
   protected static final String TAG = "OGT.NameTrack";
   private static final int DIALOG_TRACKNAME = 23;
   Uri mTrackUri;
   private EditText mTrackNameView;
   private final DialogInterface.OnClickListener mTrackNameDialogListener = new DialogInterface.OnClickListener()
   {
      @Override
      public void onClick(DialogInterface dialog, int which)
      {
         String trackName = null;
         switch (which)
         {
            case DialogInterface.BUTTON_POSITIVE:
               trackName = mTrackNameView.getText().toString();
               ContentValues values = new ContentValues();
               values.put(Tracks.NAME, trackName);
               getContentResolver().update(mTrackUri, values, null, null);
               clearNotification();
               break;
            case DialogInterface.BUTTON_NEUTRAL:
               startDelayNotification();
               break;
            case DialogInterface.BUTTON_NEGATIVE:
               clearNotification();
               break;
            default:
               Log.e(TAG, "Unknown option ending dialog:" + which);
               break;
         }
         finish();
      }


   };
   private boolean paused;

   private void clearNotification()
   {
      NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context
            .NOTIFICATION_SERVICE);
      notificationManager.cancel(R.layout.namedialog);
   }

   private void startDelayNotification()
   {
      int resId = R.string.dialog_routename_title;
      int icon = R.drawable.ic_maps_indicator_current_position;
      CharSequence contentTitle = getResources().getString(R.string.app_name);
      CharSequence contentText = getResources().getString(resId);
      Intent notificationIntent = new Intent(this, NameTrack.class);
      notificationIntent.setData(mTrackUri);
      PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

      NotificationCompat.Builder builder =
            new NotificationCompat.Builder(this)
                  .setSmallIcon(icon)
                  .setContentTitle(contentTitle)
                  .setContentText(contentText)
                  .setContentIntent(contentIntent)
                  .setOngoing(true);


      NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context
            .NOTIFICATION_SERVICE);
      notificationManager.notify(R.layout.namedialog, builder.build());
   }

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      this.setVisible(false);
      paused = false;
      mTrackUri = this.getIntent().getData();
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   protected void onResume()
   {
      super.onResume();
      if (mTrackUri != null)
      {
         showDialog(DIALOG_TRACKNAME);
      }
      else
      {
         Log.e(TAG, "Naming track without a track URI supplied.");
         finish();
      }
   }

   @Override
   protected void onPause()
   {
      super.onPause();
      paused = true;
   }

   @Override
   protected Dialog onCreateDialog(int id)
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_TRACKNAME:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.namedialog, (ViewGroup) findViewById(android.R.id.content), false);
            mTrackNameView = (EditText) view.findViewById(R.id.nameField);
            builder
                  .setTitle(R.string.dialog_routename_title)
                  .setMessage(R.string.dialog_routename_message)
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_okay, mTrackNameDialogListener)
                  .setNeutralButton(R.string.btn_skip, mTrackNameDialogListener)
                  .setNegativeButton(R.string.btn_cancel, mTrackNameDialogListener)
                  .setView(view);
            dialog = builder.create();
            dialog.setOnDismissListener(new OnDismissListener()
            {
               @Override
               public void onDismiss(DialogInterface dialog)
               {
                  if (!paused)
                  {
                     finish();
                  }
               }
            });
            return dialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   @Override
   protected void onPrepareDialog(int id, Dialog dialog)
   {
      switch (id)
      {
         case DIALOG_TRACKNAME:
            String trackName;
            Calendar c = Calendar.getInstance();
            trackName = String.format(getString(R.string.dialog_routename_default), c, c, c, c, c);
            mTrackNameView.setText(trackName);
            mTrackNameView.setSelection(0, trackName.length());
            break;
         default:
            super.onPrepareDialog(id, dialog);
            break;
      }
   }
}
   