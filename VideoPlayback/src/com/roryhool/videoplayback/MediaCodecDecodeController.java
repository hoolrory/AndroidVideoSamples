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

package com.roryhool.videoplayback;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup;

import com.roryhool.commonvideolibrary.MediaHelper;

@TargetApi( Build.VERSION_CODES.JELLY_BEAN )
public class MediaCodecDecodeController extends ControllerBase {

   private static final String TAG = "MediaCodecDecodeController";

   VideoPlayerView mVideoPlayerView;

   ScaledTextureView mTextureView;

   int mVideoWidth = 0;
   int mVideoHeight = 0;

   int mRotation = 0;

   int mDuration = 0;

   int mCurrentPosition = 0;

   DecoderThread mDecoderThread;

   PlaybackTimer mTimer;

   public MediaCodecDecodeController( Context context, VideoPlayerView view, ScaledTextureView textureView ) {
      super( context );
      mTimer = new PlaybackTimer();
      mVideoPlayerView = view;
      mTextureView = textureView;
   }

   private void calculateMediaStats( Uri uri ) {

      MediaMetadataRetriever r = new MediaMetadataRetriever();

      if ( r != null ) {
         try {
            r.setDataSource( uri.toString() );
         } catch ( RuntimeException exception ) {
            r.release();
            return;
         }

         String widthString = r.extractMetadata( MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH );
         String heightString = r.extractMetadata( MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT );

         String durationString = r.extractMetadata( MediaMetadataRetriever.METADATA_KEY_DURATION );

         r.release();

         mVideoWidth = Integer.parseInt( widthString );
         mVideoHeight = Integer.parseInt( heightString );

         mDuration = Integer.parseInt( durationString );

         mRotation = MediaHelper.GetRotation( uri );

         if ( mRotation == 90 || mRotation == 270 ) {

         }

         ViewGroup.LayoutParams params = mTextureView.getLayoutParams();
         params.width = ViewGroup.LayoutParams.MATCH_PARENT;
         params.height = ViewGroup.LayoutParams.MATCH_PARENT;
         mTextureView.setRotation( mRotation );
         mTextureView.setLayoutParams( params );
         mTextureView.requestLayout();
         mVideoPlayerView.requestLayout();
      }
   }

   @Override
   public void setVideoUri( Uri uri ) {
      super.setVideoUri( uri );
      calculateMediaStats( uri );
   }

   @Override
   public void play() {
      mTimer.start();
      mDecoderThread.play();
   }

   @Override
   public void pause() {
      mTimer.stop();
      mDecoderThread.pause();
   }

   int mSeekToMs = -1;

   @Override
   public void seekTo( int ms ) {
      mSeekToMs = ms;
   }

   @Override
   public boolean isPlaying() {
      return mTimer.isRunning();
   }

   @Override
   public void onSurfaceTextureAvailable( SurfaceTexture surfaceTexture, int width, int height ) {

      Surface surface = new Surface( surfaceTexture );

      mDecoderThread = new DecoderThread( surface, mVideoUri );
      mDecoderThread.start();
   }

   @Override
   public void onSurfaceTextureSizeChanged( SurfaceTexture surface, int width, int height ) {
   }

   @Override
   public boolean onSurfaceTextureDestroyed( SurfaceTexture surface ) {
      return false;
   }

   @Override
   public void onSurfaceTextureUpdated( SurfaceTexture surface ) {
   }

   @Override
   public int getDuration() {
      return mDuration;
   }

   @Override
   public int getCurrentPosition() {
      return (int) mTimer.getTime();
   }

   @Override
   public int getVideoWidth() {
      return mVideoWidth;
   }

   @Override
   public int getVideoHeight() {
      return mVideoHeight;
   }

   public void shutDown() {
      mDecoderThread.shutDown();
   }

   private class DecoderThread extends Thread {

      Surface mSurface;

      Uri mUri;

      MediaExtractor mExtractor;

      ByteBuffer[] mInputBuffers;
      ByteBuffer[] mOutputBuffers;

      boolean mThreadStoped;

      MediaCodec mDecoder;

      BufferInfo mInfo;

      boolean mPlaying;

      public DecoderThread( Surface surface, Uri videoUri ) {
         mSurface = surface;
         mUri = videoUri;

         setupExtractor();
      }

