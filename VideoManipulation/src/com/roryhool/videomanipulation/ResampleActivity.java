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
import java.nio.ByteBuffer;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.roryhool.commonvideolibrary.MediaHelper;

@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR2 )
public class ResampleActivity extends Activity {

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

   ImageView mVideoThumbnail;

   TextView mVideoName;

   TextView mVideoResolution;

   TextView mVideoBitRate;

   TextView mVideoFrameRate;

   TextView mVideoIFrameInterval;

   @Override
   public void onCreate( Bundle savedInstanceState ) {
      super.onCreate( savedInstanceState );

      setContentView( R.layout.activity_resample );

      mVideoThumbnail = (ImageView) findViewById( R.id.selected_video_thumbnail );

      mVideoName = (TextView) findViewById( R.id.selected_video_name );

      mVideoResolution = (TextView) findViewById( R.id.selected_video_resolution );

      mVideoBitRate = (TextView) findViewById( R.id.selected_video_bitrate );

      mVideoFrameRate = (TextView) findViewById( R.id.selected_video_framerate );

      mVideoIFrameInterval = (TextView) findViewById( R.id.selected_video_iframeinterval );

      Uri data = getIntent().getData();

      if ( data != null ) {
         mInputUri = data;
      }

      loadVideoUri( mInputUri );

      String pathWithoutExtension = mInputUri.toString().replace( ".mp4", "" );

      String resampledFileName = String.format( Locale.US, "%s_resampled.mp4", pathWithoutExtension );

      mOutputUri = Uri.parse( resampledFileName );

   }

   public void loadVideoUri( Uri uri ) {

      Bitmap bitmap = MediaHelper.GetThumbnailFromVideo( uri, 0 );
      mVideoThumbnail.setImageBitmap( bitmap );

      File file = new File( uri.toString() );

      mVideoName.setText( file.getName() );
      
      int videoWidth = MediaHelper.GetWidth( uri );
      int videoHeight = MediaHelper.GetHeight( uri );
      mVideoResolution.setText( String.format( Locale.US, "Resolution: %dx%d", videoWidth, videoHeight ) );

      mVideoBitRate.setText( String.format( Locale.US, "BitRate: %d", MediaHelper.GetBitRate( uri ) ) );


      mVideoFrameRate.setText( String.format( Locale.US, "FrameRate: %d", MediaHelper.GetFrameRate( uri ) ) );

      mVideoIFrameInterval.setText( String.format( Locale.US, "I-Frame Interval: %d", MediaHelper.GetIFrameInterval( uri ) ) );

   }

   public void onResampleClicked( View view ) {
      new ResampleTask().execute( mInputUri, mOutputUri );
   }

   class ResampleTask extends AsyncTask<Uri, Void, Uri> {

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
         resampler.setOutputResolution( VideoResampler.WIDTH_720P, VideoResampler.HEIGHT_720P );
         resampler.setOutputBitRate( VideoResampler.BITRATE_720P );

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
