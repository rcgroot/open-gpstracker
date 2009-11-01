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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;

import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * ????
 * 
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class ExportGPX extends Activity
{
   private static final int PROGRESS_STEPS = 10;
   private static final String NS_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
   private static final String NS_GPX_11 = "http://www.topografix.com/GPX/1/1";
   public static final String FILENAME = null;
   private static final String DATETIME = "yyyy-MM-dd'T'HH:mm:ss'Z'";
   protected static final String TAG = "ExportGPX";

   private RemoteViews mContentView;
   private int barProgress = 0;
   private int mProgress = 0;
   private int mGoal = 0;
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

   protected void exportGPX( final String chosenFileName )
   {
      final Intent intent = getIntent();

      new Thread( new Runnable()
         {
            public void run()
            {
               Looper.prepare();
               Uri trackUri = intent.getData();
               String fileName = "UntitledTrack";
               if( chosenFileName != null && !chosenFileName.equals( "" ))
               {
                  fileName = chosenFileName;
               }
               else
               {
                  Cursor trackCursor = null;
                  ContentResolver resolver = getContentResolver();
                  try
                  {
                     trackCursor = resolver.query( trackUri, new String[] { Tracks.NAME }, null, null, null );
                     if( trackCursor.moveToLast() )
                     {
                        fileName = trackCursor.getString( 0 );
                     }
                  }
                  finally
                  {
                     if( trackCursor != null )
                     {
                        trackCursor.close();
                     }
                  }
               }
               if( !fileName.endsWith( ".gpx" ) )
               {
                  fileName = fileName + ".gpx";
               }
               String filePath = "/sdcard/" + fileName;

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

               updateNotification();

               try
               {
                  XmlSerializer serializer = Xml.newSerializer();
                  BufferedOutputStream buf = new BufferedOutputStream( new FileOutputStream( filePath ), 8192 );
                  serializer.setOutput( buf, "UTF-8" );
                  serializer.startDocument( "UTF-8", true );
                  serializer.setPrefix( "xsi", NS_SCHEMA );
                  serializer.setPrefix( "gpx", NS_GPX_11 );
                  serializer.startTag( "", "gpx" );
                  serializer.attribute( null, "version", "1.1" );
                  serializer.attribute( null, "creator", "nl.sogeti.android.gpstracker" );
                  serializer.attribute( NS_SCHEMA, "schemaLocation", NS_GPX_11 + " http://www.topografix.com/gpx/1/1/gpx.xsd" );
                  serializer.attribute( null, "xmlns", NS_GPX_11 );

                  // Big header of the track
                  String name = serializeTrack( ExportGPX.this, serializer, trackUri );

                  serializer.text( "\n" );
                  serializer.startTag( "", "trk" );
                  serializer.text( "\n" );
                  serializer.startTag( "", "name" );
                  serializer.text( name );
                  serializer.endTag( "", "name" );

                  // The list of segments in the track
                  serializeSegments( ExportGPX.this, serializer, Uri.withAppendedPath( trackUri, "segments" ) );

                  serializer.text( "\n" );
                  serializer.endTag( "", "trk" );
                  serializer.text( "\n" );
                  serializer.endTag( "", "gpx" );
                  serializer.endDocument();

                  CharSequence text = getString( R.string.ticker_stored )+"\"" + fileName+"\"";
                  Toast toast = Toast.makeText( ExportGPX.this.getApplicationContext(), text, Toast.LENGTH_SHORT );
                  toast.show();
               }
               catch (IllegalArgumentException e)
               {
                  Log.e( TAG, "Unable to save " + e );
                  CharSequence text = getString( R.string.ticker_failed )+"\"" + fileName+"\""  + " reason: Incorrect filename";
                  Toast toast = Toast.makeText( ExportGPX.this.getApplicationContext(), text, Toast.LENGTH_LONG );
                  toast.show();
               }
               catch (IllegalStateException e)
               {
                  Log.e( TAG, "Unable to save " + e );
                  CharSequence text = getString( R.string.ticker_failed )+"\"" + fileName+"\""  + " reason: Error building XML";
                  Toast toast = Toast.makeText( ExportGPX.this.getApplicationContext(), text, Toast.LENGTH_LONG );
                  toast.show();
               }
               catch (IOException e)
               {
                  Log.e( TAG, "Unable to save " + e );
                  CharSequence text = getString( R.string.ticker_failed )+"\"" + fileName+"\""  + " reason: Error writing to SD card";
                  Toast toast = Toast.makeText( ExportGPX.this.getApplicationContext(), text, Toast.LENGTH_LONG );
                  toast.show();
               }
               finally
               {
                  mNotificationManager.cancel( R.layout.savenotificationprogress );
                  Looper.loop();
               }
            }
         } ).start();
      this.finish();
   }

   private String serializeTrack( Context context, XmlSerializer serializer, Uri trackUri ) throws IOException
   {
      ContentResolver resolver = context.getContentResolver();
      Cursor trackCursor = null;
      String name = null;

      try
      {
         trackCursor = resolver.query( trackUri, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, null, null, null );
         if( trackCursor.moveToFirst() )
         {
            name = trackCursor.getString( 1 );
            serializer.startTag( "", "metadata" );
            serializer.startTag( "", "time" );
            Date time = new Date( trackCursor.getLong( 2 ) );
            DateFormat formater = new SimpleDateFormat( DATETIME );
            serializer.text( formater.format( time ) );
            serializer.endTag( "", "time" );
            serializer.endTag( "", "metadata" );
         }
      }
      finally
      {
         if( trackCursor != null )
         {
            trackCursor.close();
         }
      }
      return name;
   }

   private void serializeSegments( Context context, XmlSerializer serializer, Uri segments ) throws IOException
   {
      Cursor segmentCursor = null;
      ContentResolver resolver = context.getContentResolver();
      try
      {
         segmentCursor = resolver.query( segments, new String[] { Segments._ID }, null, null, null );
         if( segmentCursor.moveToFirst() )
         {
            updateNotification();

            do
            {
               Uri waypoints = Uri.withAppendedPath( segments, "/" + segmentCursor.getLong( 0 ) + "/waypoints" );
               serializer.text( "\n" );
               serializer.startTag( "", "trkseg" );
               serializeWaypoints( context, serializer, waypoints );
               serializer.text( "\n" );
               serializer.endTag( "", "trkseg" );
            }
            while (segmentCursor.moveToNext());

         }
      }
      finally
      {
         if( segmentCursor != null )
         {
            segmentCursor.close();
         }
      }
   }

   private void serializeWaypoints( Context context, XmlSerializer serializer, Uri waypoints ) throws IOException
   {
      Cursor waypointsCursor = null;
      ContentResolver resolver = context.getContentResolver();
      try
      {
         waypointsCursor = resolver.query( waypoints, new String[] { Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints.TIME, Waypoints.ALTITUDE, Waypoints.ACCURACY }, null, null, null );
         if( waypointsCursor.moveToFirst() )
         {
            mGoal += waypointsCursor.getCount();
            do
            {
               mProgress++;
               updateNotification();

               serializer.text( "\n" );
               serializer.startTag( "", "trkpt" );
               serializer.attribute( null, "lat", waypointsCursor.getString( 1 ) );
               serializer.attribute( null, "lon", waypointsCursor.getString( 0 ) );
               serializer.attribute( null, "ele", waypointsCursor.getString( 3 ) );
               serializer.text( "\n" );
               serializer.startTag( "", "time" );
               Date time = new Date( waypointsCursor.getLong( 2 ) );
               DateFormat formater = new SimpleDateFormat( DATETIME );
               serializer.text( formater.format( time ) );
               serializer.endTag( "", "time" );
               serializer.text( "\n" );
               serializer.startTag( "", "name" );
               serializer.text( "point_" + waypointsCursor.getPosition() );
               serializer.endTag( "", "name" );
               serializer.text( "\n" );
               serializer.endTag( "", "trkpt" );
            }
            while (waypointsCursor.moveToNext());
         }
      }
      finally
      {
         if( waypointsCursor != null )
         {
            waypointsCursor.close();
         }
      }

   }

   private void updateNotification()
   {
      Log.d( "TAG", "Progress " + mProgress + " of " + mGoal );
      if( mProgress > 0 && mProgress < mGoal )
      {
         if( ( mProgress * PROGRESS_STEPS ) / mGoal != barProgress )
         {
            barProgress = ( mProgress * PROGRESS_STEPS ) / mGoal;
            mContentView.setProgressBar( R.id.progress, mGoal, mProgress, false );
            mNotificationManager.notify( R.layout.savenotificationprogress, mNotification );
         }
      }
      else if( mProgress == 0 )
      {
         mContentView.setProgressBar( R.id.progress, mGoal, mProgress, true );
         mNotificationManager.notify( R.layout.savenotificationprogress, mNotification );
      }
      else if( mProgress >= mGoal )
      {
         mContentView.setProgressBar( R.id.progress, mGoal, mProgress, false );
         mNotificationManager.notify( R.layout.savenotificationprogress, mNotification );
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