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

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

public class UriHelper {
	
	private static String EXTERNAL_STORAGE_DOCUMENTS_PROVIDER 	= "com.android.externalstorage.documents";
	private static String DOWNLOAD_DOCUMENTS_PROVIDER 			= "com.android.providers.downloads.documents";
	private static String MEDIA_DOCUMENTS_PROVIDER 				= "com.android.providers.media.documents";
	
	private static String PUBLIC_DOWNLOADS_CONTEXT_URI = "content://downloads/public_downloads";
	
	private static String DATA_COLUMN = "_data";
	
	private static String DOCUMENT_TYPE_AUDIO 	= "audio";
	private static String DOCUMENT_TYPE_IMAGE 	= "image";
	private static String DOCUMENT_TYPE_VIDEO 	= "video";
	private static String DOCUMENT_TYPE_PRIMARY = "primary";

	
	private static String URI_SCHEME_CONTENT 	= "content";
	private static String URI_SCHEME_FILE 		= "file";
	
	@SuppressLint("NewApi")
   public static String ContentUriToFilePath( Context context, Uri uri )
	{
		String authority = uri.getAuthority();
		String scheme = uri.getScheme();
		
		if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri) ) {

			String documentId = DocumentsContract.getDocumentId( uri );
			String[] documentIdPieces = documentId.split( ":" );
			String documentType = documentIdPieces[0];
			String documentPath = documentIdPieces[1];
			
			if ( authority.equalsIgnoreCase( EXTERNAL_STORAGE_DOCUMENTS_PROVIDER ) ) {
            	
				if ( documentType.equalsIgnoreCase( DOCUMENT_TYPE_PRIMARY ) ) {
					return String.format( Locale.US, "%s/%s", Environment.getExternalStorageDirectory(), documentPath );
				}

			} else if ( authority.equalsIgnoreCase( DOWNLOAD_DOCUMENTS_PROVIDER ) ) {

				Uri contentUri = ContentUris.withAppendedId( Uri.parse( PUBLIC_DOWNLOADS_CONTEXT_URI ), Long.valueOf( documentId ) );
				
				return CursorHelper.GetColumn( context, contentUri, DATA_COLUMN, null, null );
			
			} else if ( authority.equalsIgnoreCase( MEDIA_DOCUMENTS_PROVIDER ) ) {
				
				Uri contentUri = null;
				
				if ( documentType.equalsIgnoreCase( DOCUMENT_TYPE_AUDIO ) ) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				} else if ( documentType.equalsIgnoreCase( DOCUMENT_TYPE_IMAGE ) ) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ( documentType.equalsIgnoreCase( DOCUMENT_TYPE_VIDEO ) ) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				}
				
				return CursorHelper.GetColumn( context, contentUri, DATA_COLUMN, "_id=?", new String[] { documentPath } );
				
			}
			
         } else if ( scheme.equalsIgnoreCase( URI_SCHEME_CONTENT ) ) {
        	 
             return CursorHelper.GetColumn( context, uri, DATA_COLUMN, null, null );
             
         } else if ( scheme.equalsIgnoreCase( URI_SCHEME_FILE ) ) {
        	 
             return uri.getPath();
         }

		return null;
	}

}
