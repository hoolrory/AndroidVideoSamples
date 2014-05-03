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

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.view.Surface;

public class MediaPlayerController extends ControllerBase implements OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener {

   MediaPlayer mMediaPlayer;

   public MediaPlayerController( Context context ) {
      super( context );
   }

   @Override
   public void play() {
      if ( mMediaPlayer.getCurrentPosition() >= mMediaPlayer.getDuration() ) {
         mMediaPlayer.seekTo( 0 );
      }
      mMediaPlayer.start();
   }

   @Override
   public void pause() {
      mMediaPlayer.pause();

   }

   @Override
   public void seekTo( int ms ) {
      mMediaPlayer.seekTo( ms );
   }

   @Override
   public boolean isPlaying() {
      return mMediaPlayer.isPlaying();
   }

   @Override
   public void onSurfaceTextureAvailable( SurfaceTexture surfaceTexture, int width, int height ) {
      Surface s = new Surface( surfaceTexture );

      try {
         mMediaPlayer = new MediaPlayer();
         mMediaPlayer.setDataSource( mContext, mVideoUri );
         mMediaPlayer.setSurface( s );
         mMediaPlayer.prepare();
         mMediaPlayer.setOnBufferingUpdateListener( this );
         mMediaPlayer.setOnCompletionListener( this );
         mMediaPlayer.setOnPreparedListener( this );
         mMediaPlayer.setOnVideoSizeChangedListener( this );
         mMediaPlayer.setAudioStreamType( AudioManager.STREAM_MUSIC );
         mMediaPlayer.seekTo( 0 );
      } catch ( IllegalArgumentException e ) {
         e.printStackTrace();
      } catch ( SecurityException e ) {
         e.printStackTrace();
      } catch ( IllegalStateException e ) {
         e.printStackTrace();
      } catch ( IOException e ) {
         e.printStackTrace();
      }

      /*
      if ( mMediaPlayer != null ) {
         mSeekBar.setProgress( 0 );
         mSeekBar.setMax( mMediaPlayer.getDuration() );
         mVideoTextureView.SetVideoSize( mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight() );
      }
      
      */
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
      if ( mMediaPlayer != null ) {
         return mMediaPlayer.getDuration();
      }

      return 0;
   }

   @Override
   public int getCurrentPosition() {

      if ( mMediaPlayer != null ) {
         return mMediaPlayer.getCurrentPosition();
      }
      return 0;
   }

   @Override
   public int getVideoWidth() {

      if ( mMediaPlayer != null ) {
         return mMediaPlayer.getVideoWidth();
      }
      return 0;
   }

   @Override
   public int getVideoHeight() {

      if ( mMediaPlayer != null ) {
         return mMediaPlayer.getVideoHeight();
      }
      return 0;
   }

   @Override
   public void onVideoSizeChanged( MediaPlayer mediaPlayer, int width, int height ) {
      // TODO Auto-generated method stub

   }

   @Override
   public void onPrepared( MediaPlayer mediaPlayer ) {
      // TODO Auto-generated method stub

   }

   @Override
   public void onCompletion( MediaPlayer mediaPlayer ) {
      onCompletion();
      // showControls();
      // cancelTimer();
      // mPlayButton.setImageResource( R.drawable.ic_media_play );
   }

   @Override
   public void onBufferingUpdate( MediaPlayer mp, int percent ) {
      // TODO Auto-generated method stub

   }

}
