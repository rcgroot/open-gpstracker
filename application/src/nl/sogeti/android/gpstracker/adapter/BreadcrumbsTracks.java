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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.Pair;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

/**
 * Model containing agregrated data retrieved from the GoBreadcrumbs.com API
 * 
 * @version $Id:$
 * @author rene (c) May 9, 2011, Sogeti B.V.
 */
public class BreadcrumbsTracks
{
   public static final String DESCRIPTION = "DESCRIPTION";

   public static final String NAME = "NAME";

   public static final String ENDTIME = "ENDTIME";

   public static final String TRACK_ID = "BREADCRUMBS_TRACK_ID";

   public static final String BUNDLE_ID = "BREADCRUMBS_BUNDLE_ID";

   public static final String ACTIVITY_ID = "BREADCRUMBS_ACTIVITY_ID";

   public static final String DIFFICULTY = "DIFFICULTY";

   private static final String STARTTIME = "STARTTIME";

   public static final String ISPUBLIC = "ISPUBLIC";

   public static final String RATING = "RATING";

   private static final String LATITUDE = "LATITUDE";

   private static final String LONGITUDE = "LONGITUDE";

   private static final String TOTALDISTANCE = "TOTALDISTANCE";

   private static final String TOTALTIME = "TOTALTIME";

   private static final String TAG = "OGT.BreadcrumbsTracks";

   private static final String BREADCRUMSB_CACHE_FILE = "breadcrumbs_cache_file.data";

   /**
    * Mapping from activityId to a list of bundleIds
    */
   private static Map<Integer, List<Integer>> sActivities = new LinkedHashMap<Integer, List<Integer>>();

   /**
    * Mapping from bundleId to a list of trackIds
    */
   private static Map<Integer, List<Integer>> sBundles = new LinkedHashMap<Integer, List<Integer>>();
   /**
    * Map from activityId to a dictionary containing keys like NAME
    */
   private static Map<Integer, Map<String, String>> sActivityMappings = new HashMap<Integer, Map<String, String>>();

   /**
    * Map from bundleId to a dictionary containing keys like NAME and
    * description
    */
   private static Map<Integer, Map<String, String>> sBundleMappings = new HashMap<Integer, Map<String, String>>();

   /**
    * Map from trackId to a dictionary containing keys like NAME, ISPUBLIC,
    * DESCRIPTION and more
    */
   private static Map<Integer, Map<String, String>> sTrackMappings = new HashMap<Integer, Map<String, String>>();

   private static Set<Pair<Integer, Integer>> sScheduledTracksLoading = new HashSet<Pair<Integer, Integer>>();
   /**
    * Cache of OGT Tracks that have a Breadcrumbs track id stored in the
    * meta-data table
    */
   private Map<Long, Integer> mSyncedTracks;

   private ContentResolver mResolver;

   /**
    * Constructor: create a new BreadcrumbsTracks.
    * 
    * @param resolver Content resolver to obtain local Breadcrumbs references
    */
   public BreadcrumbsTracks(ContentResolver resolver)
   {
      mResolver = resolver;
   }

   public Integer getBundleIdForTrackId(Integer trackId)
   {
      for (Integer bundlId : sBundles.keySet())
      {
         List<Integer> trackIds = sBundles.get(bundlId);
         if (trackIds.contains(trackId))
         {
            return bundlId;
         }
      }
      return null;
   }

   public Integer getActivityIdForBundleId(Integer bundleId)
   {
      for (Integer activityId : sActivities.keySet())
      {
         List<Integer> bundleIds = sActivities.get(activityId);
         if (bundleIds.contains(bundleId))
         {
            return activityId;
         }
      }
      return null;
   }

   public void addActivity(Integer activityId, String activityName)
   {
      if (!sActivityMappings.containsKey(activityId))
      {
         sActivityMappings.put(activityId, new HashMap<String, String>());
      }
      sActivityMappings.get(activityId).put(NAME, activityName);
   }

   /**
    * Add bundle to the track list
    * 
    * @param activityId
    * @param bundleId
    * @param bundleName
    * @param bundleDescription
    */
   public void addBundle(Integer activityId, Integer bundleId, String bundleName, String bundleDescription)
   {
      if (!sActivityMappings.containsKey(activityId))
      {
         sActivityMappings.put(activityId, new HashMap<String, String>());
      }
      if (!sActivities.containsKey(activityId))
      {
         sActivities.put(activityId, new ArrayList<Integer>());
      }
      if (!sActivities.get(activityId).contains(bundleId))
      {
         sActivities.get(activityId).add(bundleId);
      }

      if (!sBundleMappings.containsKey(bundleId))
      {
         sBundleMappings.put(bundleId, new HashMap<String, String>());
      }
      sBundleMappings.get(bundleId).put(NAME, bundleName);
      sBundleMappings.get(bundleId).put(DESCRIPTION, bundleDescription);
   }

