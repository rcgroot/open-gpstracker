package nl.sogeti.android.gpstracker.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class ImportTrack extends Activity
{
   public static final SimpleDateFormat ZULU_DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );
   static
   {
      TimeZone utc = TimeZone.getTimeZone( "UTC" );
      ZULU_DATE_FORMAT.setTimeZone( utc ); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true
   }
   
   private static final String TAG = "ImportTrack";
   private Intent mIntent;

   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      mIntent = getIntent();
      Uri fileUri = mIntent.getData();
      
      try
      {
         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         XmlPullParser parser = factory.newPullParser();
         parser.setInput( new FileReader( new File(fileUri.getPath()) ) );
         doUglyXMLParsing( fileUri.getLastPathSegment(), parser );
      }
      catch (XmlPullParserException e)
      {
         e.printStackTrace();
      }
      catch (FileNotFoundException e)
      {
         e.printStackTrace();
      }
      
   }
   
   private void doUglyXMLParsing( String filename, XmlPullParser xmlParser )
   {
      int eventType;
      ContentValues lastPosition = null ;
      boolean speed = false;
      boolean elevation = false;
      boolean name = false;
      boolean time = false;
      
      Uri trackUri = null;
      Uri segment = null;
      try
      {
         eventType = xmlParser.getEventType();

         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            ContentResolver contentResolver = this.getContentResolver();
            if( eventType == XmlPullParser.START_TAG )
            {
               if( xmlParser.getName().equals( "name" ) )
               {
                  name = true;
               }
               else if( xmlParser.getName().equals( "trk" ) )
               {
                  trackUri = contentResolver.insert( Tracks.CONTENT_URI, new ContentValues( 0 ) );
               }
               else if ( xmlParser.getName().equals( "trkseg" ) )
               {
                  segment = contentResolver.insert( Uri.withAppendedPath( trackUri, "segments" ), new ContentValues( 0 ) );
               }
               else if( xmlParser.getName().equals( "trkpt" ) )
               {                  
                  lastPosition = new ContentValues(); 
                  lastPosition.put( Waypoints.LATITUDE,  new Double( xmlParser.getAttributeValue( 0) ) );
                  lastPosition.put( Waypoints.LONGITUDE, new Double( xmlParser.getAttributeValue( 1 ) ) );
               }
               else if( xmlParser.getName().equals( "speed" ) )
               {                  
                  speed = true;
               }
               else if( xmlParser.getName().equals( "ele" ) )
               {                  
                  elevation = true;
               }
               else if( xmlParser.getName().equals( "time" ) )
               {                  
                  time = true;
               }
            }
            else if( eventType == XmlPullParser.END_TAG )
            {
               if( xmlParser.getName().equals( "name" ) )
               {
                  name = false;
               }
               else if( xmlParser.getName().equals( "speed" ) )
               {
                  speed = false;
               }
               else if( xmlParser.getName().equals( "ele" ) )
               {
                  elevation = false;
               }
               else if( xmlParser.getName().equals( "time" ) )
               {
                  time = false;
               }
               else if( xmlParser.getName().equals( "trkpt" ) )
               {
                  contentResolver.insert( Uri.withAppendedPath( segment, "waypoints" ), lastPosition );
                  lastPosition = null;
               }
            }
            else if( eventType == XmlPullParser.TEXT )
            {
               if( lastPosition != null && name )
               {
                  ContentValues nameValues = new ContentValues();
                  nameValues.put( Tracks.NAME, xmlParser.getText() );
                  contentResolver.update( trackUri, nameValues , null, null );
               }
               else if( lastPosition != null && speed )
               {
                  lastPosition.put( Waypoints.SPEED, new Float( xmlParser.getText() ) );
               }
               else if( lastPosition != null && elevation )
               {
                  lastPosition.put( Waypoints.ALTITUDE, Double.parseDouble( xmlParser.getText() ) );
               }
               else if( lastPosition != null && time )
               {
                  lastPosition.put( Waypoints.TIME, new Long( ZULU_DATE_FORMAT.parse( xmlParser.getText() ).getTime() ) );
               }
            }
            eventType = xmlParser.next();
         }
      }
      catch (XmlPullParserException e)
      { 
         Log.e( TAG, "Error", e );
      }
      catch (IOException e)
      {
         Log.e( TAG, "Error", e );
      }
      catch (ParseException e)
      {
         Log.e( TAG, "Error", e );
      }
   }
}
