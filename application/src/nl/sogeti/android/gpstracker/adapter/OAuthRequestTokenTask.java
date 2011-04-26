/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
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
package nl.sogeti.android.gpstracker.adapter;

import nl.sogeti.android.gpstracker.util.Constants;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request
 * token. (OAuthGetRequestToken) After receiving the request token from Twitter,
 * pop a browser to the user to authorize the Request Token.
 * (OAuthAuthorizeToken)
 */
public class OAuthRequestTokenTask extends AsyncTask<Void, Void, Void>
{

   final String TAG = "OGT.OAuthRequestTokenTask";
   private Context context;
   private OAuthProvider provider;
   private OAuthConsumer consumer;

   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param context Required to be able to start the intent to launch the
    *           browser.
    * @param provider The OAuthProvider object
    * @param consumer The OAuthConsumer object
    */
   public OAuthRequestTokenTask(Context context, OAuthConsumer consumer, OAuthProvider provider)
   {
      this.context = context;
      this.consumer = consumer;
      this.provider = provider;
   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to
    * authorize the token.
    */
   @Override
   protected Void doInBackground(Void... params)
   {

      try
      {
         final String url = provider.retrieveRequestToken(consumer, Constants.OAUTH_CALLBACK_URL);
         Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
         intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_FROM_BACKGROUND);
         context.startActivity(intent);
      }
      catch (Exception e)
      {
         Log.e(TAG, "Error during OAUth retrieve request token", e);
      }

      return null;
   }

}