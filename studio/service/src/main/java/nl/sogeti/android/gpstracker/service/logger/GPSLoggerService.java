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
import android.preference.PreferenceManager;

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
        super("GPS Logger", 60);
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
        return mGPSListener.isLogging();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(this, "handleCommand(Intent " + intent + ")");

        if (intent.hasExtra(Commands.CONFIG_PRECISION)) {
            savePreference(Constants.PRECISION, intent.getStringExtra(Commands.CONFIG_PRECISION));
            mGPSListener.onPreferenceChange();
        }
        if (intent.hasExtra(Commands.CONFIG_DISTANCE)) {
            savePreference(Constants.LOGGING_DISTANCE, intent.getStringExtra(Commands.CONFIG_DISTANCE));
        }
        if (intent.hasExtra(Commands.CONFIG_INTERVAL)) {
            savePreference(Constants.LOGGING_INTERVAL, intent.getStringExtra(Commands.CONFIG_INTERVAL));
        }
        if (intent.hasExtra(Commands.CONFIG_SPEED_SANITY)) {
            savePreference(Constants.SPEEDSANITYCHECK, intent.getBooleanExtra(Commands.CONFIG_SPEED_SANITY, true));
        }
        if (intent.hasExtra(Commands.CONFIG_STATUS_MONITOR)) {
            savePreference(Constants.STATUS_MONITOR, intent.getBooleanExtra(Commands.CONFIG_STATUS_MONITOR, false));
        }
        if (intent.hasExtra(Commands.CONFIG_STATUS_STREAM)) {
            savePreference(Constants.BROADCAST_STREAM, intent.getBooleanExtra(Commands.CONFIG_STATUS_STREAM, false));
        }
        if (intent.hasExtra(Commands.COMMAND)) {
            executeCommandIntent(intent);
        }
    }


    private void savePreference(String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(key, value)
                .commit();
    }

    private void savePreference(String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(key, value)
                .commit();
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
    }

    private void initLogging() {
        mLoggerNotification = new LoggerNotification(this);
        mLoggerNotification.stopLogging();
        mGPSListener = new GPSListener(this, mLoggerNotification);
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
        public static final String CONFIG_DISTANCE = "nl.sogeti.android.gpstracker.extra.CONFIG_DISTANCE";
        public static final String CONFIG_INTERVAL = "nl.sogeti.android.gpstracker.extra.CONFIG_INTERVAL";
        public static final String CONFIG_SPEED_SANITY = "nl.sogeti.android.gpstracker.extra.CONFIG_SPEED_SANITY";
        public static final String CONFIG_STATUS_MONITOR = "nl.sogeti.android.gpstracker.extra.CONFIG_STATUS_MONITOR";
        public static final String CONFIG_STATUS_STREAM = "nl.sogeti.android.gpstracker.extra.CONFIG_STATUS_STREAM";

        public static final int EXTRA_COMMAND_START = 0;
        public static final int EXTRA_COMMAND_PAUSE = 1;
        public static final int EXTRA_COMMAND_RESUME = 2;
        public static final int EXTRA_COMMAND_STOP = 3;
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