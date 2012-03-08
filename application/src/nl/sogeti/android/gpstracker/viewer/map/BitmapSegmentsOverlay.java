package nl.sogeti.android.gpstracker.viewer.map;

import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.os.Handler;

import com.google.android.maps.GeoPoint;

public class BitmapSegmentsOverlay extends AsyncOverlay
{
   List<SegmentRendering> mOverlays;
   Handler mOverlayHandler;
   
   BitmapSegmentsOverlay(LoggerMap loggermap, Handler handler)
   {
      super(loggermap, handler);
      mOverlays = new LinkedList<SegmentRendering>();
      mOverlayHandler = handler;
   }

   @Override
   synchronized protected void redrawOffscreen(Canvas asyncBuffer, LoggerMap loggermap)
   {
      for( SegmentRendering segment : mOverlays)
      {
         segment.draw(asyncBuffer);
      }
   }
   
   @Override 
   synchronized protected void scheduleRecalculation()
   {
      for( SegmentRendering segment : mOverlays)
      {
         segment.calculateMedia();
         segment.calculateTrack(); 
      }
   }

   @Override
   synchronized protected boolean commonOnTap(GeoPoint tappedGeoPoint)
   {
      boolean handled = false;
      for( SegmentRendering segment : mOverlays)
      {
         if( !handled )
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
      for( SegmentRendering segment : mOverlays)
      {
         segment.closeResources();
      }
      mOverlays.clear();
   }

   synchronized public void setTrackColoringMethod(int color, double speed, double height)
   {
      for( SegmentRendering segment : mOverlays)
      {
         segment.setTrackColoringMethod(color, speed, height);
      }
   }

   public int size()
   {
      return mOverlays.size();
   }
}
