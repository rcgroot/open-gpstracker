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

import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.SegmentsColumns;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.db.GPStracking.WaypointsColumns;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Goal of this Content Provider is to make the GPS Tracking information uniformly 
 * available to this application and even other applications. The GPS-tracking 
 * database can hold, tracks, segments or waypoints 
 * 
 * A track is an actual route taken from start to finish. All the GPS locations
 * collected are waypoints. Waypoints taken in sequence without loss of GPS-signal
 * are considered connected and are grouped in segments. A route is build up out of
 * 1 or more segments.
 * 
 * 
 * For example: 
 * content://nl.sogeti.android.gpstracker/tracks 
 * is the URI that returns all the stored tracks or starts a new track on insert 
 * 
 * content://nl.sogeti.android.gpstracker/tracks/23
 * is the URI string that would return a single result row, the track with ID = 23. 
 * 
 * content://nl.sogeti.android.gpstracker/tracks/2/segments is the URI that returns 
 * all the stored segments of a track with ID = 2 
 * 
 * content://nl.sogeti.android.gpstracker/segments is the URI that returns all the 
 * stored segments or starts a new segment on insert 
 * 
 * content://nl.sogeti.android.gpstracker/segments/3 is
 * the URI string that would return a single result row, the segment with ID = 3. 
 * 
 * content://nl.sogeti.android.gpstracker/segments/1/waypoints is the URI that 
 * returns all the waypoints of a segment
 * 
 * content://nl.sogeti.android.gpstracker/waypoints is the URI that returns all 
 * the waypoints 
 * 
 * content://nl.sogeti.android.gpstracker/waypoints/52 is the URI string that 
 * would return a single result row, the waypoint with ID = 52
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class GPStrackingProvider extends ContentProvider
{

   private static final String LOG_TAG = GPStrackingProvider.class.getName();

   /* Action types as numbers for using the UriMatcher */
   private static final int TRACK = 1;
   private static final int TRACK_ID = 2;
   private static final int TRACK_SEGMENT = 3;
   private static final int SEGMENT_ID = 4;
   private static final int SEGMENT_WAYPOINT = 5;
   private static final int WAYPOINT_ID = 6;
   private static final int WAYPOINT = 7;
   private static final int SEGMENT = 8;
   private static UriMatcher sURIMatcher = new UriMatcher( UriMatcher.NO_MATCH );

   /**
    * Although it is documented that in addURI(null, path, 0) "path" should be an absolute path this does not seem to work. A relative path gets the jobs done and matches an absolute path.
    */
   static
   {
      GPStrackingProvider.sURIMatcher = new UriMatcher( UriMatcher.NO_MATCH );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "tracks", GPStrackingProvider.TRACK );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "tracks/#", GPStrackingProvider.TRACK_ID );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "tracks/#/segments", GPStrackingProvider.TRACK_SEGMENT );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "segments", GPStrackingProvider.SEGMENT );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "segments/#", GPStrackingProvider.SEGMENT_ID );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "segments/#/waypoints", GPStrackingProvider.SEGMENT_WAYPOINT );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "waypoints", GPStrackingProvider.WAYPOINT );
      GPStrackingProvider.sURIMatcher.addURI( GPStracking.AUTHORITY, "waypoints/#", GPStrackingProvider.WAYPOINT_ID );
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
         case TRACK:
            mime = Tracks.CONTENT_TYPE;
            break;
         case TRACK_ID:
            mime = Tracks.CONTENT_ITEM_TYPE;
            break;
         case TRACK_SEGMENT:
            mime = Segments.CONTENT_TYPE;
            break;
         case SEGMENT:
            mime = Segments.CONTENT_TYPE;
            break;
         case SEGMENT_ID:
            mime = Segments.CONTENT_ITEM_TYPE;
            break;
         case SEGMENT_WAYPOINT:
            mime = Waypoints.CONTENT_TYPE;
            break;
         case WAYPOINT:
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
      Uri insertedUri = null;
      int match = GPStrackingProvider.sURIMatcher.match( uri );
      long id = -1;
      switch (match)
      {
         case WAYPOINT:
            id = this.mDbHelper.insertWaypoint( values.getAsDouble( WaypointsColumns.LATITUDE ), values.getAsDouble( WaypointsColumns.LONGITUDE ) );
            insertedUri = Uri.parse( "content://" + GPStracking.AUTHORITY + "/" + GPStracking.Waypoints.TABLE + "/" + id );
            break;
         case SEGMENT:
            id = this.mDbHelper.toNextSegment();
            insertedUri =  Uri.parse( "content://" + GPStracking.AUTHORITY + "/" + GPStracking.Segments.TABLE + "/" + id );
            break;
         case TRACK:
            String name = ( values == null ) ? "" : values.getAsString( Tracks.NAME );
            id = this.mDbHelper.toNextTrack(name);
            insertedUri =  Uri.parse( "content://" + GPStracking.AUTHORITY + "/" + GPStracking.Tracks.TABLE + "/" + id );
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
      int match = GPStrackingProvider.sURIMatcher.match( uri );

      String tableName = null;
      String whereclause = null;
      switch (match)
      {
         case TRACK:
            tableName = Tracks.TABLE;
            break;
         case TRACK_ID:
            tableName = Tracks.TABLE;
            whereclause = BaseColumns._ID + " = " + new Long( uri.getLastPathSegment() ).longValue();
            break;
         case TRACK_SEGMENT:
            tableName = Segments.TABLE;
            whereclause = SegmentsColumns.TRACK + " = " + new Long( uri.getPathSegments().get( 1 ) ).longValue();
            break;
         case SEGMENT:
            tableName = Segments.TABLE;
            break;
         case SEGMENT_ID:
            tableName = Segments.TABLE;
            whereclause = BaseColumns._ID + " = " + new Long( uri.getLastPathSegment() ).longValue();
            break;
         case SEGMENT_WAYPOINT:
            tableName = Waypoints.TABLE;
            whereclause = WaypointsColumns.SEGMENT + " = " + new Long( uri.getPathSegments().get( 1 ) ).longValue();
            break;
         case WAYPOINT:
            tableName = Waypoints.TABLE;
            break;
         case WAYPOINT_ID:
            tableName = Waypoints.TABLE;
            whereclause = BaseColumns._ID + " = " + new Long( uri.getLastPathSegment() ).longValue();
            break;
         default:
            Log.e( GPStrackingProvider.LOG_TAG, "Unable to come to an action in the query uri" + uri.toString() );
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
      
      switch (match)
      {
         case TRACK_ID:
            tableName = Tracks.TABLE;
            whereclause = BaseColumns._ID + " = " + new Long( uri.getLastPathSegment() ).longValue();
            args.put( Tracks.NAME, givenValues.getAsString( Tracks.NAME ) );
            break;
         default:
            Log.e( GPStrackingProvider.LOG_TAG, "Unable to come to an action in the query uri" + uri.toString() );
            return -1;
      }
      
      // Execute the query.
      SQLiteDatabase mDb = this.mDbHelper.getWritableDatabase();
      updates = mDb.update(tableName, args , whereclause, null) ;
      return updates;
   }

}
