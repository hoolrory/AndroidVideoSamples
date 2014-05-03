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
import android.net.Uri;
import android.os.Handler;
import android.view.TextureView.SurfaceTextureListener;

public abstract class ControllerBase implements SurfaceTextureListener {

   public interface ControllerListener {
      public void onCompletion();
   }
   
   Context mContext;

   Uri mVideoUri;

   ControllerListener mListener;

   Handler mHandler;

   public ControllerBase( Context context ) {
      mContext = context;

      mHandler = new Handler();
   }

   public void setVideoUri( Uri videoUri ) {
      mVideoUri = videoUri;
   }

   public void setListener( ControllerListener listener ) {
      mListener = listener;
   }

   public abstract void play();

   public abstract void pause();

   public abstract void seekTo( int ms );

   public abstract boolean isPlaying();

   public abstract int getDuration();

   public abstract int getCurrentPosition();

   public abstract int getVideoWidth();

   public abstract int getVideoHeight();

   protected void onCompletion() {
      if ( mListener != null ) {
         mHandler.post( new Runnable() {

            @Override
            public void run() {
               mListener.onCompletion();
            }

         } );
      }
   }

}
