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

import nl.sogeti.android.gpstracker.db.GPStracking.Media;
import nl.sogeti.android.gpstracker.db.GPStracking.MediaColumns;
import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.TracksColumns;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.db.GPStracking.WaypointsColumns;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

/**
 * Class to hold bare-metal database operations exposed as functionality blocks To be used by database adapters, like a content provider, that implement a required functionality set
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class DatabaseHelper extends SQLiteOpenHelper
{
   private Context mContext;
   private final static String TAG = "OGT.DatabaseHelper";

   public DatabaseHelper(Context context)
   {
      super( context, GPStracking.DATABASE_NAME, null, GPStracking.DATABASE_VERSION );
      this.mContext = context;
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
      db.execSQL( Media.CREATE_STATEMENT );
   }

   /**
    * 
    * Will update version 1 through 5 to version 8
    * 
    * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
    * @see GPStracking.DATABASE_VERSION
    */
   @Override
   public void onUpgrade( SQLiteDatabase db, int current, int targetVersion )
   {
      Log.i( TAG, "Upgrading db from "+current+" to "+targetVersion );
      if( current <= 5 )                      // From 1-5 to 6 (these before are the same before) 
      {
         current = 6;
      } 
      if( current == 6)                     // From 6 to 7 ( no changes ) 
      {
         current = 7;
      }
      if( current == 7)                     // From 7 to 8 ( more waypoints data ) 
      {
         for( String statement : Waypoints.UPGRADE_STATEMENT_7_TO_8 )
         {
            db.execSQL( statement );
         }
         current = 8;
      }
      if( current == 8)                     // From 8 to 9 ( media Uri data ) 
      {
         db.execSQL( Media.CREATE_STATEMENT );
         current = 9;
      }
   }
   
   public void vacuum()
   {
      new Thread(){
         @Override
         public void run()
         {
            SQLiteDatabase sqldb = getWritableDatabase();
            sqldb.execSQL( "VACUUM" );
         }
      }.start();

   }
   
   int bulkInsertWaypoint( long trackId, long segmentId, ContentValues[] valuesArray )
   {
      if( trackId < 0 || segmentId < 0 )
      {
         throw new IllegalArgumentException( "Track and segments may not the less then 0." );
      }
      int inserted = 0;
      
      SQLiteDatabase sqldb = getWritableDatabase();
      sqldb.beginTransaction();
      try 
      {
         for( ContentValues args : valuesArray )
         {
            args.put( Waypoints.SEGMENT, segmentId );
            
            long id = sqldb.insert( Waypoints.TABLE, null, args );
            if( id >= 0 )
            {
               inserted++;
            }
         }
         sqldb.setTransactionSuccessful();

      }
      finally
      {
         sqldb.endTransaction();
      }
      
      return inserted;
   }

   /**
    * Creates a waypoint under the current track segment with the current time on which the waypoint is reached
    *  
    * @param track track
    * @param segment segment
    * @param latitude latitude
    * @param longitude longitude
    * @param time time
    * @param speed the measured speed
    * @return
    */
   long insertWaypoint( long trackId, long segmentId, Location location )
   {      
      if( trackId < 0 || segmentId < 0 )
      {
         throw new IllegalArgumentException( "Track and segments may not the less then 0." );
      }
      
      SQLiteDatabase sqldb = getWritableDatabase();
      
      ContentValues args = new ContentValues();
      args.put( WaypointsColumns.SEGMENT, segmentId );
      args.put( WaypointsColumns.TIME, location.getTime() );
      args.put( WaypointsColumns.LATITUDE, location.getLatitude() );
      args.put( WaypointsColumns.LONGITUDE, location.getLongitude() );
      args.put( WaypointsColumns.SPEED, location.getSpeed() );
      args.put( WaypointsColumns.ACCURACY, location.getAccuracy() );
      args.put( WaypointsColumns.ALTITUDE, location.getAltitude() );
      args.put( WaypointsColumns.BEARING, location.getBearing() );

      long waypointId = sqldb.insert( Waypoints.TABLE, null, args );

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath( Tracks.CONTENT_URI,  trackId+"/segments/"+segmentId+"/waypoints" );
      resolver.notifyChange( notifyUri, null );

      Log.d( TAG, "Waypoint stored: "+notifyUri);
      return waypointId;
   }
   
   long insertMedia( long trackId, long segmentId, long waypointId, String mediaUri )
   {
      if( trackId < 0 || segmentId < 0 || waypointId < 0 )
      {
         throw new IllegalArgumentException( "Track, segments and waypoint may not the less then 0." );
      }
      SQLiteDatabase sqldb = getWritableDatabase();
      
      ContentValues args = new ContentValues();
      args.put( MediaColumns.TRACK, trackId );
      args.put( MediaColumns.SEGMENT, segmentId );
      args.put( MediaColumns.WAYPOINT, waypointId );
      args.put( MediaColumns.URI, mediaUri );
      

//      Log.d( TAG, "Media stored in the datebase: "+mediaUri );

      long mediaId = sqldb.insert( Media.TABLE, null, args );

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath( Tracks.CONTENT_URI,  trackId+"/segments/"+segmentId+"/waypoints/"+waypointId+"/media" );
      resolver.notifyChange( notifyUri, null );
//      Log.d( TAG, "Notify: "+notifyUri );
      resolver.notifyChange( Media.CONTENT_URI, null );
//      Log.d( TAG, "Notify: "+Media.CONTENT_URI );
      
      return mediaId;
   }

   /**
    * Deletes a single track and all underlying segments and waypoints
    * 
    * @param trackId
    * @return
    */
   int deleteTrack( long trackId )
   {
      SQLiteDatabase sqldb = getWritableDatabase();
      int affected = 0;
      Cursor cursor = null; 
      long segmentId = -1;
      
      try 
      {
         sqldb.beginTransaction();
         cursor = sqldb.query( Segments.TABLE, new String[] { Segments._ID }, Segments.TRACK + "= ?", new String[]{ String.valueOf( trackId ) }, null, null, null, null );
         if (cursor.moveToFirst())
         {  
            do
            {
               segmentId = cursor.getLong( 0 ) ;
               affected += deleteSegment( sqldb, trackId, segmentId );
            }
            while( cursor.moveToNext() );
         }
         else 
         {
            Log.e(TAG, "Did not find the last active segment");
         }
         affected += sqldb.delete( Tracks.TABLE, Tracks._ID+"= ?", new String[]{ String.valueOf( trackId ) } );
         sqldb.setTransactionSuccessful();
      }
      finally
      {
         if( cursor!= null )
         {
            cursor.close();
         }
         sqldb.endTransaction();
      }
      
      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange( Tracks.CONTENT_URI, null );
      resolver.notifyChange( ContentUris.withAppendedId( Tracks.CONTENT_URI,  trackId), null );
      
      return affected ;
   }
   
   /**
    * @param mediaId
    * @return
    */
   int deleteMedia( long mediaId )
   {
      SQLiteDatabase sqldb = getWritableDatabase();

      Cursor cursor = null;
      long trackId = -1;
      long segmentId = -1;
      long waypointId = -1;
      try 
      {
         cursor  = sqldb.query( Media.TABLE, new String[] { Media.TRACK, Media.SEGMENT, Media.WAYPOINT }, Media._ID + "= ?", new String[]{ String.valueOf( mediaId ) }, null, null, null, null );
         if (cursor.moveToFirst())
         {
            trackId = cursor.getLong( 0 ) ;
            segmentId = cursor.getLong( 0 ) ;
            waypointId = cursor.getLong( 0 ) ;
         }
         else 
         {
            Log.e(TAG, "Did not find the last active segment");
         }
      }
      finally
      {
         if( cursor!= null )
         {
            cursor.close();
         }
      }
      
      int affected = sqldb.delete( Media.TABLE, Media._ID+"= ?", new String[]{ String.valueOf( mediaId ) } );
      
      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath( Tracks.CONTENT_URI,  trackId+"/segments/"+segmentId+"/waypoints/"+waypointId+"/media" );
      resolver.notifyChange( notifyUri, null );
      notifyUri = Uri.withAppendedPath( Tracks.CONTENT_URI,  trackId+"/segments/"+segmentId+"/media" );
      resolver.notifyChange( notifyUri, null );
      notifyUri = Uri.withAppendedPath( Tracks.CONTENT_URI,  trackId+"/media" );
      resolver.notifyChange( notifyUri, null );
      resolver.notifyChange( ContentUris.withAppendedId( Media.CONTENT_URI,  mediaId ), null );
      
      return affected ;
   }
   
   /**
    * Delete a segment and all member waypoints
    * 
    * @param sqldb The SQLiteDatabase in question
    * @param trackId The track id of this delete
    * @param segmentId The segment that needs deleting
    * @return
    */
   int deleteSegment(SQLiteDatabase sqldb, long trackId, long segmentId)
   {
      int affected = sqldb.delete( Segments.TABLE, Segments._ID +"= ?", new String[]{ String.valueOf( segmentId ) }  ) ;
      
      // Delete all waypoints from segments
      affected += sqldb.delete( Waypoints.TABLE, Waypoints.SEGMENT+"= ?", new String[]{ String.valueOf( segmentId ) } );
      // Delete all media from segment
      affected += sqldb.delete( 
            Media.TABLE, 
            Media.TRACK + "= ? AND "+ Media.SEGMENT + "= ?" , 
            new String[]{ String.valueOf( trackId ), String.valueOf( segmentId ) } );

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange( Uri.withAppendedPath( Tracks.CONTENT_URI, trackId+"/segments/"+segmentId ), null );
      resolver.notifyChange( Uri.withAppendedPath( Tracks.CONTENT_URI, trackId+"/segments" ), null );
      
      return affected ;
   }
  
   /**
    * Move to a fresh track with a new first segment for this track
    * @return
    */
   long toNextTrack( String name )
   {
      long currentTime = new Date().getTime();
      ContentValues args = new ContentValues();
      args.put( TracksColumns.NAME, name );
      args.put( TracksColumns.CREATION_TIME, currentTime );

      SQLiteDatabase sqldb = getWritableDatabase();
      long trackId = sqldb.insert( Tracks.TABLE, null, args );

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange( Tracks.CONTENT_URI, null );

      return trackId;
   }

   /**
    * Moves to a fresh segment to which waypoints can be connected
    * @return
    */
   long toNextSegment( long trackId )
   {
      SQLiteDatabase sqldb = getWritableDatabase();

      ContentValues args = new ContentValues();
      args.put( Segments.TRACK, trackId );
      long segmentId = sqldb.insert( Segments.TABLE, null, args );

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange( Uri.withAppendedPath( Tracks.CONTENT_URI, trackId+"/segments" ), null );

      return segmentId;
   }
}
