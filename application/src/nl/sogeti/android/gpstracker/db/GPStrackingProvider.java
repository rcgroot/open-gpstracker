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
package nl.sogeti.android.gpstracker.db;

import java.util.List;

import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.db.GPStracking.WaypointsColumns;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

/**
 * Goal of this Content Provider is to make the GPS Tracking information uniformly 
 * available to this application and even other applications. The GPS-tracking 
 * database can hold, tracks, segments or waypoints 
 * <p>
 * A track is an actual route taken from start to finish. All the GPS locations
 * collected are waypoints. Waypoints taken in sequence without loss of GPS-signal
 * are considered connected and are grouped in segments. A route is build up out of
 * 1 or more segments.
 * <p>
 * For example:<br>
 * <code>content://nl.sogeti.android.gpstracker/tracks</code>
 * is the URI that returns all the stored tracks or starts a new track on insert 
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2</code>
 * is the URI string that would return a single result row, the track with ID = 23. 
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments</code> is the URI that returns 
 * all the stored segments of a track with ID = 2 or starts a new segment on insert 
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/waypoints</code> is the URI that returns 
 * all the stored waypoints of a track with ID = 2
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments</code> is the URI that returns 
 * all the stored segments of a track with ID = 2 
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments/3</code> is
 * the URI string that would return a single result row, the segment with ID = 3 of a track with ID = 2 . 
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments/1/waypoints</code> is the URI that 
 * returns all the waypoints of a segment 1 of track 2.
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments/1/waypoints/52</code> is the URI string that 
 * would return a single result row, the waypoint with ID = 52
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class GPStrackingProvider extends ContentProvider
{

   private static final String LOG_TAG = GPStrackingProvider.class.getName();

   /* Action types as numbers for using the UriMatcher */
   private static final int TRACKS = 1;
   private static final int TRACK_ID = 2;
   private static final int TRACK_WAYPOINTS = 3;
   private static final int SEGMENTS = 8;
   private static final int SEGMENT_ID = 4;
   private static final int WAYPOINTS = 7;
   private static final int WAYPOINT_ID = 6;

   private static final String TAG = GPStrackingProvider.class.getName();

   private static UriMatcher sURIMatcher = new UriMatcher( UriMatcher.NO_MATCH );

   /**
    * Although it is documented that in addURI(null, path, 0) "path" should be an absolute path this does not seem to work. A relative path gets the jobs done and matches an absolute path.
    */
   static
   {
      GPStrackingProvider.sURIMatcher = new UriMatcher( UriMatcher.NO_MATCH );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "tracks", GPStrackingProvider.TRACKS );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "tracks/#", GPStrackingProvider.TRACK_ID );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "tracks/#/waypoints", GPStrackingProvider.TRACK_WAYPOINTS );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "tracks/#/segments", GPStrackingProvider.SEGMENTS );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "tracks/#/segments/#", GPStrackingProvider.SEGMENT_ID );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "tracks/#/segments/#/waypoints", GPStrackingProvider.WAYPOINTS );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "tracks/#/segments/#/waypoints/#", GPStrackingProvider.WAYPOINT_ID );
   }

   private DatabaseHelper mDbHelper;

   /**
    * (non-Javadoc)
    * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
    */
   @Override
   public int delete( Uri uri, String selection, String[] selectionArgs )
   {
      int match = GPStrackingProvider.sURIMatcher.match( uri );
      int affected = 0; 
      switch( match )
      {
         case GPStrackingProvider.TRACK_ID:
            affected = this.mDbHelper.deleteTrack( new Long( uri.getLastPathSegment() ).longValue() );
            break;
         default:
            affected = 0;
            break;   
      }
      return affected;
   }

   /**
    * (non-Javadoc)
    * @see android.content.ContentProvider#getType(android.net.Uri)
    */
   @Override
   public String getType( Uri uri )
   {
      int match = GPStrackingProvider.sURIMatcher.match( uri );
      String mime = null;
      switch (match)
      {
         case TRACKS:
            mime = Tracks.CONTENT_TYPE;
            break;
         case TRACK_ID:
            mime = Tracks.CONTENT_ITEM_TYPE;
            break;
         case SEGMENTS:
            mime = Segments.CONTENT_TYPE;
            break;
         case SEGMENT_ID:
            mime = Segments.CONTENT_ITEM_TYPE;
            break;
         case WAYPOINTS:
            mime = Waypoints.CONTENT_TYPE;
            break;
         case WAYPOINT_ID:
            mime = Waypoints.CONTENT_ITEM_TYPE;
            break;
      }
      return mime;
   }

   /**
    * (non-Javadoc)
    * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
    */
   @Override
   public Uri insert( Uri uri, ContentValues values )
   {
      //Log.d( TAG, "insert on "+uri );
      Uri insertedUri = null;
      int match = GPStrackingProvider.sURIMatcher.match( uri );
      List<String> pathSegments = null;
      long trackId;
      long segmentId;
      long waypointId = -1;
      switch (match)
      {
         case WAYPOINTS:
            pathSegments = uri.getPathSegments();
            trackId = Integer.parseInt( pathSegments.get( 1 ) );
            segmentId = Integer.parseInt( pathSegments.get( 3 ) );
            
            Location loc = new Location( TAG );
            
            Double latitude = values.getAsDouble( Waypoints.LATITUDE );
            Double longitude = values.getAsDouble( Waypoints.LONGITUDE );
            Long time = values.getAsLong( Waypoints.TIME );
            Float speed = values.getAsFloat( Waypoints.SPEED );
            if( time == null )
            {
               time = System.currentTimeMillis();
            }
            if( speed == null )
            {
               speed  = 0f;
            }
            loc.setLatitude( latitude );
            loc.setLongitude( longitude );
            loc.setTime( time );
            loc.setSpeed( speed );
            
            if( values.containsKey( Waypoints.ACCURACY ) )
            {
               loc.setAccuracy( values.getAsFloat( Waypoints.ACCURACY ) );
            }
            if( values.containsKey( Waypoints.ALTITUDE ) )
            {
               loc.setAltitude( values.getAsDouble( Waypoints.ALTITUDE ) );
               
            }
            if( values.containsKey( Waypoints.BEARING ) )
            {
               loc.setBearing( values.getAsFloat( Waypoints.BEARING ) );
            }
            waypointId = this.mDbHelper.insertWaypoint( 
                  trackId, 
                  segmentId, 
                  loc );
            insertedUri = ContentUris.withAppendedId( uri, waypointId );
            break;
         case SEGMENTS:
            pathSegments = uri.getPathSegments();
            trackId = Integer.parseInt( pathSegments.get( 1 ) );
            segmentId = this.mDbHelper.toNextSegment( trackId );
            insertedUri = ContentUris.withAppendedId( uri, segmentId );
            break;
         case TRACKS:
            String name = ( values == null ) ? "" : values.getAsString( Tracks.NAME );
            trackId = this.mDbHelper.toNextTrack( name );
            insertedUri = ContentUris.withAppendedId( uri, trackId );
            break;
         default:
            Log.e( GPStrackingProvider.LOG_TAG, "Unable to match the URI:" + uri.toString() );
            insertedUri =  null;
            break;
      }
      return insertedUri;
   }

   /**
    * (non-Javadoc)
    * @see android.content.ContentProvider#onCreate()
    */
   @Override
   public boolean onCreate()
   {

      if (this.mDbHelper == null)
      {
         this.mDbHelper = new DatabaseHelper( getContext() );
      }
      return true;
   }

   /**
    * (non-Javadoc)
    * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
    */
   @Override
   public Cursor query( Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder )
   {
//      Log.d( TAG, "Query on Uri:"+uri ); 
     
      int match = GPStrackingProvider.sURIMatcher.match( uri );

      String tableName = null;
      String whereclause = null;
      List<String> pathSegments = uri.getPathSegments();
      switch (match)
      {
         case TRACKS:
            tableName = Tracks.TABLE;
            break;
         case TRACK_ID:
            tableName = Tracks.TABLE;
            whereclause = Tracks._ID + " = " + new Long( pathSegments.get( 1 ) ).longValue();
            break;
         case SEGMENTS:
            tableName = Segments.TABLE;
            whereclause = Segments.TRACK + " = " + new Long( pathSegments.get( 1 ) ).longValue();
            break;
         case SEGMENT_ID:
            tableName = Segments.TABLE;
            whereclause = Segments.TRACK + " = " + new Long( pathSegments.get( 1 ) ).longValue()
              + " and " + Segments._ID   + " = " + new Long( pathSegments.get( 3 ) ).longValue();
            break;
         case WAYPOINTS:
            tableName = Waypoints.TABLE;
            whereclause = Waypoints.SEGMENT + " = " + new Long( pathSegments.get( 3 ) ).longValue();
            break;
         case WAYPOINT_ID:
            tableName = Waypoints.TABLE;
            whereclause = Waypoints.SEGMENT + " = " + new Long( pathSegments.get( 3 ) ).longValue()
              + " and " + Waypoints._ID     + " = " + new Long( pathSegments.get( 5 ) ).longValue();
            break;
         case TRACK_WAYPOINTS:
            tableName = Waypoints.TABLE + " INNER JOIN " + Segments.TABLE + " ON "+ Segments.TABLE+"."+Segments._ID +"=="+ Waypoints.SEGMENT;
            whereclause = Segments.TRACK + " = " + new Long( pathSegments.get( 1 ) ).longValue();
            break;
         default:
            Log.e( GPStrackingProvider.LOG_TAG, "Unable to come to an action in the query uri: " + uri.toString() );
            return null;
      }

      // SQLiteQueryBuilder is a helper class that creates the
      // proper SQL syntax for us.
      SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();

      // Set the table we're querying.
      qBuilder.setTables( tableName );

      // If the query ends in a specific record number, we're
      // being asked for a specific record, so set the
      // WHERE clause in our query.
      if (whereclause != null)
      {
         qBuilder.appendWhere( whereclause );
      }

      // Make the query.
      SQLiteDatabase mDb = this.mDbHelper.getWritableDatabase();
      Cursor c = qBuilder.query( mDb, projection, selection, selectionArgs, null, null, null );
      c.setNotificationUri( getContext().getContentResolver(), uri );
      return c;
   }

   /**
    * (non-Javadoc)
    * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
    */
   @Override
   public int update( Uri uri, ContentValues givenValues, String selection, String[] selectionArgs )
   {
      int updates = -1 ;

      int match = GPStrackingProvider.sURIMatcher.match( uri );

      String tableName;
      String whereclause;
      ContentValues args = new ContentValues();
      Uri notifyUri;
      
      switch (match)
      {
         case TRACK_ID:
            tableName = Tracks.TABLE;
            long trackId = new Long( uri.getLastPathSegment() ).longValue();
            whereclause = Tracks._ID + " = " + trackId;
            args.put( Tracks.NAME, givenValues.getAsString( Tracks.NAME ) );
            notifyUri = ContentUris.withAppendedId( Tracks.CONTENT_URI, trackId ) ;
            break;
         default:
            Log.e( GPStrackingProvider.LOG_TAG, "Unable to come to an action in the query uri" + uri.toString() );
            return -1;
      }
      
      // Execute the query.
      SQLiteDatabase mDb = this.mDbHelper.getWritableDatabase();
      updates = mDb.update(tableName, args , whereclause, null) ;
      
      ContentResolver resolver = this.getContext().getContentResolver();
      resolver.notifyChange( notifyUri, null );   
      
      return updates;
   }

}
