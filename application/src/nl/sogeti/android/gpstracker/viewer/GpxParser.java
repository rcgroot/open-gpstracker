package nl.sogeti.android.gpstracker.viewer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.ProgressFilterInputStream;
import nl.sogeti.android.gpstracker.util.UnicodeReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;

public class GpxParser extends AsyncTask<Uri, Integer, Boolean>
{
   private static final String LATITUDE_ATRIBUTE = "lat";
   private static final String LONGITUDE_ATTRIBUTE = "lon";
   private static final String TRACK_ELEMENT = "trkpt";
   private static final String SEGMENT_ELEMENT = "trkseg";
   private static final String NAME_ELEMENT = "name";
   private static final String TIME_ELEMENT = "time";
   private static final String ELEVATION_ELEMENT = "ele";
   private static final String COURSE_ELEMENT = "course";
   private static final String ACCURACY_ELEMENT = "accuracy";
   private static final String SPEED_ELEMENT = "speed";
   public static final SimpleDateFormat ZULU_DATE_FORMAT    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
   public static final SimpleDateFormat ZULU_DATE_FORMAT_MS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
   public static final SimpleDateFormat ZULU_DATE_FORMAT_BC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'");
   protected static final int DEFAULT_UNKNOWN_FILESIZE = 1024 * 1024 * 10;
   static
   {
      TimeZone utc = TimeZone.getTimeZone("UTC");
      ZULU_DATE_FORMAT.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true
      ZULU_DATE_FORMAT_MS.setTimeZone(utc);

   }
   
   private ContentResolver mContentResolver;
   private Uri mTrackUri;
   private Uri mSegmentUri;
   protected String mErrorDialogMessage;
   protected Exception mErrorDialogException;
   protected TrackList mTrackList;
   private int mLength;
   
   public GpxParser(TrackList trackList)
   {
      mTrackList = trackList;
      mContentResolver = mTrackList.getContentResolver();
   }

   public Boolean importUri(Uri importFileUri) 
   {
      Boolean result = new Boolean(false);
      String trackName = null;
      InputStream fis = null;
      mLength = DEFAULT_UNKNOWN_FILESIZE;
      if (importFileUri.getScheme().equals("file"))
      {
         trackName = importFileUri.getLastPathSegment();
         File file = new File(importFileUri.getPath());
         mLength = file.length() < (long) Integer.MAX_VALUE ? (int) file.length() : Integer.MAX_VALUE;
      }
      try
      {
         fis = mContentResolver.openInputStream(importFileUri);
      }
      catch (IOException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_io);
         mErrorDialogException = e;
         result = new Boolean(false);
      }
      
      if( result.booleanValue() )
      {
         result = importTrack( fis, trackName);
      }
      
