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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import nl.sogeti.android.gpstracker.util.Constants;

import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.StatusLine;
import org.apache.ogt.http.client.ClientProtocolException;
import org.apache.ogt.http.client.HttpClient;
import org.apache.ogt.http.client.methods.HttpGet;
import org.apache.ogt.http.impl.client.DefaultHttpClient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

public class CustomUpload extends BroadcastReceiver
{
   private static CustomUpload sCustomUpload = null;
   private static final String TAG = "OGT.CustomUpload";
   
   public static synchronized void initStreaming(Context ctx)
   {
      Log.d( TAG, "Start");
      if( sCustomUpload != null )
      {
         shutdownStreaming(ctx);
      }
      sCustomUpload = new CustomUpload();

      IntentFilter filter = new IntentFilter(Constants.STREAMBROADCAST);   
      ctx.registerReceiver(sCustomUpload, filter);
   }

   public static synchronized void shutdownStreaming(Context ctx)
   {
      Log.d( TAG, "Stop");
      if( sCustomUpload != null )
      {
         ctx.unregisterReceiver(sCustomUpload);
         sCustomUpload.onShutdown();
         sCustomUpload = null;
      }
   }
   
   private void onShutdown()
   {
   }
   
   @Override
   public void onReceive(Context context, Intent intent)
   {
      String prefUrl = PreferenceManager.getDefaultSharedPreferences(context).getString("CUSTOMUPLOAD_URL", "http://www.example.com");
      Location loc = intent.getParcelableExtra(Constants.EXTRA_LOCATION);
      Uri trackUri =  intent.getParcelableExtra(Constants.EXTRA_TRACK);
      String buildUrl = prefUrl;
      buildUrl = buildUrl.replace("@LAT@", Double.toString(loc.getLatitude()));
      buildUrl = buildUrl.replace("@LON@", Double.toString(loc.getLongitude()));
      buildUrl = buildUrl.replace("@ID@", trackUri.getLastPathSegment());
      
      HttpClient client = new DefaultHttpClient();
      URI uploadUri;
      try
      {
         uploadUri = new URI(buildUrl);
         HttpGet request = new HttpGet(uploadUri );
         Log.d(TAG, "Upload URI "+ request + " with URI "+uploadUri);
         HttpResponse response = client.execute(request);
         StatusLine status = response.getStatusLine();
         if (status.getStatusCode() != 200) {
             throw new IOException("Invalid response from server: " + status.toString());
         }
      }
      catch (URISyntaxException e)
      {
         Log.e( TAG, "Fail!", e);
      }
      catch (ClientProtocolException e)
      {
         Log.e( TAG, "Fail!", e);
      }
      catch (IOException e)
      {
         Log.e( TAG, "Fail!", e);
      }
   }
}