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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class DecodeWithMediaCodecActivity extends Activity {

   VideoPlayerView mVideoPlayerView;

   MediaCodecDecodeController mController;

   @Override
   public void onCreate( Bundle savedInstanceState ) {
      super.onCreate( savedInstanceState );

      getActionBar().hide();
      getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
      getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );

      setContentView( R.layout.activity_decode_with_mediacodec );

      mVideoPlayerView = (VideoPlayerView) findViewById( R.id.video_player );

      mController = new MediaCodecDecodeController( this, mVideoPlayerView, mVideoPlayerView.getTextureView() );
      mController.setVideoUri( getIntent().getData() );

      mVideoPlayerView.setController( mController );
      mController.setListener( mVideoPlayerView );
   }

   @Override
   public void onPause() {
      super.onPause();
      mController.shutDown();
   }

}
