package nl.sogeti.android.gpstracker.actions;

import nl.sogeti.android.gpstracker.R;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsSpinner;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class SpeakSummary extends Activity
{
   private final OnClickListener mOkayOnClickListener = new OnClickListener()
      {
         public void onClick( View v )
         {
            long item = mSpinner.getSelectedItemId();
            int period = 1000*SpeakSummary.this.getResources().getIntArray( R.array.intervals_values )[(int) item];
            //TODO: period *= 60 ;
            mTrackUri.toString(); 
            startService(speaker);
         }
      };
   private final OnClickListener mCancelOnClickListener = new OnClickListener() 
      {
         public void onClick( View v )
         {
            stopService( speaker );
         }
      };
      
   private Intent speaker = new Intent( this, SpeakerService.class );
   private static final String TAG = SpeakSummary.class.getName();
   private AbsSpinner mSpinner;
   private Uri mTrackUri;

   @Override
   protected void onCreate( Bundle load )
   {
      super.onCreate( load );
      setContentView( R.layout.voiceover );     
      Button okay = (Button) findViewById( R.id.voiceover_btn_okay );
      okay.setOnClickListener( this.mOkayOnClickListener );
      Button cancel = (Button) findViewById( R.id.voiceover_btn_cancel );
      cancel.setOnClickListener( this.mCancelOnClickListener );
      
      this.mTrackUri = this.getIntent().getData();
      
      mSpinner = (Spinner) findViewById(R.id.voiceover_interval);
      ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource( this, R.array.intervals, android.R.layout.simple_spinner_item );
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mSpinner.setAdapter(adapter);      
   }
}
