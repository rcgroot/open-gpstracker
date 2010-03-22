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

/**
 * ????
 * 
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class KmzCreator extends Thread
{
   public static final String NS_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
   public static final String NS_KML_22 = "http://www.opengis.net/kml/2.2";
   public static final String DATETIME = "yyyy-MM-dd'T'HH:mm:ss'Z'";
   
   private String mChosenFileName;
   private Intent mIntent;
   private XmlCreationProgressListener mProgressListener;
   private Context mContext;

   private int mProgress = 0;
   private int mGoal = 0;
   private String TAG = "OGT.KmzCreator";
   
   public KmzCreator(Context context, Intent intent, String chosenFileName, XmlCreationProgressListener listener)
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
      if( !( fileName.endsWith( ".kmz" ) || fileName.endsWith( ".zip" ) ) )
      {
         filePath = Environment.getExternalStorageDirectory() +Constants.EXTERNAL_DIR+fileName;
         fileName = fileName + ".kmz";
      }
      else
      {
         filePath = Environment.getExternalStorageDirectory() +Constants.EXTERNAL_DIR+fileName.substring( 0, fileName.length()-4 );
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
         serializer.setPrefix( "xsi", NS_SCHEMA );
         serializer.setPrefix( "kml", NS_KML_22 );
         serializer.startTag( "", "kml" );
         serializer.attribute( NS_SCHEMA, "schemaLocation", NS_KML_22 + " http://schemas.opengis.net/kml/2.2.0/ogckml22.xsd" );
         serializer.attribute( null, "xmlns", NS_KML_22 );
   
         serializer.text( "\n" );
         serializer.startTag( "", "Document" );
         serializer.text( "\n" );
         serializer.startTag( "", "name" );
         serializer.text( fileName );
         serializer.endTag( "", "name" );
         
         /* from <name/> upto <Folder/>*/
         serializeTrack( mContext, serializer, trackUri );
         
         serializer.text( "\n" );
         serializer.endTag( "", "Document" );
         
         serializer.endTag( "", "kml" );
         serializer.endDocument();
   
         CharSequence text = mContext.getString( R.string.ticker_stored )+"\"" + fileName+"\"";
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_SHORT );
         toast.show();
         
//         serializer.text( "\n" );
//         serializer.startTag( "", "Document" );
//         serializer.text( "\n" );
//         serializer.endTag( "", "Document" );
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
            serializer.text( "\n" );
            serializer.startTag( "", "Style" );
            serializer.attribute( null, "id", "lineStyle" );
            serializer.startTag( "", "LineStyle" );
            serializer.text( "\n" );
            serializer.startTag( "", "color" );
            serializer.text( "99ffac59" );
            serializer.endTag( "", "color" );
            serializer.text( "\n" );
            serializer.startTag( "", "width" );
            serializer.text( "6" );
            serializer.endTag( "", "width" );
            serializer.text( "\n" );
            serializer.endTag( "", "LineStyle" );
            serializer.text( "\n" );
            serializer.endTag( "", "Style" );
            serializer.text( "\n" );
            
            serializer.text( "\n" );
            serializer.startTag( "", "Folder" );
            
            name = trackCursor.getString( 1 );
            serializer.text( "\n" );
            serializer.startTag( "", "name" );
            serializer.text(  name );
            serializer.endTag( "", "name" );
            
            serializer.text( "\n" );
            serializer.startTag( "", "TimeStamp" );
            serializer.text( "\n" );
            serializer.startTag( "", "when" );
            Date time = new Date( trackCursor.getLong( 2 ) );
            DateFormat formater = new SimpleDateFormat( DATETIME );
            serializer.text( formater.format( time ) );
            serializer.endTag( "", "when" );
            serializer.text( "\n" );
            serializer.endTag( "", "TimeStamp" );
            
            /* Multiple <Placemark/> */
            serializeSegments( mContext, serializer, Uri.withAppendedPath( trackUri, "segments" ) );
            

            serializer.text( "\n" );
            serializer.endTag( "", "Folder" );
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
               serializer.startTag( "", "Placemark" );
               serializer.text( "\n" );
               
               /* Single <TimeSpan/> element */
               serializeSegmentToTimespan( context, serializer, waypoints );

               serializer.text( "\n" );
               serializer.startTag( "", "styleUrl" );
               serializer.text( "#lineStyle" );
               serializer.endTag( "", "styleUrl" );
               serializer.text( "\n" );
               serializer.startTag( "", "LineString" );
               serializer.text( "\n" );
               serializer.startTag( "", "extrude" );
               serializer.text( "1" );
               serializer.endTag( "", "extrude" );
               serializer.text( "\n" );
               serializer.startTag( "", "altitudeMode" );
               serializer.text( "absolute" );
               serializer.endTag( "", "altitudeMode" );
               /* Single <coordinates/> and <TimeSpan/> element */
               serializeWaypoints( context, serializer, waypoints );
               serializer.text( "\n" );
               serializer.endTag( "", "LineString" );
               serializer.text( "\n" );
               serializer.endTag( "", "Placemark" );
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
   
   private void serializeSegmentToTimespan( Context context, XmlSerializer serializer, Uri waypoints ) throws IOException
   {
      Cursor waypointsCursor = null;
      Date segmentStartTime = null ;
      Date segmentEndTime = null;
      ContentResolver resolver = context.getContentResolver();
      try
      {
         waypointsCursor = resolver.query( waypoints, new String[] { Waypoints.TIME }, null, null, null );

         if( waypointsCursor.moveToFirst() )
         {
            segmentStartTime = new Date( waypointsCursor.getLong( 0 ) );
         }
         if( waypointsCursor.moveToLast() )
         {
            segmentEndTime = new Date( waypointsCursor.getLong( 0 ) );
         }
      }
      finally
      {
         if( waypointsCursor != null )
         {
            waypointsCursor.close();
         }
      }
      
      DateFormat formater = new SimpleDateFormat( DATETIME );
      serializer.text( "\n" );
      serializer.startTag( "", "TimeSpan" );
      serializer.text( "\n" );
      serializer.startTag( "", "begin" );         
      serializer.text( formater.format( segmentStartTime ) );
      serializer.endTag( "", "begin" );
      serializer.text( "\n" );
      serializer.startTag( "", "end" );
      serializer.text( formater.format( segmentEndTime ) );
      serializer.endTag( "", "end" );
      serializer.text( "\n" );
      serializer.endTag( "", "TimeSpan" );
      
   }
   
   private void serializeWaypoints( Context context, XmlSerializer serializer, Uri waypoints ) throws IOException
   {
      Cursor waypointsCursor = null;
      ContentResolver resolver = context.getContentResolver();
      try
      {
         waypointsCursor = resolver.query( waypoints, new String[] { Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints.ALTITUDE }, null, null, null );
         if( waypointsCursor.moveToFirst() )
         {
            
            mGoal += waypointsCursor.getCount();
            serializer.text( "\n" );
            serializer.startTag( "", "coordinates" );
            do
            {
               mProgress++;
               if( mProgressListener != null )
               {
                  mProgressListener.updateNotification(mProgress, mGoal);
               }
               serializer.text( Double.toString( waypointsCursor.getDouble( 0 ) ) );
               serializer.text( "," );
               serializer.text( Double.toString( waypointsCursor.getDouble( 1 ) ) );
               serializer.text( "," );
               serializer.text( Double.toString( waypointsCursor.getDouble( 2 ) ) );
               serializer.text( " " );
            }
            while (waypointsCursor.moveToNext());
            serializer.endTag( "", "coordinates" );
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