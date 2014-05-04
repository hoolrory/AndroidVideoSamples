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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.roryhool.commonvideolibrary.Intents;
import com.roryhool.commonvideolibrary.UriHelper;

public class MainActivity extends Activity {

   private int SELECT_VIDEO_CODE = 100;

   LinearLayout mSelectedVideoLayout;

   Uri mUri;

   @Override
   public void onCreate( Bundle savedInstanceState ) {
      super.onCreate( savedInstanceState );

      setContentView( R.layout.activity_main );

      mSelectedVideoLayout = (LinearLayout) findViewById( R.id.selected_video_layout );
   }

   public void onSelectClicked( View view ) {
      startActivityForResult( Intents.GetLaunchVideoChooserIntent( this ), SELECT_VIDEO_CODE );
   }

   @Override
   public void onActivityResult( int requestCode, int resultCode, Intent data ) {

      if ( data != null ) {

         mUri = data.getData();

         if ( mUri.getScheme().equals( "content" ) ) {
            String path = UriHelper.ContentUriToFilePath( this, mUri );
            mUri = Uri.parse( path );
         }

         mSelectedVideoLayout.setVisibility( View.VISIBLE );
      }
   }

   public void onRotateClicked( View view ) {
      Intent intent = new Intent( this, RotationActivity.class );
      intent.setData( mUri );
      startActivity( intent );
   }

   public void onJoinClicked( View view ) {
      Intent intent = new Intent( this, JoinActivity.class );
      intent.setData( mUri );
      startActivity( intent );
   }

   public void onTrimClicked( View view ) {
      Intent intent = new Intent( this, TrimActivity.class );
      intent.setData( mUri );
      startActivity( intent );
   }

   public void onResampleClicked( View view ) {
      Intent intent = new Intent( this, ResampleActivity.class );
      intent.setData( mUri );
      startActivity( intent );
   }

}
