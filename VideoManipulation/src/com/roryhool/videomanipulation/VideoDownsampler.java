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
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR2 )
public class VideoDownsampler {

   private static final String TAG = "DecodeEditEncode";
   private static final boolean WORK_AROUND_BUGS = false; // avoid fatal codec bugs
   private static final boolean VERBOSE = true; // lots of logging

   // parameters for the encoder
   private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
   private static final int FRAME_RATE = 15; // 15fps
   private static final int IFRAME_INTERVAL = 10; // 10 seconds between I-frames

   private static final int TEST_R0 = 0; // dull green background
   private static final int TEST_G0 = 136;
   private static final int TEST_B0 = 0;
   private static final int TEST_R1 = 236; // pink; BT.601 YUV {120,160,200}
   private static final int TEST_G1 = 50;
   private static final int TEST_B1 = 186;
   // Replaces TextureRender.FRAGMENT_SHADER during edit; swaps green and blue channels.
   private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" + "precision mediump float;\n" + "varying vec2 vTextureCoord;\n" + "uniform samplerExternalOES sTexture;\n" + "void main() {\n" + "  gl_FragColor = texture2D(sTexture, vTextureCoord).rbga;\n" + "}\n";

   // size of a frame, in pixels
   private int mWidth = -1;
   private int mHeight = -1;
   // bit rate, in bits per second
   private int mBitRate = -1;
   // largest color component delta seen (i.e. actual vs. expected)
   private int mLargestColorDelta;

   public void testVideoEditQCIF() throws Throwable {
      setParameters( 176, 144, 1000000 );
      VideoEditWrapper.runTest( this );
   }

   public void testVideoEditQVGA() throws Throwable {
      setParameters( 320, 240, 2000000 );
      VideoEditWrapper.runTest( this );
   }

   public void testVideoEdit720p() throws Throwable {
      setParameters( 1280, 720, 6000000 );
      VideoEditWrapper.runTest( this );
   }

   /**
    * Wraps testEditVideo, running it in a new thread. Required because of the way SurfaceTexture.OnFrameAvailableListener works when the current thread has a Looper configured.
    */
   private static class VideoEditWrapper implements Runnable {
      private Throwable mThrowable;
      private VideoDownsampler mTest;

      private VideoEditWrapper( VideoDownsampler test ) {
         mTest = test;
      }

      @Override
      public void run() {
         try {
            mTest.videoEditTest();
         } catch ( Throwable th ) {
            mThrowable = th;
         }
      }

      /** Entry point. */
      public static void runTest( VideoDownsampler obj ) throws Throwable {
         VideoEditWrapper wrapper = new VideoEditWrapper( obj );
         Thread th = new Thread( wrapper, "codec test" );
         th.start();
         th.join();
         if ( wrapper.mThrowable != null ) {
            throw wrapper.mThrowable;
         }
      }
   }

   /**
    * Sets the desired frame size and bit rate.
    */
   private void setParameters( int width, int height, int bitRate ) {
      if ( ( width % 16 ) != 0 || ( height % 16 ) != 0 ) {
         Log.w( TAG, "WARNING: width or height not multiple of 16" );
      }
      mWidth = width;
      mHeight = height;
      mBitRate = bitRate;
   }

   private void videoEditTest() {
      VideoChunks sourceChunks = new VideoChunks();

      VideoChunks destChunks = editVideoFile( sourceChunks );

      checkVideoFile( destChunks );
   }

