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

import java.util.Date;

import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.SegmentsColumns;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.TracksColumns;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.db.GPStracking.WaypointsColumns;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Class to hold bare-metal database operations exposed as functionality blocks To be used by database adapters, like a content provider, that implement a required functionality set
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
class DatabaseHelper extends SQLiteOpenHelper
{

   private static final String LOG_TAG = DatabaseHelper.class.getName();
   private Context mContext;
   
   
   public DatabaseHelper(Context context)
   {
      super( context, GPStracking.DATABASE_NAME, null, GPStracking.DATABASE_VERSION );
      this.mContext = context;
    
   }

   private long getCurrentSegment( long trackId )
   {
      long segmentId = 0;
      SQLiteDatabase mDb = getWritableDatabase();
      Cursor mCursor = mDb.query( Segments.TABLE, new String[] { "max(" + BaseColumns._ID + ")" }, SegmentsColumns.TRACK + "=" + trackId, null, null, null, null, "1" );
      if (mCursor.moveToFirst())
      {
         segmentId = mCursor.getLong( 0 );
         mCursor.close();
         mDb.close();
      }
      return segmentId;
   }

   private long getCurrentTrack()
   {

      long trackId = 0;
      SQLiteDatabase mDb = getWritableDatabase();
      Cursor mCursor = mDb.query( Tracks.TABLE, new String[] { "max(" + BaseColumns._ID + ")" }, null, null, null, null, null, "1" );
      if (mCursor.moveToFirst())
      {
         trackId = mCursor.getLong( 0 );
         mCursor.close();
         mDb.close();
      }
      return trackId;
   }

   /**
    * Creates a waypoint under the current track segment with the current time on which the waypoint is reached
    * 
    * @param latitude latitude
    * @param longitude longitude
    * @param time time
    * @return
    */
   long insertWaypoint( double latitude, double longitude )
   {
      Log.d( DatabaseHelper.LOG_TAG, "inserting " + latitude + " " + longitude );

      long currentSegmentId = getCurrentSegment( getCurrentTrack() );

      long time = ( new Date() ).getTime();
      ContentValues args = new ContentValues();
      args.put( WaypointsColumns.LATITUDE, latitude );
      args.put( WaypointsColumns.LONGITUDE, longitude );
      args.put( WaypointsColumns.TIME, time );
      args.put( WaypointsColumns.SEGMENT, currentSegmentId );

      SQLiteDatabase mDb = getWritableDatabase();
      long waypointId = mDb.insert( Waypoints.TABLE, null, args );
      mDb.close();
      
      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath( Waypoints.CONTENT_URI, ""+waypointId ) ;
      resolver.notifyChange( notifyUri, null );

      updateSegmentEndtime( time );
      return waypointId;
   }

   /*
    * (non-Javadoc)
    * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
    */
   @Override
   public void onCreate( SQLiteDatabase db )
   {
      db.execSQL( Waypoints.CREATE_STATEMENT );
      db.execSQL( Segments.CREATE_STATMENT );
      db.execSQL( Tracks.CREATE_STATEMENT );
   }

   /*
    * (non-Javadoc)
    * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
    */
   @Override
   public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion )
   {
      Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to "
            + newVersion + ", which will destroy all old data");
      db.execSQL("DROP TABLE IF EXISTS "+Tracks.TABLE);
      db.execSQL("DROP TABLE IF EXISTS "+Segments.TABLE);
      db.execSQL("DROP TABLE IF EXISTS "+Waypoints.TABLE);
      onCreate(db);
   }

   /**
    * Moves to a fresh segment to which waypoints can be connected
    * @return
    */
   long toNextSegment()
   {

      long segmentId = toNextSegment( getCurrentTrack() );

      return segmentId;
   }

   /**
    * Moves to a fresh segment to which waypoints can be connected
    * @return
    */
   private long toNextSegment( long trackId )
   {
      Log.d( DatabaseHelper.LOG_TAG, "Moving to the next segment" );

      long currentTime = new Date().getTime();
      ContentValues args = new ContentValues();
      args.put( SegmentsColumns.TRACK, trackId );
      args.put( SegmentsColumns.STARTTIME, currentTime );
      args.put( SegmentsColumns.ENDTIME, currentTime );

      SQLiteDatabase mDb = getWritableDatabase();
      mDb.insert( Segments.TABLE, null, args );
      mDb.close();

      long segmentId = getCurrentSegment( trackId );
      
      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath( Segments.CONTENT_URI, ""+segmentId ) ;
      resolver.notifyChange( notifyUri, null );
      

      return segmentId;
   }

   /**
    * Move to a fresh track with a new first segment for this track
    * @return
    */
   long toNextTrack(String name)
   {
      Log.d( DatabaseHelper.LOG_TAG, "Moving to the next track" );

      ContentValues args = new ContentValues();
      long currentTime = new Date().getTime();
      args.put( TracksColumns.STARTTIME, currentTime );
      args.put( TracksColumns.ENDTIME, currentTime );
      args.put( TracksColumns.NAME, name );
      SQLiteDatabase mDb = getWritableDatabase();
      mDb.insert( Tracks.TABLE, null, args );
      mDb.close();

      long trackId = getCurrentTrack();
      toNextSegment( trackId );
      
      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath( Tracks.CONTENT_URI, ""+trackId ) ;
      resolver.notifyChange( notifyUri, null );

      return trackId;
   }

   /**
    * Adjust the current segment to a new endtime
    * 
    * @param time
    */
   private void updateSegmentEndtime( long time )
   {
      Log.d( DatabaseHelper.LOG_TAG, "updateSegmentEndtime to " + time );

      long currentSegmentId = getCurrentSegment( getCurrentTrack() );

      ContentValues args = new ContentValues();
      args.put( SegmentsColumns.ENDTIME, time );

      SQLiteDatabase mDb = getWritableDatabase();
      mDb.update( Segments.TABLE, args, BaseColumns._ID + "=" + currentSegmentId, null );
      mDb.close();
      
      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath( Segments.CONTENT_URI, ""+currentSegmentId ) ;
      resolver.notifyChange( notifyUri, null );

      updateTrackEndtime( time );
   }

   /**
    * Adjust the current track to a new endtime
    * 
    * @param time
    */
   private void updateTrackEndtime( long time )
   {
      Log.d( DatabaseHelper.LOG_TAG, "updateTrackEndtime to " + time );

      long currentTrackId = getCurrentTrack();

      ContentValues args = new ContentValues();
      args.put( TracksColumns.ENDTIME, time );

      SQLiteDatabase mDb = getWritableDatabase();
      mDb.update( Tracks.TABLE, args, BaseColumns._ID + "=" + currentTrackId, null );
      mDb.close();
      
      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath( Tracks.CONTENT_URI, ""+currentTrackId ) ;
      Log.d( DatabaseHelper.LOG_TAG, "notifyChange to " + notifyUri );
      resolver.notifyChange( notifyUri, null );
   }

}
