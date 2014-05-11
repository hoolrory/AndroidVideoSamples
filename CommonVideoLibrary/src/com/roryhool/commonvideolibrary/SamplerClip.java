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

import android.net.Uri;

public class SamplerClip {

   Uri mUri;

   long mStartTime = -1;
   long mEndTime = -1;

   int mVideoDuration;

   public SamplerClip( Uri uri ) {
      mUri = uri;

      mVideoDuration = MediaHelper.GetDuration( uri );
   }

   public void setStartTime( long startTime ) {
      mStartTime = startTime;
   }

   public void setEndTime( int endTime ) {
      mEndTime = endTime;
   }

   public Uri getUri() {
      return mUri;
   }

   public long getStartTime() {
      return mStartTime;
   }

   public long getEndTime() {
      return mEndTime;
   }

   public int getVideoDuration() {
      return mVideoDuration;
   }
}
