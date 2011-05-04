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

import java.text.ParseException;
import java.util.SortedSet;
import java.util.TreeSet;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.DateView;
import nl.sogeti.android.gpstracker.util.Pair;
import nl.sogeti.android.gpstracker.viewer.GpxParser;
import nl.sogeti.android.gpstracker.viewer.TrackList;
import oauth.signpost.OAuth;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
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
   boolean mOnline;
   private Context mContext;
   private LayoutInflater mInflater;
   private CommonsHttpOAuthConsumer mConsumer;
   private BreadcrumbsTracks mTracks;
   private DefaultHttpClient mHttpClient;
   private SortedSet<Integer> mBundlesTasks;
   private AsyncTask<Void, Void, BreadcrumbsTracks> mActivityTask;
   private AsyncTask<Void, Void, BreadcrumbsTracks> mBundlesTask;
   private AsyncTask<Integer, Void, BreadcrumbsTracks> mTracksTask;
   private SyncBreadcrumbsTrackTask mTrackSyncTask;
   private boolean mFinishing;

   public BreadcrumbsAdapter(Context ctx)
   {
      super();
      mContext = ctx;
      mInflater = LayoutInflater.from(mContext);
      mHttpClient = new DefaultHttpClient();;

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      String token = prefs.getString(OAuth.OAUTH_TOKEN, "");
      String secret = prefs.getString(OAuth.OAUTH_TOKEN_SECRET, "");
      mConsumer = new CommonsHttpOAuthConsumer(mContext.getString(R.string.CONSUMER_KEY), mContext.getString(R.string.CONSUMER_SECRET));
      mConsumer.setTokenWithSecret(token, secret);
      mOnline = !"".equals(token) && !"".equals(secret);

      mTracks = new BreadcrumbsTracks();
      mBundlesTasks = new TreeSet<Integer>();
      mBundlesTask = new GetBreadcrumbsBundlesTask(this, mHttpClient, mConsumer).execute();
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
               name = mTracks.getKeyForItem((Pair<Integer, Integer>) item, BreadcrumbsTracks.NAME);
               ((TextView) view).setText( name );
               break;
            case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
               name = mTracks.getKeyForItem((Pair<Integer, Integer>) item, BreadcrumbsTracks.NAME);
               ((TextView) view).setText( name );
               break;
            case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
               TextView nameView = (TextView) view.findViewById(R.id.listitem_name);
               TextView dateView = (TextView) view.findViewById(R.id.listitem_from);

               nameView.setText(mTracks.getKeyForItem(item, BreadcrumbsTracks.NAME));
               String dateString = mTracks.getKeyForItem(item, BreadcrumbsTracks.ENDTIME);
               try
               {
                  Long date = GpxParser.parseXmlDateTime(dateString);
                  dateView.setText(date.toString());
               }
               catch (ParseException e)
               {
                  Log.w( TAG, "Unable to parse Breadcrumbs end-time "+dateString );
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
               mBundlesTasks.add(item.second);
               executeNextRequest();
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
      return itemViewType == Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE || itemViewType == Constants.BREADCRUMBS_CONNECT_ITEM_VIEW_TYPE ; 
   }

   public BreadcrumbsTracks getBreadcrumbsTracks()
   {
      return mTracks;
   }

   public synchronized void finishedBundlesTask(GetBreadcrumbsBundlesTask getBreadcrumbsBundlesTask)
   {
      notifyDataSetChanged();
      if( !mFinishing )
      {
         mActivityTask = new GetBreadcrumbsActivitiesTask(this, mHttpClient, mConsumer);
         mActivityTask.execute();
      }
      mBundlesTask = null;
   }

   public synchronized void finishedActivitiesTask(GetBreadcrumbsActivitiesTask getBreadcrumbsActivitiesTask)
   {
      notifyDataSetChanged();
      mActivityTask = null;
      executeNextRequest();
   }

   public synchronized void finishedTrackTask(GetBreadcrumbsTracksTask getBreadcrumbsTracksTask)
   {
      notifyDataSetChanged();
      mTracksTask = null;
      executeNextRequest();
   }
   
   public synchronized void finishedTrackSyncTask(SyncBreadcrumbsTrackTask getBreadcrumbsTracksTask)
   {
      notifyDataSetChanged();
      mTrackSyncTask = null;
      executeNextRequest();
   }

   public void canceledTask(GetBreadcrumbsBundlesTask getBreadcrumbsBundlesTask)
   {
      mHttpClient.getConnectionManager().shutdown();
   }

   private synchronized void executeNextRequest()
   {
      if( mTrackSyncTask != null )
      {
         mTrackSyncTask.execute();
      }
      else if ( mBundlesTasks.size() > 0 && allTasksDone() && !mFinishing )
      {
         mTracksTask = new GetBreadcrumbsTracksTask(this, mHttpClient, mConsumer);
         Integer bundleId = mBundlesTasks.first();
         mBundlesTasks.clear();
         mTracksTask.execute(bundleId);
      }
   }

   public synchronized void shutdown()
   {
      mFinishing = true;
      if( mBundlesTask != null )
      {
         mBundlesTask.cancel(true);
      }
      if( mActivityTask != null )
      {
         mActivityTask.cancel(true);
      }
      if( mTracksTask != null )
      {
         mTracksTask.cancel(true);
      }
      if( mTrackSyncTask != null )
      {
         mTrackSyncTask.cancel(true);
      }
      if( allTasksDone() )
      {
         mHttpClient.getConnectionManager().shutdown();
      }
   }

   private synchronized boolean allTasksDone()
   {
      return (mBundlesTask        == null || mBundlesTask.getStatus()   == AsyncTask.Status.FINISHED)
            &&  (mActivityTask    == null || mActivityTask.getStatus()  == AsyncTask.Status.FINISHED) 
            &&  (mTracksTask      == null || mTracksTask.getStatus()    == AsyncTask.Status.FINISHED)
            &&  (mTrackSyncTask   == null || mTrackSyncTask.getStatus() == AsyncTask.Status.FINISHED);
   }

   public void startSyncAndOpenTask(TrackList trackList, Pair<Integer, Integer> track)
   {
      mTrackSyncTask = new SyncBreadcrumbsTrackTask( trackList, this, mHttpClient, mConsumer, track);
      executeNextRequest();
   }
   
}