   /**
    * Remove a bundle
    * 
    * @param deletedId
    */
   public void removeBundle(Integer deletedId)
   {
      sBundleMappings.remove(deletedId);
      sBundles.remove(deletedId);
      for (Integer activityId : sActivities.keySet())
      {
         if (sActivities.get(activityId).contains(deletedId))
         {
            sActivities.get(activityId).remove(deletedId);
         }
      }
   }

   /**
    * Get all bundles
    * 
    * @return
    */
   public Integer[] getAllBundleIds()
   {
      return sBundles.keySet().toArray(new Integer[sBundles.keySet().size()]);
   }

   /**
    * Add track to tracklist
    * 
    * @param trackId
    * @param trackName
    * @param bundleId
    * @param trackDescription
    * @param difficulty
    * @param startTime
    * @param endTime
    * @param isPublic
    * @param lat
    * @param lng
    * @param totalDistance
    * @param totalTime
    * @param trackRating
    */
   public void addTrack(Integer trackId, String trackName, Integer bundleId, String trackDescription, String difficulty, String startTime, String endTime,
         String isPublic, Float lat, Float lng, Float totalDistance, Integer totalTime, String trackRating)
   {

      if (!sBundles.get(bundleId).contains(trackId))
      {
         sBundles.get(bundleId).add(trackId);
         sScheduledTracksLoading.remove(Pair.create(Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE, trackId));
      }

      if (!sTrackMappings.containsKey(trackId))
      {
         sTrackMappings.put(trackId, new HashMap<String, String>());
      }
      putForTrack(trackId, NAME, trackName);
      putForTrack(trackId, ISPUBLIC, isPublic);
      putForTrack(trackId, STARTTIME, startTime);
      putForTrack(trackId, ENDTIME, endTime);
      putForTrack(trackId, DESCRIPTION, trackDescription);
      putForTrack(trackId, DIFFICULTY, difficulty);
      putForTrack(trackId, RATING, trackRating);
      putForTrack(trackId, LATITUDE, lat);
      putForTrack(trackId, LONGITUDE, lng);
      putForTrack(trackId, TOTALDISTANCE, totalDistance);
      putForTrack(trackId, TOTALTIME, totalTime);
   }

   private void putForTrack(Integer trackId, String key, Object value)
   {
      if (value != null)
      {
         sTrackMappings.get(trackId).put(key, value.toString());
      }
   }

   public void createTracks(Integer bundleId)
   {
      sBundles.put(bundleId, new ArrayList<Integer>());
   }

   public boolean areTracksLoaded(Pair<Integer, Integer> item)
   {
      return sBundles.containsKey(item.second) && item.first == Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE;
   }

   public int positions()
   {
      int size = 0;
      for (List<Integer> bundles : sActivities.values())
      {
         size++;
         size += bundles.size();
         for (Integer bundleId : bundles)
         {
            int bundleSize = sBundles.get(bundleId) != null ? sBundles.get(bundleId).size() : 0;
            size += bundleSize;
         }
      }
      return size;
   }

   public Pair<Integer, Integer> getItemForPosition(int position)
   {
      int countdown = position;
      for (Integer activityId : sActivities.keySet())
      {
         List<Integer> bundleList = sActivities.get(activityId);

         if (countdown == 0)
         {
            return Pair.create(Constants.BREADCRUMBS_ACTIVITY_ITEM_VIEW_TYPE, activityId);
         }
         countdown--;

         for (Integer bundleId : bundleList)
         {
            if (countdown == 0)
            {
               return Pair.create(Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE, bundleId);
            }
            countdown--;

            int bundleSize = sBundles.get(bundleId) != null ? sBundles.get(bundleId).size() : 0;
            if (countdown < bundleSize)
            {
               Integer trackId = sBundles.get(bundleId).get(countdown);
               return Pair.create(Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE, trackId);
            }
            countdown -= bundleSize;
         }
      }
      return null;
   }

