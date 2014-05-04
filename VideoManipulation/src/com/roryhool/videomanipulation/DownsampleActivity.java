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

import java.nio.ByteBuffer;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR2 )
public class DownsampleActivity extends Activity {

   Uri mInputUri = Uri.parse( "/mnt/sdcard/test2.mp4" );
   Uri mOutputUri;

   MediaCodec mDecoder;
   MediaCodec mEncoder;

   MediaExtractor mExtractor;

   MediaMuxer mMuxer;

   ByteBuffer[] mInputBuffers;
   ByteBuffer[] mOutputBuffers;

   BufferInfo mDecoderBufferInfo;
   BufferInfo mEncoderBufferInfo;

   int mEncoderWidth = 640;
   int mEncoderHeight = 480;

   int mBitRate = 2000000;

   @Override
   public void onCreate( Bundle savedInstanceState ) {
      super.onCreate( savedInstanceState );

      setContentView( R.layout.activity_downsample );

      Uri data = getIntent().getData();

      if ( data != null ) {
         mInputUri = data;
      }

      String pathWithoutExtension = mInputUri.toString().replace( ".mp4", "" );

      String resampledFileName = String.format( Locale.US, "%s_resampled.mp4", pathWithoutExtension );

      mOutputUri = Uri.parse( resampledFileName );

   }

   public void onDownsampleClicked( View view ) {
      new DownsampleTask().execute( (Void) null );
   }

   VideoDownsampler mTest;

   class DownsampleTask extends AsyncTask<Void, Void, Void> {

      @Override
      protected Void doInBackground( Void... params ) {

         mTest = new VideoDownsampler();
         mTest.setInput( mInputUri );
         mTest.setOutput( mOutputUri );
         mTest.setOutputResolution( VideoDownsampler.WIDTH_720P, VideoDownsampler.HEIGHT_720P );
         mTest.setOutputBitRate( VideoDownsampler.BITRATE_720P );

         try {
            mTest.run();
         } catch ( Throwable e ) {
            e.printStackTrace();
         }

         return null;
      }

      @Override
      protected void onPostExecute( Void v ) {
         Intent sendIntent = new Intent();
         sendIntent.setAction( Intent.ACTION_VIEW );
         sendIntent.setDataAndType( mOutputUri, "video/mp4" );
         startActivity( sendIntent );
      }
   }
}
