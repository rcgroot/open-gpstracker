/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Jul 27, 2010 Sogeti Nederland B.V. All Rights Reserved.
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
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.util.Constants;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

/**
 * Empty Activity that pops up the dialog to name the track
 * 
 * @version $Id$
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 */
public class ControlTracking extends Activity
{
   private static final int DIALOG_LOGCONTROL = 26;
   private static final String TAG = "OGT.ControlTracking";

   private GPSLoggerServiceManager mLoggerServiceManager;
   private Button start;
   private Button pause;
   private Button resume;
   private Button stop;

   private final View.OnClickListener mLoggingControlListener = new View.OnClickListener()
      {
         public void onClick( View v )
         {
            int id = v.getId();
            switch( id )
            {
               case R.id.logcontrol_start:
                  long loggerTrackId = mLoggerServiceManager.startGPSLogging( null );

                  Intent intent = new Intent();
                  intent.setData( ContentUris.withAppendedId( Tracks.CONTENT_URI, loggerTrackId ) );
                  ComponentName caller = ControlTracking.this.getCallingActivity();
                  if( caller != null )
                  {
                     setResult( RESULT_OK, intent );
                  }
                  break;
               case R.id.logcontrol_pause:
                  mLoggerServiceManager.pauseGPSLogging();
                  break;
               case R.id.logcontrol_resume:
                  mLoggerServiceManager.resumeGPSLogging();
                  break;
               case R.id.logcontrol_stop:
                  mLoggerServiceManager.stopGPSLogging();
                  break;
               default:
                  break;
            }
            finish();
         }
      };
   private OnClickListener mDialogClickListener = new OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            finish();
         }
      };

   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      this.setVisible( false );
      mLoggerServiceManager = new GPSLoggerServiceManager( this );
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      mLoggerServiceManager.startup( new Runnable()
         {
            public void run()
            {
               showDialog( DIALOG_LOGCONTROL );
            }
         } );
   }

   @Override
   protected void onPause()
   {
      super.onPause();
      mLoggerServiceManager.shutdown();
   }

   @Override
   protected Dialog onCreateDialog( int id )
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch( id )
      {
         case DIALOG_LOGCONTROL:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.logcontrol, null );
            builder.setTitle( R.string.dialog_tracking_title ).setIcon( android.R.drawable.ic_dialog_alert ).setNegativeButton( R.string.btn_cancel, mDialogClickListener ).setView( view );
            dialog = builder.create();
            start = (Button) view.findViewById( R.id.logcontrol_start );
            pause = (Button) view.findViewById( R.id.logcontrol_pause );
            resume = (Button) view.findViewById( R.id.logcontrol_resume );
            stop = (Button) view.findViewById( R.id.logcontrol_stop );
            start.setOnClickListener( mLoggingControlListener );
            pause.setOnClickListener( mLoggingControlListener );
            resume.setOnClickListener( mLoggingControlListener );
            stop.setOnClickListener( mLoggingControlListener );
            dialog.setOnDismissListener( new OnDismissListener()
               {
                  public void onDismiss( DialogInterface dialog )
                  {
                     finish();
                  }
               });
            return dialog;
         default:
            return super.onCreateDialog( id );
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog( int id, Dialog dialog )
   {
      switch( id )
      {
         case DIALOG_LOGCONTROL:
            updateDialogState( mLoggerServiceManager.getLoggingState() );
            break;
         default:
            break;
      }
      super.onPrepareDialog( id, dialog );
   }

   private void updateDialogState( int state )
   {
      switch( state )
      {
         case Constants.STOPPED:
            start.setEnabled( true );
            pause.setEnabled( false );
            resume.setEnabled( false );
            stop.setEnabled( false );
            break;
         case Constants.LOGGING:
            start.setEnabled( false );
            pause.setEnabled( true );
            resume.setEnabled( false );
            stop.setEnabled( true );
            break;
         case Constants.PAUSED:
            start.setEnabled( false );
            pause.setEnabled( false );
            resume.setEnabled( true );
            stop.setEnabled( true );
            break;
         default:
            Log.w( TAG, String.format( "State %d of logging, enabling and hope for the best....", state ) );
            start.setEnabled( false );
            pause.setEnabled( false );
            resume.setEnabled( false );
            stop.setEnabled( false );
            break;
      }
   }
}
