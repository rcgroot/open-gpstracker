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
package nl.sogeti.android.gpstracker.adapter;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.actions.utils.xml.GpxParser;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.Pair;
import nl.sogeti.android.gpstracker.viewer.TrackList;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * ????
 * 
 * @version $Id:$
 * @author rene (c) Apr 24, 2011, Sogeti B.V.
 */
public class BreadcrumbsAdapter extends BaseAdapter
{
   private static final String TAG = "OGT.BreadcrumbsAdapter";
   /**
    * Time in milliseconds that a persisted breadcrumbs cache is used without a refresh
    */
   private static final long CACHE_TIMEOUT = 1000*60*10 ;
   boolean mOnline;
   private Context mContext;
   private LayoutInflater mInflater;
   private CommonsHttpOAuthConsumer mConsumer;
   private BreadcrumbsTracks mTracks;
   private DefaultHttpClient mHttpClient;

   private AsyncTask< ? , ? , ? > mOngoingTask;
   private Queue<AsyncTask< ? , ? , ? >> mPlannedTasks;

   private boolean mFinishing;
   private OnSharedPreferenceChangeListener tokenChangedListener;
   private ProgressListener mListener;

   public BreadcrumbsAdapter(Context ctx, ProgressListener listener)
   {
      super();
      mContext = ctx;
      mListener = listener;
      mInflater = LayoutInflater.from(mContext);
      mHttpClient = new DefaultHttpClient();
      mTracks = new BreadcrumbsTracks(mContext.getContentResolver());
      mPlannedTasks = new LinkedList<AsyncTask< ? , ? , ? >>();

      connectionSetup();
   }

