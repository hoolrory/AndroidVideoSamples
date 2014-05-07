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
import java.util.Arrays;
import java.util.List;
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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.roryhool.commonvideolibrary.MediaHelper;
import com.roryhool.commonvideolibrary.Resolution;

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

   int mInputWidth;
   int mInputHeight;

   int mBitRate = 2000000;

   ImageView mVideoThumbnail;

   TextView mVideoName;

   TextView mVideoResolution;

   TextView mVideoBitRate;

   TextView mVideoFrameRate;

   TextView mVideoIFrameInterval;
   
   Spinner mResolutionSpinner;
   Spinner mBitRateSpinner;
   Spinner mFrameRateSpinner;
   Spinner mIFrameIntervalSpinner;

   List<String> mResolutions = Arrays.asList( "1080P", "720P", "480P", "360P", "QVGA", "QCIF" );
   
   List<String> mBitRates = Arrays.asList( "2Mbps", "1Mbps", "500Kbps", "56Kbps" );

   List<String> mFrameRates = Arrays.asList( "30fps", "15fps" );

   List<String> mIFrameIntervals = Arrays.asList( "1", "5", "10" );

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

      mResolutionSpinner = (Spinner) findViewById( R.id.resolutionSpinner );
      mBitRateSpinner = (Spinner) findViewById( R.id.bitRateSpinner );
      mFrameRateSpinner = (Spinner) findViewById( R.id.frameRateSpinner );
      mIFrameIntervalSpinner = (Spinner) findViewById( R.id.IFrameIntervalSpinner );

      Uri data = getIntent().getData();

      if ( data != null ) {
         mInputUri = data;
      }

      loadVideoUri( mInputUri );

      String pathWithoutExtension = mInputUri.toString().replace( ".mp4", "" );

      String resampledFileName = String.format( Locale.US, "%s_resampled.mp4", pathWithoutExtension );

      mOutputUri = Uri.parse( resampledFileName );

      setupSpinner( mResolutionSpinner, mResolutions );

      setupSpinner( mBitRateSpinner, mBitRates );

      setupSpinner( mFrameRateSpinner, mFrameRates );

      setupSpinner( mIFrameIntervalSpinner, mIFrameIntervals );
   }

   private void setupSpinner( Spinner spinner, List<String> items ) {
      ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item, items );
      adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
      spinner.setAdapter( adapter );
   }

   public void loadVideoUri( Uri uri ) {

      Bitmap bitmap = MediaHelper.GetThumbnailFromVideo( uri, 0 );
      mVideoThumbnail.setImageBitmap( bitmap );

      File file = new File( uri.toString() );

      mVideoName.setText( file.getName() );
      
      mInputWidth = MediaHelper.GetWidth( uri );
      mInputHeight = MediaHelper.GetHeight( uri );

      mVideoResolution.setText( String.format( Locale.US, "Resolution: %dx%d", mInputWidth, mInputHeight ) );

      mVideoBitRate.setText( String.format( Locale.US, "BitRate: %d", MediaHelper.GetBitRate( uri ) ) );

      mVideoFrameRate.setText( String.format( Locale.US, "FrameRate: %d", MediaHelper.GetFrameRate( uri ) ) );

      mVideoIFrameInterval.setText( String.format( Locale.US, "I-Frame Interval: %d", MediaHelper.GetIFrameInterval( uri ) ) );

   }

   Resolution mOutputResolution;
   int mOutputBitRate;
   int mOutputFrameRate;
   int mOutputIFrameInterval;

   public void onResampleClicked( View view ) {

      String selectedRes = (String) mResolutionSpinner.getSelectedItem();

      if ( selectedRes.equals( "1080P" ) ) {
         mOutputResolution = Resolution.RESOLUTION_1080P;
      } else if ( selectedRes.equals( "720P" ) ) {
         mOutputResolution = Resolution.RESOLUTION_720P;
      } else if ( selectedRes.equals( "480P" ) ) {
         mOutputResolution = Resolution.RESOLUTION_480P;
      } else if ( selectedRes.equals( "360P" ) ) {
         mOutputResolution = Resolution.RESOLUTION_360P;
      } else if ( selectedRes.equals( "QVGA" ) ) {
         mOutputResolution = Resolution.RESOLUTION_QVGA;
      } else if ( selectedRes.equals( "QCIF" ) ) {
         mOutputResolution = Resolution.RESOLUTION_QCIF;
      }

      if ( mInputHeight > mInputWidth ) {
         mOutputResolution = mOutputResolution.rotate();
      }

      String selectedBitRate = (String) mBitRateSpinner.getSelectedItem();

      if ( selectedBitRate.equals( "2Mbps" ) ) {
         mOutputBitRate = 2097152;
      } else if ( selectedBitRate.equals( "1Mbps" ) ) {
         mOutputBitRate = 1048576;
      } else if ( selectedBitRate.equals( "500Kbps" ) ) {
         mOutputBitRate = 512000;
      } else if ( selectedBitRate.equals( "56Kbps" ) ) {
         mOutputBitRate = 57344;
      }

      String selectedFrameRate = (String) mFrameRateSpinner.getSelectedItem();

      if ( selectedFrameRate.equals( "30fps" ) ) {
         mOutputFrameRate = 30;
      } else if ( selectedFrameRate.equals( "15fps" ) ) {
         mOutputFrameRate = 15;
      }

      String selectedIFrameInterval = (String) mIFrameIntervalSpinner.getSelectedItem();

      if ( selectedIFrameInterval.equals( "1" ) ) {
         mOutputIFrameInterval = 1;
      } else if ( selectedIFrameInterval.equals( "5" ) ) {
         mOutputIFrameInterval = 5;
      } else if ( selectedIFrameInterval.equals( "10" ) ) {
         mOutputIFrameInterval = 10;
      }

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
         resampler.addSamplerClip( new SamplerClip( inputUri ) );
         // resampler.setInput( inputUri );
         resampler.setOutput( outputUri );

         resampler.setOutputResolution( mOutputResolution.getWidth(), mOutputResolution.getHeight() );
         resampler.setOutputBitRate( mOutputBitRate );
         resampler.setOutputFrameRate( mOutputFrameRate );
         resampler.setOutputIFrameInterval( mOutputIFrameInterval );

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
