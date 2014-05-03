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

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class VideoPlayerView extends FrameLayout implements SurfaceTextureListener, ControllerBase.ControllerListener {

   ScaledTextureView mVideoTextureView;

   SeekBar mSeekBar;

   ImageButton mPlayButton;

   // ImageButton mFullscreenButton;

   // MediaPlayer mMediaPlayer;

   RelativeLayout mVideoControls;

   // Uri mVideoUri;

   boolean mControlsShowing = true;

   ControllerBase mController;

   View mFullscreenFillView;

   public VideoPlayerView( Context context ) {
      super( context );
      init( context );
   }

   public VideoPlayerView( Context context, AttributeSet attrs ) {
      super( context, attrs );
      init( context );
   }

   public VideoPlayerView( Context context, AttributeSet attrs, int defStyle ) {
      super( context, attrs, defStyle );
      init( context );
   }

   private void init( Context context ) {
      addView( View.inflate( context, R.layout.video_player, null ) );

      mVideoTextureView = (ScaledTextureView) findViewById( R.id.video_texture_view );

      mVideoTextureView.addSurfaceTextureListener( this );

      mPlayButton = (ImageButton) findViewById( R.id.play_button );

      mVideoControls = (RelativeLayout) findViewById( R.id.video_controls );
      mSeekBar = (SeekBar) findViewById( R.id.seek_bar );

      mSeekBar.setOnSeekBarChangeListener( mOnSeekBarChangeListener );
      mPlayButton.setOnClickListener( mOnPlayClickListener );

      this.setOnClickListener( mOnVideoPlayerViewClickListener );
   }

   public ScaledTextureView getTextureView() {
      return mVideoTextureView;
   }

   /*
    * public void setVideoUri( Uri videoUri ) { mVideoUri = videoUri; }
    */


   public void setController( ControllerBase controller ) {
      mController = controller;

      if ( mSurfaceTexture != null ) {
         setupController();
      }
   }

   public void setFullscreenFillView( View view ) {
      mFullscreenFillView = view;
   }

   OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {

      boolean mResumePlaying = false;

      @Override
      public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser ) {
         if ( fromUser ) {

            if ( mController != null ) {
               mController.seekTo( progress );
            }
         }
      }

      @Override
      public void onStartTrackingTouch( SeekBar seekBar ) {

         if ( mController != null ) {
            if ( mController.isPlaying() ) {
               pause();
               mResumePlaying = true;
            }
         }
      }

      @Override
      public void onStopTrackingTouch( SeekBar seekBar ) {
         if ( mResumePlaying ) {
            if ( mController != null ) {
               if ( !mController.isPlaying() ) {
                  play();
               }
            }
         }
         mResumePlaying = false;
      }

   };

   OnClickListener mOnPlayClickListener = new OnClickListener() {
      @Override
      public void onClick( View view ) {
         if ( mController != null ) {
            if ( mController.isPlaying() ) {
               pause();
            } else {
               play();
            }
         }
      }
   };

   OnClickListener mOnVideoPlayerViewClickListener = new OnClickListener() {
      @Override
      public void onClick( View view ) {
         if ( mControlsShowing ) {
            if ( mController != null ) {
               if ( mController.isPlaying() ) {
                  hideControls();
               }
            }
         } else {
            showControls();
         }
      }
   };

   public void play() {
      if ( mController != null ) {
         if ( !mController.isPlaying() ) {
            startTimer();

            mController.play();

            // TODO: Copy logic
            /*
             * if ( mControlsListener.getCurrentPosition() >= mControlsListener.getDuration() ) { mControlsListener.seekTo( 0 ); } mMediaPlayer.start();
             */
            mPlayButton.setImageResource( R.drawable.ic_media_pause );
         }
      }
   }

   public void pause() {
      if ( mController != null ) {
         if ( mController.isPlaying() ) {
            cancelTimer();

            mController.pause();

            /*
             * mMediaPlayer.pause();
             */
            mPlayButton.setImageResource( R.drawable.ic_media_play );
         }
      }
   }

   SurfaceTexture mSurfaceTexture;

   int mWidth;
   int mHeight;

   @Override
   public void onSurfaceTextureAvailable( SurfaceTexture surfaceTexture, int width, int height ) {

      mSurfaceTexture = surfaceTexture;
      mWidth = width;
      mHeight = height;

      if ( mController != null ) {
         setupController();
      }

      /*
       * Surface s = new Surface( surfaceTexture );
       * 
       * try { mMediaPlayer = new MediaPlayer(); mMediaPlayer.setDataSource( getContext(), mVideoUri ); mMediaPlayer.setSurface( s ); mMediaPlayer.prepare(); mMediaPlayer.setOnBufferingUpdateListener( this ); mMediaPlayer.setOnCompletionListener( this ); mMediaPlayer.setOnPreparedListener( this ); mMediaPlayer.setOnVideoSizeChangedListener( this ); mMediaPlayer.setAudioStreamType( AudioManager.STREAM_MUSIC ); mMediaPlayer.seekTo( 0 ); } catch ( IllegalArgumentException e ) { e.printStackTrace(); } catch ( SecurityException e ) { e.printStackTrace(); } catch ( IllegalStateException e ) { e.printStackTrace(); } catch ( IOException e ) { e.printStackTrace(); }
       * 
       * if ( mMediaPlayer != null ) { mSeekBar.setProgress( 0 ); mSeekBar.setMax( mMediaPlayer.getDuration() ); mVideoTextureView.SetVideoSize( mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight() ); }
       */
   }

   private void setupController() {

      mController.onSurfaceTextureAvailable( mSurfaceTexture, mWidth, mHeight );

      mSeekBar.setProgress( 0 );
      mSeekBar.setMax( mController.getDuration() );
      mVideoTextureView.SetVideoSize( mController.getVideoWidth(), mController.getVideoHeight() );
   }

   @Override
   public boolean onSurfaceTextureDestroyed( SurfaceTexture surfaceTexture ) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void onSurfaceTextureSizeChanged( SurfaceTexture surfaceTexture, int width, int height ) {
      // TODO Auto-generated method stub

   }

   @Override
   public void onSurfaceTextureUpdated( SurfaceTexture surfaceTexture ) {
      // TODO Auto-generated method stub

   }

   CountDownTimer mTimer = new CountDownTimer( 3000, 300 ) {

      public void onTick( long millisUntilFinished ) {
         updateProgress();
      }

      public void onFinish() {
         hideControls();
      }
   };

   private void resetTimer() {
      cancelTimer();
      startTimer();
   }

   private void cancelTimer() {
      mTimer.cancel();
   }

   private void startTimer() {
      mTimer.start();
   }

   private void updateProgress() {
      if ( mController != null && mController.getDuration() > 0 ) {
         mSeekBar.setProgress( mController.getCurrentPosition() );
      }
   }

   private void hideControls() {

      if ( !mControlsShowing ) {
         return;
      }

      mControlsShowing = false;

      AlphaAnimation alphaAnim = new AlphaAnimation( 1.0f, 0.0f );
      alphaAnim.setDuration( 200 );
      alphaAnim.setFillAfter( true );
      mPlayButton.startAnimation( alphaAnim );
      mPlayButton.setClickable( false );

      int y = getResources().getDimensionPixelSize( R.dimen.min_touch );

      TranslateAnimation translateAnim = new TranslateAnimation( 0, 0, 0, y );
      translateAnim.setDuration( 200 );
      translateAnim.setFillAfter( true );
      mVideoControls.startAnimation( translateAnim );
   }

   private void showControls() {

      if ( mControlsShowing ) {
         return;
      }
      updateProgress();
      mControlsShowing = true;

      AlphaAnimation alphaAnim = new AlphaAnimation( 0.0f, 1.0f );
      alphaAnim.setDuration( 200 );
      alphaAnim.setFillAfter( true );
      mPlayButton.startAnimation( alphaAnim );
      mPlayButton.setClickable( true );

      int y = getResources().getDimensionPixelSize( R.dimen.min_touch );

      TranslateAnimation anim = new TranslateAnimation( 0, 0, y, 0 );
      anim.setDuration( 200 );
      anim.setFillAfter( true );
      mVideoControls.startAnimation( anim );

      resetTimer();
   }

   @Override
   public void onCompletion() {
      showControls();
      cancelTimer();
      mPlayButton.setImageResource( R.drawable.ic_media_play );
   }

}
