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
import android.widget.Toast;

import java.io.File;

import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.util.Constants;

public class GpxSDCardStore extends GpxCreator {

    public GpxSDCardStore(Context context, Uri trackUri, String chosenBaseFileName, boolean attachments, ProgressListener
            listener) {
        super(context, trackUri, chosenBaseFileName, attachments, listener);
    }

    @Override
    protected Uri doInBackground(Void... params) {
        Uri contentUri = super.doInBackground(params);
        ContentProviderFileExtractor contentProviderFileExtractor = new ContentProviderFileExtractor(mContext);
        File targetDirectory = Constants.getExternalRootDataFolder(mContext);
        Uri resultFileUri;
        if (contentUri.getScheme() == "file") {
            resultFileUri = contentUri;
        } else {
            File sdcardFile = contentProviderFileExtractor.copyIntoDirectory(contentUri, targetDirectory);
            resultFileUri = Uri.fromFile(sdcardFile);
        }

        return resultFileUri;
    }

    @Override
    protected void onPostExecute(Uri resultFilename) {
        super.onPostExecute(resultFilename);
        Toast.makeText(mContext, resultFilename.getPath(), Toast.LENGTH_LONG).show();
    }
}
