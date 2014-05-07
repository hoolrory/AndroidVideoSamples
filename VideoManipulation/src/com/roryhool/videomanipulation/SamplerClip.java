package com.roryhool.videomanipulation;

import android.net.Uri;

public class SamplerClip {

   Uri mUri;

   int mStartTime = -1;
   int mEndTime = -1;

   public SamplerClip( Uri uri ) {
      mUri = uri;
   }

   public void setStartTime( int startTime ) {
      mStartTime = startTime;
   }

   public void setEndTime( int endTime ) {
      mEndTime = endTime;
   }

   public Uri getUri() {
      return mUri;
   }

   public int getStartTime() {
      return mStartTime;
   }

   public int getEndTime() {
      return mEndTime;
   }
}
