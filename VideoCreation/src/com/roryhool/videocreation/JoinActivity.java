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

import java.io.File;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.roryhool.commonvideolibrary.Intents;
import com.roryhool.commonvideolibrary.MediaHelper;
import com.roryhool.commonvideolibrary.SamplerClip;
import com.roryhool.commonvideolibrary.UriHelper;
import com.roryhool.commonvideolibrary.VideoResampler;

public class JoinActivity extends Activity {

   private static final int SELECT_VIDEO_1_CODE = 100;
   private static final int SELECT_VIDEO_2_CODE = 101;

   ImageView mVideo1Thumbnail;

   TextView mVideo1Name;

   ImageView mVideo2Thumbnail;

   TextView mVideo2Name;

   Button mJoinButton;

   Uri mUri1;
   Uri mUri2;

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

      new JoinTask().execute( mUri1, mUri2 );
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

      mUri1 = uri;

      Bitmap bitmap = MediaHelper.GetThumbnailFromVideo( uri, 0 );
      mVideo1Thumbnail.setImageBitmap( bitmap );

      File file = new File( uri.toString() );
      mVideo1Name.setText( file.getName() );
   }

   private void loadUri2( Uri uri ) {

      mUri2 = uri;

      Bitmap bitmap = MediaHelper.GetThumbnailFromVideo( uri, 0 );
      mVideo2Thumbnail.setImageBitmap( bitmap );

      File file = new File( uri.toString() );
      mVideo2Name.setText( file.getName() );

      mJoinButton.setEnabled( true );
   }

   class JoinTask extends AsyncTask<Uri, Void, Uri> {

      @Override
      protected Uri doInBackground( Uri... uris ) {

         if ( uris.length < 2 ) {
            return null;
         }

         Uri uri1 = uris[0];
         Uri uri2 = uris[1];

         VideoResampler resampler = new VideoResampler();
         SamplerClip clip1 = new SamplerClip( uri1 );
         resampler.addSamplerClip( clip1 );
         SamplerClip clip2 = new SamplerClip( uri2 );
         resampler.addSamplerClip( clip2 );

         String pathWithoutExtension1 = uri1.toString().replace( ".mp4", "" );
         String pathWithoutExtension2 = uri2.toString().replace( ".mp4", "" );

         String resampledFileName = String.format( Locale.US, "%s_joined_to_other.mp4", pathWithoutExtension1 );

         Uri outputUri = Uri.parse( resampledFileName );

         resampler.setOutput( outputUri );

         try {
            resampler.start();
         } catch ( Throwable e ) {
            e.printStackTrace();
         }

         return outputUri;
      }

      @Override
      protected void onPostExecute( Uri outputUri ) {
         Intent sendIntent = new Intent();
         sendIntent.setAction( Intent.ACTION_VIEW );
         sendIntent.setDataAndType( outputUri, "video/mp4" );
         startActivity( sendIntent );
      }
   }

}