      return result;
   }

   public Boolean importTrack( InputStream fis, String trackName )
   {
      int eventType;
      ContentValues lastPosition = null;
      Vector<ContentValues> bulk = new Vector<ContentValues>();
      boolean speed = false;
      boolean accuracy = false;
      boolean bearing = false;
      boolean elevation = false;
      boolean name = false;
      boolean time = false;
      Long importDate = new Long(new Date().getTime());
      Boolean result;
      try
      {
         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         factory.setNamespaceAware(true);
         XmlPullParser xmlParser = factory.newPullParser();


         ProgressFilterInputStream pfis = new ProgressFilterInputStream(fis, this);
         BufferedInputStream bis = new BufferedInputStream(pfis);
         UnicodeReader ur = new UnicodeReader(bis, "UTF-8");
         xmlParser.setInput(ur);


         eventType = xmlParser.getEventType();

         String attributeName;

         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               if (xmlParser.getName().equals(NAME_ELEMENT))
               {
                  name = true;
               }
               else
               {
                  ContentValues trackContent = new ContentValues();
                  trackContent.put(Tracks.NAME, trackName);
                  if (xmlParser.getName().equals("trk"))
                  {
                     startTrack(trackContent);
                  }
                  else if (xmlParser.getName().equals(SEGMENT_ELEMENT))
                  {
                     startSegment();
                  }
                  else if (xmlParser.getName().equals(TRACK_ELEMENT))
                  {
                     lastPosition = new ContentValues();
                     for (int i = 0; i < 2; i++)
                     {
                        attributeName = xmlParser.getAttributeName(i);
                        if (attributeName.equals(LATITUDE_ATRIBUTE))
                        {
                           lastPosition.put(Waypoints.LATITUDE, new Double(xmlParser.getAttributeValue(i)));
                        }
                        else if (attributeName.equals(LONGITUDE_ATTRIBUTE))
                        {
                           lastPosition.put(Waypoints.LONGITUDE, new Double(xmlParser.getAttributeValue(i)));
                        }
                     }
                  }
                  else if (xmlParser.getName().equals(SPEED_ELEMENT))
                  {
                     speed = true;
                  }
                  else if (xmlParser.getName().equals(ACCURACY_ELEMENT))
                  {
                     accuracy = true;
                  }
                  else if (xmlParser.getName().equals(COURSE_ELEMENT))
                  {
                     bearing = true;
                  }
                  else if (xmlParser.getName().equals(ELEVATION_ELEMENT))
                  {
                     elevation = true;
                  }
                  else if (xmlParser.getName().equals(TIME_ELEMENT))
                  {
                     time = true;
                  }
               }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if (xmlParser.getName().equals(NAME_ELEMENT))
               {
                  name = false;
               }
               else if (xmlParser.getName().equals(SPEED_ELEMENT))
               {
                  speed = false;
               }
               else if (xmlParser.getName().equals(ACCURACY_ELEMENT))
               {
                  accuracy = false;
               }
               else if (xmlParser.getName().equals(COURSE_ELEMENT))
               {
                  bearing = false;
               }
               else if (xmlParser.getName().equals(ELEVATION_ELEMENT))
               {
                  elevation = false;
               }
               else if (xmlParser.getName().equals(TIME_ELEMENT))
               {
                  time = false;
               }
               else if (xmlParser.getName().equals(SEGMENT_ELEMENT))
               {
                  if (mSegmentUri == null)
                  {
                     startSegment();
                  }
                  mContentResolver.bulkInsert(Uri.withAppendedPath(mSegmentUri, "waypoints"), bulk.toArray(new ContentValues[bulk.size()]));
                  bulk.clear();
               }
               else if (xmlParser.getName().equals(TRACK_ELEMENT))
               {
                  if (!lastPosition.containsKey(Waypoints.TIME))
                  {
                     lastPosition.put(Waypoints.TIME, importDate);
                  }
                  if (!lastPosition.containsKey(Waypoints.SPEED))
                  {
                     lastPosition.put(Waypoints.SPEED, 0);
                  }
                  bulk.add(lastPosition);
                  lastPosition = null;
               }
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               String text = xmlParser.getText();
               if (name)
               {
                  ContentValues nameValues = new ContentValues();
                  nameValues.put(Tracks.NAME, text);
                  if (mTrackUri == null)
                  {
                     startTrack(new ContentValues());
                  }
                  mContentResolver.update(mTrackUri, nameValues, null, null);
               }
               else if (lastPosition != null && speed)
               {
                  lastPosition.put(Waypoints.SPEED, Double.parseDouble(text));
               }
               else if (lastPosition != null && accuracy)
               {
                  lastPosition.put(Waypoints.ACCURACY, Double.parseDouble(text));
               }
               else if (lastPosition != null && bearing)
               {
                  lastPosition.put(Waypoints.BEARING, Double.parseDouble(text));
               }
               else if (lastPosition != null && elevation)
               {
                  lastPosition.put(Waypoints.ALTITUDE, Double.parseDouble(text));
               }
               else if (lastPosition != null && time)
               {
                  lastPosition.put(Waypoints.TIME, parseXmlDateTime(text));
               }
            }
            eventType = xmlParser.next();
         }
         result = new Boolean(true);
      }
      catch (XmlPullParserException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_xml);
         mErrorDialogException = e;
         result = new Boolean(false);
      }
      catch (ParseException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_parse);
         mErrorDialogException = e;
         result = new Boolean(false);
      }
      catch (IOException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_io);
         mErrorDialogException = e;
         result = new Boolean(false);
      }
      return result;
   }

   private void startSegment()
   {
      if (mTrackUri == null)
      {
         startTrack(new ContentValues());
      }
      mSegmentUri = mContentResolver.insert(Uri.withAppendedPath(mTrackUri, "segments"), new ContentValues());
   }

   private void startTrack(ContentValues trackContent)
   {
      mTrackUri = mContentResolver.insert(Tracks.CONTENT_URI, trackContent);
   }

   public static Long parseXmlDateTime(String text) throws ParseException
   {
      Long dateTime = null;
      int length = text.length();
      switch (length)
      {
         case 20:
            dateTime = new Long(ZULU_DATE_FORMAT.parse(text).getTime());
            break;
         case 23:
            dateTime = new Long(ZULU_DATE_FORMAT_BC.parse(text).getTime());
            break;
         case 24:
            dateTime = new Long(ZULU_DATE_FORMAT_MS.parse(text).getTime());
            break;
         default:
            throw new ParseException("Unable to parse dateTime "+text+" of length ", 0);
      }
      return dateTime;
   }

   @Override
   protected void onPreExecute()
   {
      mTrackList.startProgressBar(mLength);
   }
   
   @Override
   protected Boolean doInBackground(Uri... params)
   {
      return importUri( params[0] );
   }
   
   @Override
   protected void onProgressUpdate(Integer... values)
   {
      
      mTrackList.updateProgressBar(values[0], mLength);
   }
   
   @Override
   protected void onPostExecute(Boolean result)
   {
      if( result.booleanValue() )
      {
         mTrackList.stopProgressBar();
      }
      else 
      {
         mTrackList.showErrorDialog(mErrorDialogMessage, mErrorDialogException);
      }
      mTrackList = null;
      mContentResolver = null;
   }

   public void incrementProgressBy(int add)
   {
      publishProgress( new Integer(add) );
   }
};