   public String getValueForItem(Pair<Integer, Integer> item, String key)
   {
      String value = null;
      switch (item.first)
      {
         case Constants.BREADCRUMBS_ACTIVITY_ITEM_VIEW_TYPE:
            value = sActivityMappings.get(item.second).get(key);
            break;
         case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
            value = sBundleMappings.get(item.second).get(key);
            break;
         case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
            value = sTrackMappings.get(item.second).get(key);
            break;
         default:
            value = null;
            break;
      }
      return value;
   }

   @Override
   public String toString()
   {
      return "BreadcrumbsTracks [mActivityMappings=" + sActivityMappings + ", mBundleMappings=" + sBundleMappings + ", mTrackMappings=" + sTrackMappings
            + ", mActivities=" + sActivities + ", mBundles=" + sBundles + "]";
   }

   public boolean isLocalTrackOnline(Long qtrackId)
   {
      if (mSyncedTracks == null)
      {
         mSyncedTracks = new HashMap<Long, Integer>();
         Cursor cursor = null;
         try
         {
            cursor = mResolver.query(MetaData.CONTENT_URI, new String[] { MetaData.TRACK, MetaData.VALUE }, MetaData.KEY + " = ? ", new String[] { TRACK_ID },
                  null);
            if (cursor.moveToFirst())
            {
               do
               {
                  Long trackId = cursor.getLong(0);
                  try
                  {
                     Integer bcTrackId = Integer.valueOf(cursor.getString(1));
                     addSyncedTrack(trackId, bcTrackId);
                  }
                  catch (NumberFormatException e)
                  {
                     Log.w(TAG, "Illigal value stored as track id", e);
                  }
               }
               while (cursor.moveToNext());
            }
         }
         finally
         {
            if (cursor != null)
            {
               cursor.close();
            }
         }
      }
      boolean synced = mSyncedTracks.containsKey(qtrackId);
      return synced;
   }

   public void addSyncedTrack(Long trackId, Integer bcTrackId)
   {
      if (mSyncedTracks == null)
      {
         isLocalTrackOnline(-1l);
      }
      mSyncedTracks.put(trackId, bcTrackId);
   }

   public boolean isLocalTrackSynced(Long qtrackId)
   {
      boolean uploaded = isLocalTrackOnline(qtrackId);
      boolean synced = sTrackMappings.containsKey(mSyncedTracks.get(qtrackId));
      return uploaded && synced;
   }

