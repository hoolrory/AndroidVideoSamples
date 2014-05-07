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

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.roryhool.commonvideolibrary.Intents;
import com.roryhool.commonvideolibrary.MediaHelper;
import com.roryhool.commonvideolibrary.UriHelper;

public class JoinActivity extends Activity {

   private static final int SELECT_VIDEO_1_CODE = 100;
   private static final int SELECT_VIDEO_2_CODE = 101;

   ImageView mVideo1Thumbnail;

   TextView mVideo1Name;

   ImageView mVideo2Thumbnail;

   TextView mVideo2Name;

   Button mJoinButton;

   @Override
   public void onCreate( Bundle savedInstanceState ) {
      super.onCreate( savedInstanceState );

      setContentView( R.layout.activity_join );

      mVideo1Thumbnail = (ImageView) findViewById( R.id.selected_video_1_thumbnail );
      mVideo2Thumbnail = (ImageView) findViewById( R.id.selected_video_2_thumbnail );

      mVideo1Name = (TextView) findViewById( R.id.selected_video_1_name );
      mVideo2Name = (TextView) findViewById( R.id.selected_video_2_name );
      
      mJoinButton = (Button) findViewById( R.id.join_button );

      loadUri1( getIntent().getData() );
   }

   public void onJoinClicked( View view ) {

   }

   public void onSelectClicked1( View view ) {
      startActivityForResult( Intents.GetLaunchVideoChooserIntent( this ), SELECT_VIDEO_1_CODE );
   }

   public void onSelectClicked2( View view ) {
      startActivityForResult( Intents.GetLaunchVideoChooserIntent( this ), SELECT_VIDEO_2_CODE );
   }

   @Override
   public void onActivityResult( int requestCode, int resultCode, Intent data ) {
      Uri uri = data.getData();

      if ( uri.getScheme().equals( "content" ) ) {
         String path = UriHelper.ContentUriToFilePath( this, uri );
         if ( path != null ) {
            uri = Uri.parse( path );
         }
      }

      if ( requestCode == SELECT_VIDEO_1_CODE ) {
         loadUri1( uri );
      } else if ( requestCode == SELECT_VIDEO_2_CODE ) {
         loadUri2( uri );
      }
   }

   private void loadUri1( Uri uri ) {
      Bitmap bitmap = MediaHelper.GetThumbnailFromVideo( uri, 0 );
      mVideo1Thumbnail.setImageBitmap( bitmap );

      File file = new File( uri.toString() );
      mVideo1Name.setText( file.getName() );
   }

   private void loadUri2( Uri uri ) {
      Bitmap bitmap = MediaHelper.GetThumbnailFromVideo( uri, 0 );
      mVideo2Thumbnail.setImageBitmap( bitmap );

      File file = new File( uri.toString() );
      mVideo2Name.setText( file.getName() );

      mJoinButton.setEnabled( true );
   }

}
