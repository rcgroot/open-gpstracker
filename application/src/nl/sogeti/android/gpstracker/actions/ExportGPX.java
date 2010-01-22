/*------------------------------------------------------------------------------
 **     Ident: Innovation en Inspiration > Google Android 
 **    Author: rene
 ** Copyright: (c) Jan 22, 2009 Sogeti Nederland B.V. All Rights Reserved.
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


import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.GpxCreator;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RemoteViews;

/**
 * ????
 * 
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class ExportGPX extends Activity
{
   private static final int PROGRESS_STEPS = 10;
   public static final String NS_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
   public static final String NS_GPX_11 = "http://www.topografix.com/GPX/1/1";
   public static final String FILENAME = null;
   public static final String DATETIME = "yyyy-MM-dd'T'HH:mm:ss'Z'";
   public static final String TAG = "ExportGPX";

   private RemoteViews mContentView;
   private int barProgress = 0;
   private Notification mNotification;
   private NotificationManager mNotificationManager;
   private OnClickListener negativeListener = new OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            ExportGPX.this.finish();
         }
      };
   private OnClickListener positiveListener = new OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            ExportGPX.this.exportGPX( mFileNameView.getText().toString() );
         }
      };
   private EditText mFileNameView;

   @Override
   public void onCreate( Bundle savedInstanceState )
   {
      setVisible( false );
      super.onCreate( savedInstanceState );

      LayoutInflater factory = LayoutInflater.from( this );
      View view = factory.inflate( R.layout.filenamedialog, null );
      mFileNameView = (EditText) view.findViewById( R.id.fileNameField );
      createAlertFileName( view, positiveListener, negativeListener ).show();
   }

   protected void exportGPX( String chosenFileName )
   {
      GpxCreator mGpxCreator = new GpxCreator( this, getIntent(), chosenFileName, new ProgressListener() );
      mGpxCreator.start();
      this.finish();
   }
   
   public interface GpxCreationProgressListener
   {
      public void startNotification( String fileName );
      public void updateNotification(int progress, int goal);
      public void endNotification( String fileName );
   }
   
   class ProgressListener implements GpxCreationProgressListener
   {
      public void startNotification( String fileName )
      {
         String ns = Context.NOTIFICATION_SERVICE;
         mNotificationManager = (NotificationManager) ExportGPX.this.getSystemService( ns );
         int icon = android.R.drawable.ic_menu_save;
         CharSequence tickerText = getString( R.string.ticker_saving )+ "\"" + fileName + "\"";
       
         mNotification = new Notification();
         PendingIntent contentIntent = PendingIntent.getActivity( ExportGPX.this, 0, new Intent( ExportGPX.this, LoggerMap.class ).setFlags( Intent.FLAG_ACTIVITY_NEW_TASK ),
               PendingIntent.FLAG_UPDATE_CURRENT );
       
         mNotification.contentIntent = contentIntent;
         mNotification.tickerText = tickerText;
         mNotification.icon = icon;
         mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
         mContentView = new RemoteViews( getPackageName(), R.layout.savenotificationprogress );
         mContentView.setImageViewResource( R.id.icon, icon );
         mContentView.setTextViewText( R.id.progresstext, tickerText );
       
         mNotification.contentView = mContentView;
      }
      
      public void updateNotification(int progress, int goal)
      {
//         Log.d( TAG, "Progress " + progress + " of " + goal );
         if( progress > 0 && progress < goal )
         {
            if( ( progress * PROGRESS_STEPS ) / goal != barProgress )
            {
               barProgress = ( progress * PROGRESS_STEPS ) / goal;
               mContentView.setProgressBar( R.id.progress, goal, progress, false );
               mNotificationManager.notify( R.layout.savenotificationprogress, mNotification );
            }
         }
         else if( progress == 0 )
         {
            mContentView.setProgressBar( R.id.progress, goal, progress, true );
            mNotificationManager.notify( R.layout.savenotificationprogress, mNotification );
         }
         else if( progress >= goal )
         {
            mContentView.setProgressBar( R.id.progress, goal, progress, false );
            mNotificationManager.notify( R.layout.savenotificationprogress, mNotification );
         }
      }
      
      public void endNotification(String filename)
      {
         mNotificationManager.cancel( R.layout.savenotificationprogress );
      }
   }

   private Dialog createAlertFileName( View view, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener )
   {
      Builder builder = new AlertDialog.Builder( this ).setTitle( R.string.dialog_filename_title ).setMessage( R.string.dialog_filename_message ).setIcon( android.R.drawable.ic_dialog_alert )
            .setView( view ).setPositiveButton( R.string.btn_okay, positiveListener ).setNegativeButton( R.string.btn_cancel, negativeListener );

      Dialog dialog = builder.create();
      dialog.setOwnerActivity( this );
      return dialog;
   }

}