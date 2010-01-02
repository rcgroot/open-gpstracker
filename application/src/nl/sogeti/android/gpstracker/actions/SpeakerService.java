package nl.sogeti.android.gpstracker.actions;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class SpeakerService extends android.app.Service implements TextToSpeech.OnInitListener
{
   public static final String ACTION_PREPARE = "nl.sogeti.android.gpstracker.intent.action.PREPARE";
   public static final String ACTION_SPEAK = "nl.sogeti.android.gpstracker.intent.action.SPEAK";

   private static final String TAG = SpeakerService.class.getSimpleName();
   private TextToSpeech mTts;
   private Uri mTrackUri;
   private String mUpdate = "0";
   private final ContentObserver mTrackObserver = new ContentObserver( new Handler() )
      {
         @Override
         public void onChange( boolean selfUpdate )
         {
            SpeakerService.this.updateSummary();
         }
      };

   @Override
   public void onCreate()
   {
      super.onCreate();
      // success, create the TTS instance
      mTts = new TextToSpeech( this, this );
      speak( "Thank you for using the Open GPS Tracker voice update service." );
   }

   public void onStart( Intent intent, int startId )
   {
      Log.i( TAG, "sentence()" );
      this.mTrackUri = intent.getData();
      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      resolver.registerContentObserver( mTrackUri, false, this.mTrackObserver );
      
      speakUpdate();
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onDestroy()
    */
   @Override
   public void onDestroy()
   {
      super.onDestroy();
      mTts.shutdown();
   }

   protected void updateSummary()
   {
      this.mUpdate = "" + ( new Integer( this.mUpdate ).intValue() + 1 );
   }

   protected void speakUpdate()
   {
      
      speak( this.mUpdate );
   }

   private void speak( String sentence )
   {
      Log.i( TAG, sentence );
      mTts.speak( sentence, TextToSpeech.QUEUE_ADD, null );
   }

   public void onInit( int status )
   {
      // TODO Auto-generated method stub
   }

   @Override
   public IBinder onBind( Intent intent )
   {
      return null;
   }

}
