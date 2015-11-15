/*
 *   Written by Tom van Braeckel @ http://code.google.com/u/tomvanbraeckel/
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
package nl.sogeti.android.gpstracker.service.startstop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import nl.sogeti.android.gpstracker.service.logger.Constants;
import nl.sogeti.android.gpstracker.service.logger.GPSLoggerService;
import nl.sogeti.android.log.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(this, "BootReceiver.onReceive(), probably ACTION_BOOT_COMPLETED");
        String action = intent.getAction();

        // start on BOOT_COMPLETED
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(this, "BootReceiver received ACTION_BOOT_COMPLETED");

            // check in the settings if we need to auto start
            boolean startImmidiatly = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                    Constants.STARTUPATBOOT, false);

            if (startImmidiatly) {
                Log.d(this, "Starting LoggerMap activity...");
                context.startService(new Intent(context, GPSLoggerService.class));
            } else {
                Log.i(this, "Not starting Logger Service. Adjust the settings if you wanted this !");
            }
        } else {
            // this shouldn't happen !
            Log.w(this, "OpenGPSTracker's BootReceiver received " + action + ", but it's only able to respond to " +
                    Intent.ACTION_BOOT_COMPLETED + ". This shouldn't happen !");
        }
    }
}
