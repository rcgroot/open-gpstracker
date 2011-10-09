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
import java.util.Observable;
import java.util.Set;
import java.util.Vector;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaDataColumns;
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
public class BreadcrumbsTracks extends Observable
{
   public static final String DESCRIPTION = "DESCRIPTION";

   public static final String NAME = "NAME";

   public static final String ENDTIME = "ENDTIME";

   public static final String TRACK_ID = "BREADCRUMBS_TRACK_ID";

   public static final String BUNDLE_ID = "BREADCRUMBS_BUNDLE_ID";

   public static final String ACTIVITY_ID = "BREADCRUMBS_ACTIVITY_ID";

   public static final String DIFFICULTY = "DIFFICULTY";

   public static final String STARTTIME = "STARTTIME";

   public static final String ISPUBLIC = "ISPUBLIC";

   public static final String RATING = "RATING";

   public static final String LATITUDE = "LATITUDE";

   public static final String LONGITUDE = "LONGITUDE";

   public static final String TOTALDISTANCE = "TOTALDISTANCE";

   public static final String TOTALTIME = "TOTALTIME";

   private static final String TAG = "OGT.BreadcrumbsTracks";

   private static final Integer CACHE_VERSION = new Integer(2);
   private static final String BREADCRUMSB_BUNDLES_CACHE_FILE = "breadcrumbs_bundles_cache.data";
   private static final String BREADCRUMSB_ACTIVITY_CACHE_FILE = "breadcrumbs_activity_cache.data";
   /**
    * Time in milliseconds that a persisted breadcrumbs cache is used without a
    * refresh
    */
   private static final long CACHE_TIMEOUT = 1000 * 60;//1000*60*10 ;
   
   /**
    * Mapping from activityId to a list of bundleIds
    */
   private static Map<Integer, List<Integer>> sActivitiesWithBundles;

   /**
    * Mapping from bundleId to a list of trackIds
    */
   private static Map<Integer, List<Integer>> sBundlesWithTracks;
   /**
    * Map from activityId to a dictionary containing keys like NAME
    */
   private static Map<Integer, Map<String, String>> sActivityMappings;

   /**
    * Map from bundleId to a dictionary containing keys like NAME and
    * DESCRIPTION
    */
   private static Map<Integer, Map<String, String>> sBundleMappings;

   /**
    * Map from trackId to a dictionary containing keys like NAME, ISPUBLIC,
    * DESCRIPTION and more
    */
   private static Map<Integer, Map<String, String>> sTrackMappings;
   /**
    * Cache of OGT Tracks that have a Breadcrumbs track id stored in the
    * meta-data table
    */
   private Map<Long, Integer> mSyncedTracks = null;

   private static Set<Pair<Integer, Integer>> sScheduledTracksLoading;
   
   static
   {
      BreadcrumbsTracks.initCacheVariables();
   }
   
   private static void initCacheVariables()
   {
      sActivitiesWithBundles = new LinkedHashMap<Integer, List<Integer>>();
      sBundlesWithTracks = new LinkedHashMap<Integer, List<Integer>>();
      sActivityMappings = new HashMap<Integer, Map<String, String>>();
      sBundleMappings = new HashMap<Integer, Map<String, String>>();
      sTrackMappings = new HashMap<Integer, Map<String, String>>();
      sScheduledTracksLoading = new HashSet<Pair<Integer, Integer>>();
   }
   
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

