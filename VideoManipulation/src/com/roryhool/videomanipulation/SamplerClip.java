package com.roryhool.videomanipulation;

import android.net.Uri;

import com.roryhool.commonvideolibrary.MediaHelper;

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
