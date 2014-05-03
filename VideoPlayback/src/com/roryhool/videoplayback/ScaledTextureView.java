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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

public class ScaledTextureView extends TextureView implements SurfaceTextureListener {

   int mVideoWidth = 1;
   int mVideoHeight = 1;

   private List<SurfaceTextureListener> mListeners = new ArrayList<SurfaceTextureListener>();

   boolean mSurfaceAvailable = false;

   SurfaceTexture mSurfaceTexture;

   int mWidth;
   int mHeight;

   public ScaledTextureView( Context context ) {
      super( context );
      init();
   }

   public ScaledTextureView( Context context, AttributeSet attrs ) {
      super( context, attrs );
      init();
   }

   public ScaledTextureView( Context context, AttributeSet attrs, int defStyle ) {
      super( context, attrs, defStyle );
      init();
   }

   private void init() {
      setSurfaceTextureListener( this );
   }

   public void addSurfaceTextureListener( SurfaceTextureListener listener ) {
      mListeners.add( listener );
      
      if ( mSurfaceAvailable ) {
         listener.onSurfaceTextureAvailable( mSurfaceTexture, mWidth, mHeight );
      }
   }

   public void SetVideoSize( int videoWidth, int videoHeight ) {
      mVideoWidth = videoWidth;
      mVideoHeight = videoHeight;
      requestLayout();
   }

   @Override
   protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {

      int parentSpecifiedWidth = MeasureSpec.getSize( widthMeasureSpec );
      int parentSpecifiedHeight = MeasureSpec.getSize( heightMeasureSpec );

      double heightToWidthRatio = (double) mVideoHeight / (double) mVideoWidth;

      int width;
      int height;

      if ( parentSpecifiedWidth * heightToWidthRatio > parentSpecifiedHeight ) {
         width = (int) ( parentSpecifiedHeight / heightToWidthRatio );
         height = parentSpecifiedHeight;
      } else {
         width = parentSpecifiedWidth;
         height = (int) ( parentSpecifiedWidth * heightToWidthRatio );
      }

      super.onMeasure( MeasureSpec.makeMeasureSpec( width, MeasureSpec.EXACTLY ), MeasureSpec.makeMeasureSpec( height, MeasureSpec.EXACTLY ) );
   }

   @Override
   public void onSurfaceTextureAvailable( SurfaceTexture surface, int width, int height ) {
      mSurfaceTexture = surface;

      mWidth = width;
      mHeight = height;

      mSurfaceAvailable = true;

      for ( SurfaceTextureListener listener : mListeners ) {
         listener.onSurfaceTextureAvailable( surface, width, height );
      }
   }

   @Override
   public boolean onSurfaceTextureDestroyed( SurfaceTexture surface ) {
      for ( SurfaceTextureListener listener : mListeners ) {
         listener.onSurfaceTextureDestroyed( surface );
      }

      return true;
   }

   @Override
   public void onSurfaceTextureSizeChanged( SurfaceTexture surface, int width, int height ) {
      mWidth = width;
      mHeight = height;

      for ( SurfaceTextureListener listener : mListeners ) {
         listener.onSurfaceTextureSizeChanged( surface, width, height );
      }
   }

   @Override
   public void onSurfaceTextureUpdated( SurfaceTexture surface ) {
      for ( SurfaceTextureListener listener : mListeners ) {
         listener.onSurfaceTextureUpdated( surface );
      }
   }
}
