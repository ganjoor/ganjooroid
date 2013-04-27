/**
 * 
 */
package com.android.ganjoor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author Hamid Reza
 * کلاس اصلی استخراج و در صورت نیاز ذخیرۀ اطلاعات در دیتابیس گنجور رومیزی
 */
public class GanjoorDbBrowser {
	/**
	 * سازندۀ بدون پارامتر
	 *  
	 */
	public GanjoorDbBrowser(Context context){
		AppSettings.Init(context);
		//mimicking .NET Path.Combine:
		File dbPath = new File(AppSettings.getDatabasePath());		
		OpenDatbase(new File(dbPath, "ganjoor.s3db").getPath());
	}
	
	/**
	 * سازندۀ با پارامتر مسیر دیتابیس
	 * @param dbPath مسیر دیتابیس
	 */
	public GanjoorDbBrowser(String dbPath){
		OpenDatbase(dbPath);
	}
	
	
	/**
	 * دیتابیس برنامه
	 */
	private SQLiteDatabase _db = null;
	/**
	 * مسیر فرستاده شده برای دیتابیس برنامه
	 */
	private String _dbPath = "";
	/**
	 * نتیجۀ آخرین exception
	 */
	public String _LastError = null;

	/**
	 * باز کردن فایل دیتابیس گنجور رومیزی
	 * @param dbPath مسیر دیتابیس
	 * @return true if succeeds
	 */
	public Boolean OpenDatbase(String dbPath){
		_dbPath = dbPath;
		//if(!getDatabaseFileExists())
		//	return false;
     	try{
     		_db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);	
     	}
     	catch(Exception exp){
     		_LastError = exp.toString();
     	}
     	
