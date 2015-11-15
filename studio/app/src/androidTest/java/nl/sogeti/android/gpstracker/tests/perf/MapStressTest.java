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
package nl.sogeti.android.gpstracker.tests.perf;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Debug;
import android.test.ActivityInstrumentationTestCase2;
import android.test.PerformanceTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import nl.sogeti.android.gpstracker.service.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.service.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;

/**
 * Goal is to feed as the LoggerMap as many points as possible to give it a good workout.
 *
 * @author rene (c) Mar 15, 2009, Sogeti B.V.
 * @version $Id: LoggerMapStressTest.java 47 2009-05-17 19:15:00Z rcgroot $
 */
public class MapStressTest extends ActivityInstrumentationTestCase2<LoggerMap> implements PerformanceTestCase {
    private static final Class<LoggerMap> CLASS = LoggerMap.class;
    private static final String PACKAGE = "nl.sogeti.android.gpstracker";
    private static final String TAG = "OGT.MapStressTest";
    private Intermediates mIntermediates;

    public MapStressTest() {
        super(PACKAGE, CLASS);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @LargeTest
    public void testCreateTestData() throws XmlPullParserException, IOException {
        //createTrackBigTest( 2000 );
        //createTrackFromKMLData( "/mnt/sdcard/estland50k.xml" );
    }

    private void createTrackFromKMLData(String xmlResource) throws XmlPullParserException, IOException {

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new FileReader(xmlResource));


        ContentResolver resolver = this.getActivity().getContentResolver();
        Uri trackUri = resolver.insert(Tracks.CONTENT_URI, null);

        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                if ("coordinates".equals(xpp.getName())) {
                    //Start new Segment
                    Uri segmentUri = resolver.insert(Uri.withAppendedPath(trackUri, "segments"), null);
                    Uri waypointUri = Uri.withAppendedPath(segmentUri, "waypoints");
                    //Insert all coordinates as waypoints
                    xpp.next();
                    String coords = xpp.getText();
                    StringTokenizer tokizer = new StringTokenizer(coords, " ");
                    String[] tuple = new String[3];
                    String waypoint;
                    ContentValues wp = new ContentValues();
                    while (tokizer.hasMoreTokens()) {
                        waypoint = tokizer.nextToken();
                        Log.d(TAG, "Insert waypoint: " + waypoint);
                        tuple = waypoint.split(",");
                        wp.put(Waypoints.LONGITUDE, new Double(tuple[0]));
                        wp.put(Waypoints.LATITUDE, new Double(tuple[1]));
                        wp.put(Waypoints.ALTITUDE, new Double(tuple[2]));
                        resolver.insert(waypointUri, wp);
                    }
                }
            }
            eventType = xpp.next();
        }

    }

    private void createTrackBigTest(int total) {
        // zig-zag through the netherlands
        double lat1 = 52.195d;
        double lon1 = 4.685d;
        double lat2 = 51.882d;
        double lon2 = 5.040d;
        double lat3 = 52.178d;
        double lon3 = 5.421d;

        ContentResolver resolver = this.getActivity().getContentResolver();
        ContentValues wp = new ContentValues();
        wp.put(Waypoints.ACCURACY, new Double(10d));
        wp.put(Waypoints.ALTITUDE, new Double(5d));
        wp.put(Waypoints.SPEED, new Double(15d));

        // E.g. returns: content://nl.sogeti.android.gpstracker/tracks/2
        Uri trackUri = resolver.insert(Tracks.CONTENT_URI, null);
        Uri segmentUri = resolver.insert(Uri.withAppendedPath(trackUri, "segments"), null);
        Uri waypointUri = Uri.withAppendedPath(segmentUri, "waypoints");

        for (int step = 0; step < total / 2; step++) {
            double latitude = lat1 + ((lat1 - lat2) / total) * step;
            double longtitude = lon1 + ((lon2 - lon1) / total) * step;
            wp.put(Waypoints.LATITUDE, new Double(latitude));
            wp.put(Waypoints.LONGITUDE, new Double(longtitude));
            resolver.insert(waypointUri, wp);
        }
        for (int step = 0; step < total / 2; step++) {
            double latitude = lat2 + ((lat3 - lat2) / total) * step;
            double longtitude = lon2 + ((lon3 - lon2) / total) * step;
            wp.put(Waypoints.LATITUDE, new Double(latitude));
            wp.put(Waypoints.LONGITUDE, new Double(longtitude));
            resolver.insert(waypointUri, wp);
        }
    }

    /**
     * Open the first track in the list and scroll around forcing redraws during a perf test
     *
     * @throws InterruptedException
     */
    @LargeTest
    public void testBrowseFirstTrack() throws InterruptedException {
        int actions = 0;
        String[] timeActions = {"G", "G", "T", "T", "T"};

        // Start method tracing for Issue 18
        Debug.startMethodTracing("testBrowseFirstTrack");
        if (this.mIntermediates != null) {
            this.mIntermediates.startTiming(true);
        }
        while (actions < timeActions.length) {
            this.sendKeys(timeActions[actions]);
            actions++;
            Thread.sleep(300L);
        }
        if (this.mIntermediates != null) {
            this.mIntermediates.finishTiming(true);
        }
        Debug.stopMethodTracing();
        Log.d(TAG, "Completed actions: " + actions);
    }

    public int startPerformance(Intermediates intermediates) {
        this.mIntermediates = intermediates;
        return 1;
    }

    public boolean isPerformanceOnly() {
        return true;
    }

}
