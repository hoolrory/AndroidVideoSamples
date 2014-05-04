/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.roryhool.videomanipulation;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR2 )
public class VideoDownsampler {

   public static final int WIDTH_QCIF = 176;
   public static final int HEIGHT_QCIF = 144;
   public static final int BITRATE_QCIF = 1000000;

   public static final int WIDTH_QVGA = 320;
   public static final int HEIGHT_QVGA = 240;
   public static final int BITRATE_QVGA = 2000000;

   public static final int WIDTH_720P = 1280;
   public static final int HEIGHT_720P = 720;
   public static final int BITRATE_720P = 6000000;

   private static final String TAG = "DecodeEditEncode";
   private static final boolean WORK_AROUND_BUGS = false; // avoid fatal codec bugs
   private static final boolean VERBOSE = true; // lots of logging

   // parameters for the encoder
   private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
   private static final int FRAME_RATE = 15; // 15fps
   private static final int IFRAME_INTERVAL = 10; // 10 seconds between I-frames

   // size of a frame, in pixels
   private int mWidth = WIDTH_720P;
   private int mHeight = HEIGHT_720P;
   // bit rate, in bits per second
   private int mBitRate = BITRATE_720P;

   private Uri mInputUri;
   private Uri mOutputUri;

   public VideoDownsampler() {

   }

   public void setInput( Uri intputUri ) {
      mInputUri = intputUri;
   }

   public void setOutput( Uri outputUri ) {
      mOutputUri = outputUri;
   }

   public void setOutputResolution( int width, int height ) {
      if ( ( width % 16 ) != 0 || ( height % 16 ) != 0 ) {
         Log.w( TAG, "WARNING: width or height not multiple of 16" );
      }
      mWidth = width;
      mHeight = height;
   }

   public void setOutputBitRate( int bitRate ) {
      mBitRate = bitRate;
   }

   public void run() throws Throwable {
      VideoEditWrapper wrapper = new VideoEditWrapper();
      Thread th = new Thread( wrapper, "codec test" );
      th.start();
      th.join();
      if ( wrapper.mThrowable != null ) {
         throw wrapper.mThrowable;
      }
   }

   /**
    * Wraps testEditVideo, running it in a new thread. Required because of the way 
    * SurfaceTexture.OnFrameAvailableListener works when the current thread has a Looper configured.
    */
   private class VideoEditWrapper implements Runnable {
      private Throwable mThrowable;

      @Override
      public void run() {
         try {
            resampleVideo();
         } catch ( Throwable th ) {
            mThrowable = th;
         }
      }
   }

