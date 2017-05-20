/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) 2017 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.actions.tasks;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Copies a ContentProvider file to a directory
 */
public class ContentProviderFileExtractor {

    private Context context;

    public ContentProviderFileExtractor(Context context) {
        this.context = context;
    }

    public File copyIntoDirectory(Uri source, File targetDirectory) {
        String fileName = source.getLastPathSegment();
        File targetFile = new File(targetDirectory, fileName);
        ParcelFileDescriptor description = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        try {
            byte[] buffer = new byte[8192];
            description = context.getContentResolver().openFileDescriptor(source, "r");
            fis = new FileInputStream(description.getFileDescriptor());
            bis = new BufferedInputStream(fis, 8192);
            fos = new FileOutputStream(targetFile);
            bos = new BufferedOutputStream(fos, 8192);

            int n;
            while ((n = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }
        } catch (IOException e) {
            targetFile = null;
        } finally {
            close(bos);
            close(bis);
            close(fos);
            close(fis);
            close(description);
        }

        return targetFile;
    }

    private void close(ParcelFileDescriptor description) {
        if (description != null) {
            try {
                description.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
