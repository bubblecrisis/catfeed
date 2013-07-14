package utils;

import android.database.Cursor;

public class CursorUtils {

	
	public static String getString(Cursor cursor, String column) {
		return cursor.getString(cursor.getColumnIndex(column));
	}

	public static Long getLong(Cursor cursor, String column) {
		return cursor.getLong(cursor.getColumnIndex(column));
	}

	public static boolean getBoolean(Cursor cursor, String column) {
		return cursor.getInt(cursor.getColumnIndex(column)) != 0;
	}
	
	public static byte[] getBlob(Cursor cursor, String column) {
		return cursor.getBlob(cursor.getColumnIndex(column));
	}
}
