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
import android.util.Log;
/**
 * Class to hold bare-metal database operations exposed as functionality blocks To be used by database adapters, like a content provider, that implement a required functionality set
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
class DatabaseHelper extends SQLiteOpenHelper
{
   private Context mContext;
   private final static String LOG = "nl.sogeti.android.gpstracker.db.DatabaseHelper";

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
   }

   /*
    * (non-Javadoc)
    * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
    */
   @Override
   public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion )
   {
      db.execSQL("DROP TABLE IF EXISTS "+Tracks.TABLE);
      db.execSQL("DROP TABLE IF EXISTS "+Segments.TABLE);
      db.execSQL("DROP TABLE IF EXISTS "+Waypoints.TABLE);
      onCreate(db);
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
      SQLiteDatabase sqldb = getWritableDatabase();
      long segmentId = getCurrentSegment( sqldb );
      long trackId = getCurrentTrack(sqldb) ;
      
      long time = ( new Date() ).getTime();
      ContentValues args = new ContentValues();
      args.put( WaypointsColumns.LATITUDE, latitude );
      args.put( WaypointsColumns.LONGITUDE, longitude );
      args.put( WaypointsColumns.TIME, time );
      args.put( WaypointsColumns.SEGMENT, segmentId );

      long waypointId = sqldb.insert( Waypoints.TABLE, null, args );

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri;
      notifyUri = Uri.withAppendedPath( Waypoints.CONTENT_URI, ""+waypointId ) ;
      resolver.notifyChange( notifyUri, null );    
      notifyUri = Uri.withAppendedPath( Segments.CONTENT_URI, ""+segmentId ) ;
      resolver.notifyChange( notifyUri, null );    
      notifyUri = Uri.withAppendedPath( Tracks.CONTENT_URI, ""+trackId ) ;
      resolver.notifyChange( notifyUri, null );      

      return waypointId;
   }
   
   /**
    * Deletes a single track and all underlying segments and waypoints
    * 
    * @param trackId
    * @return
    */
   int deleteTrack(long trackId)
   {
      SQLiteDatabase sqldb = getWritableDatabase();
      int affected = 0;
      Cursor cursor = null; 
      long segmentId = -1;
      
      try 
      {
         cursor = sqldb.query( Segments.TABLE, new String[] { Segments._ID }, Segments.TRACK + "= ?", new String[]{ ""+trackId }, null, null, null, null );
         if (cursor.moveToFirst())
         {
            segmentId = cursor.getLong( 0 ) ;
            affected += deleteSegment( sqldb, segmentId );
            
         }
         else 
         {
            Log.e(LOG, "Did not find the last active segment");
         }
      }
      finally
      {
         if( cursor!= null )
         {
            cursor.close();
         }
      }
      affected += sqldb.delete( Tracks.TABLE, Tracks._ID+"= ?", new String[]{ ""+trackId } );
      
      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange( Tracks.CONTENT_URI, null );
      
      return affected ;
   }
   
   
   /**
    * Delete a segment and all member waypoints
    * 
    * @param segmentId
    * @return
    */
   int deleteSegment(SQLiteDatabase sqldb, long segmentId)
   {
      int affected = sqldb.delete( Segments.TABLE, Segments._ID +"= ?", new String[]{ ""+segmentId }  ) ;
      affected += sqldb.delete( Waypoints.TABLE, Waypoints.SEGMENT+"= ?", new String[]{ ""+segmentId } );

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange( Segments.CONTENT_URI, null );
      resolver.notifyChange( Waypoints.CONTENT_URI, null );
      
      return affected ;
   }
  
   /**
    * Move to a fresh track with a new first segment for this track
    * @return
    */
   long toNextTrack(String name)
   {
      long currentTime = new Date().getTime();
      ContentValues args = new ContentValues();
      args.put( TracksColumns.NAME, name );
      args.put( TracksColumns.CREATION_TIME, currentTime );

      SQLiteDatabase sqldb = getWritableDatabase();
      sqldb.insert( Tracks.TABLE, null, args );

      long trackId = getCurrentTrack( sqldb );

      toNextSegment();

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath( Tracks.CONTENT_URI, ""+trackId ) ;
      resolver.notifyChange( notifyUri, null );

      return trackId;
   }

   /**
    * Moves to a fresh segment to which waypoints can be connected
    * @return
    */
   long toNextSegment()
   {
      SQLiteDatabase sqldb = getWritableDatabase();
      long trackId = getCurrentTrack(sqldb) ;

      ContentValues args = new ContentValues();
      args.put( SegmentsColumns.TRACK, trackId );
      sqldb.insert( Segments.TABLE, null, args );

      long segmentId = getCurrentSegment( sqldb );

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath( Segments.CONTENT_URI, ""+segmentId ) ;
      resolver.notifyChange( notifyUri, null );

      return segmentId;
   }

   private long getCurrentSegment(SQLiteDatabase sqldb)
   {
      long trackId = getCurrentTrack(sqldb) ;
      long segmentId = 0;
      Cursor cursor = null; 
      try 
      {
         cursor = sqldb.query( Segments.TABLE, new String[] { "max(" + Segments._ID + ")" }, SegmentsColumns.TRACK + "=" + trackId, null, null, null, null, "1" );
         if (cursor.moveToFirst())
         {
            segmentId = cursor.getLong( 0 );
            
         }
         else 
         {
            Log.e(LOG, "Did not find the last active segment");
         }
      }
      finally
      {
         if( cursor!= null )
         {
            cursor.close();
         }
      }
      return segmentId;
   }

   private long getCurrentTrack(SQLiteDatabase sqldb)
   {

      long trackId = 0;
      Cursor mCursor = null; 
      try
      {
         mCursor = sqldb.query( Tracks.TABLE, new String[] { "max(" + Tracks._ID + ")" }, null, null, null, null, null, null );
         if (mCursor.moveToFirst())
         {
            trackId = mCursor.getLong( 0 );
            mCursor.close();
         }
         else 
         {
            Log.e(LOG, "Did not find the last active track");
         }
      }
      finally
      {
         if( mCursor!= null )
         {
            mCursor.close();
         }
      }
      return trackId;
   }
}
