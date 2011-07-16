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
package nl.sogeti.android.gpstracker.adapter.tasks;

import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsAdapter;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsTracks;
import android.os.AsyncTask;
import android.util.Log;

/**
 * ????
 * 
 * @version $Id:$
 * @author rene (c) May 29, 2011, Sogeti B.V.
 */
public abstract class BreadcrumbsTask extends AsyncTask<Void, Void, Void>
{
   private static final String TAG = "OGT.BreadcrumbsTask";

   private ProgressListener mListener;
   private String mErrorText;
   private Exception mException;

   private BreadcrumbsAdapter mAdapter;

   public BreadcrumbsTask(BreadcrumbsAdapter adapter, ProgressListener listener)
   {
      mListener = listener;
      mAdapter = adapter;
   }

   protected void handleError(Exception e, String text)
   {
      Log.e(TAG, "Received error will cancel background task " + this.getClass().getName(), e);
      mException = e;
      mErrorText = text;
      cancel(true);
   }

   @Override
   protected void onPreExecute()
   {
      if( mListener != null )
      {
         mListener.setIndeterminate(true);
         mListener.started();
      }
   }

   @Override
   protected void onPostExecute(Void result)
   {
      this.updateTracksData(mAdapter.getBreadcrumbsTracks());
      mAdapter.finishedTask();
      if( mListener != null )
      {
         mListener.finished(null);
      }
   }

   protected abstract void updateTracksData(BreadcrumbsTracks tracks);

   @Override
   protected void onCancelled()
   {
      if( mListener != null )
      {
         mListener.finished(null);
      }
      mAdapter.finishedTask();
      if (mListener != null && mErrorText != null && mException != null)
      {
         mListener.showError("retrieving data from GoBreadcrumbs", mErrorText, mException);
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