   /**
    * Edits a video file, saving the contents to a new file. This involves decoding and re-encoding, not to mention conversions between YUV and RGB, and so may be lossy.
    * <p>
    * If we recognize the decoded format we can do this in Java code using the ByteBuffer[] output, but it's not practical to support all OEM formats. By using a SurfaceTexture for output and a Surface for input, we can avoid issues with obscure formats and can use a fragment shader to do transformations.
    */
   private VideoChunks editVideoFile( VideoChunks inputData ) {
      if ( VERBOSE )
         Log.d( TAG, "editVideoFile " + mWidth + "x" + mHeight );
      VideoChunks outputData = new VideoChunks();
      MediaCodec decoder = null;
      MediaCodec encoder = null;
      InputSurface inputSurface = null;
      OutputSurface outputSurface = null;
      try {

         mExtractor = new MediaExtractor();
         try {
            mExtractor.setDataSource( "/mnt/sdcard/test2.mp4" );
         } catch ( IOException e ) {
            e.printStackTrace();
         }

         MediaMetadataRetriever r = new MediaMetadataRetriever();
         r.setDataSource( "/mnt/sdcard/test2.mp4" );
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

         // MediaFormat inputFormat = inputData.getMediaFormat();
         // Create an encoder format that matches the input format. (Might be able to just
         // re-use the format used to generate the video, since we want it to be the same.)
         MediaFormat outputFormat = MediaFormat.createVideoFormat( MIME_TYPE, mWidth, mHeight );
         outputFormat.setInteger( MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface );
         outputFormat.setInteger( MediaFormat.KEY_BIT_RATE, mBitRate );
      
         outputFormat.setInteger( MediaFormat.KEY_FRAME_RATE, FRAME_RATE );
         outputFormat.setInteger( MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL );
         outputData.setMediaFormat( outputFormat );
         encoder = MediaCodec.createEncoderByType( MIME_TYPE );
         encoder.configure( outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE );
         inputSurface = new InputSurface( encoder.createInputSurface() );
         inputSurface.makeCurrent();
         encoder.start();
         // OutputSurface uses the EGL context created by InputSurface.
         decoder = MediaCodec.createDecoderByType( MIME_TYPE );
         outputSurface = new OutputSurface();
         // outputSurface.changeFragmentShader( FRAGMENT_SHADER );

         decoder.configure( mExtractFormat, outputSurface.getSurface(), null, 0 );
         decoder.start();
         editVideoData( inputData, decoder, outputSurface, inputSurface, encoder, outputData );
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
      return outputData;
   }

   MediaMuxer mMuxer = null;
   int mTrackIndex = -1;
   boolean mMuxerStarted = false;

   MediaExtractor mExtractor = null;

   MediaFormat mExtractFormat = null;

   int mExtractIndex = 0;

   int mVideoDuration = 0;

   private void editVideoData( VideoChunks inputData, MediaCodec decoder, OutputSurface outputSurface, InputSurface inputSurface, MediaCodec encoder, VideoChunks outputData ) {
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
                  // inputData.getChunkData( inputChunk, inputBuf );
                  // int flags = inputData.getChunkFlags( inputChunk );
                  // long time = inputData.getChunkTime( inputChunk );

                  int sampleSize = mExtractor.readSampleData( inputBuf, 0 );
                  if ( sampleSize < 0 ) {
                     Log.d( TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM" );
                     decoder.queueInputBuffer( inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM );
                     // isEOS = true;
                  } else {
                     Log.d( TAG, "InputBuffer ADVANCING" );
                     decoder.queueInputBuffer( inputBufIndex, 0, sampleSize, mExtractor.getSampleTime(), 0 );
                     mExtractor.advance();
                  }

                  /*decoder.queueInputBuffer( inputBufIndex, 0, inputBuf.position(), time, flags );
                  if ( VERBOSE ) {
                     Log.d( TAG, "submitted frame " + inputChunk + " to dec, size=" + inputBuf.position() + " flags=" + flags );
                  }*/

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
                  mMuxer = new MediaMuxer( "/mnt/sdcard/testASDF.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 );
               } catch ( IOException ioe ) {
                  throw new RuntimeException( "MediaMuxer creation failed", ioe );
               }

               MediaFormat newFormat = encoder.getOutputFormat();
               // now that we have the Magic Goodies, start the muxer
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
                  outputData.addChunk( encodedData, info.flags, info.presentationTimeUs );
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

   /**
    * Checks the video file to see if the contents match our expectations. We decode the video to a Surface and check the pixels with GL.
    */
   private void checkVideoFile( VideoChunks inputData ) {
      OutputSurface surface = null;
      MediaCodec decoder = null;
      mLargestColorDelta = -1;
      if ( VERBOSE )
         Log.d( TAG, "checkVideoFile" );
      try {
         surface = new OutputSurface( mWidth, mHeight );
         MediaFormat format = inputData.getMediaFormat();
         decoder = MediaCodec.createDecoderByType( MIME_TYPE );
         decoder.configure( format, surface.getSurface(), null, 0 );
         decoder.start();
         int badFrames = checkVideoData( inputData, decoder, surface );
         if ( badFrames != 0 ) {
            // fail( "Found " + badFrames + " bad frames" );
         }
      } finally {
         if ( surface != null ) {
            surface.release();
         }
         if ( decoder != null ) {
            decoder.stop();
            decoder.release();
         }
         Log.i( TAG, "Largest color delta: " + mLargestColorDelta );
      }
   }

   /**
    * Checks the video data.
    * 
    * @return the number of bad frames
    */
   private int checkVideoData( VideoChunks inputData, MediaCodec decoder, OutputSurface surface ) {
      final int TIMEOUT_USEC = 1000;
      ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
      ByteBuffer[] decoderOutputBuffers = decoder.getOutputBuffers();
      MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
      int inputChunk = 0;
      int checkIndex = 0;
      int badFrames = 0;
      boolean outputDone = false;
      boolean inputDone = false;
      while ( !outputDone ) {
         if ( VERBOSE )
            Log.d( TAG, "check loop" );
         // Feed more data to the decoder.
         if ( !inputDone ) {
            int inputBufIndex = decoder.dequeueInputBuffer( TIMEOUT_USEC );
            if ( inputBufIndex >= 0 ) {
               if ( inputChunk == inputData.getNumChunks() ) {
                  // End of stream -- send empty frame with EOS flag set.
                  decoder.queueInputBuffer( inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM );
                  inputDone = true;
                  if ( VERBOSE )
                     Log.d( TAG, "sent input EOS" );
               } else {
                  // Copy a chunk of input to the decoder. The first chunk should have
                  // the BUFFER_FLAG_CODEC_CONFIG flag set.
                  ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                  inputBuf.clear();
                  inputData.getChunkData( inputChunk, inputBuf );
                  int flags = inputData.getChunkFlags( inputChunk );
                  long time = inputData.getChunkTime( inputChunk );
                  decoder.queueInputBuffer( inputBufIndex, 0, inputBuf.position(), time, flags );
                  if ( VERBOSE ) {
                     Log.d( TAG, "submitted frame " + inputChunk + " to dec, size=" + inputBuf.position() + " flags=" + flags );
                  }
                  inputChunk++;
               }
            } else {
               if ( VERBOSE )
                  Log.d( TAG, "input buffer not available" );
            }
         }
         if ( !outputDone ) {
            int decoderStatus = decoder.dequeueOutputBuffer( info, TIMEOUT_USEC );
            if ( decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER ) {
               // no output available yet
               if ( VERBOSE )
                  Log.d( TAG, "no output from decoder available" );
            } else if ( decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ) {
               decoderOutputBuffers = decoder.getOutputBuffers();
               if ( VERBOSE )
                  Log.d( TAG, "decoder output buffers changed" );
            } else if ( decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ) {
               MediaFormat newFormat = decoder.getOutputFormat();
               if ( VERBOSE )
                  Log.d( TAG, "decoder output format changed: " + newFormat );
            } else if ( decoderStatus < 0 ) {
               // fail( "unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus );
            } else { // decoderStatus >= 0
               ByteBuffer decodedData = decoderOutputBuffers[decoderStatus];
               if ( VERBOSE )
                  Log.d( TAG, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")" );
               if ( ( info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM ) != 0 ) {
                  if ( VERBOSE )
                     Log.d( TAG, "output EOS" );
                  outputDone = true;
               }
               boolean doRender = ( info.size != 0 );
               // As soon as we call releaseOutputBuffer, the buffer will be forwarded
               // to SurfaceTexture to convert to a texture. The API doesn't guarantee
               // that the texture will be available before the call returns, so we
               // need to wait for the onFrameAvailable callback to fire.
               decoder.releaseOutputBuffer( decoderStatus, doRender );
               if ( doRender ) {
                  if ( VERBOSE )
                     Log.d( TAG, "awaiting frame " + checkIndex );
                  // assertEquals( "Wrong time stamp", computePresentationTime( checkIndex ), info.presentationTimeUs );
                  surface.awaitNewImage();
                  surface.drawImage();
                  if ( !checkSurfaceFrame( checkIndex++ ) ) {
                     badFrames++;
                  }
               }
            }
         }
      }
      return badFrames;
   }

   /**
    * Checks the frame for correctness, using GL to check RGB values.
    * 
    * @return true if the frame looks good
    */
   private boolean checkSurfaceFrame( int frameIndex ) {
      ByteBuffer pixelBuf = ByteBuffer.allocateDirect( 4 ); // TODO - reuse this
      boolean frameFailed = false;
      for ( int i = 0; i < 8; i++ ) {
         // Note the coordinates are inverted on the Y-axis in GL.
         int x, y;
         if ( i < 4 ) {
            x = i * ( mWidth / 4 ) + ( mWidth / 8 );
            y = ( mHeight * 3 ) / 4;
         } else {
            x = ( 7 - i ) * ( mWidth / 4 ) + ( mWidth / 8 );
            y = mHeight / 4;
         }
         GLES20.glReadPixels( x, y, 1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuf );
         int r = pixelBuf.get( 0 ) & 0xff;
         int g = pixelBuf.get( 1 ) & 0xff;
         int b = pixelBuf.get( 2 ) & 0xff;
         // Log.d(TAG, "GOT(" + frameIndex + "/" + i + "): r=" + r + " g=" + g + " b=" + b);
         int expR, expG, expB;
         if ( i == frameIndex % 8 ) {
            // colored rect (green/blue swapped)
            expR = TEST_R1;
            expG = TEST_B1;
            expB = TEST_G1;
         } else {
            // zero background color (green/blue swapped)
            expR = TEST_R0;
            expG = TEST_B0;
            expB = TEST_G0;
         }
         if ( !isColorClose( r, expR ) || !isColorClose( g, expG ) || !isColorClose( b, expB ) ) {
            Log.w( TAG, "Bad frame " + frameIndex + " (rect=" + i + ": rgb=" + r + "," + g + "," + b + " vs. expected " + expR + "," + expG + "," + expB + ")" );
            frameFailed = true;
         }
      }
      return !frameFailed;
   }

   /**
    * Returns true if the actual color value is close to the expected color value. Updates mLargestColorDelta.
    */
   boolean isColorClose( int actual, int expected ) {
      final int MAX_DELTA = 8;
      int delta = Math.abs( actual - expected );
      if ( delta > mLargestColorDelta ) {
         mLargestColorDelta = delta;
      }
      return ( delta <= MAX_DELTA );
   }

   /**
    * Generates the presentation time for frame N, in microseconds.
    */
   private static long computePresentationTime( int frameIndex ) {
      return 123 + frameIndex * 1000000 / FRAME_RATE;
   }

   /**
    * The elementary stream coming out of the "video/avc" encoder needs to be fed back into the decoder one chunk at a time. If we just wrote the data to a file, we would lose the information about chunk boundaries. This class stores the encoded data in memory, retaining the chunk organization.
    */
   private static class VideoChunks {
      private MediaFormat mMediaFormat;
      private ArrayList<byte[]> mChunks = new ArrayList<byte[]>();
      private ArrayList<Integer> mFlags = new ArrayList<Integer>();
      private ArrayList<Long> mTimes = new ArrayList<Long>();

      /**
       * Sets the MediaFormat, for the benefit of a future decoder.
       */
      public void setMediaFormat( MediaFormat format ) {
         mMediaFormat = format;
      }

      /**
       * Gets the MediaFormat that was used by the encoder.
       */
      public MediaFormat getMediaFormat() {
         return mMediaFormat;
      }

      /**
       * Adds a new chunk. Advances buf.position to buf.limit.
       */
      public void addChunk( ByteBuffer buf, int flags, long time ) {
         byte[] data = new byte[buf.remaining()];
         buf.get( data );
         mChunks.add( data );
         mFlags.add( flags );
         mTimes.add( time );
      }

      /**
       * Returns the number of chunks currently held.
       */
      public int getNumChunks() {
         return mChunks.size();
      }

      /**
       * Copies the data from chunk N into "dest". Advances dest.position.
       */
      public void getChunkData( int chunk, ByteBuffer dest ) {
         byte[] data = mChunks.get( chunk );
         dest.put( data );
      }

      /**
       * Returns the flags associated with chunk N.
       */
      public int getChunkFlags( int chunk ) {
         return mFlags.get( chunk );
      }

      /**
       * Returns the timestamp associated with chunk N.
       */
      public long getChunkTime( int chunk ) {
         return mTimes.get( chunk );
      }
   }
}