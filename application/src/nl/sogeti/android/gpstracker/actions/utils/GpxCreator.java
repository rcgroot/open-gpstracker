/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Jan 21, 2010 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.actions.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.ExportGPX;
import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.Constants;

import org.xmlpull.v1.XmlSerializer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

public class GpxCreator extends Thread
{
   private String mChosenFileName;
   private Intent mIntent;
   private GpxCreationProgressListener mProgressListener;
   private Context mContext;

   private int mProgress = 0;
   private int mGoal = 0;
   private String TAG = "OGT.GpxCreator";
   
   public GpxCreator(Context context, Intent intent, String chosenFileName, GpxCreationProgressListener listener)
   {
      mChosenFileName = chosenFileName;
      mContext = context;
      mIntent = intent;
      mProgressListener = listener;
   }
   
   public void run()
   {
      Looper.prepare();
      Uri trackUri = mIntent.getData();
      String fileName = "UntitledTrack";
      if( mChosenFileName != null && !mChosenFileName.equals( "" ))
      {
         fileName = mChosenFileName;
      }
      else
      {
         Cursor trackCursor = null;
         ContentResolver resolver = mContext.getContentResolver();
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
      
      String filePath;
      if( !( fileName.endsWith( ".gpx" ) || fileName.endsWith( ".xml" ) ) )
      {
         filePath = Environment.getExternalStorageDirectory() +Constants.EXTERNAL_DIR+fileName;
         fileName = fileName + ".gpx";
      }
      else
      {
         filePath = Environment.getExternalStorageDirectory() +"/"+fileName.substring( 0, fileName.length()-4 );
      }
      new File( filePath ).mkdirs();
      filePath = filePath+"/"+fileName;
   
      if( mProgressListener != null )
      {
         mProgressListener.startNotification( fileName );
         mProgressListener.updateNotification( mProgress, mGoal );
      }
   
      try
      {
         XmlSerializer serializer = Xml.newSerializer();
         BufferedOutputStream buf = new BufferedOutputStream( new FileOutputStream( filePath ), 8192 );
         serializer.setOutput( buf, "UTF-8" );
         serializer.startDocument( "UTF-8", true );
         serializer.setPrefix( "xsi", ExportGPX.NS_SCHEMA );
         serializer.setPrefix( "gpx", ExportGPX.NS_GPX_11 );
         serializer.startTag( "", "gpx" );
         serializer.attribute( null, "version", "1.1" );
         serializer.attribute( null, "creator", "nl.sogeti.android.gpstracker" );
         serializer.attribute( ExportGPX.NS_SCHEMA, "schemaLocation", ExportGPX.NS_GPX_11 + " http://www.topografix.com/gpx/1/1/gpx.xsd" );
         serializer.attribute( null, "xmlns", ExportGPX.NS_GPX_11 );
   
         // Big header of the track
         String name = serializeTrack( mContext, serializer, trackUri );
   
         serializer.text( "\n" );
         serializer.startTag( "", "trk" );
         serializer.text( "\n" );
         serializer.startTag( "", "name" );
         serializer.text( name );
         serializer.endTag( "", "name" );
   
         // The list of segments in the track
         serializeSegments( mContext, serializer, Uri.withAppendedPath( trackUri, "segments" ) );
   
         serializer.text( "\n" );
         serializer.endTag( "", "trk" );
         serializer.text( "\n" );
         serializer.endTag( "", "gpx" );
         serializer.endDocument();
   
         CharSequence text = mContext.getString( R.string.ticker_stored )+"\"" + fileName+"\"";
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_SHORT );
         toast.show();
      }
      catch (IllegalArgumentException e)
      {
         Log.e( TAG , "Unable to save " + e );
         CharSequence text = mContext.getString( R.string.ticker_failed )+"\"" + filePath+"\""  + mContext.getString( R.string.error_filename );
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      catch (IllegalStateException e)
      {
         Log.e( TAG, "Unable to save " + e );
         CharSequence text = mContext.getString( R.string.ticker_failed )+"\"" + filePath+"\""  + mContext.getString( R.string.error_buildxml );
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      catch (IOException e)
      {
         Log.e( TAG, "Unable to save " + e );
         CharSequence text = mContext.getString( R.string.ticker_failed )+"\"" + filePath+"\""  + mContext.getString( R.string.error_writesdcard );
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      finally
      {
         if( mProgressListener != null )
         {
            mProgressListener.endNotification( filePath );
         }
         Looper.loop();
      }
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
            DateFormat formater = new SimpleDateFormat( ExportGPX.DATETIME );
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
            if( mProgressListener != null )
            {
               mProgressListener.updateNotification( mProgress, mGoal );
            }
   
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
               if( mProgressListener != null )
               {
                  mProgressListener.updateNotification(mProgress, mGoal);
               }
   
               serializer.text( "\n" );
               serializer.startTag( "", "trkpt" );
               serializer.attribute( null, "lat", Double.toString( waypointsCursor.getDouble( 1 ) ) );
               serializer.attribute( null, "lon", Double.toString( waypointsCursor.getDouble( 0 ) ) );
               serializer.text( "\n" );
               serializer.startTag( "", "ele" );
               serializer.text( Double.toString( waypointsCursor.getDouble( 3 ) ) );
               serializer.endTag( "", "ele" );
               serializer.text( "\n" );
               serializer.startTag( "", "time" );
               Date time = new Date( waypointsCursor.getLong( 2 ) );
               DateFormat formater = new SimpleDateFormat( ExportGPX.DATETIME );
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
}