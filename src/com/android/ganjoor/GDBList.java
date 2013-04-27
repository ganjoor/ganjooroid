/**
 * 
 */
package com.android.ganjoor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * @author Hamid Reza
 * پردازش لیستهای دریافت مجموعه ها را با این کلاس انجام می دهم
 */
public class GDBList {
	
	/**
	 * سازنده را مخفی نگه می داریم تا ساخته شدن نمونه کاملاً در کنترل خودمان باشد
	 * و بتوانیم به ازای فایلهای مشکلدار ورودی به راحتی
	 * null
	 * برگردانیم
	 */
	private GDBList(){
		_Items = new LinkedList<GDBInfo>();
	}
	
	/**
	 * سازندۀ کپی کنندۀ کلاس
	 * @param source منبع
	 */
	public GDBList(GDBList source){
		this._ListUrl = source._ListUrl;
		_Items = new LinkedList<GDBInfo>();
		for (GDBInfo Item : source._Items) {
			_Items.add(new GDBInfo(Item));
		}
	}
	
	/**
	 * متد پردازش و ساخت نمونه
	 * @param nListId به لیستهای ورودی یک شناسۀ یکتا نسبت می دهیم تا بعدا بتوانیم در صورت مخلوط کردن آنها آنها را ردگیری و فیلتر کنیم
	 * @param filePath مسیر فایل ورودی
	 * @param db یک نمونه از دیتابیس جهت حذف شاعران موجود
	 * @return نمونه ایجاد شده
	 * @throws IOException 
	 * @throws XmlPullParserException 
	 */
	public static GDBList Build(int nListId, String path, GanjoorDbBrowser db) throws IOException, XmlPullParserException{
		
		GDBList result = new GDBList();
		
		FileInputStream f = new FileInputStream(path);
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(f, null);
        int eventType = xpp.getEventType();
        while(eventType != XmlPullParser.END_DOCUMENT){
        	if(eventType == XmlPullParser.START_TAG){
        		String tagName = xpp.getName();
        		if(tagName.equals("gdb")){
        			GDBInfo gdbInfo = GDBInfo.BuildFromXmlPullParser(nListId, xpp, eventType);
        			if(gdbInfo != null){
        				if(db.getPoet(gdbInfo._PoetID) == null){        					
        					result._Items.add(gdbInfo);
        				}
        			}
        		}
        	}
        	eventType = xpp.next();
        }
		
        f.close();
        if(result._Items.size() == 0)
        	return null;
        return result;
	}
	
	/**
	 * 
	 * @param lists لیستهای ورودی
	 * @return مخلوط شده لیستهای ورودی با یکی کردن موارد تکراری
	 */
	public static GDBList Mix(List<GDBList> lists){
		if(lists == null)
			return null;
		if(lists.size() > 0){
			GDBList result = new GDBList(lists.get(0));
			for(int i=1; i<lists.size(); i++){
				GDBList list = lists.get(i);
				for(GDBInfo Item : list._Items){
					int idx = result.FindSimilarIndex(Item);
					if(idx == -1){
						result._Items.add(Item);
					}
					else{
						if(result._Items.get(idx)._PubDate.compareTo(Item._PubDate) <0){
							GDBInfo repItem = new GDBInfo(Item);
							result._Items.set(idx, repItem);
						}
					}
				}
				
			}
			return result;
		}
		return null;
	}
	
	/**
	 * جستجوی موارد مشابه
	 * @param inputItem
	 * @return اندیس جدیدترین مورد مشابه
	 */
	public int FindSimilarIndex(GDBInfo inputItem){
		int CatId = inputItem._CatID;
		int idx = -1;
		for (int i=0; i<this._Items.size(); i++) {			
			if(this._Items.get(i)._CatID == CatId){
				if(idx == -1){
					idx = i;
				}
				else{
					if(this._Items.get(idx)._PubDate.compareTo(this._Items.get(i)._PubDate) < 0){
						idx = i;
					}
				}
			}
		}
		return idx;
	}
	
	/**
	 * نشانی لیست
	 */
	public String _ListUrl;
	
	/**
	 * لیست آیتمها
	 */
	public List<GDBInfo> _Items;
	
	
}
