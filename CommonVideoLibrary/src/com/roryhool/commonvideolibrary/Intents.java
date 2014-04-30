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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class Intents {

   @SuppressLint( "InlinedApi" )
   public static Intent GetLaunchVideoChooserIntent( Context context ) {
      if ( Build.VERSION.SDK_INT < 19 ) {
         Intent intent = new Intent();
         intent.addCategory( Intent.CATEGORY_OPENABLE );
         intent.setType( "video/mp4" );
         intent.setAction( Intent.ACTION_GET_CONTENT );
         return Intent.createChooser( intent, context.getString( R.string.select_video ) );
      } else {
         Intent intent = new Intent( Intent.ACTION_OPEN_DOCUMENT );
         intent.addCategory( Intent.CATEGORY_OPENABLE );
         intent.setType( "video/mp4" );
         intent.setAction( Intent.ACTION_GET_CONTENT );
         return Intent.createChooser( intent, context.getString( R.string.select_video ) );
      }
   }
}
