package nl.sogeti.android.gpstracker.viewer.map.overlay;

import java.util.LinkedList;
import java.util.List;

import nl.sogeti.android.gpstracker.fragment.LoggerMapFragment;

import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;

import com.google.android.maps.GeoPoint;

public class BitmapSegmentsOverlay extends AsyncOverlay
{
   private static final String TAG = "GG.BitmapSegmentsOverlay";

   List<SegmentRendering> mOverlays;
   Handler mOverlayHandler;

   public BitmapSegmentsOverlay(LoggerMapFragment loggermap, Handler handler)
   {
      super(loggermap, handler);
      mOverlays = new LinkedList<SegmentRendering>();
      mOverlayHandler = handler;
   }

   @Override
   synchronized protected void redrawOffscreen(Canvas asyncBuffer, LoggerMapFragment loggermap)
   {
      for (SegmentRendering segment : mOverlays)
      {
         segment.draw(asyncBuffer);
      }
   }

   @Override
   public synchronized void scheduleRecalculation()
   {
      for (SegmentRendering segment : mOverlays)
      {
         segment.calculateMedia();
         segment.calculateTrack();
      }
   }

   @Override
   synchronized protected boolean commonOnTap(GeoPoint tappedGeoPoint)
   {
      boolean handled = false;
      for (SegmentRendering segment : mOverlays)
      {
         if (!handled)
         {
            handled = segment.commonOnTap(tappedGeoPoint);
         }
      }
      return handled;
   }

   synchronized public void addSegment(SegmentRendering segment)
   {
      segment.setBitmapHolder(this);
      mOverlays.add(segment);
   }

   synchronized public void clearSegments()
   {
      for (SegmentRendering segment : mOverlays)
      {
         segment.closeResources();
      }
      mOverlays.clear();
      reset();
   }

   synchronized public void setTrackColoringMethod(int color, double speed, double height)
   {
      for (SegmentRendering segment : mOverlays)
      {
         segment.setTrackColoringMethod(color, speed, height);
      }
      scheduleRecalculation();
   }

   public int size()
   {
      return mOverlays.size();
   }
}