   public void addActivity(Integer activityId, String activityName)
   {
      if (!sActivityMappings.containsKey(activityId))
      {
         sActivityMappings.put(activityId, new HashMap<String, String>());
      }
      sActivityMappings.get(activityId).put(NAME, activityName);
      setChanged ();
      notifyObservers();
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
      if (!sActivitiesWithBundles.containsKey(activityId))
      {
         sActivitiesWithBundles.put(activityId, new ArrayList<Integer>());
      }
      if (!sActivitiesWithBundles.get(activityId).contains(bundleId))
      {
         sActivitiesWithBundles.get(activityId).add(bundleId);
      }
      if (!sBundleMappings.containsKey(bundleId))
      {
         sBundleMappings.put(bundleId, new HashMap<String, String>());
      }
      if (!sBundlesWithTracks.containsKey(bundleId))
      {
         sBundlesWithTracks.put(bundleId, new ArrayList<Integer>());
      }
      sBundleMappings.get(bundleId).put(NAME, bundleName);
      sBundleMappings.get(bundleId).put(DESCRIPTION, bundleDescription);
      setChanged ();
      notifyObservers();
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
      if (!sBundlesWithTracks.containsKey(bundleId))
      {
         sBundlesWithTracks.put(bundleId, new ArrayList<Integer>());
      }
      if (!sBundlesWithTracks.get(bundleId).contains(trackId))
      {
         sBundlesWithTracks.get(bundleId).add(trackId);
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
      notifyObservers();
   }

   public void addSyncedTrack(Long trackId, Integer bcTrackId)
   {
      if (mSyncedTracks == null)
      {
         isLocalTrackOnline(-1l);
      }
      mSyncedTracks.put(trackId, bcTrackId);
      setChanged ();
      notifyObservers();
   }

   public void addTracksLoadingScheduled(Pair<Integer, Integer> item)
   {
      sScheduledTracksLoading.add(item);
      setChanged ();
      notifyObservers();
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
   
   public void setAllTracksForBundleId(Integer mBundleId, Set<Integer> updatedbcTracksIdList)
   {
      List<Integer> trackIdList = sBundlesWithTracks.get(mBundleId);
      for( int location = 0 ; location < trackIdList.size() ; location ++ )
      {
         Integer oldTrackId = trackIdList.get(location);
         if (!updatedbcTracksIdList.contains(oldTrackId))
         {
            removeTrack(mBundleId, oldTrackId);
         }
      }
      setChanged ();
      notifyObservers();
   }

   private void putForTrack(Integer trackId, String key, Object value)
   {
      if (value != null)
      {
         sTrackMappings.get(trackId).put(key, value.toString());
      }
      setChanged ();
      notifyObservers();
   }

   /**
    * Remove a bundle
    * 
    * @param deletedId
    */
   public void removeBundle(Integer deletedId)
   {
      sBundleMappings.remove(deletedId);
      sBundlesWithTracks.remove(deletedId);
      for (Integer activityId : sActivitiesWithBundles.keySet())
      {
         if (sActivitiesWithBundles.get(activityId).contains(deletedId))
         {
            sActivitiesWithBundles.get(activityId).remove(deletedId);
         }
      }
      setChanged ();
      notifyObservers();
   }
   
   /**
    * Remove a track
    * 
    * @param deletedId
    */
   public void removeTrack(Integer bundleId, Integer trackId)
   {
      sTrackMappings.remove(trackId);
      if( sBundlesWithTracks.containsKey(bundleId) )
      {
         sBundlesWithTracks.get(bundleId).remove(trackId);
      }
      setChanged ();
      notifyObservers();
      
      mResolver.delete(MetaData.CONTENT_URI, MetaData.TRACK + " = ? AND " + MetaData.KEY + " = ? ", new String[] { trackId.toString(), TRACK_ID } );
      if( mSyncedTracks != null && mSyncedTracks.containsKey(trackId)) 
      {
         mSyncedTracks.remove(trackId);
      }
   }

   public int positions()
   {
      int size = 0;
      for (List<Integer> bundles : sActivitiesWithBundles.values())
      {
         size++;
         size += bundles.size();
         for (Integer bundleId : bundles)
         {
            int bundleSize = sBundlesWithTracks.get(bundleId) != null ? sBundlesWithTracks.get(bundleId).size() : 0;
            size += bundleSize;
         }
      }
      return size;
   }

   public Integer getBundleIdForTrackId(Integer trackId)
   {
      for (Integer bundlId : sBundlesWithTracks.keySet())
      {
         List<Integer> trackIds = sBundlesWithTracks.get(bundlId);
         if (trackIds.contains(trackId))
         {
            return bundlId;
         }
      }
      return null;
   }

   public Integer getActivityIdForBundleId(Integer bundleId)
   {
      for (Integer activityId : sActivitiesWithBundles.keySet())
      {
         List<Integer> bundleIds = sActivitiesWithBundles.get(activityId);
         if (bundleIds.contains(bundleId))
         {
            return activityId;
         }
      }
      return null;
   }

   /**
    * Get all bundles
    * 
    * @return
    */
   public Integer[] getAllBundleIds()
   {
      return sBundlesWithTracks.keySet().toArray(new Integer[sBundlesWithTracks.keySet().size()]);
   }

   public Pair<Integer, Integer> getItemForPosition(int position)
   {
      int countdown = position;
      for (Integer activityId : sActivitiesWithBundles.keySet())
      {
         List<Integer> bundleList = sActivitiesWithBundles.get(activityId);
   
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
   
            int bundleSize = sBundlesWithTracks.get(bundleId) != null ? sBundlesWithTracks.get(bundleId).size() : 0;
            if (countdown < bundleSize)
            {
               Integer trackId = sBundlesWithTracks.get(bundleId).get(countdown);
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
         if (sActivitiesWithBundles.containsKey(activityId) && activity.equals(sActivityMappings.get(activityId).get(NAME)))
         {
            for (Integer bundleId : sActivitiesWithBundles.get(activityId))
            {
               bundles.add(sBundleMappings.get(bundleId).get(NAME));
            }
         }
      }
      Collections.sort(bundles);
      if (!bundles.contains(ctx.getString(R.string.app_name)))
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

   public static Integer getIdForBundle(Integer activityId, String selectedItem)
   {
      List<Integer> bundles = sActivitiesWithBundles.get(activityId);
      bundles = bundles != null ? bundles : new LinkedList<Integer>();
      for (Integer bundleId : bundles)
      {
         if (selectedItem.equals(sBundleMappings.get(bundleId).get(NAME)))
         {
            return bundleId;
         }
      }
      return -1;
   }

   private boolean isLocalTrackOnline(Long qtrackId)
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
                     mSyncedTracks.put(trackId, bcTrackId);
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
         setChanged ();
         notifyObservers();
      }
      boolean synced = mSyncedTracks.containsKey(qtrackId);
      return synced;
   }

   public boolean isLocalTrackSynced(Long qtrackId)
   {
      boolean uploaded = isLocalTrackOnline(qtrackId);
      boolean synced = sTrackMappings.containsKey(mSyncedTracks.get(qtrackId));
      return uploaded && synced;
   }

   public boolean areTracksLoaded(Pair<Integer, Integer> item)
   {
      return sBundlesWithTracks.containsKey(item.second) && item.first == Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE;
   }

   public boolean areTracksLoadingScheduled(Pair<Integer, Integer> item)
   {
      return sScheduledTracksLoading.contains(item);
   }

   /**
    * Read the static breadcrumbs data from private file
    * 
    * @param ctx
    * @return is refresh is needed
    */
   @SuppressWarnings("unchecked")
   public boolean readCache(Context ctx)
   {
   
      FileInputStream fis = null;
      ObjectInputStream ois = null;
      Date bundlesPersisted = null, activitiesPersisted = null;
      Object[] cache;
      synchronized (BREADCRUMSB_BUNDLES_CACHE_FILE)
      {
         try
         {
            fis = ctx.openFileInput(BREADCRUMSB_BUNDLES_CACHE_FILE);
            ois = new ObjectInputStream(fis);
   
            cache = (Object[]) ois.readObject();
            // new Object[] { CACHE_VERSION, new Date(), sActivitiesWithBundles, sBundlesWithTracks, sBundleMappings, sTrackMappings };
            if( cache[0] instanceof Integer && CACHE_VERSION.equals(cache[0]))
            {
               bundlesPersisted = (Date) cache[1];
               Map<Integer, List<Integer>> activities = (Map<Integer, List<Integer>>) cache[2];
               Map<Integer, List<Integer>> bundles = (Map<Integer, List<Integer>>) cache[3];
               Map<Integer, Map<String, String>> bundlemappings = (Map<Integer, Map<String, String>>) cache[4];
               Map<Integer, Map<String, String>> trackmappings = (Map<Integer, Map<String, String>>) cache[5];
               sActivitiesWithBundles = activities != null ? activities : sActivitiesWithBundles;
               sBundlesWithTracks = bundles != null ? bundles : sBundlesWithTracks;
               sBundleMappings = bundlemappings != null ? bundlemappings : sBundleMappings;
               sTrackMappings = trackmappings != null ? trackmappings : sTrackMappings;
            }
            else 
            {
               clearPersistentCache(ctx);
            }
            
   
            fis = ctx.openFileInput(BREADCRUMSB_ACTIVITY_CACHE_FILE);
            ois = new ObjectInputStream(fis);
            cache = (Object[]) ois.readObject();
            // new Object[] { CACHE_VERSION, new Date(), sActivityMappings }; 
            if( cache[0] instanceof Integer && CACHE_VERSION.equals(cache[0]))
            {
               activitiesPersisted = (Date) cache[1];
               Map<Integer, Map<String, String>> activitymappings = (Map<Integer, Map<String, String>>) cache[2];
               sActivityMappings = activitymappings != null ? activitymappings : sActivityMappings;
            }
            else 
            {
               clearPersistentCache(ctx);
            }
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
      }
      setChanged ();
      notifyObservers();
   
      boolean refreshNeeded = false;
      refreshNeeded = refreshNeeded || bundlesPersisted == null || activitiesPersisted == null;
      refreshNeeded = refreshNeeded || (activitiesPersisted.getTime() < new Date().getTime() - CACHE_TIMEOUT * 10);
      refreshNeeded = refreshNeeded || (bundlesPersisted.getTime() < new Date().getTime() - CACHE_TIMEOUT);
   
      return refreshNeeded;
   }

   public void persistCache(Context ctx)
   {
   
      FileOutputStream fos = null;
      ObjectOutputStream oos = null;
      Object[] cache;
      synchronized (BREADCRUMSB_BUNDLES_CACHE_FILE)
      {
         try
         {
            fos = ctx.openFileOutput(BREADCRUMSB_BUNDLES_CACHE_FILE, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            cache = new Object[] { CACHE_VERSION, new Date(), sActivitiesWithBundles, sBundlesWithTracks, sBundleMappings, sTrackMappings };
            oos.writeObject(cache);
   
            fos = ctx.openFileOutput(BREADCRUMSB_ACTIVITY_CACHE_FILE, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            cache = new Object[] { CACHE_VERSION, new Date(), sActivityMappings };
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
   }
   
   public void clearAllCache(Context ctx)
   {
      BreadcrumbsTracks.initCacheVariables();
      setChanged ();
      clearPersistentCache(ctx);
      notifyObservers();
   }

   public void clearPersistentCache(Context ctx)
   {
      Log.w( TAG, "Deleting old Breadcrumbs cache files"); 
      synchronized (BREADCRUMSB_BUNDLES_CACHE_FILE)
      {
         ctx.deleteFile(BREADCRUMSB_ACTIVITY_CACHE_FILE);
         ctx.deleteFile(BREADCRUMSB_BUNDLES_CACHE_FILE);
      }
   }

   @Override
   public String toString()
   {
      return "BreadcrumbsTracks [mActivityMappings=" + sActivityMappings + ", mBundleMappings=" + sBundleMappings + ", mTrackMappings=" + sTrackMappings
            + ", mActivities=" + sActivitiesWithBundles + ", mBundles=" + sBundlesWithTracks + "]";
   }
}
