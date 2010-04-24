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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.db.GPStracking.Media;
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
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

/**
 * Create a GPX version of a stored track
 * 
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class GpxCreator extends XmlCreator
{
   public static final String NS_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
   public static final String NS_GPX_11 = "http://www.topografix.com/GPX/1/1";
   public static final String DATETIME = "yyyy-MM-dd'T'HH:mm:ss'Z'";

   private String mChosenBaseFileName;
   private Intent mIntent;
   private XmlCreationProgressListener mProgressListener;
   private Context mContext;
   private String TAG = "OGT.GpxCreator";


   public GpxCreator(Context context, Intent intent, String chosenBaseFileName, XmlCreationProgressListener listener)
   {
      mChosenBaseFileName = chosenBaseFileName;
      mContext = context;
      mIntent = intent;
      mProgressListener = listener;
   }

   public void run()
   {
      Looper.prepare();
      Uri trackUri = mIntent.getData();
      String fileName = "UntitledTrack";
      if( mChosenBaseFileName != null && !mChosenBaseFileName.equals( "" ) )
      {
         fileName = mChosenBaseFileName;
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

      if( !( fileName.endsWith( ".gpx" ) || fileName.endsWith( ".xml" ) ) )
      {
         setExportDirectoryPath( Environment.getExternalStorageDirectory() + Constants.EXTERNAL_DIR + fileName );
         setXmlFileName( fileName + ".gpx" );
      }
      else
      {
         setExportDirectoryPath( Environment.getExternalStorageDirectory() + Constants.EXTERNAL_DIR + fileName.substring( 0, fileName.length() - 4 ) );
         setXmlFileName( fileName );
      }
      new File( getExportDirectoryPath() ).mkdirs();
      String xmlFilePath = getExportDirectoryPath() + "/" + getXmlFileName();
      
      if( mProgressListener != null )
      {
         mProgressListener.startNotification( fileName );
         mProgressListener.updateNotification( getProgress(), getGoal() );
      }

      String resultFilename = xmlFilePath;
      try
      {
         XmlSerializer serializer = Xml.newSerializer();
         BufferedOutputStream buf = new BufferedOutputStream( new FileOutputStream( xmlFilePath ), 8192 );
         serializer.setOutput( buf, "UTF-8" );

         serializeTrack( trackUri, serializer );

         if( isNeedsBundling() )
         {
            resultFilename = bundlingMediaAndXml( fileName, ".zip" );
         }

         fileName = new File( resultFilename ).getName();

         CharSequence text = mContext.getString( R.string.ticker_stored ) + "\"" + fileName + "\"";
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_SHORT );
         toast.show();
      }
      catch( FileNotFoundException e )
      {
         Log.e( TAG, "Unable to save " + e );
         CharSequence text = mContext.getString( R.string.ticker_failed ) + "\"" + xmlFilePath + "\"" + mContext.getString( R.string.error_filenotfound );
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      catch( IllegalArgumentException e )
      {
         Log.e( TAG, "Unable to save " + e );
         CharSequence text = mContext.getString( R.string.ticker_failed ) + "\"" + xmlFilePath + "\"" + mContext.getString( R.string.error_filename );
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      catch( IllegalStateException e )
      {
         Log.e( TAG, "Unable to save " + e );
         CharSequence text = mContext.getString( R.string.ticker_failed ) + "\"" + xmlFilePath + "\"" + mContext.getString( R.string.error_buildxml );
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      catch( IOException e )
      {
         Log.e( TAG, "Unable to save " + e );
         CharSequence text = mContext.getString( R.string.ticker_failed ) + "\"" + xmlFilePath + "\"" + mContext.getString( R.string.error_writesdcard );
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      finally
      {
         if( mProgressListener != null )
         {
            mProgressListener.endNotification( resultFilename );
         }
         Looper.loop();
      }

   }

   private void serializeTrack( Uri trackUri, XmlSerializer serializer ) throws IllegalArgumentException, IllegalStateException, IOException
   {
      serializer.startDocument( "UTF-8", true );
      serializer.setPrefix( "xsi", NS_SCHEMA );
      serializer.setPrefix( "gpx", NS_GPX_11 );
      serializer.text( "\n" );
      serializer.startTag( "", "gpx" );
      serializer.attribute( null, "version", "1.1" );
      serializer.attribute( null, "creator", "nl.sogeti.android.gpstracker" );
      serializer.attribute( NS_SCHEMA, "schemaLocation", NS_GPX_11 + " http://www.topografix.com/gpx/1/1/gpx.xsd" );
      serializer.attribute( null, "xmlns", NS_GPX_11 );

      // Big header of the track
      String name = serializeTrackHeader( mContext, serializer, trackUri );

      serializer.text( "\n" );
      serializer.startTag( "", "trk" );
      serializer.text( "\n" );
      serializer.startTag( "", "name" );
      serializer.text( name );
      serializer.endTag( "", "name" );

      // The list of segments in the track
      serializeSegments( serializer, Uri.withAppendedPath( trackUri, "segments" ) );

      serializer.text( "\n" );
      serializer.endTag( "", "trk" );
      serializer.text( "\n" );
      serializer.endTag( "", "gpx" );
      serializer.endDocument();
   }

   private String serializeTrackHeader( Context context, XmlSerializer serializer, Uri trackUri ) throws IOException
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
            serializer.text( "\n" );
            serializer.startTag( "", "metadata" );
            serializer.text( "\n" );
            serializer.startTag( "", "time" );
            Date time = new Date( trackCursor.getLong( 2 ) );
            DateFormat formater = new SimpleDateFormat( DATETIME );
            serializer.text( formater.format( time ) );
            serializer.endTag( "", "time" );
            serializer.text( "\n" );
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

   private void serializeSegments( XmlSerializer serializer, Uri segments ) throws IOException
   {
      Cursor segmentCursor = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         segmentCursor = resolver.query( segments, new String[] { Segments._ID }, null, null, null );
         if( segmentCursor.moveToFirst() )
         {
            if( mProgressListener != null )
            {
               mProgressListener.updateNotification( getProgress(), getGoal() );
            }

            do
            {
               Uri waypoints = Uri.withAppendedPath( segments, segmentCursor.getLong( 0 ) + "/waypoints" );
               serializer.text( "\n" );
               serializer.startTag( "", "trkseg" );
               serializeWaypoints( serializer, waypoints );
               serializer.text( "\n" );
               serializer.endTag( "", "trkseg" );
            }
            while( segmentCursor.moveToNext() );

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

   private void serializeWaypoints( XmlSerializer serializer, Uri waypoints ) throws IOException
   {
      Cursor waypointsCursor = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         waypointsCursor = resolver.query( waypoints, new String[] { Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints.TIME, Waypoints.ALTITUDE, Waypoints._ID }, null, null, null );
         if( waypointsCursor.moveToFirst() )
         {
            increaseGoal(  waypointsCursor.getCount() );
            do
            {
               increaseProgress( 1 );
               if( mProgressListener != null )
               {
                  mProgressListener.updateNotification( getProgress(), getGoal() );
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
               DateFormat formater = new SimpleDateFormat( DATETIME );
               serializer.text( formater.format( time ) );
               serializer.endTag( "", "time" );
               serializeWaypointDescription( mContext, serializer, Uri.withAppendedPath( waypoints, waypointsCursor.getLong( 4 ) + "/media" ) );
               serializer.text( "\n" );
               serializer.endTag( "", "trkpt" );
            }
            while( waypointsCursor.moveToNext() );
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

   private void serializeWaypointDescription( Context context, XmlSerializer serializer, Uri media ) throws IOException
   {
      String mediaPathPrefix = Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.EXTERNAL_DIR;
      Cursor mediaCursor = null;
      ContentResolver resolver = context.getContentResolver();
      try
      {
         mediaCursor = resolver.query( media, new String[] { Media.URI }, null, null, null );
         if( mediaCursor.moveToFirst() )
         {
            do
            {
               Uri mediaUri = Uri.parse( mediaCursor.getString( 0 ) );
               if( mediaUri.getScheme().equals( "file" ) )
               {
                  if( mediaUri.getLastPathSegment().endsWith( "3gp" ) )
                  {
                     serializer.text( "\n" );
                     serializer.startTag( "", "link" );
                     serializer.attribute( null, "href", includeMediaFile( mediaPathPrefix + mediaUri.getLastPathSegment() ) );
                     serializer.startTag( "", "text" );
                     serializer.text( mediaUri.getLastPathSegment() );
                     serializer.endTag( "", "text" );
                     serializer.endTag( "", "link" );
                  }
                  else if( mediaUri.getLastPathSegment().endsWith( "jpg" ) )
                  {
                     serializer.text( "\n" );
                     serializer.startTag( "", "link" );
                     serializer.attribute( null, "href", includeMediaFile( mediaPathPrefix + mediaUri.getLastPathSegment() ) );
                     serializer.startTag( "", "text" );
                     serializer.text( mediaUri.getLastPathSegment() );
                     serializer.endTag( "", "text" );
                     serializer.endTag( "", "link" );
                  }
                  else if( mediaUri.getLastPathSegment().endsWith( "txt" ) )
                  {
                     serializer.text( "\n" );
                     serializer.startTag( "", "desc" );
                     BufferedReader buf = new BufferedReader( new FileReader( mediaUri.getEncodedPath() ) );
                     String line;
                     while( ( line = buf.readLine() ) != null )
                     {
                        serializer.text( line );
                        serializer.text( "\n" );
                     }
                     serializer.endTag( "", "desc" );
                  }
               }
               else if( mediaUri.getScheme().equals( "content" ) )
               {
                  if( mediaUri.getAuthority().equals( GPStracking.AUTHORITY + ".string" ) )
                  {
                     serializer.text( "\n" );
                     serializer.startTag( "", "name" );
                     serializer.text( mediaUri.getLastPathSegment() );
                     serializer.endTag( "", "name" );
                  }
                  else if( mediaUri.getAuthority().equals( "media" ) )
                  {

                     Cursor mediaItemCursor = null;
                     try
                     {
                        mediaItemCursor = resolver.query( mediaUri, new String[] { MediaColumns.DATA, MediaColumns.DISPLAY_NAME }, null, null, null );
                        if( mediaItemCursor.moveToFirst() )
                        {
                           serializer.text( "\n" );
                           serializer.startTag( "", "link" );
                           serializer.attribute( null, "href", includeMediaFile( mediaItemCursor.getString( 0 ) ) );
                           serializer.startTag( "", "text" );
                           serializer.text( mediaItemCursor.getString( 1 ) );
                           serializer.endTag( "", "text" );
                           serializer.endTag( "", "link" );
                        }
                     }
                     finally
                     {
                        if( mediaItemCursor != null )
                        {
                           mediaItemCursor.close();
                        }
                     }
                  }
               }
            }
            while( mediaCursor.moveToNext() );
         }
      }
      finally
      {
         if( mediaCursor != null )
         {
            mediaCursor.close();
         }
      }
   }
}