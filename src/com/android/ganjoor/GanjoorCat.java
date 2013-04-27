/**
 * 
 */
package com.android.ganjoor;

/**
 * @author Hamid Reza
 * @database-table cat
 * @summary اطلاعات بخشها
 */
public class GanjoorCat {
	/**
    * سازندۀ پیش فرض : یک نمونه از کلاس را با مقادیر ورودی می سازد
    */
    public GanjoorCat(int ID, int PoetID, String Text, int ParentID, String Url, int StartPoem)
    {
        _ID = ID;
        _PoetID = PoetID;
        _Text = Text;
        _ParentID = ParentID;
        _Url = Url;
        _StartPoem = StartPoem;
    }	
    /**
    * سازندۀ پیش فرض : یک نمونه از کلاس را با مقادیر ورودی می سازد
    */
    public GanjoorCat(int ID, int PoetID, String Text, int ParentID, String Url)
    {
    	this(ID, PoetID, Text, ParentID, Url, 0);
    }
    /**
    * سازندۀ پیش فرض : یک نمونه از کلاس را با مقادیر ورودی می سازد
    */
    public GanjoorCat(GanjoorCat baseCat, int StartPoem)
    {
    	this(baseCat._ID, baseCat._PoetID, baseCat._Text, baseCat._ParentID, baseCat._Url, StartPoem);
    }
    /**
    * @summary شناسۀ رکورد بخش
     * @database-field id
    */
    public int _ID;
    /**
    * @summary شناسۀ رکورد شاعر
    * @database-field poet_id
    */
    public int _PoetID;
    /**
    * @summary عنوان بخش
    * @database-field text
    */
    public String _Text;
    /**
    * @summary شناسۀ رکورد بخش والد
    * id از رکورد دیگری از جدول cat
    * اگر صفر باشد یعنی مربوط به بخشهای ریشه (شاعران) است
    * @database-field parent_id
    */
    public int _ParentID;
    /**
    * @summary نشانی بخش در سایت گنجور ganjoor.net
    * @database-field url
    */
    public String _Url;
     /**
    * در صورتی که تعداد شعرهای بخش بیشتر از «حداکثر تعداد عنوانها در فهرست اشعار یک بخش» باشد مقدار غیر صفر برای این
    * فیلد نشان می دهد فهرست اشعار باید نه از ابتدا که از این عدد به بعد تا همان حداکثر نمایش داده شود.
    */
    public int _StartPoem;
 
}