      private void setupExtractor() {
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

         mDecoder = MediaCodec.createDecoderByType( "video/avc" );
         mDecoder.configure( mExtractor.getTrackFormat( videoIndex ), mSurface, null, 0 );
         mDecoder.start();

         mInfo = new BufferInfo();

         mInputBuffers = mDecoder.getInputBuffers();
         mOutputBuffers = mDecoder.getOutputBuffers();
      }

      @Override
      public void run() {

         boolean isEOS = false;

         // Log.d( TAG, String.format( "Got ibuffers %d and o buffers %d", mInputBuffers.length, mOutputBuffers.length ) );

         while ( !mThreadStoped ) {

            if ( mPlaying ) {

               if ( isEOS ) {
                  seekTo( 0, MediaExtractor.SEEK_TO_CLOSEST_SYNC );
                  isEOS = false;
               }

               if ( mSeekToMs != -1 ) {
                  seekTo( mSeekToMs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC );
                  mSeekToMs = -1;
               }

               long timertime = mTimer.getTime();

               if ( timertime < mCurrentPosition ) {
                  continue;
               }

               int inIndex = mDecoder.dequeueInputBuffer( 10000 );

               // Log.d( TAG, String.format( "Got index %d", inIndex ) );

               if ( inIndex >= 0 ) {
                  ByteBuffer buffer = mInputBuffers[inIndex];
                  int sampleSize = mExtractor.readSampleData( buffer, 0 );
                  if ( sampleSize < 0 ) {
                     // Log.d( TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM" );
                     mDecoder.queueInputBuffer( inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM );
                     isEOS = true;
                  } else {
                     // Log.d( TAG, "InputBuffer ADVANCING" );
                     mDecoder.queueInputBuffer( inIndex, 0, sampleSize, mExtractor.getSampleTime(), 0 );
                     if ( ( mExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC ) > 0 ) {
                        // Log.d( TAG, String.format( Locale.US, "Sync frame at %d", mExtractor.getSampleTime() ) );
                     }
                     mExtractor.advance();
                  }
               }

               int outIndex = mDecoder.dequeueOutputBuffer( mInfo, 10000 );
               mCurrentPosition = (int) mInfo.presentationTimeUs / 1000;

               // Log.d( TAG, String.format( Locale.US, "current position is %d", mCurrentPosition ) );

               switch ( outIndex ) {
               case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                  Log.d( TAG, "INFO_OUTPUT_BUFFERS_CHANGED" );
                  mInputBuffers = mDecoder.getInputBuffers();
                  mOutputBuffers = mDecoder.getOutputBuffers();
                  break;
               case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                  Log.d( TAG, "New format " + mDecoder.getOutputFormat() );
                  break;
               case MediaCodec.INFO_TRY_AGAIN_LATER:
                  Log.d( TAG, "dequeueOutputBuffer timed out!" );
                  break;
               default:
                  ByteBuffer buffer = mOutputBuffers[outIndex];
                  // Log.v( TAG, "We can't use this buffer but render it due to the API limit, " + buffer );

                  mDecoder.releaseOutputBuffer( outIndex, true );
                  break;
               }

               if ( ( mInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM ) != 0 ) {
                  // Log.d( TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM" );
                  isEOS = true;
               }

               if ( isEOS ) {
                  pause();
                  onCompletion();
               }
            }
         }

         mDecoder.stop();
         mDecoder.release();
         mExtractor.release();
      }

      private void seekTo( long ms, int seekMode ) {

         // Log.d( TAG, String.format( Locale.US, "seeking to %d", ms ) );

         mTimer.setTime( ms );
         mExtractor.seekTo( ms * 1000, seekMode );
         mCurrentPosition = (int) mExtractor.getSampleTime() / 1000;
         // Log.d( TAG, String.format( Locale.US, "seeking extractor to %d, sample time is now %d", ms, mExtractor.getSampleTime() ) );
         mDecoder.flush();
         mInputBuffers = mDecoder.getInputBuffers();
         mOutputBuffers = mDecoder.getOutputBuffers();

         mInfo = new BufferInfo();
      }

      public void pause() {
         mPlaying = false;
         mTimer.stop();
      }

      public void play() {
         mPlaying = true;
         mTimer.start();
      }

      public void shutDown() {
         mThreadStoped = true;
      }
   }

}
