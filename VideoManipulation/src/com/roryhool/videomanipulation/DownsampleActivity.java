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

import android.annotation.TargetApi;
import android.app.Activity;
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

   private static final String TAG = DownsampleActivity.class.getSimpleName();

   private static final String MIME_TYPE = "video/avc";
   private static final int FRAME_RATE = 25;
   private static final int IFRAME_INTERVAL = 1;

   Uri mUri;

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

   private int mTrackIndex;
   private boolean mMuxerStarted;

   @Override
   public void onCreate( Bundle savedInstanceState ) {
      super.onCreate( savedInstanceState );

      setContentView( R.layout.activity_downsample );

      mUri = getIntent().getData();

   }

   public void onDownsampleClicked( View view ) {
      new DownsampleTask().execute( (Void) null );
   }

   DecodeEditEncodeTest mTest;

   class DownsampleTask extends AsyncTask<Void, Void, Void> {

      @Override
      protected Void doInBackground( Void... params ) {

         mTest = new DecodeEditEncodeTest();
         mTest.setContext( DownsampleActivity.this );
         try {
            mTest.testVideoEditQCIF();
         } catch ( Throwable e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
         return null;
      }

   }

   /*
   private void setup() {
      mExtractor = new MediaExtractor();
      try {
         mExtractor.setDataSource( mUri.toString() );
      } catch ( IOException e ) {
         e.printStackTrace();
      }

      int videoIndex = 0;

      for ( int trackIndex = 0; trackIndex < mExtractor.getTrackCount(); trackIndex++ ) {
         MediaFormat format = mExtractor.getTrackFormat( trackIndex );

         String mime = format.getString( MediaFormat.KEY_MIME );
         if ( mime != null ) {
            if ( mime.equals( "video/avc" ) ) {
               mExtractor.selectTrack( trackIndex );
               videoIndex = trackIndex;
               break;
            }
         }
      }

      mDecoderBufferInfo = new BufferInfo();

      mInputBuffers = mDecoder.getInputBuffers();
      mOutputBuffers = mDecoder.getOutputBuffers();

      MediaFormat format = MediaFormat.createVideoFormat( MIME_TYPE, mEncoderWidth, mEncoderHeight );

      Log.d( TAG, String.format( "KAJM - getting size %d, %d", mEncoderWidth, mEncoderHeight ) );
      format.setInteger( MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface );
      format.setInteger( MediaFormat.KEY_BIT_RATE, mBitRate );
      format.setInteger( MediaFormat.KEY_FRAME_RATE, FRAME_RATE );
      format.setInteger( MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL );
      format.setInteger( "stride", mEncoderWidth );
      format.setInteger( "slice-height", mEncoderHeight );

      mEncoder = MediaCodec.createEncoderByType( MIME_TYPE );
      mEncoder.configure( format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE );
      Surface surface = mEncoder.createInputSurface();
      mEncoder.start();

      String pathWithoutExtension = mUri.toString().replace( ".mp4", "" );

      String downsampledFileName = String.format( Locale.US, "%s_downsampled_to_%d_%d.mp4", pathWithoutExtension, mEncoderWidth, mEncoderHeight );

      try {
         mMuxer = new MediaMuxer( downsampledFileName, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 );
      } catch ( IOException ioe ) {
         throw new RuntimeException( "MediaMuxer creation failed", ioe );
      }

      mDecoder = MediaCodec.createDecoderByType( "video/avc" );
      mDecoder.configure( mExtractor.getTrackFormat( videoIndex ), surface, null, 0 );
      mDecoder.start();
   }

   private class EncoderThread extends Thread {

      @Override
      public void run() {

         boolean succeeded = true;
         try {
            setup();

            boolean framesRemaining = true;

            int frameCount = 0;

            while ( framesRemaining ) {
               drainEncoder( false );
               framesRemaining = renderFromSource( computePresentationTimeMs( frameCount ) );
               frameCount++;
            }

            drainEncoder( true );
         } catch ( Exception e ) {
            succeeded = false;
            e.printStackTrace();
         } finally {
            releaseEncoder();
         }

         int status = -1;

         if ( succeeded ) {
            status = ENCODER_SUCCEEDED;
         } else {
            status = ENCODER_SUCCEEDED;
         }

         Message.obtain( mHandler, ENCODER_STATUS, status, -1 ).sendToTarget();
      }
   }

   private boolean renderFromSource( long time ) {
      Canvas canvas = null;
      try {
         canvas = mSurface.lockCanvas( null );
      } catch ( IllegalArgumentException e ) {
         e.printStackTrace();
         return false;
      } catch ( OutOfResourcesException e ) {
         e.printStackTrace();
         return false;
      }

      boolean framesRemaining = mSource.renderFrame( canvas, time, 1000 / FRAME_RATE );

      mSurface.unlockCanvasAndPost( canvas );

      return framesRemaining;
   }

   private void releaseEncoder() {

      if ( mEncoder != null ) {
         mEncoder.stop();
         mEncoder.release();
         mEncoder = null;
      }
      if ( mSurface != null ) {
         mSurface.release();
         mSurface = null;
      }
      if ( mMuxer != null ) {
         mMuxer.stop();
         mMuxer.release();
         mMuxer = null;
      }
   }

   private void drainEncoder( boolean endOfStream ) {
      final int TIMEOUT_USEC = 10000;

      if ( endOfStream ) {
         mEncoder.signalEndOfInputStream();
      }

      ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
      while ( true ) {
         int encoderStatus = mEncoder.dequeueOutputBuffer( mEncoderBufferInfo, TIMEOUT_USEC );
         if ( encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER ) {
            // no output available yet
            if ( !endOfStream ) {
               break; // out of while
            } else {
               Log.d( TAG, "no output available, spinning to await EOS" );
            }
         } else if ( encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ) {
            // not expected for an encoder
            encoderOutputBuffers = mEncoder.getOutputBuffers();
         } else if ( encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ) {
            // should happen before receiving buffers, and should only happen once
            if ( mMuxerStarted ) {
               throw new RuntimeException( "format changed twice" );
            }
            MediaFormat newFormat = mEncoder.getOutputFormat();
            Log.d( TAG, "encoder output format changed: " + newFormat );

            // now that we have the Magic Goodies, start the muxer
            mTrackIndex = mMuxer.addTrack( newFormat );
            mMuxer.start();
            mMuxerStarted = true;
         } else if ( encoderStatus < 0 ) {
            Log.w( TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus );
            // let's ignore it
         } else {
            ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
            if ( encodedData == null ) {
               throw new RuntimeException( "encoderOutputBuffer " + encoderStatus + " was null" );
            }

            if ( ( mEncoderBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG ) != 0 ) {
               // The codec config data was pulled out and fed to the muxer when we got
               // the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
               Log.d( TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG" );
               mEncoderBufferInfo.size = 0;
            }

            if ( mEncoderBufferInfo.size != 0 ) {
               if ( !mMuxerStarted ) {
                  throw new RuntimeException( "muxer hasn't started" );
               }

               // adjust the ByteBuffer values to match BufferInfo (not needed?)
               encodedData.position( mEncoderBufferInfo.offset );
               encodedData.limit( mEncoderBufferInfo.offset + mEncoderBufferInfo.size );

               mMuxer.writeSampleData( mTrackIndex, encodedData, mEncoderBufferInfo );
               Log.d( TAG, "sent " + mEncoderBufferInfo.size + " bytes to muxer" );
            }

            mEncoder.releaseOutputBuffer( encoderStatus, false );

            if ( ( mEncoderBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM ) != 0 ) {
               if ( !endOfStream ) {
                  Log.w( TAG, "reached end of stream unexpectedly" );
               } else {
                  Log.d( TAG, "end of stream reached" );
               }
               break; // out of while
            }
         }
      }
   }

   private long computePresentationTimeMs( int frameIndex ) {
      return frameIndex * 1000 / FRAME_RATE;
   }
   */

}
