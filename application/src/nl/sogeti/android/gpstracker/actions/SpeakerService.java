package nl.sogeti.android.gpstracker.actions;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class SpeakerService extends Activity implements TextToSpeech.OnInitListener
{
   private static final int MY_DATA_CHECK_CODE = 0;
   private static final String TAG = SpeakerService.class.getName();
   private TextToSpeech mTts;
   private Uri mTrackUri;
   private Timer timer;
   private String mUpdate = "0";
   private final ContentObserver mTrackObserver = new ContentObserver( new Handler() )
      {
         @Override
         public void onChange( boolean selfUpdate )
         {
            SpeakerService.this.updateSummary();
         }
      };
   private final TimerTask mTimerTask = new TimerTask()
      {
         @Override
         public void run()
         {
            SpeakerService.this.speakUpdate();
         }
      };

   @Override
   protected void onCreate( Bundle load )
   {
      super.onCreate( load );

      this.timer = new Timer();

      Intent checkIntent = new Intent();
      checkIntent.setAction( TextToSpeech.Engine.ACTION_CHECK_TTS_DATA );
      startActivityForResult( checkIntent, MY_DATA_CHECK_CODE );
   }

   public int onStartCommand( Intent intent, int flags, int startId )
   {
      this.mTrackUri = this.getIntent().getData();
      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      resolver.registerContentObserver( mTrackUri, false, this.mTrackObserver );
      int period;
      
      return 0;
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onDestroy()
    */
   @Override
   protected void onDestroy()
   {
      super.onDestroy();
      mTts.shutdown();
   }

   protected void stopTimer()
   {
      this.timer.cancel();
   }

   protected void startTimer( long period )
   {
      this.timer.scheduleAtFixedRate( this.mTimerTask, period, period );
   }

   protected void updateSummary()
   {
      this.mUpdate = "" + ( new Integer( this.mUpdate ).intValue() + 1 );
   }

   protected void onActivityResult( int requestCode, int resultCode, Intent data )
   {
      if( requestCode == MY_DATA_CHECK_CODE )
      {
         if( resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS )
         {
            // success, create the TTS instance
            mTts = new TextToSpeech( this, this );

            speak( "Thank you for using the Open GPS Tracker voice update service." );
         }
         else
         {
            // missing data, install it
            Intent installIntent = new Intent();
            installIntent.setAction( TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA );
            startActivity( installIntent );
         }
      }
   }

   protected void speakUpdate()
   {
      speak( this.mUpdate );
   }

   private void speak( String sentence )
   {
      Log.i( TAG, sentence );
      mTts.speak( sentence, TextToSpeech.QUEUE_FLUSH, null );
   }

   public void onInit( int status )
   {
      // TODO Auto-generated method stub
   }

}
