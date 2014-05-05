/**
   Copyright (c) 2014 Rory Hool
   
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
   
       http://www.apache.org/licenses/LICENSE-2.0
   
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 **/

package com.roryhool.videomanipulation;

import java.io.File;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.roryhool.commonvideolibrary.MediaHelper;

public class TrimActivity extends Activity {

   ImageView mVideoThumbnail;

   TextView mVideoName;

   TextView mVideoDuration;

   TextView mTrimStartTime;
   TextView mTrimEndTime;

   SeekBar mTrimStartSeekBar;
   SeekBar mTrimEndSeekBar;

   int mTrimStart;

   int mTrimEnd;

   int mDuration;

   Uri mInputUri;
   Uri mOutputUri;

   @Override
   public void onCreate( Bundle savedInstanceState ) {
      super.onCreate( savedInstanceState );

      setContentView( R.layout.activity_trim );

      mVideoThumbnail = (ImageView) findViewById( R.id.selected_video_thumbnail );

      mVideoName = (TextView) findViewById( R.id.selected_video_name );

      mVideoDuration = (TextView) findViewById( R.id.selected_video_duration );

      mTrimStartTime = (TextView) findViewById( R.id.trim_start );

      mTrimEndTime = (TextView) findViewById( R.id.trim_end );

      mTrimStartSeekBar = (SeekBar) findViewById( R.id.trim_start_seek_bar );
      mTrimStartSeekBar.setOnSeekBarChangeListener( mTrimStartOnSeekBarChangeListener );

      mTrimEndSeekBar = (SeekBar) findViewById( R.id.trim_end_seek_bar );
      mTrimEndSeekBar.setOnSeekBarChangeListener( mTrimEndOnSeekBarChangeListener );

      mInputUri = getIntent().getData();

      loadVideoUri( mInputUri );

      String pathWithoutExtension = mInputUri.toString().replace( ".mp4", "" );

      String trimmedFileName = String.format( Locale.US, "%s_trimmed.mp4", pathWithoutExtension );

      mOutputUri = Uri.parse( trimmedFileName );
   }

   public void loadVideoUri( Uri uri ) {

      Bitmap bitmap = MediaHelper.GetThumbnailFromVideo( uri, 0 );
      mVideoThumbnail.setImageBitmap( bitmap );

      File file = new File( uri.toString() );

      mVideoName.setText( file.getName() );

      mDuration = MediaHelper.GetDuration( uri );

      mTrimStartSeekBar.setMax( mDuration );

      mTrimEndSeekBar.setMax( mDuration );

      setTrimStart( 0 );

      setTrimEnd( mDuration );

      mTrimEndSeekBar.setProgress( mDuration );

      mVideoDuration.setText( String.format( Locale.US, "Duration: %d", mDuration ) );
   }

   OnSeekBarChangeListener mTrimStartOnSeekBarChangeListener = new OnSeekBarChangeListener() {

      @Override
      public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser ) {
         setTrimStart( progress );
      }

      @Override
      public void onStartTrackingTouch( SeekBar seekBar ) {

      }

      @Override
      public void onStopTrackingTouch( SeekBar seekBar ) {

      }

   };

   OnSeekBarChangeListener mTrimEndOnSeekBarChangeListener = new OnSeekBarChangeListener() {

      @Override
      public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser ) {
         setTrimEnd( progress );
      }

      @Override
      public void onStartTrackingTouch( SeekBar seekBar ) {

      }

      @Override
      public void onStopTrackingTouch( SeekBar seekBar ) {

      }

   };

   private void setTrimStart( int start ) {
      if ( start < 0 ) {
         start = 0;
      }

      if ( start > mTrimEnd ) {
         start = mTrimEnd;
      }

      mTrimStart = start;
      mTrimStartSeekBar.setProgress( mTrimStart );

      mTrimStartTime.setText( "Trim Start: " + mTrimStart );
   }

   private void setTrimEnd( int end ) {
      if ( end > mDuration ) {
         end = mDuration;
      }

      if ( end < mTrimStart ) {
         end = mTrimStart;
      }

      mTrimEnd = end;
      mTrimEndSeekBar.setProgress( mTrimEnd );

      mTrimEndTime.setText( "Trim End: " + mTrimEnd );
   }

   public void onTrimClicked( View view ) {

      new TrimTask().execute( mInputUri, mOutputUri );
   }

   class TrimTask extends AsyncTask<Uri, Void, Uri> {

      @Override
      protected Uri doInBackground( Uri... uris ) {

         if ( uris.length < 2 ) {
            return null;
         }

         Uri inputUri = uris[0];
         Uri outputUri = uris[1];

         VideoResampler resampler = new VideoResampler();
         resampler.setInput( inputUri );
         resampler.setOutput( outputUri );

         resampler.setStartTime( mTrimStart );
         resampler.setEndTime( mTrimEnd );

         try {
            resampler.start();
         } catch ( Throwable e ) {
            e.printStackTrace();
         }

         return outputUri;
      }

      @Override
      protected void onPostExecute( Uri outputUri ) {
         Intent sendIntent = new Intent();
         sendIntent.setAction( Intent.ACTION_VIEW );
         sendIntent.setDataAndType( outputUri, "video/mp4" );
         startActivity( sendIntent );
      }
   }

}
