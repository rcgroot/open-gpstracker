/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) 2015 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.service.logger;

import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import nl.sogeti.android.gpstracker.service.R;
import nl.sogeti.android.gpstracker.service.db.GPStracking;
import nl.sogeti.android.gpstracker.service.util.ExternalConstants;
import nl.sogeti.android.log.Log;

public class GPSListener implements LocationListener, GpsStatus.Listener {

    private static final String TAG = "WakeLockTag";
    /**
     * <code>MAX_REASONABLE_SPEED</code> is about 324 kilometer per hour or 201
     * mile per hour.
     */
    private static final int MAX_REASONABLE_SPEED = 90;
    /**
     * <code>MAX_REASONABLE_ALTITUDECHANGE</code> between the last few waypoints
     * and a new one the difference should be less then 200 meter.
     */
    private static final int MAX_REASONABLE_ALTITUDECHANGE = 200;
    private static final Boolean DEBUG = false;
    private static final boolean VERBOSE = false;
    private final Service mService;
    private final LoggerNotification mLoggerNotification;
    Location mPreviousLocation;
    private boolean mStartNextSegment;
    private LocationManager mLocationManager;
    /**
     * If speeds should be checked to sane values
     */
    private boolean mSpeedSanityCheck;

    /**
     * If broadcasts of location about should be sent to stream location
     */
    private boolean mStreamBroadcast;
    /**
     * <code>mAcceptableAccuracy</code> indicates the maximum acceptable accuracy
     * of a waypoint in meters.
     */
    private float mMaxAcceptableAccuracy = 20;

    private float mDistance;
    private int mPrecision;
    private long mTrackId = -1;
    private long mSegmentId = -1;
    private long mWaypointId = -1;
    private int mLoggingState = ExternalConstants.STATE_STOPPED;
    /**
     * Should the GPS Status monitor update the mLoggerNotification bar
     */
    private boolean mStatusMonitor;
    /**
     * Time thread to runs tasks that check whether the GPS listener has received
     * enough to consider the GPS system alive.
     */
    private Timer mHeartbeatTimer;
    /**
     * Task that will be run periodically during active logging to verify that
     * the logging really happens and that the GPS hasn't silently stopped.
     */
    private TimerTask mHeartbeat = null;
    /**
     * Number of milliseconds that a functioning GPS system needs to provide a
     * location. Calculated to be either 120 seconds or 4 times the requested
     * period, whichever is larger.
     */
    private long mCheckPeriod;
    private float mBroadcastDistance;
    private long mLastTimeBroadcast;
    private PowerManager.WakeLock mWakeLock;

    private Vector<Location> mWeakLocations;
    private Queue<Double> mAltitudes;

    public GPSListener(GPSLoggerService gpsLoggerService, LoggerNotification loggerNotification) {
        mService = gpsLoggerService;
        mLoggerNotification = loggerNotification;
    }

    public void onCreate() {
        mHeartbeatTimer = new Timer("heartbeat", true);

        mWeakLocations = new Vector<>(3);
        mAltitudes = new LinkedList<>();
        mLoggingState = ExternalConstants.STATE_STOPPED;
        mStartNextSegment = false;
        mLocationManager = (LocationManager) mService.getSystemService(Context.LOCATION_SERVICE);
        SharedPreferences sharedPreferences = preferences();
        mSpeedSanityCheck = sharedPreferences.getBoolean(Constants.SPEEDSANITYCHECK, true);
        mStreamBroadcast = sharedPreferences.getBoolean(Constants.BROADCAST_STREAM, false);

        boolean startImmidiatly = preferences().getBoolean(Constants
                .LOGATSTARTUP, false);

        crashRestoreState();
        if (startImmidiatly && mLoggingState == ExternalConstants.STATE_STOPPED) {
            startLogging();
            ContentValues values = new ContentValues();
            values.put(GPStracking.Tracks.NAME, "Recorded at startup");
            mService.getContentResolver().update(ContentUris.withAppendedId(GPStracking.Tracks.CONTENT_URI, mTrackId), values, null, null);
        } else {
            broadCastLoggingState();
        }
    }