   public SpinnerAdapter getActivityAdapter(Context ctx)
   {
      List<String> activities = new Vector<String>();
      for (Integer activityId : sActivityMappings.keySet())
      {
         String name = sActivityMappings.get(activityId).get(NAME);
         name = name != null ? name : ""; 
         activities.add(name);
      }
      Collections.sort(activities);
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, activities);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      return adapter;
   }

   public SpinnerAdapter getBundleAdapter(Context ctx, CharSequence activity)
   {
      List<String> bundles = new Vector<String>();
      for (Integer activityId : sActivityMappings.keySet())
      {
         if (sActivities.containsKey(activityId) && activity.equals(sActivityMappings.get(activityId).get(NAME)))
         {
            for (Integer bundleId : sActivities.get(activityId))
            {
               bundles.add( sBundleMappings.get(bundleId).get(NAME) );
            }
         }
      }
      Collections.sort(bundles);
      if( !bundles.contains(ctx.getString(R.string.app_name)))
      {
         bundles.add(ctx.getString(R.string.app_name));
      }
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, bundles);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      return adapter;
   }

   public static Integer getIdForActivity(String selectedItem)
   {
      for (Integer activityId : sActivityMappings.keySet())
      {
         if (selectedItem.equals(sActivityMappings.get(activityId).get(NAME)))
         {
            return activityId;
         }
      }
      return -1;
   }

   public static Integer getIdForBundle(String selectedItem)
   {
      for (Integer bundleId : sBundleMappings.keySet())
      {
         if (selectedItem.equals(sBundleMappings.get(bundleId).get(NAME)))
         {
            return bundleId;
         }
      }
      return -1;
   }

   /**
    * Read the static breadcrumbs data from private file
    * 
    * @param ctx
    * @return the date of persistence or null if failed
    */
   @SuppressWarnings("unchecked")
   public Date readCache(Context ctx)
   {
      FileInputStream fis = null;
      ObjectInputStream ois = null;
      Date persisted = null;
      try
      {
         fis = ctx.openFileInput(BREADCRUMSB_CACHE_FILE);
         ois = new ObjectInputStream(fis);

         Object[] cache = (Object[]) ois.readObject();
         // { activities, bundles, activityMappings, bundleMappings, trackMappings }
         Map<Integer, List<Integer>> activities = (Map<Integer, List<Integer>>) cache[1];
         sActivities = activities != null ? activities : sActivities;
         Map<Integer, List<Integer>> bundles = (Map<Integer, List<Integer>>) cache[2];
         sBundles = bundles != null ? bundles : sBundles;
         Map<Integer, Map<String, String>> activitymappings = (Map<Integer, Map<String, String>>) cache[3];
         sActivityMappings = activitymappings != null ? activitymappings : sActivityMappings;
         Map<Integer, Map<String, String>> bundlemappings = (Map<Integer, Map<String, String>>) cache[4];
         sBundleMappings = bundlemappings != null ? bundlemappings : sBundleMappings;
         Map<Integer, Map<String, String>> trackmappings = (Map<Integer, Map<String, String>>) cache[5];
         sTrackMappings = trackmappings != null ? trackmappings : sTrackMappings;

         persisted = (Date) cache[0];
      }
      catch (OptionalDataException e)
      {
         clearPersistentCache(ctx);
         Log.w(TAG, "Unable to read persisted breadcrumbs cache", e);
      }
      catch (ClassNotFoundException e)
      {
         clearPersistentCache(ctx);
         Log.w(TAG, "Unable to read persisted breadcrumbs cache", e);
      }
      catch (IOException e)
      {
         clearPersistentCache(ctx);
         Log.w(TAG, "Unable to read persisted breadcrumbs cache", e);
      }
      catch (ClassCastException e)
      {
         clearPersistentCache(ctx);
         Log.w(TAG, "Unable to read persisted breadcrumbs cache", e);
      }
      catch (ArrayIndexOutOfBoundsException e)
      {
         clearPersistentCache(ctx);
         Log.w(TAG, "Unable to read persisted breadcrumbs cache", e);
      }
      finally
      {
         if (fis != null)
         {
            try
            {
               fis.close();
            }
            catch (IOException e)
            {
               Log.w(TAG, "Error closing file stream after reading cache", e);
            }
         }
         if (ois != null)
         {
            try
            {
               ois.close();
            }
            catch (IOException e)
            {
               Log.w(TAG, "Error closing object stream after reading cache", e);
            }
         }
      }
      return persisted;
   }

   public void persistCache(Context ctx)
   {
      FileOutputStream fos = null;
      ObjectOutputStream oos = null;
      try
      {
         fos = ctx.openFileOutput(BREADCRUMSB_CACHE_FILE, Context.MODE_PRIVATE);
         oos = new ObjectOutputStream(fos);

         Map<Integer, List<Integer>> activities = sActivities;
         Map<Integer, List<Integer>> bundles = sBundles;
         Map<Integer, Map<String, String>> activityMappings = sActivityMappings;
         Map<Integer, Map<String, String>> bundleMappings = sBundleMappings;
         Map<Integer, Map<String, String>> trackMappings = sTrackMappings;

         Object[] cache = new Object[] { new Date(), activities, bundles, activityMappings, bundleMappings, trackMappings };
         oos.writeObject(cache);
      }
      catch (FileNotFoundException e)
      {
         Log.e(TAG, "Error in file stream during persist cache", e);
      }
      catch (IOException e)
      {
         Log.e(TAG, "Error in object stream during persist cache", e);
      }
      finally
      {
         if (fos != null)
         {
            try
            {
               fos.close();
            }
            catch (IOException e)
            {
               Log.w(TAG, "Error closing file stream after writing cache", e);
            }
         }
         if (oos != null)
         {
            try
            {
               oos.close();
            }
            catch (IOException e)
            {
               Log.w(TAG, "Error closing object stream after writing cache", e);
            }
         }
      }
   }

   public void clearPersistentCache(Context ctx)
   {
      ctx.deleteFile(BREADCRUMSB_CACHE_FILE);
   }

   public void addTracksLoadingScheduled(Pair<Integer, Integer> item)
   {
      sScheduledTracksLoading.add(item);
   }

   public boolean areTracksLoadingScheduled(Pair<Integer, Integer> item)
   {
      return sScheduledTracksLoading.contains(item);
   }

   /**
    * Cleans old bundles based a set of all bundles
    * 
    * @param mBundleIds
    */
   public void setAllBundleIds(Set<Integer> mBundleIds)
   {
      for (Integer oldBundleId : getAllBundleIds())
      {
         if (!mBundleIds.contains(oldBundleId))
         {
            removeBundle(oldBundleId);
         }
      }
   }

}
