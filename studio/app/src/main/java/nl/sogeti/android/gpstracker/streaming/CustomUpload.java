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
package nl.sogeti.android.gpstracker.streaming;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;

import nl.sogeti.android.gpstracker.service.R;
import nl.sogeti.android.gpstracker.service.util.ExternalConstants;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.log.Log;


public class CustomUpload extends BroadcastReceiver {
    private static final String CUSTOM_UPLOAD_BACKLOG_DEFAULT = "20";
    private static final int NOTIFICATION_ID = R.string.customupload_failed;
    private static CustomUpload sCustomUpload = null;
    private static Queue<URL> sRequestBacklog = new LinkedList<>();

    public static synchronized void initStreaming(Context ctx) {
        Log.d(CustomUpload.class, "initStreaming(Context)");
        if (sCustomUpload != null) {
            shutdownStreaming(ctx);
        }
        sCustomUpload = new CustomUpload();
        sRequestBacklog = new LinkedList<>();

        IntentFilter filter = new IntentFilter(ExternalConstants.STREAM_BROADCAST);
        ctx.registerReceiver(sCustomUpload, filter);
    }

    public static synchronized void shutdownStreaming(Context ctx) {
        Log.d(CustomUpload.class, "shutdownStreaming(Context)");
        if (sCustomUpload != null) {
            ctx.unregisterReceiver(sCustomUpload);
            sCustomUpload.onShutdown();
            sCustomUpload = null;
        }
    }

    private void onShutdown() {
        Log.d(this, "onShutdown()");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(this, "onReceive(Context, Intent)");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefUrl = preferences.getString(Constants.CUSTOMUPLOAD_URL, "http://www.example.com");
        Integer prefBacklog = Integer.valueOf(preferences.getString(Constants.CUSTOMUPLOAD_BACKLOG,
                CUSTOM_UPLOAD_BACKLOG_DEFAULT));
        Location loc = intent.getParcelableExtra(ExternalConstants.EXTRA_LOCATION);
        Uri trackUri = intent.getParcelableExtra(ExternalConstants.EXTRA_TRACK);
        String buildUrl = prefUrl;
        buildUrl = buildUrl.replace("@LAT@", Double.toString(loc.getLatitude()));
        buildUrl = buildUrl.replace("@LON@", Double.toString(loc.getLongitude()));
        buildUrl = buildUrl.replace("@ID@", trackUri.getLastPathSegment());
        buildUrl = buildUrl.replace("@TIME@", Long.toString(loc.getTime()));
        buildUrl = buildUrl.replace("@SPEED@", Float.toString(loc.getSpeed()));
        buildUrl = buildUrl.replace("@ACC@", Float.toString(loc.getAccuracy()));
        buildUrl = buildUrl.replace("@ALT@", Double.toString(loc.getAltitude()));
        buildUrl = buildUrl.replace("@BEAR@", Float.toString(loc.getBearing()));

        URL uploadUri;
        try {
            uploadUri = new URL(buildUrl);
            if (uploadUri.getHost() != null && ("http".equals(uploadUri.getProtocol()) || "https".equals(uploadUri
                    .getProtocol()))) {
                sRequestBacklog.add(uploadUri);
            } else {
                Log.e(this, "URL does not have correct scheme or host " + uploadUri);
            }
            if (sRequestBacklog.size() > prefBacklog) {
                sRequestBacklog.poll();
            }
            new Uploader(context).execute();
        } catch (IOException e) {
            notifyError(context, e);
        }
    }

    private static void clearNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context
                .NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private static void notifyError(Context context, Exception e) {
        Log.e(CustomUpload.class, "Custom upload failed", e);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context
                .NOTIFICATION_SERVICE);

        int icon = R.drawable.ic_maps_indicator_current_position;
        CharSequence tickerText = context.getText(R.string.customupload_failed);
        Intent notificationIntent = new Intent(context, CustomUpload.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(icon)
                        .setContentTitle(tickerText)
                        .setContentText(e.getMessage())
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    static class Uploader extends AsyncTask<Void, Void, Void> {

        private final Context mContext;

        Uploader(Context ctx) {
            this.mContext = ctx;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                while (!sRequestBacklog.isEmpty()) {
                    URL request = sRequestBacklog.peek();
                    HttpURLConnection connection = (HttpURLConnection) request.openConnection();
                    sRequestBacklog.poll();
                    connection.connect();
                    int status = connection.getResponseCode();
                    if (status != 200) {
                        throw new IOException("Invalid response from server: " + status);
                    }
                    clearNotification(mContext);
                }
            } catch (IOException e) {
                notifyError(mContext, e);
            }

            return null;
        }
    }
}