    public void onDestroy() {
        mHeartbeatTimer.cancel();
        mHeartbeatTimer.purge();
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
        mLocationManager.removeGpsStatusListener(this);
        stopListening();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (VERBOSE) {
            Log.v(this, "onLocationChanged( Location " + location + " )");
        }
        // Might be claiming GPS disabled but when we were paused this changed and this location proves so
        if (mLoggerNotification.isShowingDisabled()) {
            mLoggerNotification.stopDisabledProvider(R.string.service_gpsenabled);
        }
        Location filteredLocation = locationFilter(location);
        if (filteredLocation != null) {
            if (mStartNextSegment) {
                mStartNextSegment = false;
                // Obey the start segment if the previous location is unknown or far away
                if (mPreviousLocation == null || filteredLocation.distanceTo(mPreviousLocation) > 4 *
                        mMaxAcceptableAccuracy) {
                    startNewSegment();
                }
            } else if (mPreviousLocation != null) {
                mDistance += mPreviousLocation.distanceTo(filteredLocation);
            }
            storeLocation(filteredLocation);
            broadcastLocation(filteredLocation);
            mPreviousLocation = location;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (DEBUG) {
            Log.d(this, "onStatusChanged( String " + provider + ", int " + status + ", Bundle " + extras + " )");
        }
        if (status == LocationProvider.OUT_OF_SERVICE) {
            Log.e(this, String.format("Provider %s changed to status %d", provider, status));
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (DEBUG) {
            Log.d(this, "onProviderEnabled( String " + provider + " )");
        }
        if (mPrecision != ExternalConstants.LOGGING_GLOBAL && provider.equals(LocationManager.GPS_PROVIDER)) {
            mLoggerNotification.stopDisabledProvider(R.string.service_gpsenabled);
            mStartNextSegment = true;
        } else if (mPrecision == ExternalConstants.LOGGING_GLOBAL && provider.equals(LocationManager.NETWORK_PROVIDER)) {
            mLoggerNotification.stopDisabledProvider(R.string.service_dataenabled);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (DEBUG) {
            Log.d(this, "onProviderDisabled( String " + provider + " )");
        }
        ;
        if (mPrecision != ExternalConstants.LOGGING_GLOBAL && provider.equals(LocationManager.GPS_PROVIDER)) {
            mLoggerNotification.startDisabledProvider(R.string.service_gpsdisabled, mTrackId);
        } else if (mPrecision == ExternalConstants.LOGGING_GLOBAL && provider.equals(LocationManager.NETWORK_PROVIDER)) {
            mLoggerNotification.startDisabledProvider(R.string.service_datadisabled, mTrackId);
        }

    }

    @Override
    public synchronized void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                if (mStatusMonitor) {
                    GpsStatus status = mLocationManager.getGpsStatus(null);
                    mLoggerNotification.mSatellites = 0;
                    Iterable<GpsSatellite> list = status.getSatellites();
                    for (GpsSatellite satellite : list) {
                        if (satellite.usedInFix()) {
                            mLoggerNotification.mSatellites++;
                        }
                    }
                    mLoggerNotification.updateLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);
                }
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                break;
            case GpsStatus.GPS_EVENT_STARTED:
                break;
            default:
                break;
        }
    }

