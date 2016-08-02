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
package nl.sogeti.android.gpstracker.service.startstop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import nl.sogeti.android.gpstracker.integration.ServiceConstants;
import nl.sogeti.android.gpstracker.service.logger.GPSLoggerService;
import nl.sogeti.android.gpstracker.service.logger.LoggerPersistence;
import timber.log.Timber;

public class DockReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_DOCK_EVENT)) {
            Bundle extras = intent.getExtras();
            boolean start = false;
            boolean stop = false;
            if (extras != null && extras.containsKey(Intent.EXTRA_DOCK_STATE)) {
                int dockstate = extras.getInt(Intent.EXTRA_DOCK_STATE, -1);
                LoggerPersistence persistence = new LoggerPersistence(context);
                if (dockstate == Intent.EXTRA_DOCK_STATE_CAR) {
                    start = persistence.shouldLogAtDockCar();
                } else if (dockstate == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                    stop = persistence.shouldLogAtUndockCar();
                }
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
        } else {
            Timber.w("OpenGPSTracker's BootReceiver received " + action + ", but it's only able to respond to " +
                    Intent.ACTION_BOOT_COMPLETED + ". This shouldn't happen !");
        }
    }
}
