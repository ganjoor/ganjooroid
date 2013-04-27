/**
 * 
 */
package com.android.ganjoor;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Environment;

/**
 * @author Hamid Reza
 * کلاس ذخیره و بازیابی تنظیمات برنامه
 */
public class AppSettings {
	//Settings:
	
	// DatabasePath
	/**
	 * بازگشت مقدار مسیر دیتابیس ganjoor.s3db
	 * @return مسیر دیتابیس ganjoor.s3db
	 */
	public static String getDatabasePath() {
		if(_db == null)
			return null;	
		return _db.GetStringOptionValue("dbpath",
				Environment.getExternalStorageDirectory() + "/Android/data/com.android.ganjoor"
				);
	}
	/**
	 * ذخیرۀ مقدار مسیر دیتابیس ganjoor.s3db
	 * @param Value مسیر دیتابیس ganjoor.s3db
	 */
	public static void setDatabasePath(String Value){
		if(_db == null)
			return;	
		_db.SetStringOptionValue("dbpath", Value);
	}
	
	//LastPoemIdVisited
	
	/**
	 * شناسۀ رکورد آخرین شعری که کاربر داشته آن را می دیده
	 */
	public static int getLastPoemIdVisited(){
		if(_db == null){
			return 0;
		}
		return _db.GetIntegerOptionValue("lastpoemvisited");
		
	}
	/**
	 * ذخیرۀ شناسۀ رکورد آخرین شعری که کاربر داشته آن را می دیده
	 * @param Value شناسۀ رکورد آخرین شعری که کاربر داشته آن را می دیده
	 */
	public static void setLastPoemIdVisited(int Value){
		if(_db == null){
			return;
		}
		_db.SetIntegerOptionValue("lastpoemvisited", Value);
	}
	
	/**
	 * شناسۀ رکورد آخرین بخشی که کاربر داشته آن را می دیده
	 */
	public static int getLastCatIdVisited(){
		if(_db == null){
			return 0;
		}
		return _db.GetIntegerOptionValue("lastcatvisited");
		
	}
	/**
	 * ذخیرۀ شناسۀ رکورد آخرین بخشی که کاربر داشته آن را می دیده
	 * @param Value شناسۀ رکورد آخرین بخشی که کاربر داشته آن را می دیده
	 */
	public static void setLastCatIdVisited(int Value){
		if(_db == null){
			return;
		}
		_db.SetIntegerOptionValue("lastcatvisited", Value);
	}
	
	/**
	 * اندازۀ پیش فرض فونت نمایش
	 */
	public static final float DEF_TEXT_SIZE = 20.0f;
	
	public static final float SMALL_TEXT_SIZE = 12.0f;
	public static final float MIDDLE_TEXT_SIZE = 16.0f;
	public static final float MIDLRG_TEXT_SIZE = 20.0f;
	public static final float LARGE_TEXT_SIZE = 26.0f;
	
	/**
	 * اندازۀ فونت انتخاب شده برای نمایش اشعار
	 * @return
	 */
	public static float getGanjoorViewFontSize(){
		if(_db == null){
			return DEF_TEXT_SIZE;
		}
		return _db.GetFloatOptionValue("vufontsize", DEF_TEXT_SIZE);
	}

	/**
	 * ذخیرۀ اندازۀ فونت انتخاب شده برای نمایش اشعار
	 */	
	public static void setGanjoorViewFontSize(float fSize){
		if(_db != null){
			_db.SetFloatOptionValue("vufontsize", fSize);
		}
	}
	
	// Downloads Path
	/**
	 * @return مسیر ذخیرۀ فایلهای دریافتی
	 */
	public static String getDownloadPath() {
		if(_db == null)
			return null;	
		return _db.GetStringOptionValue("dwnldpath",
				Environment.getExternalStorageDirectory() + "/Android/data/com.android.ganjoor"
				);
	}
	/**
	 * ذخیرۀ مقدار مسیر فایلهای دریافتی
	 * @param Value مسیر فایلهای دریافتی
	 */
	public static void setDownloadPath(String Value){
		if(_db == null)
			return;	
		_db.SetStringOptionValue("dwnldpath", Value);
	}
	
	/**
	 * در دریافت مجموعه ها از داونلود منیجر اندروید استفاده شود یا خیر
	 * @return
	 */
	public static Boolean getUseAndroidDownloadManager(){
		if(_db == null)
			return false;
		return _db.GetIntegerOptionValue("usedownloadmanager", 1) == 1;
	}
	
	/**
	 * ذخیرۀ آپشن استفاده از داونلود منیجر اندروید
	 * @param Value
	 */
	public static void setUseAndroidDownloadManager(Boolean Value){
		if(_db == null)
			return;
		_db.SetIntegerOptionValue("usedownloadmanager", Value ? 1 : 0);
	}
	
	

	//Initialization:
	/**
	 * قبل از استفاده از کلاس حتماً باید این متد با مقدار غیر null برای پارامتر ورودی ارسال شود
	 * @param context نمونۀ کلاس Activity
	 */
	public static void Init(Context context){
		if(_db == null){
			_db = new AppSettingsOpenHelper(context);
		}
		if(_typeface == null){
			Typeface.createFromAsset(context.getAssets(), "irsans.ttf");
		}		

	}
	
	/**
	 * فونت نمایش متون
	 */	
	public static void setApplicationTypeface(Typeface typeface){
		_typeface = typeface;
	}
	
	public static Typeface getApplicationTypeface(){
		return _typeface;
	}
	
	//Internals:
	/**
	 * دیتابیس ذخیره و بازیابی تنظیمات
	 */
	private static AppSettingsOpenHelper _db = null;
	
	/**
	 * فونت نمایش متون
	 * فونت نمایش متن، رفرنسی از آن جهت تنظیم فونت کنترلهایی که بعدا اضافه می شوند نگهداری می شود
	 */
	private static Typeface _typeface = null;

}
