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
import nl.sogeti.android.gpstracker.actions.utils.XmlCreationProgressListener;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

/**
 * Send a GPX file with Android SEND Intent
 * 
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class SendGPX extends Activity
{
   private static final int PROGRESS_STEPS = 10;
   protected static final String TAG = "EmailGPX";

   private RemoteViews mContentView;
   private int barProgress = 0;
   private Notification mNotification;
   private NotificationManager mNotificationManager;


   @Override
   public void onCreate( Bundle savedInstanceState )
   {
      setVisible( false );
      super.onCreate( savedInstanceState );
      exportGPX( "track_send" );
   }

   protected void exportGPX( String chosenFileName )
   {
      nl.sogeti.android.gpstracker.actions.utils.GpxCreator mGpxCreator = new nl.sogeti.android.gpstracker.actions.utils.GpxCreator( this, getIntent(), chosenFileName, new ProgressListener() );
      mGpxCreator.start();
      this.finish();
   }
   
   class ProgressListener implements XmlCreationProgressListener
   {
      public void startNotification( String fileName )
      {
         String ns = Context.NOTIFICATION_SERVICE;
         mNotificationManager = (NotificationManager) SendGPX.this.getSystemService( ns );
         int icon = android.R.drawable.ic_menu_save;
         CharSequence tickerText = getString( R.string.ticker_saving )+ "\"" + fileName + "\"";
       
         mNotification = new Notification();
         PendingIntent contentIntent = PendingIntent.getActivity( SendGPX.this, 0, new Intent( SendGPX.this, LoggerMap.class ).setFlags( Intent.FLAG_ACTIVITY_NEW_TASK ),
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
      public void updateNotification( int progress, int goal )
      {
//         Log.d( "TAG", "Progress " + progress + " of " + goal );
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
      public void endNotification( String filename )
      {
         mNotificationManager.cancel( R.layout.savenotificationprogress );
         Intent emailIntent = new Intent(Intent.ACTION_SEND);
         emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body) ); 
         emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject) );
         emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+filename)); 
         emailIntent.setType("text/xml");
         startActivity(Intent.createChooser(emailIntent, getString(R.string.email_chooser) )); 
      }
   }
}