    public void onPreferenceChange() {
        sendRequestLocationUpdatesMessage();
        crashProtectState();
        mLoggerNotification.updateLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);
        broadCastLoggingState();
        SharedPreferences preferences = preferences();
        mSpeedSanityCheck = preferences.getBoolean(Constants.SPEEDSANITYCHECK, true);
        mLocationManager.removeGpsStatusListener(this);
        sendRequestStatusUpdateMessage();
        mLoggerNotification.updateLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);
        mStreamBroadcast = preferences.getBoolean(Constants.BROADCAST_STREAM, false);
    }

    private void sendRequestLocationUpdatesMessage() {
        stopListening();
        mStatusMonitor = preferences().getBoolean(Constants.STATUS_MONITOR, false);
        mPrecision = Integer.valueOf(preferences().getString(Constants.PRECISION,
                "2"));
        long intervaltime;
        float distance, accuracy;
        switch (mPrecision) {
            case (ExternalConstants.LOGGING_FINE): // Fine
                accuracy = LoggingConstants.FINE_ACCURACY;
                intervaltime = LoggingConstants.FINE_INTERVAL;
                distance = LoggingConstants.FINE_DISTANCE;
                startListening(LocationManager.GPS_PROVIDER, intervaltime, distance, accuracy);
                break;
            case (ExternalConstants.LOGGING_NORMAL): // Normal
                accuracy = LoggingConstants.NORMAL_ACCURACY;
                intervaltime = LoggingConstants.NORMAL_INTERVAL;
                distance = LoggingConstants.NORMAL_DISTANCE;
                startListening(LocationManager.GPS_PROVIDER, intervaltime, distance, accuracy);
                break;
            case (ExternalConstants.LOGGING_COARSE): // Coarse
                accuracy = LoggingConstants.COARSE_ACCURACY;
                intervaltime = LoggingConstants.COARSE_INTERVAL;
                distance = LoggingConstants.COARSE_DISTANCE;
                startListening(LocationManager.GPS_PROVIDER, intervaltime, distance, accuracy);
                break;
            case (ExternalConstants.LOGGING_GLOBAL): // Global
                accuracy = LoggingConstants.GLOBAL_ACCURACY;
                intervaltime = LoggingConstants.GLOBAL_INTERVAL;
                distance = LoggingConstants.GLOBAL_DISTANCE;
                startListening(LocationManager.NETWORK_PROVIDER, intervaltime, distance, accuracy);
                if (!isNetworkConnected()) {
                    mLoggerNotification.startDisabledProvider(R.string.service_connectiondisabled, mTrackId);
                }
                break;
            case (ExternalConstants.LOGGING_CUSTOM): // Global
                SharedPreferences defaultSharedPreferences = preferences();
                String intervalPreference = defaultSharedPreferences.getString(Constants.LOGGING_INTERVAL, "15000");
                try {
                    intervaltime = 60 * 1000 * Long.valueOf(intervalPreference);
                } catch (NumberFormatException e) {
                    intervaltime = LoggingConstants.NORMAL_INTERVAL;
                    SharedPreferences.Editor edit = defaultSharedPreferences.edit();
                    edit.putString(Constants.LOGGING_INTERVAL, "" + intervaltime / 60000);
                    edit.commit();
                }
                distance = Float.valueOf(defaultSharedPreferences.getString(Constants.LOGGING_DISTANCE, "10"));
                accuracy = Math.max(10f, Math.min(distance, 50f));
                startListening(LocationManager.GPS_PROVIDER, intervaltime, distance, accuracy);
                break;
            default:
                Log.e(this, "Unknown precision " + mPrecision);
                break;
        }
    }

    private SharedPreferences preferences() {
        return PreferenceManager.getDefaultSharedPreferences(mService);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) mService.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connMgr.getActiveNetworkInfo();

        return (info != null && info.isConnected());
    }

    /**
     * Some GPS waypoints received are of to low a quality for tracking use. Here
     * we filter those out.
     *
     * @param proposedLocation
     * @return either the (cleaned) original or null when unacceptable
     */
    public Location locationFilter(Location proposedLocation) {
        // Do no include log wrong 0.0 lat 0.0 long, skip to next value in while-loop
        if (proposedLocation != null && (proposedLocation.getLatitude() == 0.0d
                || proposedLocation.getLongitude() == 0.0d)) {
            Log.w(this, "A wrong location was received, 0.0 latitude and 0.0 longitude... ");
            proposedLocation = null;
        }

        // Do not log a waypoint which is more inaccurate then is configured to be acceptable
        if (proposedLocation != null && proposedLocation.getAccuracy() > mMaxAcceptableAccuracy) {
            Log.w(this, String.format("A weak location was received, lots of inaccuracy... (%f is more then max %f)",
                    proposedLocation.getAccuracy(),
                    mMaxAcceptableAccuracy));
            proposedLocation = addBadLocation(proposedLocation);
        }

        // Do not log a waypoint which might be on any side of the previous waypoint
        if (proposedLocation != null && mPreviousLocation != null && proposedLocation.getAccuracy() > mPreviousLocation
                .distanceTo(proposedLocation)) {
            Log.w(this,
                    String.format("A weak location was received, not quite clear from the previous waypoint... (%f more " +
                                    "then max %f)",
                            proposedLocation.getAccuracy(), mPreviousLocation.distanceTo(proposedLocation)));
            proposedLocation = addBadLocation(proposedLocation);
        }

        // Speed checks, check if the proposed location could be reached from the previous one in sane speed
        // Common to jump on network logging and sometimes jumps on Samsung Galaxy S type of devices
        if (mSpeedSanityCheck && proposedLocation != null && mPreviousLocation != null) {
            // To avoid near instant teleportation on network location or glitches cause continent hopping
            float meters = proposedLocation.distanceTo(mPreviousLocation);
            long seconds = (proposedLocation.getTime() - mPreviousLocation.getTime()) / 1000L;
            float speed = meters / seconds;
            if (speed > MAX_REASONABLE_SPEED) {
                Log.w(this, "A strange location was received, a really high speed of " + speed + " m/s, prob wrong...");
                proposedLocation = addBadLocation(proposedLocation);
                // Might be a messed up Samsung Galaxy S GPS, reset the logging
                if (speed > 2 * MAX_REASONABLE_SPEED && mPrecision != ExternalConstants.LOGGING_GLOBAL) {
                    Log.w(this, "A strange location was received on GPS, reset the GPS listeners");
                    stopListening();
                    mLocationManager.removeGpsStatusListener(this);
                    mLocationManager = (LocationManager) mService.getSystemService(Context.LOCATION_SERVICE);
                    sendRequestStatusUpdateMessage();
                    sendRequestLocationUpdatesMessage();
                }
            }
        }

        // Remove speed if not sane
        if (mSpeedSanityCheck && proposedLocation != null && proposedLocation.getSpeed() > MAX_REASONABLE_SPEED) {
            Log.w(this, "A strange speed, a really high speed, prob wrong...");
            proposedLocation.removeSpeed();
        }

        // Remove altitude if not sane
        if (mSpeedSanityCheck && proposedLocation != null && proposedLocation.hasAltitude()) {
            if (!addSaneAltitude(proposedLocation.getAltitude())) {
                Log.w(this, "A strange altitude, a really big difference, prob wrong...");
                proposedLocation.removeAltitude();
            }
        }
        // Older bad locations will not be needed
        if (proposedLocation != null) {
            mWeakLocations.clear();
        }
        return proposedLocation;
    }

    /**
     * Trigged by events that start a new segment
     */
    private void startNewSegment() {
        this.mPreviousLocation = null;
        Uri newSegment = mService.getContentResolver().insert(Uri.withAppendedPath(GPStracking.Tracks.CONTENT_URI, mTrackId +
                "/segments"), new ContentValues(0));
        mSegmentId = Long.valueOf(newSegment.getLastPathSegment()).longValue();
        crashProtectState();
    }

    protected boolean isLogging() {
        return mLoggingState == ExternalConstants.STATE_LOGGING;
    }

    /**
     * Use the ContentResolver mechanism to store a received location
     *
     * @param location
     */
    public void storeLocation(Location location) {
        if (!isLogging()) {
            Log.e(this, String.format("Not logging but storing location %s, prepare to fail", location.toString()));
        }
        ContentValues args = new ContentValues();

        args.put(GPStracking.Waypoints.LATITUDE, Double.valueOf(location.getLatitude()));
        args.put(GPStracking.Waypoints.LONGITUDE, Double.valueOf(location.getLongitude()));
        args.put(GPStracking.Waypoints.SPEED, Float.valueOf(location.getSpeed()));
        args.put(GPStracking.Waypoints.TIME, Long.valueOf(System.currentTimeMillis()));
        if (location.hasAccuracy()) {
            args.put(GPStracking.Waypoints.ACCURACY, Float.valueOf(location.getAccuracy()));
        }
        if (location.hasAltitude()) {
            args.put(GPStracking.Waypoints.ALTITUDE, Double.valueOf(location.getAltitude()));

        }
        if (location.hasBearing()) {
            args.put(GPStracking.Waypoints.BEARING, Float.valueOf(location.getBearing()));
        }

        Uri waypointInsertUri = Uri.withAppendedPath(GPStracking.Tracks.CONTENT_URI, mTrackId + "/segments/" + mSegmentId +
                "/waypoints");
        Uri inserted = mService.getContentResolver().insert(waypointInsertUri, args);
        mWaypointId = Long.parseLong(inserted.getLastPathSegment());
    }

    /**
     * Consult broadcast options and execute broadcast if necessary
     *
     * @param location
     */
    public void broadcastLocation(Location location) {
        Intent intent = new Intent(ExternalConstants.STREAM_BROADCAST);

        if (mStreamBroadcast) {
            final long minDistance = (long) preferences().getFloat
                    ("streambroadcast_distance_meter", 5000F);
            final long minTime = 60000 * Long.parseLong(preferences().getString
                    ("streambroadcast_time", "1"));
            final long nowTime = location.getTime();
            if (mPreviousLocation != null) {
                mBroadcastDistance += location.distanceTo(mPreviousLocation);
            }
            if (mLastTimeBroadcast == 0) {
                mLastTimeBroadcast = nowTime;
            }
            long passedTime = (nowTime - mLastTimeBroadcast);
            intent.putExtra(ExternalConstants.EXTRA_DISTANCE, (int) mBroadcastDistance);
            intent.putExtra(ExternalConstants.EXTRA_TIME, (int) passedTime / 60000);
            intent.putExtra(ExternalConstants.EXTRA_LOCATION, location);
            intent.putExtra(ExternalConstants.EXTRA_TRACK, ContentUris.withAppendedId(GPStracking.Tracks.CONTENT_URI, mTrackId));

            boolean distanceBroadcast = minDistance > 0 && mBroadcastDistance >= minDistance;
            boolean timeBroadcast = minTime > 0 && passedTime >= minTime;
            if (distanceBroadcast || timeBroadcast) {
                if (distanceBroadcast) {
                    mBroadcastDistance = 0;
                }
                if (timeBroadcast) {
                    mLastTimeBroadcast = nowTime;
                }
                mService.sendBroadcast(intent, "android.permission.ACCESS_FINE_LOCATION");
            }
        }
    }

    /**
     * Store a bad location, when to many bad locations are stored the the
     * storage is cleared and the least bad one is returned
     *
     * @param location bad location
     * @return null when the bad location is stored or the least bad one if the
     * storage was full
     */
    private Location addBadLocation(Location location) {
        mWeakLocations.add(location);
        if (mWeakLocations.size() < 3) {
            location = null;
        } else {
            Location best = mWeakLocations.lastElement();
            for (Location whimp : mWeakLocations) {
                if (whimp.hasAccuracy() && best.hasAccuracy() && whimp.getAccuracy() < best.getAccuracy()) {
                    best = whimp;
                } else {
                    if (whimp.hasAccuracy() && !best.hasAccuracy()) {
                        best = whimp;
                    }
                }
            }
            synchronized (mWeakLocations) {
                mWeakLocations.clear();
            }
            location = best;
        }
        return location;
    }

    void stopListening() {
        if (mHeartbeat != null) {
            mHeartbeat.cancel();
            mHeartbeat = null;
        }
        mLocationManager.removeUpdates(this);
    }


    /**
     * Builds a bit of knowledge about altitudes to expect and return if the
     * added value is deemed sane.
     *
     * @param altitude
     * @return whether the altitude is considered sane
     */
    private boolean addSaneAltitude(double altitude) {
        boolean sane = true;
        double avg = 0;
        int elements = 0;
        // Even insane altitude shifts increases alter perception
        mAltitudes.add(altitude);
        if (mAltitudes.size() > 3) {
            mAltitudes.poll();
        }
        for (Double alt : mAltitudes) {
            avg += alt;
            elements++;
        }
        avg = avg / elements;
        sane = Math.abs(altitude - avg) < MAX_REASONABLE_ALTITUDECHANGE;

        return sane;
    }

    private void crashProtectState() {
        SharedPreferences preferences = preferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(LoggingConstants.SERVICESTATE_TRACKID, mTrackId);
        editor.putLong(LoggingConstants.SERVICESTATE_SEGMENTID, mSegmentId);
        editor.putInt(LoggingConstants.SERVICESTATE_PRECISION, mPrecision);
        editor.putInt(LoggingConstants.SERVICESTATE_STATE, mLoggingState);
        editor.putFloat(LoggingConstants.SERVICESTATE_DISTANCE, mDistance);
        editor.commit();
        if (DEBUG) {
            Log.d(this, "crashProtectState()");
        }
    }

    private synchronized void crashRestoreState() {
        SharedPreferences preferences = preferences();
        long previousState = preferences.getInt(LoggingConstants.SERVICESTATE_STATE, ExternalConstants.STATE_STOPPED);
        if (previousState == ExternalConstants.STATE_LOGGING || previousState == ExternalConstants.STATE_PAUSED) {
            Log.w(this, "Recovering from a crash or kill and restoring state.");
            mLoggerNotification.startLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);

            mTrackId = preferences.getLong(LoggingConstants.SERVICESTATE_TRACKID, -1);
            mSegmentId = preferences.getLong(LoggingConstants.SERVICESTATE_SEGMENTID, -1);
            mPrecision = preferences.getInt(LoggingConstants.SERVICESTATE_PRECISION, -1);
            mDistance = preferences.getFloat(LoggingConstants.SERVICESTATE_DISTANCE, 0F);
            if (previousState == ExternalConstants.STATE_LOGGING) {
                mLoggingState = ExternalConstants.STATE_PAUSED;
                GPSLoggerServiceManager.resumeGPSLogging(mService);
            } else if (previousState == ExternalConstants.STATE_PAUSED) {
                mLoggingState = ExternalConstants.STATE_LOGGING;
                pauseLogging();
            }
        }
    }


    public synchronized void startLogging() {
        Log.d(this, "startLogging()");
        if (this.mLoggingState == ExternalConstants.STATE_STOPPED) {
            startNewTrack();
            sendRequestLocationUpdatesMessage();
            sendRequestStatusUpdateMessage();
            this.mLoggingState = ExternalConstants.STATE_LOGGING;
            updateWakeLock();
            mLoggerNotification.startLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);
            crashProtectState();
            broadCastLoggingState();
        }
    }

    public synchronized void pauseLogging() {
        Log.d(this, "pauseLogging()");
        if (this.mLoggingState == ExternalConstants.STATE_LOGGING) {
            mLocationManager.removeGpsStatusListener(this);
            stopListening();
            mLoggingState = ExternalConstants.STATE_PAUSED;
            mPreviousLocation = null;
            updateWakeLock();
            mLoggerNotification.updateLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);
            mLoggerNotification.mSatellites = 0;
            mLoggerNotification.updateLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);
            crashProtectState();
            broadCastLoggingState();
        }
    }

    public synchronized void resumeLogging() {
        Log.d(this, "resumeLogging()");
        if (this.mLoggingState == ExternalConstants.STATE_PAUSED) {
            if (mPrecision != ExternalConstants.LOGGING_GLOBAL) {
                mStartNextSegment = true;
            }
            sendRequestLocationUpdatesMessage();
            sendRequestStatusUpdateMessage();

            this.mLoggingState = ExternalConstants.STATE_LOGGING;
            updateWakeLock();
            mLoggerNotification.updateLogging(mPrecision, mLoggingState, mStatusMonitor, mTrackId);
            crashProtectState();
            broadCastLoggingState();
        }
    }

    public synchronized void stopLogging() {
        Log.d(this, "stopLogging()");
        mLoggingState = ExternalConstants.STATE_STOPPED;
        crashProtectState();
        updateWakeLock();

        mLocationManager.removeGpsStatusListener(this);
        stopListening();
        mLoggerNotification.stopLogging();

        broadCastLoggingState();
    }

    public Uri storeMetaData(String key, String value) {
        Uri uri = Uri.withAppendedPath(GPStracking.Tracks.CONTENT_URI, mTrackId + "/metadata");

        if (mTrackId >= 0) {
            Cursor cursor = null;
            try {
                cursor = mService.getContentResolver().query(
                        uri, new String[]{GPStracking.MetaData.VALUE},
                        GPStracking.MetaData.KEY + " = ? ", new String[]{key}, null);
                if (cursor.moveToFirst()) {
                    ContentValues args = new ContentValues();
                    args.put(GPStracking.MetaData.VALUE, value);
                    mService.getContentResolver().update(
                            uri, args,
                            GPStracking.MetaData.KEY + " = ? ", new String[]{key});
                } else {
                    ContentValues args = new ContentValues();
                    args.put(GPStracking.MetaData.KEY, key);
                    args.put(GPStracking.MetaData.VALUE, value);
                    mService.getContentResolver().insert(uri, args);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return uri;
    }

    /**
     * Send a system broadcast to notify a change in the logging or precision
     */
    private void broadCastLoggingState() {
        Intent broadcast = new Intent(ExternalConstants.LOGGING_STATE_CHANGED_ACTION);
        broadcast.putExtra(ExternalConstants.EXTRA_LOGGING_PRECISION, mPrecision);
        broadcast.putExtra(ExternalConstants.EXTRA_LOGGING_STATE, mLoggingState);
        mService.getApplicationContext().sendBroadcast(broadcast);
    }

    void startListening(String provider, long intervaltime, float distance, float accuracy) {
        mMaxAcceptableAccuracy = accuracy;
        mLocationManager.removeUpdates(this);
        mLocationManager.requestLocationUpdates(provider, intervaltime, distance, this);
        mCheckPeriod = Math.max(12 * intervaltime, 120 * 1000);
        if (mHeartbeat != null) {
            mHeartbeat.cancel();
            mHeartbeat = null;
        }
        mHeartbeat = new Heartbeat(provider);
        mHeartbeatTimer.schedule(mHeartbeat, mCheckPeriod, mCheckPeriod);
    }


    private void updateWakeLock() {
        if (this.mLoggingState == ExternalConstants.STATE_LOGGING) {
            PowerManager pm = (PowerManager) mService.getSystemService(Context.POWER_SERVICE);
            if (this.mWakeLock != null) {
                this.mWakeLock.release();
                this.mWakeLock = null;
            }
            this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            this.mWakeLock.acquire();
        } else {
            if (this.mWakeLock != null) {
                this.mWakeLock.release();
                this.mWakeLock = null;
            }
        }
    }

    /**
     * Trigged by events that start a new track
     */
    private void startNewTrack() {
        mDistance = 0;
        Uri newTrack = mService.getContentResolver().insert(GPStracking.Tracks.CONTENT_URI, new ContentValues(0));
        mTrackId = Long.valueOf(newTrack.getLastPathSegment()).longValue();
        startNewSegment();
    }

    protected void storeMediaUri(Uri mediaUri) {
        if (isMediaPrepared()) {
            Uri mediaInsertUri = Uri.withAppendedPath(GPStracking.Tracks.CONTENT_URI, mTrackId + "/segments/" + mSegmentId +
                    "/waypoints/" + mWaypointId + "/media");
            ContentValues args = new ContentValues();
            args.put(GPStracking.Media.URI, mediaUri.toString());
            mService.getContentResolver().insert(mediaInsertUri, args);
        } else {
            Log.e(this, "No logging done under which to store the track");
        }
    }

    protected boolean isMediaPrepared() {
        return !(mTrackId < 0 || mSegmentId < 0 || mWaypointId < 0);
    }

    private void sendRequestStatusUpdateMessage() {
        mLocationManager.addGpsStatusListener(this);
    }

    public void removeGpsStatusListener() {
        mLocationManager.removeGpsStatusListener(this);
    }

    public long getTrackId() {
        return mTrackId;
    }

    public int getLoggingState() {
        return mLoggingState;
    }

    /**
     * Provides the cached last stored waypoint it current logging is active alse
     * null.
     *
     * @return last waypoint location or null
     */
    Location getLastWaypoint() {
        Location myLastWaypoint = null;
        if (isLogging()) {
            myLastWaypoint = mPreviousLocation;
        }
        return myLastWaypoint;
    }

    float getTrackedDistance() {
        float distance = 0F;
        if (isLogging()) {
            distance = mDistance;
        }
        return distance;
    }

    /**
     * Task to determine if the GPS is alive
     */
    class Heartbeat extends TimerTask {

        private String mProvider;

        public Heartbeat(String provider) {
            mProvider = provider;
        }

        @Override
        public void run() {
            if (mLoggingState == ExternalConstants.STATE_LOGGING) {
                // Collect the last location from the last logged location or a more recent from the last weak location
                Location checkLocation = mPreviousLocation;
                synchronized (mWeakLocations) {
                    if (!mWeakLocations.isEmpty()) {
                        if (checkLocation == null) {
                            checkLocation = mWeakLocations.lastElement();
                        } else {
                            Location weakLocation = mWeakLocations.lastElement();
                            checkLocation = weakLocation.getTime() > checkLocation.getTime() ? weakLocation : checkLocation;
                        }
                    }
                }
                // Is the last known GPS location something nearby we are not told?
                Location managerLocation = mLocationManager.getLastKnownLocation(mProvider);
                if (managerLocation != null && checkLocation != null) {
                    if (checkLocation.distanceTo(managerLocation) < 2 * mMaxAcceptableAccuracy) {
                        checkLocation = managerLocation.getTime() > checkLocation.getTime() ? managerLocation : checkLocation;
                    }
                }

                if (checkLocation == null || checkLocation.getTime() + mCheckPeriod < new Date().getTime()) {
                    Log.w(this, "GPS system failed to produce a location during logging: " + checkLocation);
                    mLoggerNotification.startPoorSignal(mTrackId);
                    if (mStatusMonitor) {
                        mLoggerNotification.soundGpsSignalAlarm();
                    }

                    mLoggingState = ExternalConstants.STATE_PAUSED;
                    GPSLoggerServiceManager.resumeGPSLogging(GPSListener.this.mService);
                } else {
                    mLoggerNotification.stopPoorSignal();
                }
            }
        }
    }
}
