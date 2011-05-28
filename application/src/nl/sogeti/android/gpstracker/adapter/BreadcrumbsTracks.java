package nl.sogeti.android.gpstracker.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.Pair;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
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

   /**
    * Map from activityId to a dictionary
    */
   private static Map<Integer, Map<String, String>> sActivityMappings = new HashMap<Integer, Map<String, String>>();

   /**
    * Mapping from activityId to a list of bundleIds
    */
   private static Map<Integer, List<Integer>> sActivities = new LinkedHashMap<Integer, List<Integer>>();
   
   /**
    * Map from bundleId to a dictionary
    */
   private static Map<Integer, Map<String, String>> sBundleMappings = new HashMap<Integer, Map<String, String>>();

   /**
    * Map from bundleId to a dictionary
    */
   private Map<Integer, Map<String, String>> mTrackMappings = new HashMap<Integer, Map<String, String>>();



   /**
    * Mapping from bundleId to a list of trackIds
    */
   private Map<Integer, List<Integer>> mBundles = new LinkedHashMap<Integer, List<Integer>>();

   private ContentResolver mResolver;

   private Map<Long, Integer> mSyncedTracks;

   /**
    * Constructor: create a new BreadcrumbsTracks.
    * 
    * @param resolver Content resolver to obtain local breadcrumbs references
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
   }

   public Integer getBundleIdForTrackId(Integer trackId)
   {
      for (Integer bundlId : mBundles.keySet())
      {
         List<Integer> trackIds = mBundles.get(bundlId);
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
      sActivities.get(activityId).add(bundleId);

      if (!sBundleMappings.containsKey(bundleId))
      {
         sBundleMappings.put(bundleId, new HashMap<String, String>());
      }
      sBundleMappings.get(bundleId).put(NAME, bundleName);
      sBundleMappings.get(bundleId).put(DESCRIPTION, bundleDescription);
   }

   public void addTrack(Integer trackId, String trackName, Integer bundleId, String trackDescription, String difficulty, String startTime, String endTime,
         String isPublic, Float lat, Float lng, Float totalDistance, Integer totalTime, String trackRating)
   {

      mBundles.get(bundleId).add(trackId);

      if (!mTrackMappings.containsKey(trackId))
      {
         mTrackMappings.put(trackId, new HashMap<String, String>());
      }

      mTrackMappings.get(trackId).put(NAME, trackName);
      mTrackMappings.get(trackId).put(ISPUBLIC, isPublic);
      mTrackMappings.get(trackId).put(STARTTIME, startTime);
      mTrackMappings.get(trackId).put(ENDTIME, endTime);
      if (trackDescription != null)
      {
         mTrackMappings.get(trackId).put(DESCRIPTION, trackDescription);
      }
      if (trackDescription != null)
      {
         mTrackMappings.get(trackId).put(DIFFICULTY, difficulty);
      }
      if (trackRating != null)
      {
         mTrackMappings.get(trackId).put(RATING, trackRating);
      }

      //      mTrackMappings.get(trackId).put(LATITUDE, lat);
      //      mTrackMappings.get(trackId).put(LONGITUDE, lng);
      //      mTrackMappings.get(trackId).put(TOTALDISTANCE, totalDistance);
      //      mTrackMappings.get(trackId).put(TOTALTIME, totalTime);
   }

   public void createTracks(Integer bundleId)
   {
      mBundles.put(bundleId, new ArrayList<Integer>());
   }

   public boolean areTracksLoaded(Pair<Integer, Integer> item)
   {
      return mBundles.containsKey(item.second);
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
            int bundleSize = mBundles.get(bundleId) != null ? mBundles.get(bundleId).size() : 0;
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

            int bundleSize = mBundles.get(bundleId) != null ? mBundles.get(bundleId).size() : 0;
            if (countdown < bundleSize)
            {
               Integer trackId = mBundles.get(bundleId).get(countdown);
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
      return "BreadcrumbsTracks [mActivityMappings=" + sActivityMappings + ", mBundleMappings=" + sBundleMappings + ", mTrackMappings=" + mTrackMappings
            + ", mActivities=" + sActivities + ", mBundles=" + mBundles + "]";
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
                  Integer bcTrackId = Integer.valueOf(cursor.getString(1));
                  mSyncedTracks.put(trackId, bcTrackId);
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

   public boolean isLocalTrackSynced(Long qtrackId)
   {
      boolean uploaded = isLocalTrackOnline(qtrackId);
      boolean synced = mTrackMappings.containsKey(mSyncedTracks.get(qtrackId));
      return uploaded && synced;
   }

   public static SpinnerAdapter getActivityAdapter(Context ctx)
   {
      List<CharSequence> activities = new Vector<CharSequence>();
      for( Integer activityId : sActivityMappings.keySet() )
      {
         activities.add( sActivityMappings.get(activityId).get(NAME) );
      }
      ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(ctx, android.R.layout.simple_spinner_item, activities);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      return adapter;
   }

   public static SpinnerAdapter getBundleAdapter(Context ctx, CharSequence activity )
   {
      List<CharSequence> bundles = new Vector<CharSequence>();
      for( Integer activityId : sActivityMappings.keySet() )
      {
         if( sActivities.containsKey(activityId) && activity.equals( sActivityMappings.get(activityId).get(NAME) ) )
         {
            for( Integer bundleId : sActivities.get(activityId) )
            {
               bundles.add(ctx.getString(R.string.dialog_bundle, sBundleMappings.get(bundleId).get(NAME)) );
            }
         }
      }
      bundles.add(ctx.getString(R.string.dialog_bundle, ctx.getString(R.string.app_name)));
      ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(ctx, android.R.layout.simple_spinner_item, bundles);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      return adapter;
   }

   public static Integer getIdForActivity(String selectedItem)
   {
      for( Integer activityId : sActivityMappings.keySet() )
      {
         if( selectedItem.equals( sActivityMappings.get(activityId).get(NAME) ) );
         {
            return activityId;
         }
      }
      return -1;
   }

   public static Integer getIdForBundle(String selectedItem)
   {
      for( Integer bundleId : sBundleMappings.keySet() )
      {
         if( selectedItem.equals( sBundleMappings.get(bundleId).get(NAME) ) );
         {
            return bundleId;
         }
      }
      return -1;
   }

}
