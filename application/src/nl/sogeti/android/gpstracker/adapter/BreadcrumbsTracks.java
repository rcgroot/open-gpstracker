package nl.sogeti.android.gpstracker.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.Pair;

public class BreadcrumbsTracks
{
   /**
    * Map from activityId to a dictionary
    */
   private Map<Integer, Map<String, String>> mActivityMappings = new HashMap<Integer, Map<String, String>>();
   
   /**
    * Map from bundleId to a dictionary
    */
   private Map<Integer, Map<String, String>> mBundleMappings = new HashMap<Integer, Map<String, String>>();

   /**
    * Map from bundleId to a dictionary
    */
   private Map<Integer, Map<String, String>> mTrackMappings = new HashMap<Integer, Map<String, String>>();

   
   /**
    * Mapping from activityId to a list of bundleIds
    */
   private Map<Integer, List<Integer> > mActivities = new LinkedHashMap<Integer, List<Integer>>();

   /**
    * Mapping from bundleId to a list of trackIds
    */
   private Map<Integer, List<Integer> > mBundles = new LinkedHashMap<Integer, List<Integer>>();
   
   public void addActivity(Integer activityId, String activityName)
   {
      if( !mActivityMappings.containsKey(activityId) )
      {
         mActivityMappings.put(activityId, new HashMap<String, String>() );
      }
      mActivityMappings.get(activityId).put("NAME", activityName);
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
      if( !mActivityMappings.containsKey(activityId) )
      {
         mActivityMappings.put(activityId, new HashMap<String, String>() );
      }
      if( !mActivities.containsKey(activityId) )
      {
         mActivities.put(activityId, new ArrayList<Integer>());
      }
      mActivities.get(activityId).add(bundleId);

      if( !mBundleMappings.containsKey(bundleId) )
      {
         mBundleMappings.put(bundleId, new HashMap<String, String>() );
      }
      mBundleMappings.get(bundleId).put("NAME", bundleName);
      mBundleMappings.get(bundleId).put("DESCRIPTION", bundleDescription);
   }
   
   public void addTrack(Integer trackId, String trackName, Integer bundleId, String trackDescription, String difficulty, String startTime, String endTime,
         Boolean isPublic, Float lat, Float lng, Float totalDistance, Integer totalTime, Integer trackRating)
   {

      mBundles.get(bundleId).add(trackId);
      
      if( !mTrackMappings.containsKey(trackId) )
      {
         mTrackMappings.put(trackId, new HashMap<String, String>() );
      }
      mTrackMappings.get(trackId).put("NAME", trackName);
      mTrackMappings.get(trackId).put("DESCRIPTION", trackDescription);
   }

   public void createTracks(Integer bundleId)
   {
      mBundles.put( bundleId, new ArrayList<Integer>() );
   }

   public boolean areTracksLoaded(Pair<Integer, Integer> item)
   {
      return mBundles.containsKey(item.second);
   }

   public int positions()
   {
      int size = 0;
      for( List<Integer> bundles : mActivities.values() )
      {
         size++;
         size += bundles.size();
         for( Integer bundleId : bundles )
         {
            int bundleSize = mBundles.get(bundleId) != null ? mBundles.get(bundleId).size() : 0 ;
            size += bundleSize;
         }
      }
      return size;
   }

   public Pair<Integer, Integer> getItemForPosition(int position)
   {
      int countdown = position;
      for( Integer activityId : mActivities.keySet() )
      {
         List<Integer> bundleList = mActivities.get(activityId);
         
         if( countdown == 0 )
         {
            return Pair.create( Constants.BREADCRUMBS_ACTIVITY_ITEM_VIEW_TYPE, activityId );
         }
         countdown--;
         
         for( Integer bundleId : bundleList )
         {
            if( countdown == 0 )
            {
               return Pair.create( Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE, bundleId );
            }
            countdown--;
            
            int bundleSize = mBundles.get(bundleId) != null ? mBundles.get(bundleId).size() : 0 ;
            if( countdown <  bundleSize )
            {
               Integer trackId = mBundles.get(bundleId).get(countdown);
               return Pair.create( Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE, trackId);
            }
            countdown -= bundleSize;
         }
      }
      return null;
   }

   public String getKeyForItem(Pair<Integer, Integer> item, String key)
   {
      String value = null;
      switch (item.first)
      {
         case Constants.BREADCRUMBS_ACTIVITY_ITEM_VIEW_TYPE:
            value = mActivityMappings.get(item.second).get(key);
            break;
         case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
            value = mBundleMappings.get(item.second).get(key);
            break;         
         case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
            value = mTrackMappings.get(item.second).get(key);
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
      return "BreadcrumbsTracks [mActivityMappings=" + mActivityMappings + ", mBundleMappings=" + mBundleMappings + ", mTrackMappings=" + mTrackMappings
            + ", mActivities=" + mActivities + ", mBundles=" + mBundles + "]";
   }

}
