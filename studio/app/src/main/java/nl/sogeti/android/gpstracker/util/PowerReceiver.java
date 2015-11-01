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
package nl.sogeti.android.gpstracker.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import nl.sogeti.android.gpstracker.logger.GPSLoggerService;

public class PowerReceiver extends BroadcastReceiver
{
   @Override
   public void onReceive(Context context, Intent intent)
   {
      boolean start = false;
      boolean stop = false;
      String action = intent.getAction();
      Log.d(this, "OpenGPSTracker's PowerReceiver received: " + action);
      if (action.equals(Intent.ACTION_POWER_CONNECTED))
      {
         start = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.LOGATPOWERCONNECTED,
               false);
      }
      else if (action.equals(Intent.ACTION_POWER_DISCONNECTED))
      {
         stop = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.STOPATPOWERDISCONNECTED,
               false);
      }
      else
      {
         Log.w(this, "OpenGPSTracker's PowerReceiver received " + action + ", but it's only able to respond to " +
               Intent.ACTION_POWER_CONNECTED + " and " + Intent.ACTION_POWER_DISCONNECTED
               + ". This shouldn't happen!");
      }

      if (start)
      {
         Intent serviceIntent = new Intent(Constants.SERVICENAME);
         serviceIntent.putExtra(GPSLoggerService.COMMAND, GPSLoggerService.EXTRA_COMMAND_START);
         context.startService(serviceIntent);
      }
      else if (stop)
      {
         Intent serviceIntent = new Intent(Constants.SERVICENAME);
         serviceIntent.putExtra(GPSLoggerService.COMMAND, GPSLoggerService.EXTRA_COMMAND_STOP);
         context.startService(serviceIntent);
      }
   }
}