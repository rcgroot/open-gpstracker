package nl.sogeti.android.gpstracker.actions;

import java.util.Timer;
import java.util.TimerTask;

import nl.sogeti.android.gpstracker.R;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsSpinner;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class SpeakSummary extends Activity
{
   private static final int MY_DATA_CHECK_CODE = 42;
   private final OnClickListener mOkayOnClickListener = new OnClickListener()
      {
         public void onClick( View v )
         {
            long item = mSpinner.getSelectedItemId();
            int period = 1000 * SpeakSummary.this.getResources().getIntArray( R.array.intervals_values )[(int) item];
            startTimer( period );
         }
      };
   private final OnClickListener mCancelOnClickListener = new OnClickListener()
      {
         public void onClick( View v )
         {
            stopTimer();
         }
      };
   private final TimerTask mTimerTask = new TimerTask()
      {
         @Override
         public void run()
         {
            Log.d( TAG, "TimerTask#run()" );
            Intent speak = new Intent( SpeakerService.ACTION_SPEAK, mTrackUri, SpeakSummary.this, SpeakerService.class );
            SpeakSummary.this.startService( speak );
         }
      };
   protected Intent mSpeaker;
   private static final String TAG = SpeakSummary.class.getSimpleName();
   private AbsSpinner mSpinner;
   private Uri mTrackUri;
   private Timer mTimer ;
   private Button mOkayButton;

   @Override
   protected void onCreate( Bundle load )
   {
      Log.d( TAG, "onCreate(" );
      super.onCreate( load );
      setContentView( R.layout.voiceover );
      mOkayButton = (Button) findViewById( R.id.voiceover_btn_okay );
      mOkayButton.setOnClickListener( this.mOkayOnClickListener );
      Button cancel = (Button) findViewById( R.id.voiceover_btn_cancel );
      cancel.setOnClickListener( this.mCancelOnClickListener );

      this.mTrackUri = this.getIntent().getData();
      mSpeaker = new Intent( SpeakerService.ACTION_PREPARE, mTrackUri, SpeakSummary.this, SpeakerService.class );
      
      mSpinner = (Spinner) findViewById( R.id.voiceover_interval );
      ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource( this, R.array.intervals, android.R.layout.simple_spinner_item );
      adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
      mSpinner.setAdapter( adapter );
      
      checkTTS();
   }

   private void checkTTS()
   {
      Intent checkIntent = new Intent();
      checkIntent.setAction( TextToSpeech.Engine.ACTION_CHECK_TTS_DATA );
      startActivityForResult( checkIntent, MY_DATA_CHECK_CODE );
   }

   protected void onActivityResult( int requestCode, int resultCode, Intent data )
   {
      if( requestCode == MY_DATA_CHECK_CODE )
      {
         if( resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS )
         {
            mOkayButton.setEnabled( true );
         }
         else
         {
            // missing data, install it
            Intent installIntent = new Intent();
            installIntent.setAction( TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA );
            
            checkTTS();
         }
      }
   }
   
   protected void stopTimer()
   {
      this.stopService( mSpeaker );
      
      if( this.mTimer != null )
      {
         this.mTimer.cancel();
         this.mTimer.purge();
         this.mTimer = null;
      }
   }

   protected void startTimer( long period )
   {
      if( period > 10000 )
      {
         this.startService( mSpeaker );
         this.mTimer = new Timer();
         this.mTimer.scheduleAtFixedRate( this.mTimerTask, period, period );
      }
      else 
      {
         Log.e( TAG, "Update interval is to small, should not happen! "+ period + "miliseconds" );
      }
   }
}