   public void connectionSetup()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      String token = prefs.getString(TrackList.OAUTH_TOKEN, "");
      String secret = prefs.getString(TrackList.OAUTH_TOKEN_SECRET, "");
      mOnline = !"".equals(token) && !"".equals(secret);
      if (mOnline)
      {
         mConsumer = new CommonsHttpOAuthConsumer(mContext.getString(R.string.CONSUMER_KEY), mContext.getString(R.string.CONSUMER_SECRET));
         mConsumer.setTokenWithSecret(token, secret);
         
         Date persisted = mTracks.readCache(mContext);
         if (persisted == null || persisted.getTime() < new Date().getTime() - CACHE_TIMEOUT)
         {
            mPlannedTasks.add(new GetBreadcrumbsBundlesTask(this, mListener, mHttpClient, mConsumer));
            mPlannedTasks.add(new GetBreadcrumbsActivitiesTask(this, mListener, mHttpClient, mConsumer));
            executeNextTask();
         }
      }
      else
      {
         tokenChangedListener = new OnSharedPreferenceChangeListener()
         {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
               if (TrackList.OAUTH_TOKEN.equals(key))
               {
                  prefs.unregisterOnSharedPreferenceChangeListener(this);
                  connectionSetup();
               }
            }
         };
         prefs.registerOnSharedPreferenceChangeListener(tokenChangedListener);
      }
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getCount()
    */
   public int getCount()
   {
      if (mOnline)
      {
         return mTracks.positions();
      }
      else
      {
         return 1;
      }
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getItem(int)
    */
   public Object getItem(int position)
   {
      if (mOnline)
      {
         return mTracks.getItemForPosition(position);
      }
      else
      {
         return Constants.BREADCRUMBS_CONNECT;
      }

   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getItemId(int)
    */
   public long getItemId(int position)
   {
      return position;
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getView(int, android.view.View,
    * android.view.ViewGroup)
    */
   public View getView(int position, View convertView, ViewGroup parent)
   {
      View view;
      if (mOnline)
      {
         int type = getItemViewType(position);
         if (convertView == null)
         {
            switch (type)
            {
               case Constants.BREADCRUMBS_ACTIVITY_ITEM_VIEW_TYPE:
                  view = mInflater.inflate(R.layout.breadcrumbs_activity, null);
                  break;
               case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
                  view = mInflater.inflate(R.layout.breadcrumbs_bundle, null);
                  break;
               case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
                  view = mInflater.inflate(R.layout.breadcrumbs_track, null);
                  break;
               default:
                  view = new TextView(null);
                  break;
            }
         }
         else
         {
            view = convertView;
         }
         Pair<Integer, Integer> item = mTracks.getItemForPosition(position);
         String name;
         switch (type)
         {
            case Constants.BREADCRUMBS_ACTIVITY_ITEM_VIEW_TYPE:
               name = mTracks.getValueForItem((Pair<Integer, Integer>) item, BreadcrumbsTracks.NAME);
               ((TextView) view).setText(name);
               break;
            case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
               name = mTracks.getValueForItem((Pair<Integer, Integer>) item, BreadcrumbsTracks.NAME);
               ((TextView) view).setText(name);
               break;
            case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
               TextView nameView = (TextView) view.findViewById(R.id.listitem_name);
               TextView dateView = (TextView) view.findViewById(R.id.listitem_from);

               nameView.setText(mTracks.getValueForItem(item, BreadcrumbsTracks.NAME));
               String dateString = mTracks.getValueForItem(item, BreadcrumbsTracks.ENDTIME);
               try
               {
                  Long date = GpxParser.parseXmlDateTime(dateString);
                  dateView.setText(date.toString());
               }
               catch (ParseException e)
               {
                  Log.w(TAG, "Unable to parse Breadcrumbs end-time " + dateString);
               }
               break;
            default:
               view = new TextView(null);
               break;
         }
      }
      else
      {
         if (convertView == null)
         {
            view = mInflater.inflate(R.layout.breadcrumbs_connect, null);
         }
         else
         {
            view = convertView;
         }
         ((TextView) view).setText(R.string.breadcrumbs_connect);

      }
      return view;
   }

   @Override
   public int getViewTypeCount()
   {
      int types = 4;
      return types;
   }

   @Override
   public int getItemViewType(int position)
   {
      if (mOnline)
      {
         Pair<Integer, Integer> item = mTracks.getItemForPosition(position);
         if (item.first == Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE)
         {
            if (!mTracks.areTracksLoaded(item))
            {
               mPlannedTasks.add(new GetBreadcrumbsTracksTask(this, mListener, mHttpClient, mConsumer, item.second));
               executeNextTask();
            }
         }
         return item.first;
      }
      else
      {
         return Constants.BREADCRUMBS_CONNECT_ITEM_VIEW_TYPE;
      }
   }

   @Override
   public boolean areAllItemsEnabled()
   {
      return false;
   };

   @Override
   public boolean isEnabled(int position)
   {
      int itemViewType = getItemViewType(position);
      return itemViewType == Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE || itemViewType == Constants.BREADCRUMBS_CONNECT_ITEM_VIEW_TYPE;
   }

   public BreadcrumbsTracks getBreadcrumbsTracks()
   {
      return mTracks;
   }

   public synchronized void finishedTask()
   {
      mOngoingTask = null;
      notifyDataSetChanged();
      executeNextTask();
   }

   private synchronized void executeNextTask()
   {
      if (mPlannedTasks.size() > 0 && allTasksDone() && !mFinishing)
      {
         Object next = mPlannedTasks.poll();
         if (next instanceof GetBreadcrumbsActivitiesTask)
         {
            GetBreadcrumbsActivitiesTask task = (GetBreadcrumbsActivitiesTask) next;
            mOngoingTask = task;
            task.execute();
         }
         else if (next instanceof GetBreadcrumbsBundlesTask)
         {
            GetBreadcrumbsBundlesTask task = (GetBreadcrumbsBundlesTask) next;
            mOngoingTask = task;
            task.execute();
         }
         else if (next instanceof GetBreadcrumbsTracksTask)
         {
            GetBreadcrumbsTracksTask task = (GetBreadcrumbsTracksTask) next;
            mOngoingTask = task;
            task.execute();
         }
         else if (next instanceof DownloadBreadcrumbsTrackTask)
         {
            DownloadBreadcrumbsTrackTask task = (DownloadBreadcrumbsTrackTask) next;
            mOngoingTask = task;
            task.execute();
         }
         else if (next instanceof UploadBreadcrumbsTrackTask)
         {
            UploadBreadcrumbsTrackTask task = (UploadBreadcrumbsTrackTask) next;
            mOngoingTask = task;
            task.execute();
         }
         if (mOngoingTask != null && mOngoingTask instanceof GetBreadcrumbsTracksTask)
         {
            mPlannedTasks.clear();
         }
      }
   }

   public synchronized void shutdown()
   {
      mOnline = false;
      notifyDataSetChanged();
      mFinishing = true;
      if (mOngoingTask != null)
      {
         mOngoingTask.cancel(true);
      }
      if (allTasksDone())
      {
         mHttpClient.getConnectionManager().shutdown();
         mHttpClient = null;
      }
      if (tokenChangedListener != null)
      {
         PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(tokenChangedListener);
      }
      mTracks.persistCache(mContext);
   }

   private synchronized boolean allTasksDone()
   {
      return mOngoingTask == null || mOngoingTask.getStatus() == AsyncTask.Status.FINISHED;
   }

   public void startDownloadTask(TrackList trackList, Pair<Integer, Integer> track)
   {
      mPlannedTasks.add(new DownloadBreadcrumbsTrackTask(trackList, trackList, this, mHttpClient, mConsumer, track));
      executeNextTask();
   }

   public void startUploadTask(TrackList trackList, Uri trackUri)
   {
      mPlannedTasks.add(new UploadBreadcrumbsTrackTask(trackList, this, mHttpClient, mConsumer, trackUri));
      executeNextTask();
   }

   public boolean isOnline()
   {
      return mOnline;
   }
}
