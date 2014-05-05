package com.roryhool.commonvideolibrary;

public class Resolution {
   
   public static final Resolution RESOLUTION_1080P = new Resolution( 1920, 1080 );
   public static final Resolution RESOLUTION_720P = new Resolution( 1280, 720 );
   public static final Resolution RESOLUTION_480P = new Resolution( 740, 480 );
   public static final Resolution RESOLUTION_360P = new Resolution( 640, 360 );
   public static final Resolution RESOLUTION_QVGA = new Resolution( 320, 240 );
   public static final Resolution RESOLUTION_QCIF = new Resolution( 176, 144 );

   int mWidth;
   int mHeight;

   public Resolution( int width, int height ) {
      mWidth = width;
      mHeight = height;
   }

   public int getWidth() {
      return mWidth;
   }

   public void setWidth( int width ) {
      mWidth = width;
   }

   public int getHeight() {
      return mHeight;
   }

   public void setHeight( int height ) {
      mHeight = height;
   }

   public Resolution rotate() {
      return new Resolution( mHeight, mWidth );
   }

}
