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

package com.roryhool.videocreation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;

import com.roryhool.videocreation.SurfaceEncoder.EncoderListener;
import com.roryhool.videocreation.SurfaceEncoder.EncoderSource;

public class RenderFromSurfaceActivity extends Activity {

   SurfaceView mSurfaceView;

   String      mFilePath;

   Uri mOutputUri;

   @Override
   protected void onCreate( Bundle savedInstanceState ) {
      super.onCreate( savedInstanceState );
      setContentView( R.layout.activity_render_from_surface );
   }

   public void onRenderClicked( View view ) {

      mOutputUri = Uri.parse( Environment.getExternalStorageDirectory().getPath() + "/test.mp4" );
      SurfaceEncoder encoder = new SurfaceEncoder();

      encoder.setEncoderSource( mEncoderSource );
      encoder.addEncoderListener( mEncoderListener );
      encoder.setOutputUri( mOutputUri );
      encoder.start();
   }
   
   EncoderSource mEncoderSource = new EncoderSource() {

      @Override
      public void renderFrame( Canvas canvas, long time, long interval ) {
         canvas.drawRGB( (int) ( Math.random() * 255 ), (int) ( Math.random() * 255 ), (int) ( Math.random() * 255 ) );
      }

      @Override
      public int getWidth() {
         return 1280;
      }

      @Override
      public int getHeight() {
         return 720;
      }
      
      public long getDuration() {
         return 10 * 1000; // 10 seconds
      }

   };

   EncoderListener mEncoderListener = new EncoderListener() {

      @Override
      public void encoderSucceeded() {

         Intent sendIntent = new Intent();
         sendIntent.setAction( Intent.ACTION_VIEW );
         sendIntent.setDataAndType( mOutputUri, "video/mp4" );
         startActivity( sendIntent );
      }

      @Override
      public void encoderFailed() {
         
      }
   };

}
