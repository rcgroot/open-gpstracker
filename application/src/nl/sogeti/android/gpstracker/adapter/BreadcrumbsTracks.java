package nl.sogeti.android.gpstracker.adapter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class BreadcrumbsTracks
{
   /**
    * Map from activityId to a name
    */
   private HashMap<Integer, String> mActivityMappings = new HashMap<Integer, String>();
   /**
    * Map from bundleId to a name
    */
   private HashMap<Integer, String> mBundleNameMappings = new HashMap<Integer, String>();
   /**
    * Map from bundleId to a description
    */
   private HashMap<Integer, String> mBundleDescriptionMappings = new HashMap<Integer, String>();
   
   /**
    * Mapping from activityId to a list of bundleIds
    */
   private LinkedHashMap<Integer, List<Integer> > activities = new LinkedHashMap<Integer, List<Integer>>();

   
   public void addBundle(Integer activityId, Integer bundleId, String bundleName, String bundleDescription)
   {
      if( !activities.containsKey(activityId) )
      {
         activities.put(activityId, new LinkedList<Integer>());
      }
      activities.get(activityId).add(bundleId);
      mBundleNameMappings.put(bundleId, bundleName);
      mBundleDescriptionMappings.put(bundleId, bundleDescription);
   }
   
   public void addActivity(Integer activityId, String activityName)
   {
      mActivityMappings.put(activityId, activityName);
   }
   
   public int positions()
   {
      int size = 0;
      for( List<Integer> bundles : activities.values() )
      {
         size++;
         size += bundles.size();
      }
      return size;
   }

   public Object getTrackForPosition(int position)
   {
      int countdown = position;
      for( Integer activity : activities.keySet() )
      {
         List<Integer> bundles = activities.get(activity);
         if( countdown == 0 )
         {
            return mActivityMappings.get(activity);
         }
         else if( countdown-1 < bundles.size() )
         {
            return mBundleNameMappings.get( bundles.get(countdown-1) );
         }
         else
         {
            countdown -= bundles.size()+1;
         }
      }
      return null;
   }

   @Override
   public String toString()
   {
      return "BreadcrumbsTracks [activityMappings=" + mActivityMappings + ", bundleNameMappings=" + mBundleNameMappings + ", bundleDescriptionMappings="
            + mBundleDescriptionMappings + ", activities=" + activities + "]";
   }

   
   
}
