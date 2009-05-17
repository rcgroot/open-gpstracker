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
package nl.sogeti.android.gpstracker.tests.db;

import junit.framework.Assert;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.db.GPStrackingProvider;
import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.db.GPStracking.WaypointsColumns;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Basically test that the functions offered by the content://nl.sogeti.android.gpstracker does what is documented.
 * 
 *
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class GPStrackingProviderTest extends ProviderTestCase2<GPStrackingProvider>
{

   private ContentResolver mResolver;

   public GPStrackingProviderTest()
   {
      super( GPStrackingProvider.class, GPStracking.AUTHORITY );

   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      this.mResolver = getMockContentResolver();
   }
   
   @SmallTest
   public void testQuerySegmentsCursor()
   {
      Cursor cursor = this.mResolver.query( Segments.CONTENT_URI, null, null, null, null );
      Assert.assertNotNull( "Curson should not be null", cursor );
      Assert.assertTrue( "Curson should be a cursor", cursor instanceof android.database.Cursor );
      Assert.assertEquals( "No segments are loaded", 0, cursor.getCount() );
      cursor.close();
   }
   
   @SmallTest
   public void testQueryTracksCursor()
   {
      Cursor cursor = this.mResolver.query( Tracks.CONTENT_URI, null, null, null, null );
      Assert.assertNotNull( "Curson should not be null", cursor );
      Assert.assertTrue( "Curson should be a cursor", cursor instanceof android.database.Cursor );
      Assert.assertEquals( "No tracks are loaded", 0, cursor.getCount() );
      cursor.close();
   }

   @SmallTest
   public void testQueryWaypointsCursor()
   {
      Cursor cursor = this.mResolver.query( Waypoints.CONTENT_URI, null, null, null, null );
      Assert.assertNotNull( "Curson should not be null", cursor );
      Assert.assertTrue( "Curson should be a cursor", cursor instanceof android.database.Cursor );
      Assert.assertEquals( "No waypoints are loaded", 0, cursor.getCount() );
      cursor.close();
   }

   @SmallTest
   public void testStartTracks()
   {
      Uri firstTrack = Uri.parse( Tracks.CONTENT_URI + "/1" );
      Uri secondTrack = Uri.parse( Tracks.CONTENT_URI + "/2" );
      Uri newTrackUri;

      newTrackUri = this.mResolver.insert( Tracks.CONTENT_URI, null );
      Assert.assertEquals( "Fresh new track 1", firstTrack, newTrackUri );

      newTrackUri = this.mResolver.insert( Tracks.CONTENT_URI, null );
      Assert.assertEquals( "Fresh new track 2", secondTrack, newTrackUri );
   }
   
   /**
    * Create a track with a name
    */
   @SmallTest
   public void testStartTracksWithName()
   {
      String testname = "testStartTracksWithName";
      
      ContentValues values = new ContentValues();
      values.put( Tracks.NAME, testname);
      Uri newTrackUri = this.mResolver.insert( Tracks.CONTENT_URI, values );
      
      Cursor trackCursor = this.mResolver.query( newTrackUri, new String[] { Tracks.NAME }, null, null, null );
      Assert.assertTrue( "Should be possble to move to the first track", trackCursor.moveToFirst() );
      Assert.assertEquals( "This track query should have 1 track", 1, trackCursor.getCount() );
      Assert.assertEquals( "Name should be the same", testname, trackCursor.getString( 0 ) );
   }
   
   /**
    * Create a track with a name
    */
   @SmallTest
   public void testUpdateTrackWithName()
   {
      Cursor trackCursor;
      Uri newTrackUri;
      String testname = "testUpdateTrackWithName";
     
      newTrackUri = this.mResolver.insert( Tracks.CONTENT_URI, null );      
      trackCursor = this.mResolver.query( newTrackUri, new String[] { Tracks.NAME }, null, null, null );
      Assert.assertTrue( "Should be possble to move to the first track", trackCursor.moveToFirst() );
      Assert.assertEquals( "This track query should have 1 track", 1, trackCursor.getCount() );
      Assert.assertEquals( "Name should be the same", "", trackCursor.getString( 0 ) );
      
     
      ContentValues values = new ContentValues();
      values.put( Tracks.NAME, testname);
      int updates = this.mResolver.update( newTrackUri, values, null, null );      
      trackCursor.requery();
      Assert.assertEquals( "One row should be updated", 1, updates );
      Assert.assertTrue( "Should be possble to move to the first track", trackCursor.moveToFirst() );
      Assert.assertEquals( "This track query should have 1 track", 1, trackCursor.getCount() );
      Assert.assertEquals( "Name should be the same", testname, trackCursor.getString( 0 ) );
   }


   /**
    * Start a track, insert 2 waypoints and expect 1 track with 1 segment with the 2 waypoints that where inserted
    */
   @SmallTest
   public void testTrackWaypointWaypoint()
   {
      ContentValues wp = new ContentValues();
      wp.put( WaypointsColumns.LONGITUDE, new Double( 200d ) );
      wp.put( WaypointsColumns.LATITUDE, new Double( 100d ) );

      // E.g. returns: content://nl.sogeti.android.gpstracker/tracks/2
      Uri trackUri = this.mResolver.insert( Tracks.CONTENT_URI, null );
      this.mResolver.insert( Waypoints.CONTENT_URI, wp );
      this.mResolver.insert( Waypoints.CONTENT_URI, wp );

      // E.g. content://nl.sogeti.android.gpstracker/tracks/2/segments
      Uri segments = Uri.withAppendedPath( trackUri, "/segments" );
      Cursor tracksCursor = this.mResolver.query( segments, new String[] { Segments._ID }, null, null, null );
      tracksCursor.moveToFirst();
      int segmentId = tracksCursor.getInt( 0 );
      Assert.assertEquals( "This track should have a segment", 1, tracksCursor.getCount() );
      Assert.assertTrue( "Should be possble to move to the first track", tracksCursor.moveToFirst() );

      // E.g. content://nl.sogeti.android.gpstracker/segments/1/waypoints
      Uri waypoints = Uri.withAppendedPath( Segments.CONTENT_URI, "/" + segmentId + "/waypoints" );
      Cursor waypointCursor = this.mResolver.query( waypoints, new String[] { WaypointsColumns.LONGITUDE, WaypointsColumns.LATITUDE }, null, null, null );
      Assert.assertEquals( "This segment should list waypoints", 2, waypointCursor.getCount() );
      Assert.assertTrue( "Should be possble to move to the first waypoint", waypointCursor.moveToFirst() );

      do
      {
         Assert.assertEquals( "First Longitude", 200d, waypointCursor.getDouble( 0 ) );
         Assert.assertEquals( "First Latitude", 100d, waypointCursor.getDouble( 1 ) );
      }
      while (waypointCursor.moveToNext());
   }
   
   /**
    * Create a track with a name
    */
   @SmallTest
   public void testMakeTwoTracks()
   {
      String testname = "track";
      Uri newTrackUri;
      ContentValues values; 
      Cursor trackCursor ;
      double coord = 1d;
      ContentValues wp ;
      
      Cursor cursor = this.mResolver.query( Waypoints.CONTENT_URI, new String[] { }, null, null, null );
      Assert.assertEquals( "We should now have 0 waypoints", 0, cursor.getCount() );
      cursor.close();
      
      wp = new ContentValues();
      wp.put( WaypointsColumns.LONGITUDE, new Double( coord ) );
      wp.put( WaypointsColumns.LATITUDE, new Double( coord ) );
      this.mResolver.insert( Waypoints.CONTENT_URI, wp );
      coord++;
      
      values = new ContentValues();
      values.put( Tracks.NAME, testname+1 );
      newTrackUri = this.mResolver.insert( Tracks.CONTENT_URI, values );
      
      wp = new ContentValues();
      wp.put( WaypointsColumns.LONGITUDE, new Double( coord ) );
      wp.put( WaypointsColumns.LATITUDE, new Double( coord ) );
      this.mResolver.insert( Waypoints.CONTENT_URI, wp );
      coord++;
      
      trackCursor = this.mResolver.query( newTrackUri, new String[] { Tracks.NAME }, null, null, null );
      Assert.assertTrue( "Should be possble to move to the first track", trackCursor.moveToFirst() );
      Assert.assertEquals( "This track query should have 1 track", 1, trackCursor.getCount() );
      Assert.assertEquals( "Name should be the same", testname+1 , trackCursor.getString( 0 ) );
      trackCursor.close();
      
      values = new ContentValues();
      values.put( Tracks.NAME, testname+2 );
      newTrackUri = this.mResolver.insert( Tracks.CONTENT_URI, values );
      trackCursor = this.mResolver.query( newTrackUri, new String[] { Tracks.NAME }, null, null, null );
      Assert.assertTrue( "Should be possble to move to the first track", trackCursor.moveToFirst() );
      Assert.assertEquals( "This track query should have 1 track", 1, trackCursor.getCount() );
      Assert.assertEquals( "Name should be the same", testname+2, trackCursor.getString( 0 ) );
      trackCursor.close();
      
      wp = new ContentValues();
      wp.put( WaypointsColumns.LONGITUDE, new Double( coord ) );
      wp.put( WaypointsColumns.LATITUDE, new Double( coord ) );
      this.mResolver.insert( Waypoints.CONTENT_URI, wp );
      coord++;
      wp = new ContentValues();
      wp.put( WaypointsColumns.LONGITUDE, new Double( coord ) );
      wp.put( WaypointsColumns.LATITUDE, new Double( coord ) );
      this.mResolver.insert( Waypoints.CONTENT_URI, wp );
      coord++;
      
      cursor = this.mResolver.query( Segments.CONTENT_URI, new String[] { Segments.TRACK }, null, null, null );
      Assert.assertEquals( "We should now have 2 segements", 2, cursor.getCount() );
      Assert.assertTrue( "Working", cursor.moveToFirst() );
      
      Assert.assertEquals("First segment is first track", 1, cursor.getLong( 0 ) );
      Assert.assertTrue( "Working", cursor.moveToNext() );
      Assert.assertEquals("Second segment is second track", 2,  cursor.getLong( 0 ));
      cursor.close();
      cursor = this.mResolver.query( Waypoints.CONTENT_URI, new String[] { }, null, null, null );
      Assert.assertEquals( "We should now have 4 waypoints", 4, cursor.getCount() );
      cursor.close();
   }
   
   @SmallTest
   public void testDeleteEmptyTrack()
   {
      // E.g. returns: content://nl.sogeti.android.gpstracker/tracks/2
      Uri trackUri = this.mResolver.insert( Tracks.CONTENT_URI, null );
      Cursor trackCursor = this.mResolver.query( trackUri, new String[] { Tracks._ID }, null, null, null );
      Assert.assertEquals( "One track inserted", 1, trackCursor.getCount() );

      int affected = this.mResolver.delete( trackUri, null, null);
      Assert.assertEquals( "One track and one segment deleted", 2, affected );
      
      trackCursor.requery();
      
      Assert.assertEquals( "No track left", 0, trackCursor.getCount() );
      trackCursor.close();
   }
   
   @SmallTest
   public void testDeleteSimpleTrack()
   {
      ContentValues wp ;
      double coord = 1d;
      
      // E.g. returns: content://nl.sogeti.android.gpstracker/tracks/2
      Uri trackUri = this.mResolver.insert( Tracks.CONTENT_URI, null );
      Cursor trackCursor = this.mResolver.query( trackUri, new String[] { Tracks._ID }, null, null, null );
      Cursor segmentCursor = this.mResolver.query( Uri.withAppendedPath( trackUri, "segments" ), new String[] { Segments._ID }, null, null, null );
      
      Assert.assertEquals( "One track created", 1, trackCursor.getCount() );
      Assert.assertEquals( "One segment created", 1, segmentCursor.getCount() );
      
      // Stuff 2 waypoints as the track contents
      wp = new ContentValues();
      wp.put( WaypointsColumns.LONGITUDE, new Double( coord ) );
      wp.put( WaypointsColumns.LATITUDE, new Double( coord ) );
      Uri wp1 = this.mResolver.insert( Waypoints.CONTENT_URI, wp );
      wp = new ContentValues();
      wp.put( WaypointsColumns.LONGITUDE, new Double( coord ) );
      wp.put( WaypointsColumns.LATITUDE, new Double( coord ) );
      Uri wp2 = this.mResolver.insert( Waypoints.CONTENT_URI, wp );
      
      // Pivot of the test case THE DELETE
      int affected = this.mResolver.delete( trackUri, null, null);
      
      Assert.assertEquals( "One track, one segments and two waypoints deleted", 4, affected );
      Assert.assertTrue("The cursor to the track is still valid", trackCursor.requery() );       
      Assert.assertEquals( "No track left", 0, trackCursor.getCount() );
      Assert.assertTrue("The cursor to the segments is still valid", segmentCursor.requery() );
      Assert.assertEquals( "No segments left", 0, segmentCursor.getCount() );
      
      Cursor wpCursor = this.mResolver.query( wp1, null, null, null, null );
      Assert.assertEquals( "Waypoint 1 is gone", 0, wpCursor.getCount() );
      wpCursor.close();
      
      wpCursor = this.mResolver.query( wp2, null, null, null, null );    
      Assert.assertEquals( "Waypoint 2 is gone", 0, wpCursor.getCount() );   
      wpCursor.close();
      
      trackCursor.close();
      segmentCursor.close();
   }
   
}
