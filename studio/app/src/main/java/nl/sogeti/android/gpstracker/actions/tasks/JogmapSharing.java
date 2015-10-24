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

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.MultipartStreamer;

/**
 * ????
 *
 * @author rene (c) Jul 9, 2011, Sogeti B.V.
 * @version $Id:$
 */
public class JogmapSharing extends GpxCreator
{

   private static final String TAG = "OGT.JogmapSharing";
   private String jogmapResponseText;

   public JogmapSharing(Context context, Uri trackUri, String chosenBaseFileName, boolean attachments,
                        ProgressListener listener)
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

   private void sendToJogmap(Uri fileUri)
   {
      String authCode = PreferenceManager.getDefaultSharedPreferences(mContext).getString(Constants.JOGRUNNER_AUTH, "");
      File gpxFile = new File(fileUri.getEncodedPath());
      URL jogmap = null;
      int statusCode = 0;
      HttpURLConnection connection = null;
      MultipartStreamer multipart = null;
      try
      {
         jogmap = new URL(mContext.getString(R.string.jogmap_post_url));
         connection = (HttpURLConnection) jogmap.openConnection();
         multipart = new MultipartStreamer(connection, MultipartStreamer.HttpMultipartMode.STRICT, MultipartStreamer
               .StreamingMode.DEFAULT);
         multipart.addFormField("id", authCode);
         multipart.addFilePart("mFile", gpxFile);
         multipart.flush();

         statusCode = connection.getResponseCode();
         InputStream stream = connection.getInputStream();
         jogmapResponseText = XmlCreator.convertStreamToString(stream);
      }
      catch (IOException e)
      {
         String text = mContext.getString(R.string.jogmap_failed) + e.getLocalizedMessage();
         handleError(mContext.getString(R.string.jogmap_task), e, text);
      }
      finally
      {
         close(multipart);
         if (connection != null)
         {
            connection.disconnect();
         }
      }
      if (statusCode != 200)
      {
         Log.e(TAG, "Wrong status code " + statusCode);
         jogmapResponseText = mContext.getString(R.string.jogmap_failed) + jogmapResponseText;
         handleError(mContext.getString(R.string.jogmap_task), new HttpException("Unexpected status reported by " +
               "Jogmap"), jogmapResponseText);
      }
   }

   private void close(Closeable connection)
   {
      try
      {
         if (connection != null)
         {
            connection.close();
         }
      }
      catch (IOException e)
      {
         Log.w(TAG, "Failed to close ", e);
      }
   }

   @Override
   protected void onPostExecute(Uri resultFilename)
   {
      super.onPostExecute(resultFilename);

      CharSequence text = mContext.getString(R.string.jogmap_success) + jogmapResponseText;
      Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_LONG);
      toast.show();
   }
}
