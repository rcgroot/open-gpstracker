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
package nl.sogeti.android.gpstracker.streaming;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import nl.sogeti.android.gpstracker.service.util.ExternalConstants;

/**
 * Created by rene on 15-11-15.
 */
public class StartStop extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int state = intent.getIntExtra(ExternalConstants.EXTRA_LOGGING_STATE, ExternalConstants.STATE_UNKNOWN);
        if (state == ExternalConstants.STATE_LOGGING) {
            StreamUtils.initStreams(context);
        } else {
            StreamUtils.shutdownStreams(context);
        }
    }
}
