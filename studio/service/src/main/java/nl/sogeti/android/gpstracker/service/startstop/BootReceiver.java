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

import nl.sogeti.android.gpstracker.service.logger.GPSLoggerService;
import nl.sogeti.android.gpstracker.service.logger.LoggerPersistence;
import timber.log.Timber;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.d("BootReceiver.onReceive(), probably ACTION_BOOT_COMPLETED");
        String action = intent.getAction();

        // start on BOOT_COMPLETED
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Timber.d("BootReceiver received ACTION_BOOT_COMPLETED");
            LoggerPersistence persistence = new LoggerPersistence(context);
            // check in the settings if we need to auto start
            boolean startImmediately = persistence.shouldLogAtBoot();

            if (startImmediately) {
                Timber.d("Starting LoggerMap activity...");
                context.startService(new Intent(context, GPSLoggerService.class));
            } else {
                Timber.i("Not starting Logger Service. Adjust the settings if you wanted this !");
            }
        } else {
            // this shouldn't happen !
            Timber.w("OpenGPSTracker's BootReceiver received " + action + ", but it's only able to respond to " +
                    Intent.ACTION_BOOT_COMPLETED + ". This shouldn't happen !");
        }
    }
}
