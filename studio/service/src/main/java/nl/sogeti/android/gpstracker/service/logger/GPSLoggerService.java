/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
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

import android.content.ContentUris;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;

import nl.sogeti.android.gpstracker.service.db.GPStracking;
import nl.sogeti.android.gpstracker.service.linger.LingerService;
import nl.sogeti.android.gpstracker.service.util.ExternalConstants;
import nl.sogeti.android.log.Log;

/**
 * A system service as controlling the background logging of gps locations.
 *
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 * @version $Id$
 */
public class GPSLoggerService extends LingerService {

    private GPSListener mGPSListener;
    private LoggerNotification mLoggerNotification;
    private IBinder mBinder = new GPSLoggerServiceImplementation();

    public GPSLoggerService() {
        super("GPS Logger", 10);
    }

    @Override
    protected void didCreate() {
        Log.d(this, "didCreate()");
        initLogging();
    }

    @Override
    protected void didContinue() {
        Log.d(this, "didCreate()");
        initLogging();
    }

    @Override
    protected void didDestroy() {
        Log.d(this, "didDestroy()");
        if (mGPSListener.isLogging()) {
            Log.w(this, "Destroying an actively logging service");
        }
        mGPSListener.removeGpsStatusListener();
        mGPSListener.stopListening();
        if (mGPSListener.getLoggingState() != ExternalConstants.STATE_PAUSED) {
            mLoggerNotification.stopLogging();
        }
        mGPSListener.onDestroy();
        mLoggerNotification = null;
    }

    @Override
    protected boolean shouldContinue() {
        boolean isLogging = mGPSListener.isLogging();
        Log.d(this, "shouldContinue() " + isLogging);

        if (isLogging) {
            mGPSListener.verifyLoggingState();
        }
        return isLogging;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(this, "handleCommand(Intent " + intent.getAction() + ")");
        LoggerPersistence persistence = new LoggerPersistence(this);
        if (intent.hasExtra(Commands.CONFIG_PRECISION)) {
            int precision = intent.getIntExtra(Commands.CONFIG_PRECISION, ExternalConstants.LOGGING_NORMAL);
            persistence.setPrecision(precision);
            mGPSListener.onPreferenceChange();
        }
        if (intent.hasExtra(Commands.CONFIG_INTERVAL_DISTANCE)) {
            float interval = intent.getFloatExtra(Commands.CONFIG_INTERVAL_DISTANCE, LoggingConstants.NORMAL_DISTANCE);
            persistence.setCustomLocationIntervalMetres(interval);
            mGPSListener.onPreferenceChange();
        }
        if (intent.hasExtra(Commands.CONFIG_INTERVAL_TIME)) {
            long interval = intent.getLongExtra(Commands.CONFIG_INTERVAL_TIME, LoggingConstants.NORMAL_INTERVAL);
            persistence.setCustomLocationIntervalMinutes(interval);
            mGPSListener.onPreferenceChange();
        }
        if (intent.hasExtra(Commands.CONFIG_SPEED_SANITY)) {
            persistence.isSpeedChecked(intent.getBooleanExtra(Commands.CONFIG_SPEED_SANITY, true));
        }
        if (intent.hasExtra(Commands.CONFIG_STATUS_MONITOR)) {
            persistence.isStatusMonitor(intent.getBooleanExtra(Commands.CONFIG_STATUS_MONITOR, false));
            mGPSListener.onPreferenceChange();
        }
        if (intent.hasExtra(Commands.CONFIG_STREAM_BROADCAST)) {
            persistence.getStreamBroadcast(intent.getBooleanExtra(Commands.CONFIG_STREAM_BROADCAST, false));
            persistence.setBroadcastIntervalMeters(intent.getFloatExtra(Commands.CONFIG_STREAM_INTERVAL_DISTANCE, 1L));
            persistence.setBroadcastIntervalMinutes(intent.getLongExtra(Commands.CONFIG_STREAM_INTERVAL_TIME, 1L));
        }
        if (intent.hasExtra(Commands.CONFIG_START_AT_BOOT)) {
            persistence.shouldLogAtBoot(intent.getBooleanExtra(Commands.CONFIG_START_AT_BOOT, false));
        }
        if (intent.hasExtra(Commands.CONFIG_START_AT_POWER_CONNECT)) {
            persistence.shouldLogAtPowerConnected(intent.getBooleanExtra(Commands.CONFIG_START_AT_POWER_CONNECT, false));
        }
        if (intent.hasExtra(Commands.CONFIG_STOP_AT_POWER_DISCONNECT)) {
            persistence.shouldLogAtPowerDisconnected(intent.getBooleanExtra(Commands.CONFIG_STOP_AT_POWER_DISCONNECT, false));
        }
        if (intent.hasExtra(Commands.CONFIG_START_AT_DOCK)) {
            persistence.shouldLogAtDockCar(intent.getBooleanExtra(Commands.CONFIG_START_AT_DOCK, false));
        }
        if (intent.hasExtra(Commands.CONFIG_STOP_AT_UNDOCK)) {
            persistence.shouldLogAtUndockCar(intent.getBooleanExtra(Commands.CONFIG_STOP_AT_UNDOCK, false));
        }
        if (intent.hasExtra(Commands.COMMAND)) {
            executeCommandIntent(intent);
        }
    }

