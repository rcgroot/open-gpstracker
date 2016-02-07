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
package nl.sogeti.android.gpstracker.tests.utils;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Feeder of GPS-location information
 *
 * @author Maarten van Berkel (maarten.van.berkel@sogeti.nl / +0586)
 * @version $Id$
 */
public class MockGPSLoggerDriver implements Runnable {
    private static final String TAG = "MockGPSLoggerDriver";
    private boolean running = true;
    private int mTimeout;
    private Context mContext;
    private TelnetPositionSender sender;
    private ArrayList<SimplePosition> positions;
    private int mRouteResource;

    /**
     * Constructor: create a new MockGPSLoggerDriver.
     *
     * @param context context of the test package
     * @param route   resource identifier for the xml route
     * @param timeout time to idle between waypoints in miliseconds
     */
    public MockGPSLoggerDriver(Context context, int route, int timeout) {
        this();
        this.mTimeout = timeout;
        this.mRouteResource = route;// R.xml.denhaagdenbosch;
        this.mContext = context;
    }

    public MockGPSLoggerDriver() {
        this.sender = new TelnetPositionSender();
    }

    public static String createGPGGALocationCommand(double longitude, double latitude, double elevation) {

        final String COMMAND_GPS = "GPGGA," + // $--GGA,
                "%1$02d" + // hh      c.get(Calendar.HOUR_OF_DAY)
                "%2$02d" + // mm      c.get(Calendar.MINUTE)
                "%3$02d." + // ss.     c.get(Calendar.SECOND)
                "%4$03d," + // sss,    c.get(Calendar.MILLISECOND)
                "%5$03d" + // llll    latDegree
                "%6$09.6f," + //         latMinute
                "%7$c," + //         latDirection
                "%8$03d" + //         longDegree
                "%9$09.6f," + //         longMinutett
                "%10$c," + //         longDirection
                "1,05,02.1,00545.5,M,-26.0,M,,";

        Calendar c = Calendar.getInstance();

        double absLong = Math.abs(longitude);
        int longDegree = (int) Math.floor(absLong);
        char longDirection = 'E';
        if (longitude < 0) {
            longDirection = 'W';
        }

        double longMinute = (absLong - Math.floor(absLong)) * 60;

        double absLat = Math.abs(latitude);
        int latDegree = (int) Math.floor(absLat);
        char latDirection = 'N';
        if (latitude < 0) {
            latDirection = 'S';
        }

        double latMinute = (absLat - Math.floor(absLat)) * 60;

        String command = String.format(COMMAND_GPS, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar
                        .SECOND), c.get(Calendar.MILLISECOND), latDegree, latMinute,
                latDirection, longDegree, longMinute, longDirection);
        return command;
    }

    /**
     * Create a NMEA GPRMC sentence
     *
     * @param longitude
     * @param latitude
     * @param elevation
     * @param speed     in mps
     * @return
     */
    public static String createGPRMCLocationCommand(double longitude, double latitude, double elevation, double speed) {
        speed *= 0.51; // from m/s to knots
        final String COMMAND_GPS = "GPRMC," + "%1$02d" + // hh      c.get(Calendar.HOUR_OF_DAY)
                "%2$02d" + // mm      c.get(Calendar.MINUTE)
                "%3$02d." + // ss.     c.get(Calendar.SECOND)
                "%4$03d,A," + // ss,     c.get(Calendar.MILLISECOND)

                "%5$03d" + // llll    latDegree
                "%6$09.6f," + //         latMinute

                "%7$c," + //         latDirection (N or S)

                "%8$03d" + //         longDegree
                "%9$09.6f," + //         longMinutett

                "%10$c," + //         longDirection (E or W)
                "%14$.2f," + //         Speed over ground in knot
                "0," + //         Track made good in degrees True
                "%11$02d" + // dd
                "%12$02d" + // mm
                "%13$02d," + // yy
                "0," + //         Magnetic variation degrees (Easterly var. subtracts from true course)
                "E," + //         East/West
                "mode"; // Just as workaround....

        Calendar c = Calendar.getInstance();
        double absLong = Math.abs(longitude);
        int longDegree = (int) Math.floor(absLong);
        char longDirection = 'E';
        if (longitude < 0) {
            longDirection = 'W';
        }
        double longMinute = (absLong - Math.floor(absLong)) * 60;
        double absLat = Math.abs(latitude);
        int latDegree = (int) Math.floor(absLat);
        char latDirection = 'N';
        if (latitude < 0) {
            latDirection = 'S';
        }
        double latMinute = (absLat - Math.floor(absLat)) * 60;

        String command = String.format(COMMAND_GPS, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar
                        .SECOND), c.get(Calendar.MILLISECOND), latDegree, latMinute,
                latDirection, longDegree, longMinute, longDirection, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH),
                c.get(Calendar.YEAR) - 2000, speed);
        return command;

    }

    public static String calulateChecksum(String nmeaCommand) {
        byte[] chars = null;
        try {
            chars = nmeaCommand.getBytes("ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte xor = 0;
        for (int i = 0; i < chars.length; i++) {
            xor ^= chars[i];
        }
        return Integer.toHexString((int) xor).toUpperCase();
    }

    public int getPositions() {
        return this.positions.size();
    }

    public void run() {
        prepareRun(this.mRouteResource);

        while (this.running && (this.positions.size() > 0)) {
            SimplePosition position = this.positions.remove(0);
            //String nmeaCommand = createGPGGALocationCommand(position.getLongitude(), position.getLatitude(), 0);
            String nmeaCommand = createGPRMCLocationCommand(position.lng, position.lat, 0, 0);
            String checksum = calulateChecksum(nmeaCommand);
            this.sender.sendCommand("geo nmea $" + nmeaCommand + "*" + checksum + "\r\n");

            try {
                Thread.sleep(this.mTimeout);
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted");
            }
        }
    }

    private void prepareRun(int xmlResource) {
        this.positions = new ArrayList<SimplePosition>();
        XmlResourceParser xmlParser = this.mContext.getResources().getXml(xmlResource);
        doUglyXMLParsing(this.positions, xmlParser);
        xmlParser.close();
    }

    private void doUglyXMLParsing(ArrayList<SimplePosition> positions, XmlResourceParser xmlParser) {
        int eventType;
        try {
            eventType = xmlParser.getEventType();

            SimplePosition lastPosition = null;
            boolean speed = false;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xmlParser.getName().equals("trkpt") || xmlParser.getName().equals("rtept") || xmlParser.getName()
                            .equals
                                    ("wpt")) {
                        lastPosition = new SimplePosition(xmlParser.getAttributeFloatValue(0, 12.3456F), xmlParser
                                .getAttributeFloatValue(1, 12.3456F));
                        positions.add(lastPosition);
                    }
                    if (xmlParser.getName().equals("speed")) {
                        speed = true;
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (xmlParser.getName().equals("speed")) {
                        speed = false;
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (lastPosition != null && speed) {
                        lastPosition.speed = Float.parseFloat(xmlParser.getText());
                    }
                }


                eventType = xmlParser.next();
            }
        } catch (XmlPullParserException e) { /* ignore */
        } catch (IOException e) {/* ignore */
        }
    }

    public void stop() {
        this.running = false;
    }

    public void sendSMS(String string) {
        this.sender.sendCommand("sms send 31886606607 " + string + "\r\n");
    }

    class SimplePosition {

        public float speed;
        public double lat, lng;

        public SimplePosition(float latitude, float longtitude) {
            this.lat = latitude;
            this.lng = longtitude;
        }

    }

}