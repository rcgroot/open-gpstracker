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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import nl.sogeti.android.gpstracker.settings.Helper;
import nl.sogeti.android.log.Log;


public class StreamUtils {
    /**
     * Initialize all appropriate stream listeners
     *
     * @param ctx
     */
    public static void initStreams(final Context ctx) {
        Log.d(StreamUtils.class, "initStreams(Context)");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean streams_enabled = sharedPreferences.getBoolean(Helper.BROADCAST_STREAM, false);
        if (streams_enabled && sharedPreferences.getBoolean("VOICEOVER_ENABLED", false)) {
            VoiceOver.initStreaming(ctx);
        } else {
            VoiceOver.shutdownStreaming(ctx);
        }
        if (streams_enabled && sharedPreferences.getBoolean("CUSTOMUPLOAD_ENABLED", false)) {
            CustomUpload.initStreaming(ctx);
        } else {
            CustomUpload.shutdownStreaming(ctx);
        }
    }
}
