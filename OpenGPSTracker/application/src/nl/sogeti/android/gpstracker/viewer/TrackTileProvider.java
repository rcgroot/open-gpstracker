package nl.sogeti.android.gpstracker.viewer;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;

import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.util.Constants;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

public class TrackTileProvider implements TileProvider
{
   private static final String TAG = "TrackTileProvider";
   private SharedPreferences mSharedPreferences;
   private ContentResolver resolver;
   private SegmentOverlay mLastSegmentOverlay;
   private List<SegmentOverlay> mOverlays;
   private long mTrackId;
   private Context mContext;

   public TrackTileProvider(Context ctx)
   {
      mContext = ctx;
      mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
      resolver = ctx.getContentResolver();
      mOverlays = new LinkedList<SegmentOverlay>();
   }

   public long setTrackId(long trackId, double avgSpeed)
   {
      mTrackId = trackId;
      Cursor segments = null;
      int trackColoringMethod = Integer.valueOf(mSharedPreferences.getString(Constants.TRACKCOLORING, "2")).intValue();
      long mLastSegment = -1;
      try
      {
         Uri segmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mTrackId + "/segments");
         segments = resolver.query(segmentsUri, new String[] { Segments._ID }, null, null, null);
         if (segments != null && segments.moveToFirst())
         {
            do
            {
               long segmentsId = segments.getLong(0);
               Uri segmentUri = ContentUris.withAppendedId(segmentsUri, segmentsId);
               SegmentOverlay segmentOverlay = new SegmentOverlay(mContext, segmentUri, trackColoringMethod, avgSpeed);
               mOverlays.add(segmentOverlay);
               mLastSegmentOverlay = segmentOverlay;
               if (segments.isFirst())
               {
                  segmentOverlay.addPlacement(SegmentOverlay.FIRST_SEGMENT);
               }
               if (segments.isLast())
               {
                  segmentOverlay.addPlacement(SegmentOverlay.LAST_SEGMENT);
               }
               mLastSegment = segmentsId;
            }
            while (segments.moveToNext());
         }
      }
      finally
      {
         if (segments != null)
         {
            segments.close();
         }
      }
      return mLastSegment;
   }

   public void shutdown()
   {
      for (SegmentOverlay segment : mOverlays)
      {
         segment.closeResources();
      }
   }

   @Override
   public Tile getTile(int x, int y, int zoom)
   {
      int WIDTH = 256;
      int HEIGHT = 256;
      Log.w(TAG, "public Tile getTile(int x:" + x + ", int y:" + y + ", int zoom:" + zoom + ")");
      Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bitmap);
      LatLng topleft = tileToGeopoint(x, y, zoom);
      LatLng bottomRight = tileToGeopoint(x + 1, y + 1, zoom);
      for (SegmentOverlay segment : mOverlays)
      {
         segment.draw(topleft, bottomRight, zoom, canvas);
      }

      Paint paint = new Paint();
      paint.setColor(Color.BLACK);
      canvas.drawText("(" + topleft + "," + bottomRight + ")", WIDTH / 2, HEIGHT / 2, paint);
      paint.setColor(Color.rgb(0, 0, 0));
      paint.setAlpha(128);
      paint.setStrokeWidth(10);
      canvas.drawRect(0, 0, WIDTH, HEIGHT, paint);

      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
      byte[] pixels = stream.toByteArray();
      return new Tile(WIDTH, HEIGHT, pixels);
   }

   /**
    * Based on the python example of http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    * 
    * @param xtile
    * @param ytile
    * @return LatLng of the north-west corner of the tile
    */
   private LatLng tileToGeopoint(int xtile, int ytile, int zoom)
   {
      double n = Math.pow(2, zoom);
      double lon_deg = xtile / n * 360.0 - 180.0;
      double lat_rad = Math.atan(Math.sinh(Math.PI * (1 - 2 * ytile / n)));
      double lat_deg = Math.toDegrees(lat_rad);

      return new LatLng(lat_deg, lon_deg);
   }

   public void setTrackColoringMethod(int coloring, double avgspeed)
   {
      for (SegmentOverlay segment : mOverlays)
      {
         segment.setTrackColoringMethod(coloring, avgspeed);
      }
   }

   public int getSegmentCount()
   {
      return mOverlays.size();
   }

   public void calculateMediaOnLastSegment()
   {
      if (mLastSegmentOverlay != null)
      {
         mLastSegmentOverlay.calculateMedia();
      }
   }

   public long getTrackId()
   {
      return mTrackId;
   }

}
