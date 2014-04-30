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

package com.roryhool.commonvideolibrary;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.HandlerBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.util.Matrix;

public class MediaHelper {

   public static Bitmap GetThumbnailFromVideo( Uri uri, long timeMs ) {
      MediaMetadataRetriever retriever = new MediaMetadataRetriever();
      retriever.setDataSource( uri.toString() );
      return retriever.getFrameAtTime( timeMs * 1000 );
   }

   @TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR1 )
   public static int GetRotation( Uri uri ) {
      MediaMetadataRetriever retriever = new MediaMetadataRetriever();
      retriever.setDataSource( uri.toString() );
      String rotation = retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION );

      return Integer.parseInt( rotation );
   }

   public static Uri RotateVideo( Uri uri, int rotation ) {

      Uri rotatedVideoUri = null;

      try {
         IsoFile file = new IsoFile( uri.toString() );

         List<Box> boxes = file.getMovieBox().getBoxes();

         for ( Box box : boxes ) {
            if ( box instanceof TrackBox ) {
               TrackBox trackBox = (TrackBox) box;
               
               HandlerBox handlerBox = trackBox.getMediaBox().getHandlerBox();
               if ( handlerBox.getHandlerType().toLowerCase( Locale.US ).equals( "vide" ) ) {
                  TrackHeaderBox trackHeaderBox = trackBox.getTrackHeaderBox();
                  trackHeaderBox.setMatrix( GetMatrixFromRotation( rotation ) );
               }
            }
         }

         String pathWithoutExtension = uri.toString().replace( ".mp4", "" );

         String rotatedFileName = String.format( Locale.US, "%s_rotated_to_%d.mp4", pathWithoutExtension, rotation );

         FileOutputStream videoFileOutputStream = new FileOutputStream( rotatedFileName );
         file.getBox( videoFileOutputStream.getChannel() );

         file.close();
         videoFileOutputStream.close();

         rotatedVideoUri = Uri.parse( rotatedFileName );

      } catch ( IOException e ) {
         e.printStackTrace();

         return null;
      }

      return rotatedVideoUri;
   }

   private static Matrix GetMatrixFromRotation( int rotation ) {
      switch ( rotation ) {
      case 90:
         return Matrix.ROTATE_90;
      case 180:
         return Matrix.ROTATE_180;
      case 270:
         return Matrix.ROTATE_270;
      default:
      case 0:
         return Matrix.ROTATE_0;
      }
   }
}
