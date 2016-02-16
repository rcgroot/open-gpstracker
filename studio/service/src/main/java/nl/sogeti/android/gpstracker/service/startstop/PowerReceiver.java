/*------------------------------------------------------------------------------
 **    Author: Tobias Jahn <tjahn@users.sourceforge.net>
 ** Copyright: (c) Dec 1, 2012 Tobias Jahn, All Rights Reserved.
 **------------------------------------------------------------------------------
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

import nl.sogeti.android.gpstracker.integration.ServiceConstants;
import nl.sogeti.android.gpstracker.service.logger.GPSLoggerService;
import nl.sogeti.android.gpstracker.service.logger.LoggerPersistence;
import nl.sogeti.android.log.Log;

public class PowerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean start = false;
        boolean stop = false;
        String action = intent.getAction();
        Log.d(this, "OpenGPSTracker's PowerReceiver received: " + action);
        LoggerPersistence persistence = new LoggerPersistence(context);
        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            start = persistence.shouldLogAtPowerConnected();
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            stop = persistence.shouldLogAtPowerDisconnected();
        } else {
            Log.w(this, "OpenGPSTracker's PowerReceiver received " + action + ", but it's only able to respond to " +
                    Intent.ACTION_POWER_CONNECTED + " and " + Intent.ACTION_POWER_DISCONNECTED
                    + ". This shouldn't happen!");
        }

        if (start) {
            Intent serviceIntent = new Intent(context, GPSLoggerService.class);
            serviceIntent.putExtra(ServiceConstants.Commands.COMMAND, ServiceConstants.Commands.EXTRA_COMMAND_START);
            context.startService(serviceIntent);
        } else if (stop) {
            Intent serviceIntent = new Intent(context, GPSLoggerService.class);
            serviceIntent.putExtra(ServiceConstants.Commands.COMMAND, ServiceConstants.Commands.EXTRA_COMMAND_STOP);
            context.startService(serviceIntent);
        }
    }
}