    	return getIsConnected();			
	}
	
	/**
	 * باز کردن مجدد فایل دیتابیس باز شدۀ قبلی
	 * @return نتیجه
	 */
	public Boolean OpenDatbase(){
		if(_dbPath.isEmpty()){
			return false;
		}
		return OpenDatbase(_dbPath);
	}
	
	/**
	 * بستن فایل دیتابیس
	 */
	public void CloseDatabase(){
		if(getIsConnected()){
			_db.close();
			_db = null;
		}
	}
	
	
	/**
	 * ایجاد یک بانک گنجور خالی
	 * @param fileName مسیر فایل
	 * @param failIfExists اگر وجود داشته باشد کاری صورت نگیرد
	 * @return نتیجۀ عملیات
	 */
	public static Boolean CreateNewPoemDatabase(String fileName, Boolean failIfExists){
		if(failIfExists){
			File f = new File(fileName);
			if(f.exists())
				return false;
		}
		File dbPath = new File(fileName).getParentFile();		
		try{			
			dbPath.mkdirs();
		}
		catch(SecurityException e){
			return false;
		}	
		dbPath = new File(fileName);
		if(dbPath.isDirectory()){
			dbPath.delete();
		}
		SQLiteDatabase newDb;
		try{			
			newDb = SQLiteDatabase.openDatabase(dbPath.getAbsoluteFile().toString(), null, SQLiteDatabase.CREATE_IF_NECESSARY);
		}
		catch(Exception exp){
			return false;
		}
		Boolean result = CreateEmptyDB(newDb);
		newDb.close();
		return result;
		
	}
	
	/**
	 * ایجاد جداول بانک گنجور
	 * @param newDb دیتابیس مقصد
	 * @return نتیجۀ عملیات
	 */
	private static Boolean CreateEmptyDB(SQLiteDatabase newDb){
        String sql = "CREATE TABLE [cat] ([id] INTEGER  PRIMARY KEY NOT NULL,[poet_id] INTEGER  NULL,[text] NVARCHAR(100)  NULL,[parent_id] INTEGER  NULL,[url] NVARCHAR(255)  NULL);";
        try{
        	newDb.execSQL(sql);
        }catch(SQLException exp){
        	return false;
        }
        
        sql = "CREATE TABLE poem (id INTEGER PRIMARY KEY, cat_id INTEGER, title NVARCHAR(255), url NVARCHAR(255));";
        try{
        	newDb.execSQL(sql);
        }catch(SQLException exp){
        	return false;
        }
        
        sql = "CREATE TABLE [poet] ([id] INTEGER  PRIMARY KEY NOT NULL,[name] NVARCHAR(20)  NULL,[cat_id] INTEGER  NULL  NULL, [description] TEXT);";
        try{
        	newDb.execSQL(sql);
        }catch(SQLException exp){
        	return false;
        }
        
        sql = "CREATE TABLE [verse] ([poem_id] INTEGER  NULL,[vorder] INTEGER  NULL,[position] INTEGER  NULL,[text] TEXT  NULL);";
        try{
        	newDb.execSQL(sql);
        }catch(SQLException exp){
        	return false;
        }

        return true;
	}
	
	/**
	 * @return فایل دیتابیس در مسیر تعیین شده وجود دارد یا خیر
	 */
	public Boolean getDatabaseFileExists(){
		if(getIsConnected())
			return true;
		File f = new File(_dbPath);	    
     	return f.exists();  			
	}
	
	/**
	 * آیا به دیتابیس وصل شده ایم یا خیر
	 * @return true if yes
	 */
	public Boolean getIsConnected(){
		return _db != null;
	}
	
	private static final int IDX_POET_ID	=	0;
	private static final int IDX_POET_NAME	=	1;
	private static final int IDX_POET_CATID	=	2;
	private static final int IDX_POET_BIO	=	3;
	
	/**
	 * دسترسی به فهرست شاعران دیتابیس
	 * @return فهرست شاعران - در صورت عدم اتصال به دیتابیس این متد یک لیست خالی بر می گرداند
	 */
	public List<GanjoorPoet> getPoets(){
		LinkedList<GanjoorPoet> poets = new LinkedList<GanjoorPoet>();
		if(getIsConnected()){
			Cursor cursor = _db.query("poet", new String[]{"id", "name", "cat_id", "description"}, null, null, null, null, "name");
			if(cursor.moveToFirst()) {
				do{
					poets.add(
							new GanjoorPoet(
									cursor.getInt(IDX_POET_ID),
									cursor.getString(IDX_POET_NAME),
									cursor.getInt(IDX_POET_CATID),
									cursor.getString(IDX_POET_BIO)
									)
							);
					
				}while(cursor.moveToNext());
			}
		}
		return poets;		
	}
	
	public GanjoorPoet getPoet(int PoetId){
		if(getIsConnected()){
			Cursor cursor = _db.query("poet", new String[]{"id", "name", "cat_id", "description"}, "id = "+ PoetId, null, null, null, "name");
			if(cursor.moveToFirst()){
				return new GanjoorPoet(
						cursor.getInt(IDX_POET_ID),
						cursor.getString(IDX_POET_NAME),
						cursor.getInt(IDX_POET_CATID),
						cursor.getString(IDX_POET_BIO)
						);
			}
		}
		return null;		
	}
	
	private static final int IDX_CAT_ID			=	0;
	private static final int IDX_CAT_POETID		=	1;
	private static final int IDX_CAT_TEXT		=	2;
	private static final int IDX_CAT_PARENTID	=	3;
	private static final int IDX_CAT_URL		=	4;
	
	/**
	 * استخراج اطلاعات یک بخش از روی شناسۀ رکورد آن
	 * @param CatId شناسۀ رکورد متناظر
	 * @return اگر بخش موجود نباشد یا به دیتابیس متصل نباشیم null
	 */
	public GanjoorCat getCat(int CatId){
		if(getIsConnected()){
			Cursor cursor = _db.query("cat", new String[]{"id", "poet_id", "text", "parent_id", "url"}, "id = " + CatId, null, null, null, "id", "1");
			if(cursor.moveToFirst()){
				return new GanjoorCat(
						cursor.getInt(IDX_CAT_ID),
						cursor.getInt(IDX_CAT_POETID),
						cursor.getString(IDX_CAT_TEXT),
						cursor.getInt(IDX_CAT_PARENTID),
						cursor.getString(IDX_CAT_URL)
						);
			}
		}
		return null;
	}
	/**
	 * فهرست زیربخشهای یک بخش
	 * @param CatId شناسۀ رکورد بخش
	 * @return در صورت عدم اتصال به دیتابیس یک لیست خالی باز می گردد
	 */
	public List<GanjoorCat> getSubCats(int CatId){
		LinkedList<GanjoorCat> cats = new LinkedList<GanjoorCat>();
		if(getIsConnected()){
			Cursor cursor = _db.query("cat", new String[]{"id", "poet_id", "text", "parent_id", "url"}, "parent_id = " + CatId, null, null, null, "id");
			if(cursor.moveToFirst()) {
				do{
					cats.add(
							new GanjoorCat(
									cursor.getInt(IDX_CAT_ID),
									cursor.getInt(IDX_CAT_POETID),
									cursor.getString(IDX_CAT_TEXT),
									cursor.getInt(IDX_CAT_PARENTID),
									cursor.getString(IDX_CAT_URL)
									)
							);
					
				}while(cursor.moveToNext());
			}
		}
		return cats;		
	}
	
	private static final int IDX_POEM_ID			=	0;
	private static final int IDX_POEM_CATID			=	1;
	private static final int IDX_POEM_TITLE			=	2;
	private static final int IDX_POEM_URL			=	3;
	private static final int IDX_POEM_FIRSTVERSE	=	4;
		
	
	/**
	 * فهرست شعرهای یک بخش
	 * @param CatId شناسۀ رکورد بخش
	 * @return در صورت عدم اتصال به دیتابیس یک لیست خالی باز می گردد
	 */
	public List<GanjoorPoem> getPoems(int CatId){
		return getPoems(CatId, false);
	}
	/**
	 * فهرست شعرهای یک بخش
	 * @param CatId شناسۀ رکورد بخش 
	 * @param IncludeFirstVerse مصرع اول هم گنجانده شود
	 * @return
	 */
	public List<GanjoorPoem> getPoems(int CatId, Boolean IncludeFirstVerse){
		LinkedList<GanjoorPoem> poems = new LinkedList<GanjoorPoem>();
		if(getIsConnected()){
			Cursor cursor;
			if(IncludeFirstVerse){
				//این کوئری شعرهای بدون مصرع را نادیده می گیرد
				// هر چند به نظر نمی رسد این ایراد مهمی در برنامۀ نمایش شعر ایجاد کند
				// اما برای حالتهایی مثل ویرایشگر شعر ایجاد اشکال خواهد کرد
				// راه دیگر استفاده از دو کوئری مجزا برای شعرها و مصرع اول آنها است که در
				// گنجور رومیزی استفاده شده که این ایراد را ندارد
				// اما مسلما کندتر است.
				cursor = _db.rawQuery("SELECT p.id, p.cat_id, p.title, p.url, v.text FROM poem p INNER JOIN verse v ON p.id = v.poem_id WHERE v.vorder = 1 AND p.cat_id = "+ CatId +" ORDER BY p.id", null);				
			}
			else{
				cursor = _db.query("poem", new String[]{"id", "cat_id", "title", "url"}, "cat_id = " + CatId, null, null, null, "id");
			}
			
			if(cursor.moveToFirst()) {
				do{
					poems.add(
							new GanjoorPoem(
									cursor.getInt(IDX_POEM_ID),
									cursor.getInt(IDX_POEM_CATID),
									cursor.getString(IDX_POEM_TITLE),
									cursor.getString(IDX_POEM_URL),
									IsPoemFaved(cursor.getInt(IDX_POEM_CATID)),
									IncludeFirstVerse ? cursor.getString(IDX_POEM_FIRSTVERSE) : ""										
									)
							);
					
				}while(cursor.moveToNext());
			}
		}
		return poems;			
	}
	/**
	 * @param PoemId شناسۀ رکورد شعر
	 * @return اطلاعات شعر
	 */
	public GanjoorPoem getPoem(int PoemId){
		if(getIsConnected()){
			Cursor cursor = _db.query("poem", new String[]{"id", "cat_id", "title", "url"}, "id = " + PoemId, null, null, null, "id", "1");
			if(cursor.moveToFirst()) {
				return new GanjoorPoem(
						cursor.getInt(IDX_POEM_ID),
						cursor.getInt(IDX_POEM_CATID),
						cursor.getString(IDX_POEM_TITLE),
						cursor.getString(IDX_POEM_URL),
						IsPoemFaved(cursor.getInt(IDX_POEM_CATID)),
						""										
						);				
			}			
		}
		return null;
	}
	/**
	 * آیا شعر یا یکی از مصاریع آن نشانه گذاری شده (پیاده سازی نشده)
	 * @param PoemId شناسۀ رکورد شعر
	 * @return false!
	 */
	public Boolean IsPoemFaved(int PoemId){
		return false;
	}
	/**
	 * آیا شعر چندبندی است
	 * @param PoemId شناسۀ رکورد شعر
	 * @return چندبندی یا نه
	 */
	public Boolean IsPoemMultiPart(int PoemId){
		Cursor cursor = _db.query("verse", new String[]{"position"}, "poem_id = " +PoemId + " AND (position = 2 OR position = 3)", null, null, null, null, "1");
		return cursor.moveToFirst();		
	}
	
	private static final int IDX_VERSE_POEMID		=	0;
	private static final int IDX_VERSE_ORDER		=	1;
	private static final int IDX_VERSE_POSITION		=	2;
	private static final int IDX_VERSE_TEXT			=	3;
	
	/**
	 * مصرعهای شعر
	 * @param PoemId شناسۀ رکورد شعر
	 * @return اگر اتصال برقرار نباشد خالی
	 */
	public List<GanjoorVerse> getVerses(int PoemId){
		LinkedList<GanjoorVerse> verses = new LinkedList<GanjoorVerse>();
		if(getIsConnected()){
			Cursor cursor = _db.query("verse", new String[]{"poem_id", "vorder", "position", "text"}, "poem_id = " + PoemId, null, null, null, "vorder");
			if(cursor.moveToFirst()) {
				do{
					verses.add(
							new GanjoorVerse(
									cursor.getInt(IDX_VERSE_POEMID),
									cursor.getInt(IDX_VERSE_ORDER),
									cursor.getInt(IDX_VERSE_POSITION),
									cursor.getString(IDX_VERSE_TEXT)
									)
							);
					
				}while(cursor.moveToNext());
			}
		}
		return verses;				
	}
	/**
	 * مصرع بعدی یک مصرع را باز می گرداند
	 * @param Verse مصرع فعلی
	 * @return مصرع بعدی یا null اگر مشکلی پیش آید
	 */
	public GanjoorVerse getNextVerse(GanjoorVerse Verse){
		if(Verse == null)
			return null;
		return getNextVerse(Verse._PoemID, Verse._Order);
	}
	/**
	 * مصرع بعدی یک مصرع را باز می گرداند
	 * @param PoemId شناسۀ رکورد شعر
	 * @param VerseOrder ترتیب مصرع در شعر
	 * @return  مصرع بعدی یا null اگر مشکلی پیش آید
	 */
	public GanjoorVerse getNextVerse(int PoemId, int VerseOrder){
		if(getIsConnected()){
			Cursor cursor = _db.query("verse", new String[]{"poem_id", "vorder", "position", "text"}, "poem_id = " + PoemId + " AND vorder = " + (VerseOrder + 1), null, null, null, "vorder", "1");
			if(cursor.moveToFirst()) {
				return new GanjoorVerse(
						cursor.getInt(IDX_VERSE_POEMID),
						cursor.getInt(IDX_VERSE_ORDER),
						cursor.getInt(IDX_VERSE_POSITION),
						cursor.getString(IDX_VERSE_TEXT)
						);				
			}
			
		}
		return null;		
	}

	/**
	 * شعر بعدی شعر در یک بخش را بر می گرداند
	 * @param PoemId شناسۀ رکورد شعر
	 * @param CatId شناسۀ رکورد بخش
	 * @return شعر بعدی یا null
	 * @todo: add a method to getNextPoem without specifying CatId
	 */
	public GanjoorPoem getNextPoem(int PoemId, int CatId){
		return getRelPoem(PoemId, CatId, true);
	}
	/**
	 * شعر بعدی شعر در یک بخش را بر می گرداند
	 * @param Poem اطلاعات شعر فعلی
	 * @return شعر بعدی یا null
	 */
	public GanjoorPoem getNextPoem(GanjoorPoem Poem){
		if(Poem == null){
			return null;
		}
		return getNextPoem(Poem._ID, Poem._CatID);
	}
	/**
	 * شعر قبلی شعر در یک بخش را بر می گرداند
	 * @param PoemId شناسۀ رکورد شعر
	 * @param CatId شناسۀ رکورد بخش
	 * @return شعر قبلی یا null
	 * @todo: add a method to getPrevPoem without specifying CatId
	 */
	public GanjoorPoem getPrevPoem(int PoemId, int CatId){
		return getRelPoem(PoemId, CatId, false);
	}
	/**
	 * شعر قبلی شعر در یک بخش را بر می گرداند
	 * @param Poem اطلاعات شعر فعلی
	 * @return شعر قبلی یا null
	 */
	public GanjoorPoem getPrevPoem(GanjoorPoem Poem){
		if(Poem == null){
			return null;
		}
		return getPrevPoem(Poem._ID, Poem._CatID);
	}
	/**
	 * شعر بعدی یا قبلی یک شعر در یک بخش را بر می گرداند
	 * @param PoemId شناسۀّ رکورد شعر
	 * @param CatId شناسۀ بخش
	 * @param NextOne بعدی را برگرداند یا قبلی را
	 * @return شعر بعدی یا قبلی یا null
	 */
	private GanjoorPoem getRelPoem(int PoemId, int CatId, Boolean NextOne){
		if(getIsConnected()){
			String cmpOperator = NextOne ? ">" : "<";
			String orderClause = NextOne ? "id" : "id DESC";
			Cursor cursor = _db.query("poem", new String[]{"id", "cat_id", "title", "url"}, "cat_id = " + CatId + " AND id "+cmpOperator+" " + PoemId, null, null, null, orderClause, "1");
			if(cursor.moveToFirst()) {
				return new GanjoorPoem(
						cursor.getInt(IDX_POEM_ID),
						cursor.getInt(IDX_POEM_CATID),
						cursor.getString(IDX_POEM_TITLE),
						cursor.getString(IDX_POEM_URL),
						IsPoemFaved(cursor.getInt(IDX_POEM_CATID)),
						""										
						);				
			}			
		}
		return null;		
		
	}
	
	/**
	 * شاعر بعدی را بر اساس حروف الفبا برمی گرداند
	 * @param Poet  اطلاعات رکورد شاعر فعلی
	 * @return اطلاعات شاعر بعدی یا null
	 */
	public GanjoorPoet getNextPoet(GanjoorPoet Poet){
		return getRelPoet(Poet, true);
	}
	/**
	 * شاعر قبلی را بر اساس حروف الفبا برمی گرداند
	 * @param Poet  اطلاعات رکورد شاعر فعلی
	 * @return اطلاعات شاعر بعدی یا null
	 */
	public GanjoorPoet getPrevPoet(GanjoorPoet Poet){
		return getRelPoet(Poet, false);
	}
	/**
	 * شاعر بعدی یا قبلی یک شاعر را بر می گرداند
	 * @param Poet اطلاعات رکورد شاعر
	 * @param NextOne بعدی یا قبلی
	 * @return اطلاعات شاعر بعدی یا قبلی یا null
	 */
	private GanjoorPoet getRelPoet(GanjoorPoet Poet, Boolean NextOne){
		if(getIsConnected()){
			String cmpOperator = NextOne ? "> '" : "< '";
			String orderClause = NextOne ? "name" : "name DESC";
			Cursor cursor = _db.query("poet", new String[]{"id", "name", "cat_id", "description"}, "name "+ cmpOperator+ Poet._Name + "'", null, null, null, orderClause, "1");
			if(cursor.moveToFirst()) {
				return new GanjoorPoet(
						cursor.getInt(IDX_POET_ID),
						cursor.getString(IDX_POET_NAME),
						cursor.getInt(IDX_POET_CATID),
						cursor.getString(IDX_POET_BIO)
						);
			}			
			
		}
		return null;
	}
	/**
	 * 
	 * @return بخش بعدی در بخشهای شاعر
	 */
	public GanjoorCat getNextCat(GanjoorCat Cat){
		return getRelCat(Cat, true);
	}
	/**
	 * بخش قبلی در بخشهای شاعر
	 */
	public GanjoorCat getPrevCat(GanjoorCat Cat){
		return getRelCat(Cat, false);
	}
	/**
	 * 
	 * @param Cat بخش فعلی
	 * @param NextOne
	 * @return بخش بعدی یا قبلی در بخشهای شاعر را بر می گرداند
	 */
	private GanjoorCat getRelCat(GanjoorCat Cat, Boolean NextOne){
		if(getIsConnected()){
			String cmpOperator = NextOne ? ">" : "<";
			String orderClause = NextOne ? "id" : "id DESC";
			Cursor cursor = _db.query("cat", new String[]{"id", "poet_id", "text", "parent_id", "url"}, "poet_id = " + Cat._PoetID + " AND parent_id = "+ Cat._ParentID+" AND id "+ cmpOperator+" " + Cat._ID, null, null, null, orderClause, "1");
			if(cursor.moveToFirst()) {
				return new GanjoorCat(
						cursor.getInt(IDX_CAT_ID),
						cursor.getInt(IDX_CAT_POETID),
						cursor.getString(IDX_CAT_TEXT),
						cursor.getInt(IDX_CAT_PARENTID),
						cursor.getString(IDX_CAT_URL)
						);
			}
			else{
				GanjoorCat ParentCat = getCat(Cat._ParentID);
				if(ParentCat != null){
					return getRelCat(ParentCat, NextOne);
				}
			}
			
		}
		return null;		
	}
	/**
	 * کپی استریم ورودی به استریم خروجی
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	private static final void copyInputStream(InputStream in, OutputStream out)
	  throws IOException
	  {
	    byte[] buffer = new byte[1024];
	    int len;

	    while((len = in.read(buffer)) >= 0)
	      out.write(buffer, 0, len);

	    in.close();
	    out.close();
	  }	
	/**
	 * فایل gdb را که پسوندش ممکن است zip باشد به دیتابیس اضافه می کند
	 * @param fileName مسیر کامل فایل
	 * @return true if succeeds
	 */
	public Boolean ImportGdb(String fileName){
		if(!fileName.toLowerCase().endsWith(".gdb")){
			File f = new File(fileName);		
			try {
				ZipFile zipFile = new ZipFile(f);
				Enumeration<?> entries = zipFile.entries();
				
				 while(entries.hasMoreElements()) {
				        ZipEntry entry = (ZipEntry)entries.nextElement();			        
				        
				        if(entry.getName().endsWith(".gdb")){			
				        	File gdbFile = new File(new File(new File(fileName).getParent()), entry.getName());
					        copyInputStream(zipFile.getInputStream(entry),
							           new BufferedOutputStream(new FileOutputStream(gdbFile.toString())));
						
					        Boolean result = ImportDbFastUnsafe(gdbFile.toString());					     
					        gdbFile.delete();
					        return result;
				        }

				 }
				
				 zipFile.close();
			} catch (ZipException e) {
		
				return ImportDbFastUnsafe(fileName);
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}		
		return ImportDbFastUnsafe(fileName);		
	}
	
	/**
	 * نمونۀ سادۀ متد ImportDb که چکها و تغییر شناسه ها را در صورت 
	 * تکراری بودن شناسه ها انجام نمی دهد
	 * @param fileName فایل ورودی
	 * @return نتیجۀ کل عملیات
	 */
	public Boolean ImportDbFastUnsafe(String dbPath){
		try{
		if(!getIsConnected()){
			return false;
		}
		GanjoorDbBrowser gdbOpener = new GanjoorDbBrowser(dbPath);
		if(!gdbOpener.getIsConnected()){
			this._LastError = gdbOpener._LastError;
			return false;
		}	
		
		//آپگرید دیتابیسهای قدیمی
		gdbOpener.UpgradeOldDbs();
		
		//یک چک مقدماتی و البته ناکافی
		List<GanjoorPoet> gdbPoets = gdbOpener.getPoets();
		for(GanjoorPoet gdbPoet : gdbPoets){
			if(this.getPoet(gdbPoet._ID) != null){
				this._LastError = "مجموعۀ ورودی شامل شاعرانی است که شناسۀ آنها با شناسۀ شاعران موجود همسان است";
				return false;
			}
		}
		
		Cursor cursor;
		String sql;
		Boolean bResult = true;
		try{
		_db.beginTransaction();
		
		//کپی cat
		cursor = gdbOpener._db.query("cat", new String[]{"id", "poet_id", "text", "parent_id", "url"}, null, null, null, null, null);
		if(cursor.moveToFirst()) {
			do{
				sql = String.format("INSERT INTO cat (id, poet_id, text, parent_id, url) VALUES (%d, %d, \"%s\", %d, \"%s\");",			
								cursor.getInt(IDX_CAT_ID),
								cursor.getInt(IDX_CAT_POETID),
								cursor.getString(IDX_CAT_TEXT),
								cursor.getInt(IDX_CAT_PARENTID),
								cursor.getString(IDX_CAT_URL)
								);
				_db.execSQL(sql);				
			}while(cursor.moveToNext());
		}
		
		//کپی poet
		cursor = gdbOpener._db.query("poet", new String[]{"id", "name", "cat_id", "description"}, null, null, null, null, null);
		if(cursor.moveToFirst()) {
			do{
				sql = String.format("INSERT INTO poet (id, name, cat_id, description) VALUES (%d, \"%s\", %d, \"%s\");",			
						cursor.getInt(IDX_POET_ID),
						cursor.getString(IDX_POET_NAME),
						cursor.getInt(IDX_POET_CATID),
						cursor.getString(IDX_POET_BIO)
						);
				_db.execSQL(sql);
			}while(cursor.moveToNext());
		}
			
		//کپی poem
		cursor = gdbOpener._db.query("poem", new String[]{"id", "cat_id", "title", "url"}, null, null, null, null, null);
		if(cursor.moveToFirst()) {
			do{
				sql = String.format("INSERT INTO poem (id, cat_id, title, url) VALUES (%d, %d, \"%s\", \"%s\");",			
						cursor.getInt(IDX_POEM_ID),
						cursor.getInt(IDX_POEM_CATID),
						cursor.getString(IDX_POEM_TITLE),
						cursor.getString(IDX_POEM_URL)
						);
				_db.execSQL(sql);
			}while(cursor.moveToNext());
		}
		
		//کپی verse
		cursor = gdbOpener._db.query("verse", new String[]{"poem_id", "vorder", "position", "text"}, null, null, null, null, null);
		if(cursor.moveToFirst()) {
			do{
				sql = String.format("INSERT INTO verse (poem_id, vorder, position, text) VALUES (%d, %d, %d, \"%s\");",			
						cursor.getInt(IDX_VERSE_POEMID),
						cursor.getInt(IDX_VERSE_ORDER),
						cursor.getInt(IDX_VERSE_POSITION),
						cursor.getString(IDX_VERSE_TEXT).replace("\"", "\"\"")
						);
				_db.execSQL(sql);
			}while(cursor.moveToNext());
		}	
		
		_db.setTransactionSuccessful();
		}catch(Exception expData){
			bResult = false;
		}
		finally{
			_db.endTransaction();			
		}
		
		
		gdbOpener.CloseDatabase();
		
		return bResult;
		}
		catch(Exception exp){
			exp.printStackTrace();
			return false;
		}
	}
	
	//ورژن گذاری و پشتیبانی از دیتابیسهای قدیمی
	private static final int DatabaseVersion = 2;
	
	//تبدیل دیتابیسهای قدیمی تر
	//تمام مراحل آپگرید دیتابیسهای قدیمی از ورژن دات نت
	// به اینجا منتقل نشده، فقط مواردی که ایجاد مشکل بحرانی می کنند اضاففه شده اند.
	private void UpgradeOldDbs(){
		//اگر جدول poet سه تا فیلد داشته باشد یعنی فیلد 
		//description را ندارد و باید آن را اضافه کنیم
		Cursor cursor = _db.rawQuery("PRAGMA table_info('poet')", null);
		int n = 0;
		if(cursor.moveToFirst()){
			n++;
			while(cursor.moveToNext()){
				n++;
			}
		}
		if(n == 3){
			try{
			_db.execSQL("ALTER TABLE poet ADD description TEXT");
			_db.execSQL("DELETE FROM gver");
			_db.execSQL("INSERT INTO gver (curver) VALUES ("+String.valueOf(DatabaseVersion)+")");
			}finally{
				
			}
		}
		
	}
	
	/**
	 * یک بخش را به همراه تمام زیر بخشها و شعرهای متعلقه حذف می کند
	 * @param Cat بخشی که باید حذف شود
	 */
	public void DeleteCat(GanjoorCat Cat){
		if(Cat == null)
			return;
		List<GanjoorCat> subCats = getSubCats(Cat._ID);
		for(GanjoorCat subCat : subCats){
			DeleteCat(subCat);
		}		
		String sql = String.format("DELETE FROM verse WHERE poem_id IN (SELECT id FROM poem WHERE cat_id=%d);", Cat._ID);
		_db.execSQL(sql);
		sql = String.format("DELETE FROM poem WHERE cat_id=%d;", Cat._ID);
		_db.execSQL(sql);
		sql = String.format("DELETE FROM cat WHERE id=%d;", Cat._ID);
		_db.execSQL(sql);		
	}
	
	/**
	 * آثار شاعر را از دیتابیس حذف می کند
	 * @param Poet شاعری که باید آثارش حذف شود
	 */
	public void DeletePoet(GanjoorPoet Poet){
		DeleteCat(getCat(Poet._CatID));
		String sql = String.format("DELETE FROM poet WHERE id=%d;", Poet._ID);
		_db.execSQL(sql);		
	}
	
	
	
	

}
