/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Jul 9, 2011 Sogeti Nederland B.V. All Rights Reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.util.Constants;

import org.apache.ogt.http.HttpEntity;
import org.apache.ogt.http.HttpException;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.HttpClient;
import org.apache.ogt.http.client.methods.HttpPost;
import org.apache.ogt.http.entity.mime.MultipartEntity;
import org.apache.ogt.http.entity.mime.content.FileBody;
import org.apache.ogt.http.entity.mime.content.StringBody;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.util.EntityUtils;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * ????
 *
 * @version $Id:$
 * @author rene (c) Jul 9, 2011, Sogeti B.V.
 */
public class JogmapSharing extends GpxCreator
{


   private static final String TAG = "OGT.JogmapSharing";
   private String jogmapResponseText;

   public JogmapSharing(Context context, Uri trackUri, String chosenBaseFileName, boolean attachments, ProgressListener listener)
   {
      super(context, trackUri, chosenBaseFileName, attachments, listener);
   }

   @Override
   protected Uri doInBackground(Void... params)
   {
      Uri result = super.doInBackground(params);
      sendToJogmap(result);
      return result;
   }
   
   @Override
   protected void onPostExecute(Uri resultFilename)
   {
      super.onPostExecute(resultFilename);
      
      CharSequence text = mContext.getString(R.string.osm_success) + jogmapResponseText;
      Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_LONG);
      toast.show();   
   }
   
   private void sendToJogmap(Uri fileUri)
   {
      String authCode = PreferenceManager.getDefaultSharedPreferences(mContext).getString(Constants.JOGRUNNER_AUTH, "");
      File gpxFile = new File(fileUri.getEncodedPath());
      HttpClient httpclient = new DefaultHttpClient();
      URI jogmap = null;
      int statusCode = 0;
      HttpEntity responseEntity = null;
      try
      {
         jogmap = new URI(mContext.getString(R.string.jogmap_post_url));
         HttpPost method = new HttpPost(jogmap);

         MultipartEntity entity = new MultipartEntity();
         entity.addPart("id", new StringBody(authCode));
         entity.addPart("mFile", new FileBody(gpxFile));
         method.setEntity(entity);
         HttpResponse response = httpclient.execute(method);

         statusCode = response.getStatusLine().getStatusCode();
         responseEntity = response.getEntity();
         InputStream stream = responseEntity.getContent();
         jogmapResponseText = XmlCreator.convertStreamToString(stream);
      }
      catch (IOException e)
      {
         String text = mContext.getString(R.string.jogmap_failed) + e.getLocalizedMessage();
         handleError(mContext.getString(R.string.jogmap_task), e, text);
      }
      catch (URISyntaxException e)
      {
         String text = mContext.getString(R.string.jogmap_failed) + e.getLocalizedMessage();
         handleError(mContext.getString(R.string.jogmap_task), e, text);
      }
      finally
      {
         if (responseEntity != null)
         {
            try
            {
               EntityUtils.consume(responseEntity);
            }
            catch (IOException e)
            {
               Log.e(TAG, "Failed to close the content stream", e);
            }
         }
      }
      if (statusCode != 200)
      {
         Log.e(TAG, "Wrong status code " + statusCode);
         jogmapResponseText = mContext.getString(R.string.jogmap_failed) + jogmapResponseText;
         handleError(mContext.getString(R.string.jogmap_task), new HttpException("Unexpected status reported by Jogmap"), jogmapResponseText);
      }
   }

   
}
