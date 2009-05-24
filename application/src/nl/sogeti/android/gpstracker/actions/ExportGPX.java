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

import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;

import org.xmlpull.v1.XmlSerializer;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Xml;

/**
 * ????
 *
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class ExportGPX extends BroadcastReceiver
{
   public static final String FILENAME = "filename";
   private static final String DATETIME = "yyyy-MM-dd'T'HH:mm:ss'Z'";

   @Override
   public void onReceive( final Context context, final Intent intent )
   {
      new Thread(new Runnable() {
         public void run() {
            String filename = "/sdcard/"+intent.getExtras().getString( FILENAME );
            try 
            {
               XmlSerializer serializer =  Xml.newSerializer();
               BufferedOutputStream buf = new BufferedOutputStream(new FileOutputStream(filename), 8192);
               serializer.setOutput( buf, "UTF-8" );
               serializer.startDocument( "UTF-8", true );
               serializer.startTag( "http://www.topografix.com/GPX/1/1", "gpx" );

               Uri trackUri = intent.getData();

               // Big header of the track
               String name = serializeTrack( context, serializer, trackUri );

               serializer.text( "\n" );
               serializer.startTag( "http://www.topografix.com/GPX/1/1", "trk" );
               serializer.text( "\n" );
               serializer.startTag( "http://www.topografix.com/GPX/1/1", "name" );
               serializer.text( name );
               serializer.endTag( "http://www.topografix.com/GPX/1/1", "name" );

               // The list of segments in the track
               serializeSegments( context, serializer, Uri.withAppendedPath( trackUri, "segments" ) );

               serializer.text( "\n" );
               serializer.endTag( "http://www.topografix.com/GPX/1/1", "trk" );
               serializer.text( "\n" );
               serializer.endTag( "http://www.topografix.com/GPX/1/1", "gpx" );
               serializer.endDocument();
            }
            catch (IllegalArgumentException e)
            {
               e.printStackTrace();
            }
            catch (IllegalStateException e)
            {
               e.printStackTrace();
            }
            catch (IOException e)
            {
               e.printStackTrace();
            }
         }
      }).start();


   }

   private String serializeTrack(Context context,  XmlSerializer serializer, Uri trackUri ) throws IOException
   {
      ContentResolver resolver = context.getContentResolver();
      Cursor trackCursor = null;
      String name = null;

      try 
      {
         trackCursor = resolver.query(
               trackUri, 
               new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, 
               null, null, null);
         if( trackCursor.moveToFirst() )
         { 
            name = trackCursor.getString( 1 ) ;
            serializer.startTag( "http://www.topografix.com/GPX/1/1", "metadata" );
            serializer.startTag( "http://www.topografix.com/GPX/1/1", "time" );
            Date time = new Date( trackCursor.getLong( 2 ) );
            DateFormat formater = new SimpleDateFormat(DATETIME);
            serializer.text( formater.format( time ) );
            serializer.endTag( "http://www.topografix.com/GPX/1/1", "time" );
            serializer.endTag( "http://www.topografix.com/GPX/1/1", "metadata" );
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
      try {
         segmentCursor = resolver.query(
               segments, 
               new String[] { Segments._ID }, 
               null, null, null);
         if( segmentCursor.moveToFirst() )
         { 
            do
            {
               Uri waypoints = Uri.withAppendedPath( Uri.withAppendedPath( Segments.CONTENT_URI, ""+segmentCursor.getLong( 0 ) ), "waypoints" );
               serializer.text( "\n" );
               serializer.startTag( "http://www.topografix.com/GPX/1/1", "trkseg" );
               serializeWaypoints( context, serializer, waypoints );
               serializer.text( "\n" );
               serializer.endTag( "http://www.topografix.com/GPX/1/1", "trkseg" );
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

   private void serializeWaypoints( Context context, XmlSerializer serializer, Uri waypoints ) throws IOException
   {
      Cursor waypointsCursor = null; 
      ContentResolver resolver = context.getContentResolver();
      try {
         waypointsCursor = resolver.query(
               waypoints, 
               new String[] { Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints.TIME }, 
               null, null, null);
         if( waypointsCursor.moveToFirst() )
         { 
            do
            {
               serializer.text( "\n" );
               serializer.startTag( "http://www.topografix.com/GPX/1/1", "trkpt" );
               serializer.attribute( null, "lon", waypointsCursor.getString( 0 ) );
               serializer.attribute( null, "lat", waypointsCursor.getString( 1 ) );
               serializer.text( "\n" );
               serializer.startTag( "http://www.topografix.com/GPX/1/1", "time" );
               Date time = new Date( waypointsCursor.getLong( 2 ) );
               DateFormat formater = new SimpleDateFormat(DATETIME);
               serializer.text( formater.format( time ) );
               serializer.endTag( "http://www.topografix.com/GPX/1/1", "time" );
               serializer.text( "\n" );
               serializer.endTag( "http://www.topografix.com/GPX/1/1", "trkpt" );
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
}