    private void executeCommandIntent(Intent intent) {
        switch (intent.getIntExtra(Commands.COMMAND, -1)) {
            case Commands.EXTRA_COMMAND_START:
                mGPSListener.startLogging();
                // Start a naming of the track
                Uri uri = ContentUris.withAppendedId(GPStracking.Tracks.CONTENT_URI, mGPSListener.getTrackId());
                Intent namingIntent = new Intent(ExternalConstants.NAMING_ACTION, uri);
                namingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                namingIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(namingIntent);
                break;
            case Commands.EXTRA_COMMAND_PAUSE:
                mGPSListener.pauseLogging();
                break;
            case Commands.EXTRA_COMMAND_RESUME:
                mGPSListener.resumeLogging();
                break;
            case Commands.EXTRA_COMMAND_STOP:
                mGPSListener.stopLogging();
                break;
            default:
                break;
        }

        if (mGPSListener.isLogging()) {
            setLingerDuration(mGPSListener.getCheckPeriod() / 1000L);
        } else {
            setLingerDuration(10L);
        }
    }

    private void initLogging() {
        mLoggerNotification = new LoggerNotification(this);
        mLoggerNotification.stopLogging();
        LoggerPersistence persistence = new LoggerPersistence(this);
        mGPSListener = new GPSListener(this, persistence, mLoggerNotification);
        mGPSListener.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        initLogging();
        return this.mBinder;
    }

    public static class Commands {
        public static final String COMMAND = "nl.sogeti.android.gpstracker.extra.COMMAND";
        public static final String CONFIG_PRECISION = "nl.sogeti.android.gpstracker.extra.CONFIG_PRECISION";
        public static final String CONFIG_INTERVAL_DISTANCE = "nl.sogeti.android.gpstracker.extra.CONFIG_INTERVAL_DISTANCE";
        public static final String CONFIG_INTERVAL_TIME = "nl.sogeti.android.gpstracker.extra.CONFIG_INTERVAL_TIME";
        public static final String CONFIG_SPEED_SANITY = "nl.sogeti.android.gpstracker.extra.CONFIG_SPEED_SANITY";
        public static final String CONFIG_STATUS_MONITOR = "nl.sogeti.android.gpstracker.extra.CONFIG_STATUS_MONITOR";
        public static final String CONFIG_STREAM_BROADCAST = "nl.sogeti.android.gpstracker.extra.CONFIG_STREAM_BROADCAST";
        public static final String CONFIG_STREAM_INTERVAL_DISTANCE = "nl.sogeti.android.gpstracker.extra.CONFIG_STREAM_INTERVAL_DISTANCE";
        public static final String CONFIG_STREAM_INTERVAL_TIME = "nl.sogeti.android.gpstracker.extra.CONFIG_STREAM_INTERVAL_TIME";
        public static final int EXTRA_COMMAND_START = 0;
        public static final int EXTRA_COMMAND_PAUSE = 1;
        public static final int EXTRA_COMMAND_RESUME = 2;
        public static final int EXTRA_COMMAND_STOP = 3;
        public static final String CONFIG_START_AT_BOOT = "nl.sogeti.android.gpstracker.extra.CONFIG_START_AT_BOOT";
        public static final String CONFIG_START_AT_POWER_CONNECT = "nl.sogeti.android.gpstracker.extra.CONFIG_START_AT_POWER_CONNECT";
        public static final String CONFIG_STOP_AT_POWER_DISCONNECT = "nl.sogeti.android.gpstracker.extra.CONFIG_STOP_AT_POWER_DISCONNECT";
        public static final String CONFIG_START_AT_DOCK = "nl.sogeti.android.gpstracker.extra.CONFIG_START_AT_DOCK";
        public static final String CONFIG_STOP_AT_UNDOCK = "nl.sogeti.android.gpstracker.extra.CONFIG_STOP_AT_UNDOCK";
    }

    private class GPSLoggerServiceImplementation extends IGPSLoggerServiceRemote.Stub {
        @Override
        public long getTrackId() throws RemoteException {
            return mGPSListener.getTrackId();
        }

        @Override
        public int loggingState() throws RemoteException {
            return mGPSListener.getLoggingState();
        }

        @Override
        public Uri storeMediaUri(Uri mediaUri) throws RemoteException {
            mGPSListener.storeMediaUri(mediaUri);
            return null;
        }

        @Override
        public boolean isMediaPrepared() throws RemoteException {
            return mGPSListener.isMediaPrepared();
        }

        @Override
        public Uri storeMetaData(String key, String value) throws RemoteException {
            return mGPSListener.storeMetaData(key, value);
        }

        @Override
        public Location getLastWaypoint() throws RemoteException {
            return mGPSListener.getLastWaypoint();
        }

        @Override
        public float getTrackedDistance() throws RemoteException {
            return mGPSListener.getTrackedDistance();
        }
    }
}