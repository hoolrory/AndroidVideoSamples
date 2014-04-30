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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class CursorHelper {

	public static String GetColumn( Context context, Uri uri, String column, String selection, String[] selectionArgs ) {

		String path = null;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query( uri, new String[] { column }, selection, selectionArgs, null );
			if ( cursor != null && cursor.moveToFirst() ) {
				int column_index = cursor.getColumnIndexOrThrow( column );
				path = cursor.getString( column_index );
			}
		} finally {
			if ( cursor != null ) {
				cursor.close();
			}
		}
		
		return path;
	}
}
