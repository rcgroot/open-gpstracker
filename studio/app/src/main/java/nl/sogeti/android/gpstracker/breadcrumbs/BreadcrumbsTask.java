/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) May 29, 2011 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.breadcrumbs;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.concurrent.Executor;

import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;

/**
 * ????
 *
 * @author rene (c) May 29, 2011, Sogeti B.V.
 * @version $Id:$
 */
public abstract class BreadcrumbsTask extends AsyncTask<Void, Void, Void>
{
   private static final String TAG = "OGT.BreadcrumbsTask";
   protected BreadcrumbsService mService;
   protected Context mContext;
   private ProgressListener mListener;
   private String mErrorText;
   private Exception mException;
   private String mTask;

   public BreadcrumbsTask(Context context, BreadcrumbsService adapter, ProgressListener listener)
   {
      mContext = context;
      mListener = listener;
      mService = adapter;
   }

   @TargetApi(11)
   public void executeOn(Executor executor)
   {
      if (Build.VERSION.SDK_INT >= 11)
      {
         executeOnExecutor(executor);
      }
      else
      {
         execute();
      }
   }

   protected void handleError(String task, Exception e, String text)
   {
      Log.e(TAG, "Received error will cancel background task " + this.getClass().getName(), e);

      mService.removeAuthentication();
      mTask = task;
      mException = e;
      mErrorText = text;
      cancel(true);
   }

   @Override
   protected void onPreExecute()
   {
      if (mListener != null)
      {
         mListener.setIndeterminate(true);
         mListener.started();
      }
   }

   @Override
   protected void onPostExecute(Void result)
   {
      this.updateTracksData(mService.getBreadcrumbsTracks());
      if (mListener != null)
      {
         mListener.finished(null);
      }
   }

   protected abstract void updateTracksData(BreadcrumbsTracks tracks);

   @Override
   protected void onCancelled()
   {
      if (mListener != null)
      {
         mListener.finished(null);
      }
      if (mListener != null && mErrorText != null && mException != null)
      {
         mListener.showError(mTask, mErrorText, mException);
      }
      else if (mException != null)
      {
         Log.e(TAG, "Incomplete error after cancellation:" + mErrorText, mException);
      }
      else
      {
         Log.e(TAG, "Incomplete error after cancellation:" + mErrorText);
      }
   }
}
