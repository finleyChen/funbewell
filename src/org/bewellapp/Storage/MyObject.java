package org.bewellapp.Storage;

import android.content.ContentValues;

public class MyObject{

	public MyObject(ContentValues _values){
		values = _values;
		}
	
	public void setValue(ContentValues _values){
		values = _values;
	}
	
	public ContentValues getValue(){
				return values;
			}
	
	private ContentValues values;
}