   /**
    * Edits a video file, saving the contents to a new file. This involves decoding and re-encoding, not to mention conversions between YUV and RGB, and so may be lossy.
    * <p>
    * If we recognize the decoded format we can do this in Java code using the ByteBuffer[] output, but it's not practical to support all OEM formats. By using a SurfaceTexture for output and a Surface for input, we can avoid issues with obscure formats and can use a fragment shader to do transformations.
    */
   private void resampleVideo() {
      if ( VERBOSE )
         Log.d( TAG, "resampleVideo " + mWidth + "x" + mHeight );
      MediaCodec decoder = null;
      MediaCodec encoder = null;
      InputSurface inputSurface = null;
      OutputSurface outputSurface = null;
      try {

         mExtractor = new MediaExtractor();
         try {
            mExtractor.setDataSource( mInputUri.toString() );
         } catch ( IOException e ) {
            e.printStackTrace();
         }

         MediaMetadataRetriever r = new MediaMetadataRetriever();
         r.setDataSource( mInputUri.toString() );
         mVideoDuration = Integer.parseInt( r.extractMetadata( MediaMetadataRetriever.METADATA_KEY_DURATION ) );

         for ( int trackIndex = 0; trackIndex < mExtractor.getTrackCount(); trackIndex++ ) {
            mExtractFormat = mExtractor.getTrackFormat( trackIndex );

            String mime = mExtractFormat.getString( MediaFormat.KEY_MIME );
            if ( mime != null ) {
               if ( mime.equals( "video/avc" ) ) {
                  mExtractor.selectTrack( trackIndex );
                  mExtractIndex = trackIndex;
                  break;
               }
            }
         }

         mExtractFormat = mExtractor.getTrackFormat( mExtractIndex );

         // Create an encoder format that matches the input format. (Might be able to just
         // re-use the format used to generate the video, since we want it to be the same.)
         MediaFormat outputFormat = MediaFormat.createVideoFormat( MIME_TYPE, mWidth, mHeight );
         outputFormat.setInteger( MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface );
         outputFormat.setInteger( MediaFormat.KEY_BIT_RATE, mBitRate );
      
         outputFormat.setInteger( MediaFormat.KEY_FRAME_RATE, FRAME_RATE );
         outputFormat.setInteger( MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL );
         encoder = MediaCodec.createEncoderByType( MIME_TYPE );
         encoder.configure( outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE );
         inputSurface = new InputSurface( encoder.createInputSurface() );
         inputSurface.makeCurrent();
         encoder.start();
         // OutputSurface uses the EGL context created by InputSurface.
         decoder = MediaCodec.createDecoderByType( MIME_TYPE );
         outputSurface = new OutputSurface();

         decoder.configure( mExtractFormat, outputSurface.getSurface(), null, 0 );
         decoder.start();
         resampleVideo( decoder, outputSurface, inputSurface, encoder );
      } finally {
         if ( VERBOSE )
            Log.d( TAG, "shutting down encoder, decoder" );
         if ( outputSurface != null ) {
            outputSurface.release();
         }
         if ( inputSurface != null ) {
            inputSurface.release();
         }
         if ( encoder != null ) {
            encoder.stop();
            encoder.release();
         }
         if ( decoder != null ) {
            decoder.stop();
            decoder.release();
         }

         if ( mExtractor != null ) {
            mExtractor.release();
            mExtractor = null;
         }

         if ( mMuxer != null ) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
         }
      }
   }

   MediaMuxer mMuxer = null;
   int mTrackIndex = -1;
   boolean mMuxerStarted = false;

   MediaExtractor mExtractor = null;

   MediaFormat mExtractFormat = null;

   int mExtractIndex = 0;

   int mVideoDuration = 0;

   private void resampleVideo( MediaCodec decoder, OutputSurface outputSurface, InputSurface inputSurface, MediaCodec encoder ) {
      final int TIMEOUT_USEC = 10000;
      ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
      ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
      MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
      int inputChunk = 0;
      int outputCount = 0;

      boolean outputDone = false;
      boolean inputDone = false;
      boolean decoderDone = false;
      while ( !outputDone ) {
         if ( VERBOSE )
            Log.d( TAG, "edit loop" );
         // Feed more data to the decoder.
         if ( !inputDone ) {
            int inputBufIndex = decoder.dequeueInputBuffer( TIMEOUT_USEC );
            if ( inputBufIndex >= 0 ) {
               if ( mExtractor.getSampleTime() / 1000 >= mVideoDuration ) {
                  // End of stream -- send empty frame with EOS flag set.
                  decoder.queueInputBuffer( inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM );
                  inputDone = true;
                  if ( VERBOSE )
                     Log.d( TAG, "sent input EOS (with zero-length frame)" );
               } else {
                  // Copy a chunk of input to the decoder. The first chunk should have
                  // the BUFFER_FLAG_CODEC_CONFIG flag set.
                  ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                  inputBuf.clear();

                  int sampleSize = mExtractor.readSampleData( inputBuf, 0 );
                  if ( sampleSize < 0 ) {
                     Log.d( TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM" );
                     decoder.queueInputBuffer( inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM );
                  } else {
                     Log.d( TAG, "InputBuffer ADVANCING" );
                     decoder.queueInputBuffer( inputBufIndex, 0, sampleSize, mExtractor.getSampleTime(), 0 );
                     mExtractor.advance();
                  }

                  inputChunk++;
               }
            } else {
               if ( VERBOSE )
                  Log.d( TAG, "input buffer not available" );
            }
         }
         // Assume output is available. Loop until both assumptions are false.
         boolean decoderOutputAvailable = !decoderDone;
         boolean encoderOutputAvailable = true;
         while ( decoderOutputAvailable || encoderOutputAvailable ) {
            // Start by draining any pending output from the encoder. It's important to
            // do this before we try to stuff any more data in.
            int encoderStatus = encoder.dequeueOutputBuffer( info, TIMEOUT_USEC );
            if ( encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER ) {
               // no output available yet
               if ( VERBOSE )
                  Log.d( TAG, "no output from encoder available" );
               encoderOutputAvailable = false;
            } else if ( encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ) {
               encoderOutputBuffers = encoder.getOutputBuffers();
               if ( VERBOSE )
                  Log.d( TAG, "encoder output buffers changed" );
            } else if ( encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ) {

               try {
                  mMuxer = new MediaMuxer( mOutputUri.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 );
               } catch ( IOException ioe ) {
                  throw new RuntimeException( "MediaMuxer creation failed", ioe );
               }

               MediaFormat newFormat = encoder.getOutputFormat();

               mTrackIndex = mMuxer.addTrack( newFormat );
               mMuxer.start();
               mMuxerStarted = true;
               if ( VERBOSE )
                  Log.d( TAG, "encoder output format changed: " + newFormat );
            } else if ( encoderStatus < 0 ) {
               // fail( "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus );
            } else { // encoderStatus >= 0
               ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
               if ( encodedData == null ) {
                  // fail( "encoderOutputBuffer " + encoderStatus + " was null" );
               }
               // Write the data to the output "file".
               if ( info.size != 0 ) {
                  encodedData.position( info.offset );
                  encodedData.limit( info.offset + info.size );
                  outputCount++;

                  mMuxer.writeSampleData( mTrackIndex, encodedData, info );

                  if ( VERBOSE )
                     Log.d( TAG, "encoder output " + info.size + " bytes" );
               }
               outputDone = ( info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM ) != 0;
               encoder.releaseOutputBuffer( encoderStatus, false );
            }
            if ( encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER ) {
               // Continue attempts to drain output.
               continue;
            }
            // Encoder is drained, check to see if we've got a new frame of output from
            // the decoder. (The output is going to a Surface, rather than a ByteBuffer,
            // but we still get information through BufferInfo.)
            if ( !decoderDone ) {
               int decoderStatus = decoder.dequeueOutputBuffer( info, TIMEOUT_USEC );
               if ( decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER ) {
                  // no output available yet
                  if ( VERBOSE )
                     Log.d( TAG, "no output from decoder available" );
                  decoderOutputAvailable = false;
               } else if ( decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ) {
                  // decoderOutputBuffers = decoder.getOutputBuffers();
                  if ( VERBOSE )
                     Log.d( TAG, "decoder output buffers changed (we don't care)" );
               } else if ( decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ) {
                  // expected before first buffer of data
                  MediaFormat newFormat = decoder.getOutputFormat();
                  if ( VERBOSE )
                     Log.d( TAG, "decoder output format changed: " + newFormat );
               } else if ( decoderStatus < 0 ) {
                  // fail( "unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus );
               } else { // decoderStatus >= 0
                  if ( VERBOSE )
                     Log.d( TAG, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")" );
                  // The ByteBuffers are null references, but we still get a nonzero
                  // size for the decoded data.
                  boolean doRender = ( info.size != 0 );
                  // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                  // to SurfaceTexture to convert to a texture. The API doesn't
                  // guarantee that the texture will be available before the call
                  // returns, so we need to wait for the onFrameAvailable callback to
                  // fire. If we don't wait, we risk rendering from the previous frame.
                  decoder.releaseOutputBuffer( decoderStatus, doRender );
                  if ( doRender ) {
                     // This waits for the image and renders it after it arrives.
                     if ( VERBOSE )
                        Log.d( TAG, "awaiting frame" );
                     outputSurface.awaitNewImage();
                     outputSurface.drawImage();
                     // Send it to the encoder.
                     inputSurface.setPresentationTime( info.presentationTimeUs * 1000 );
                     if ( VERBOSE )
                        Log.d( TAG, "swapBuffers" );
                     inputSurface.swapBuffers();
                  }
                  if ( ( info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM ) != 0 ) {
                     // forward decoder EOS to encoder
                     if ( VERBOSE )
                        Log.d( TAG, "signaling input EOS" );
                     if ( WORK_AROUND_BUGS ) {
                        // Bail early, possibly dropping a frame.
                        return;
                     } else {
                        encoder.signalEndOfInputStream();
                     }
                  }
               }
            }
         }
      }
      if ( inputChunk != outputCount ) {
         throw new RuntimeException( "frame lost: " + inputChunk + " in, " + outputCount + " out" );
      }
   }

}