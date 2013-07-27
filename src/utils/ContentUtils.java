package utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import com.catfeed.Constants;

import android.content.ContentValues;
import android.util.Log;

public class ContentUtils {

	public static ContentValues createValues(Object model) {
		Field[] fields = model.getClass().getDeclaredFields();
		ContentValues values = new ContentValues(fields.length);
		try {
			for (Field f: fields) {
				if (isPersistent(f)) {
					if (String.class.equals(f.getType())) {
						values.put(f.getName(), (String) f.get(model));					
					}
					else if (Integer.class.equals(f.getType())) {
						values.put(f.getName(), (Integer) f.get(model));					
					}
					else if (Long.class.equals(f.getType())) {
						values.put(f.getName(), (Long) f.get(model));				
					}
					else if (Boolean.class.equals(f.getType())) {
						Boolean value = (Boolean) f.get(model);
						values.put(f.getName(), (value != null && value == true)? 1: 0);					
					}
					else {
						Object value = f.get(model);
						if (value != null) values.put(f.getName(), (String) value);					
					}					
				}
			}
		}
		catch(Exception e) {
			Log.e(Constants.LOGTAG, "ContentUtils.createValues():" + e.toString());
		}
		return values;
	}
	
	private static boolean isPersistent(Field f) {
		return !Modifier.isTransient(f.getModifiers()) &&
			   !Modifier.isStatic(f.getModifiers());
	}
}
