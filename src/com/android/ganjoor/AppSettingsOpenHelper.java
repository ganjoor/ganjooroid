/**
 * 
 */
package com.android.ganjoor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author Hamid Reza
 * برای ذخیرۀ تنظیمات از یک دیتابیس sqlite استفاده می کنیم.
 */
public class AppSettingsOpenHelper extends SQLiteOpenHelper  {
	// مشخصات دیتابیس و جدول
	private static final String DATABASE_NAME = "ganjoor-options";
	private static final int DATABASE_VERSION = 2;
	private static final String OPTIONS_TABLE_NAME = "option";
	private static final String OPTIONS_TABLE_CREATESQL = 
		"CREATE TABLE "+ OPTIONS_TABLE_NAME + " (" +
		"name TEXT, value TEXT);";

	/**
	 * سازنده پیش فرض
	 * @param context 
	 */
	AppSettingsOpenHelper(Context context){
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	/**
	 * ایجاد دیتابیس
	 */
	@Override
	public void onCreate(SQLiteDatabase db){
		db.execSQL(OPTIONS_TABLE_CREATESQL);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}	
	
	/**
	 * استخراج اطلاعات تنظیمات رشته ای از دیتابیس
	 * @param OptionName رشتۀ نام تنظیمات
	 * @return مقدار ذخیره شده یا خالی اگر مقداری ذخیره نشده باشد
	 */
	public String GetStringOptionValue(String OptionName){
		return GetStringOptionValue(OptionName, "");
	}
	/**
	 * استخراج اطلاعات تنظیمات رشته ای از دیتابیس
	 * @param OptionName رشتۀ نام تنظیمات
	 * @param DefaultValue مقداری که در صورت پیدا نشدن مقدار در دیتابیس بازخواهد گشت
	 * @return مقدار ذخیره شده یا پیش فرض
	 */
	public String GetStringOptionValue(String OptionName, String DefaultValue){
		String result = GetDbStringOptionValue(OptionName);
		if(result == null)
			return DefaultValue;
		return result;
	}

	/**
	 * استخراج اطلاعات تنظیمات عدد صحیح از دیتابیس
	 * @param OptionName رشتۀ نام تنظیمات
	 * @return مقدار ذخیره شده یا صفر اگر مقداری ذخیره نشده باشد
	 */	
	public int GetIntegerOptionValue(String OptionName){
		return GetIntegerOptionValue(OptionName, 0);
	}	

	/**
	 * استخراج اطلاعات تنظیمات عدد اعشاری از دیتابیس
	 * @param OptionName رشتۀ نام تنظیمات
	 * @param DefaultValue مقداری که در صورت پیدا نشدن مقدار در دیتابیس بازخواهد گشت
	 * @return مقدار ذخیره شده یا پیش فرض
	 */
	
	public float GetFloatOptionValue(String OptionName, float DefaultValue){
		String result = GetStringOptionValue(OptionName, String.valueOf(DefaultValue));
		return Float.parseFloat(result);
	}
	
	/**
	 * استخراج اطلاعات تنظیمات اعشاری از دیتابیس
	 * @param OptionName رشتۀ نام تنظیمات
	 * @return مقدار ذخیره شده یا صفر اگر مقداری ذخیره نشده باشد
	 */	
	public float GetFloatOptionValue(String OptionName){
		return GetFloatOptionValue(OptionName, 0.0f);
	}	

	/**
	 * استخراج اطلاعات تنظیمات عدد صحیح از دیتابیس
	 * @param OptionName رشتۀ نام تنظیمات
	 * @param DefaultValue مقداری که در صورت پیدا نشدن مقدار در دیتابیس بازخواهد گشت
	 * @return مقدار ذخیره شده یا پیش فرض
	 */
	
	public int GetIntegerOptionValue(String OptionName, int DefaultValue){
		String result = GetStringOptionValue(OptionName, String.valueOf(DefaultValue));
		return Integer.parseInt(result);
	}
	
	/**
	 * استخراج اطلاعات تنظیمات رشته ای از دیتابیس
	 * @param OptionName رشتۀ نام تنظیمات
	 * @return مقدار ذخیره شده یا null اگر مقداری ذخیره نشده باشد
	 */
	private String GetDbStringOptionValue(String OptionName){
		SQLiteDatabase db;
		try{
			db = getReadableDatabase();
		}
		catch(Exception e){
			db = getWritableDatabase();		
		}
		finally{
		
		}
		
		Cursor cursor = db.query(OPTIONS_TABLE_NAME, new String[]{"value"}, "name = '"+OptionName+"'", null, null, null, null, "1");
		if(cursor.moveToFirst())
			return cursor.getString(0);
		return null;
		
	}
	/**
	 * 
	 * @param OptionName رشتۀ نام تنظیمات
	 * @return این که آیا تنظیماتی با این رشتۀ نام در دیتابیس ذخیره شده یا خیر
	 */
	private Boolean OptionValueExistsInDb(String OptionName){
		return GetDbStringOptionValue(OptionName) != null;
	}
	/**
	 * ذخیرۀ مقدار برای تنظیمات با رشتۀ نام ارسال شده
	 * @param OptionName رشتۀ نام تنظیمات
	 * @param Value رشتۀ مقدار
	 */
	public void SetStringOptionValue(String OptionName, String Value){
		SQLiteDatabase db = getWritableDatabase();
		String sqlCommand;
		if(OptionValueExistsInDb(OptionName)){
			sqlCommand = "UPDATE "+OPTIONS_TABLE_NAME+ " SET value = '"+Value+"' WHERE name = '"+OptionName+"'";
		}
		else{
			sqlCommand = "INSERT INTO "+OPTIONS_TABLE_NAME + " (name, value) VALUES ('"+OptionName+"', '"+Value+"')";
		}
		db.execSQL(sqlCommand);
		
	}
	/**
	 * ذخیرۀ مقدار برای تنظیمات با رشتۀ نام ارسال شده
	 * @param OptionName رشتۀ نام تنظیمات
	 * @param Value  مقدار
	 */
	
	public void SetIntegerOptionValue(String OptionName, int Value){
		SetStringOptionValue(OptionName, String.valueOf(Value));
	}
	/**
	 * ذخیرۀ مقدار برای تنظیمات با رشتۀ نام ارسال شده
	 * @param OptionName رشتۀ نام تنظیمات
	 * @param Value  مقدار
	 */
	
	public void SetFloatOptionValue(String OptionName, float Value){
		SetStringOptionValue(OptionName, String.valueOf(Value));
	}
	
}
