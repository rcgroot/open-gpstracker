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
package nl.sogeti.android.gpstracker.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.IOException;

import nl.sogeti.android.gpstracker.integration.ContentConstants;

/**
 * Various application wide constants
 *
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 * @version $Id$
 */
public class Constants {
    public static final String CUSTOMUPLOAD_URL = "CUSTOMUPLOAD_URL";
    public static final String CUSTOMUPLOAD_BACKLOG = "CUSTOM_UPLOAD_BACKLOG";
    public static final String DISABLEBLANKING = "disableblanking";
    public static final String DISABLEDIMMING = "disabledimming";
    public static final String SATELLITE = "SATELLITE";
    public static final String TRAFFIC = "TRAFFIC";
    public static final String SPEED = "showspeed";
    public static final String ALTITUDE = "showaltitude";
    public static final String DISTANCE = "showdistance";
    public static final String COMPASS = "COMPASS";
    public static final String LOCATION = "LOCATION";
    public static final String TRACKCOLORING = "trackcoloring";
    public static final String UNITS = "units";
    public static final int UNITS_DEFAULT = 0;
    public static final int UNITS_IMPERIAL = 1;
    public static final int UNITS_METRIC = 2;
    public static final int UNITS_NAUTIC = 3;
    public static final int UNITS_METRICPACE = 4;
    public static final int UNITS_IMPERIALPACE = 5;
    public static final int UNITS_IMPERIALSURFACE = 6;
    public static final int UNITS_METRICSURFACE = 7;
    public static final String SDDIR_DIR = "SDDIR_DIR";
    public static final String DEFAULT_EXTERNAL_DIR = "/OpenGPSTracker/";
    public static final String TMPICTUREFILE_SUBPATH = "media_tmp.tmp";
    public static final Uri NAME_URI = Uri.parse("content://" + ContentConstants.AUTHORITY + ".string");
    public static final String EXPORT_TYPE = "SHARE_TYPE";
    public static final String EXPORT_GPXTARGET = "EXPORT_GPXTARGET";
    public static final String EXPORT_KMZTARGET = "EXPORT_KMZTARGET";
    public static final String EXPORT_TXTTARGET = "EXPORT_TXTTARGET";
    public static final double MIN_STATISTICS_SPEED = 1.0d;

    public static final String NAME = "NAME";
    public static final int SECTIONED_HEADER_ITEM_VIEW_TYPE = 0;
    public static final String AUTHORITY = "nl.sogeti.android.gpstracker.fileprovider";
    public static final String FILE_SCHEME = "file";
    public static final String PATH_SEPARATOR = "/";

    public static File getSdCardTmpFile(Context context) {
        File dir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dir = new File(getMarshmallowTmpFolder(context), TMPICTUREFILE_SUBPATH);
        } else {
            dir = new File(getLegacyTmpFile(context), TMPICTUREFILE_SUBPATH);
        }

        return dir;
    }

    @NonNull
    private static File getLegacyTmpFile(Context context) {
        return new File(getStorageDirectory(context), TMPICTUREFILE_SUBPATH);
    }

    @NonNull
    private static File getMarshmallowTmpFolder(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }
        File media = new File(cacheDir, "media");
        media.mkdirs();

        return media;
    }

    /**
     * Based on preference return the SD-Card directory in which Open GPS Tracker creates and stores files shared tracks,
     *
     * @param ctx
     * @return
     */
    public static File getStorageDirectory(Context ctx) {
        File dir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dir = getMarshmallowSdCardDirectory(ctx);
        } else {
            dir = getLegacySdCardDirectory(ctx);
        }

        return dir;
    }

    private static File getMarshmallowSdCardDirectory(Context context) {
        File sdcardFolder = context.getExternalFilesDir("tracks");
        if (sdcardFolder == null) {
            sdcardFolder = new File(context.getFilesDir(), "tracks");
            sdcardFolder.mkdirs();
        }

        return sdcardFolder;
    }

    @NonNull
    private static File getLegacySdCardDirectory(Context ctx) {
        // Read preference and ensure start and end with '/' symbol
        String dir = PreferenceManager.getDefaultSharedPreferences(ctx).getString(SDDIR_DIR, DEFAULT_EXTERNAL_DIR);
        if (!dir.startsWith("/")) {
            dir = "/" + dir;
        }
        if (!dir.endsWith("/")) {
            dir = dir + "/";
        }
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), dir);
        // If neither exists or can be created fall back to default
        if (!file.exists() && !file.mkdirs()) {
            file = new File(Environment.getExternalStorageDirectory(), DEFAULT_EXTERNAL_DIR);
        }

        return file;
    }

    public static Uri getUriFromFile(Context context, File newFile) {
        Uri fileUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fileUri = FileProvider.getUriForFile(context, AUTHORITY, newFile);
        } else {
            fileUri = new Uri.Builder()
                    .scheme(FILE_SCHEME)
                    .appendEncodedPath(PATH_SEPARATOR)
                    .appendEncodedPath(newFile.getAbsolutePath())
                    .build();
        }

        return fileUri;
    }

    public static Uri getPictureUriFromFile(Context context, File newFile, String width, String height) {
        return getUriFromFile(context, newFile).buildUpon()
                .appendQueryParameter("width", width)
                .appendQueryParameter("height", height)
                .build();
    }

    /**
     * Just to start failing early
     *
     * @throws IOException
     */
    private static void verifySdCardAvailibility() throws IOException {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            throw new IOException("The ExternalStorage is not mounted, unable to export files for sharing.");
        }
    }

}
