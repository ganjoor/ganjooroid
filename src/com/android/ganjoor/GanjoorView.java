/**
 * 
 */
package com.android.ganjoor;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * @author Hamid Reza
 * کلاس نمایش محتوای دیتابیس گنجور
 */
public class GanjoorView {
	/**
	 * سازندۀ کلاس
	 * @param ParentView
	 */
	public GanjoorView(Context context, GanjoorDbBrowser DbBrowser, LinearLayout ParentView, Typeface iranSans, float fTextFontSize){
		_Context = context;
		_DbBrowser = DbBrowser;
		_ParentView = ParentView;
		_Typeface = iranSans;
		_CurCatId = 0;
		_CurPoemId = 0;
		_TextFontSize = fTextFontSize;
		_history = new Stack<GanjoorBrowsingHistory>();
	}

	private Context _Context;
	/**
	 * این کنترل در خارج از کلاس ایجاد می شود و در سازندۀ کلاس مقداردهی می شود
	 */
	private LinearLayout _ParentView;
	/**
	 * اطلاعات دیتابیس از طریق این متغیر استخراج می شود
	 */
	private GanjoorDbBrowser _DbBrowser;
	
	/**
	 * فونت نمایش متون
	 */
	private Typeface _Typeface;
	/**
	 * تاریخچۀ حرکت در بین آیتمها جهت عملیاتی کردن دکمۀ برگشت
	 */
	private Stack<GanjoorBrowsingHistory> _history;
	
	/**
	 * پردازش حرکات لمسی چپ به راست و راست به چپ
	 */
	
	public static final int COLOR_TITLE = Color.MAGENTA;
	public static final int COLOR_BREAK = Color.GREEN;
	public static final int COLOR_LINKCAT = Color.BLUE;
	private static final int COLOR_LINKPOEM = Color.BLUE;
	private static final int COLOR_FOREHI = 0xFFFFFF00;
	private static final int COLOR_BACKHI = 0xFFFFFF00;
	private static final int DEF_PADDING = 5;
	
	/**
	 * نمایش فهرست شاعران
	 */
	public void ListPoets(Boolean AddToHistory){
		if(AddToHistory){
			UpdateHistory();
		}
		if(_DbBrowser.getIsConnected()){
			CleanScreen();
			List<GanjoorPoet> poets = _DbBrowser.getPoets();
			if(poets.size() == 0){
				AddView(CreateTextView("برای دریافت مجموعه های شعر از منو عنوان 'دریافت مجموعه ها' را انتخاب کنید.", false));
			}
			else
			for (GanjoorPoet Poet : poets) {
				TextView tvPoet = CreateTextView(Poet._Name, true);
				tvPoet.setTag(Poet);
				tvPoet.setOnClickListener(new OnClickListener() {					
					@Override
					public void onClick(View v) {
						Object tag = v.getTag();
						if(tag != null){
							ShowPoet((GanjoorPoet)tag, true);
						}
						}						
					});
				AddView(tvPoet);
			}
			_CurCatId = 0;
			_CurPoemId = 0;
			if(_tracker != null){
				_tracker.onCurrentItemChanged();
			}			
			
		}
		else{
			ShowConfigureDbPath();
			_CurCatId = 0;
			_CurPoemId = 0;
			if(_tracker != null){
				_tracker.onCurrentItemChanged();
			}			
		}
	}	
	
	/**
	 * نمایش صفحۀ اطلاعات شاعر
	 * @param Poet شاعر
	 */
	public void ShowPoet(GanjoorPoet Poet, Boolean AddToHistory){
		if(AddToHistory){
			UpdateHistory();
		}		
		CleanScreen();
		if(Poet == null)
			return;
		TextView tvTitle = CreateTextView(Poet._Name, false);
		tvTitle.setTextColor(COLOR_TITLE);
		AddView(tvTitle);
		TextView tvBio = CreateTextView(Poet._Bio, false);		
		tvBio.setGravity(android.view.Gravity.RIGHT);
		AddView(tvBio);
		
		ShowCat(_DbBrowser.getCat(Poet._CatID), false, false);
		_CurCatId = Poet._CatID;
		_CurPoemId = 0;
		if(_tracker != null){
			_tracker.onCurrentItemChanged();
		}
	}

	private static final int MAX_FIRST_VERSE_CHARS = 50;
	
	/**
	 * نمایش محتوای یک بخش
	 * @param Cat بخش
	 */	
	public void ShowCat(GanjoorCat Cat, Boolean AddToHistory){
		ShowCat(Cat, true, AddToHistory);
	}
	/**
	 * نمایش محتوای یک بخش
	 * @param Cat بخش
	 * @param ShowTitle نمایش عنوان و حذف کنترلها - این متد در متد نمایش اطلاعات شاعر با مقدار false برای این پارامتر صدا زده می شود
	 */
	public void ShowCat(GanjoorCat Cat, Boolean ShowTitle, Boolean AddToHistory){
		
		if(AddToHistory){
			UpdateHistory();
		}
		
		if(ShowTitle){
			CleanScreen();
			if(Cat == null)
				return;
			TextView tvTitle = CreateTextView(Cat._Text, false);
			tvTitle.setTextColor(COLOR_TITLE);
			AddView(tvTitle);
		}
		if(Cat == null)
			return;
		
		
		for (GanjoorCat SubCat : _DbBrowser.getSubCats(Cat._ID)) {
			TextView tvCat = CreateTextView(SubCat._Text, true);
			tvCat.setTag(SubCat);
			tvCat.setTextColor(COLOR_LINKCAT);
			tvCat.setOnClickListener(new OnClickListener() {					
				@Override
				public void onClick(View v) {
					Object tag = v.getTag();
					if(tag != null){
						ShowCat((GanjoorCat)tag, true);
					}
					}						
				});
			AddView(tvCat);			
		}
		for (GanjoorPoem Poem : _DbBrowser.getPoems(Cat._ID, true)) {
			// برای نمایش مصرع یا خط اول شعر در عنوان باید این مسئله را در نظر داشته باشیم
			// که عناوین طولانی ممکن است اشکال نمایشی ایجاد کنند
			// تکه کد زیر این مسئله را بررسی و در صورت نیاز عنوان را کوتاه می کند.
			String poemTitle = Poem._Title;
			int highlightStart = -1;
			int highlightLength = 0;			
			if(Poem._FirstVerse != ""){
				poemTitle += " : ";
				highlightStart = poemTitle.length();
				String firstVerse;
				if(Poem._FirstVerse.length() > MAX_FIRST_VERSE_CHARS){
					firstVerse = Poem._FirstVerse.substring(0, MAX_FIRST_VERSE_CHARS) + " ...";
				}
				else{
					firstVerse = Poem._FirstVerse;
				}
				poemTitle += firstVerse; 
				highlightLength = firstVerse.length();
			}
			
			TextView tvPoem = CreateTextView(poemTitle, true, highlightStart, highlightLength, false);
			tvPoem.setTag(Poem);
			tvPoem.setTextColor(COLOR_LINKPOEM);
			tvPoem.setOnClickListener(new OnClickListener() {					
				@Override
				public void onClick(View v) {
					Object tag = v.getTag();
					if(tag != null){
						ShowPoem((GanjoorPoem)tag, true);
					}
					}						
				});
			AddView(tvPoem);					
		}
		_CurCatId = Cat._ID;
		_CurPoemId = 0;	
		
		if(ShowTitle){
			if(_tracker != null){
				_tracker.onCurrentItemChanged();
			}			
		}
	}
	
	/**
	 * نمایش یک شعر
	 * @param Poem شعر
	 */
	public void ShowPoem(GanjoorPoem Poem, Boolean AddToHistory){
		if(AddToHistory){
			UpdateHistory();
		}
		
		CleanScreen();
		if(Poem == null)
			return;
		TextView tvTitle = CreateTextView(Poem._Title, false);
		tvTitle.setTextColor(COLOR_TITLE);
		AddView(tvTitle);
		AddEmptyLine();

		for (GanjoorVerse Verse : _DbBrowser.getVerses(Poem._ID)) {
			TextView tvVerse = CreateTextView(Verse._Text, false);			
			AddView(tvVerse);
			switch(Verse._Position){
			case GanjoorVerse.POSITION_LEFT:
				AddEmptyLine();				
				break;
			case GanjoorVerse.POSITION_CENTEREDVERSE2:
				tvVerse.setTextColor(COLOR_BREAK);				
				AddEmptyLine();
				break;
			case GanjoorVerse.POSITION_SINGLE:
			case GanjoorVerse.POSITION_PARAGRAPH:
				tvVerse.setGravity(android.view.Gravity.RIGHT);
				break;
			case GanjoorVerse.POSITION_CENTEREDVERSE1:
				tvVerse.setTextColor(COLOR_BREAK);
				GanjoorVerse nextVerse = _DbBrowser.getNextVerse(Verse);
				if(nextVerse == null){
					AddEmptyLine();
				}
				else
					if(nextVerse._Position != GanjoorVerse.POSITION_CENTEREDVERSE2)
						AddEmptyLine();				
				break;
			}
		}		
		_CurCatId = Poem._CatID;
		_CurPoemId = Poem._ID;
		if(_tracker != null){
			_tracker.onCurrentItemChanged();
		}		
	}

	/**
	 * حذف کنترلهای مربوط به نمایش شعر یا بخش
	 */
	private void CleanScreen(){
		undoHighlight();
		_ParentView.removeAllViews();

	}
	
	private void AddEmptyLine(){
		AddView(CreateTextView(" ", false));
	}
	
	/**
	 * نمایش پیغام در نمای اصلی
	 * @param Msg پیغام
	 */
	private void NotifyMessage(String Msg){
		CleanScreen();
		AddView(CreateTextView(Msg, false));		
	}
	/**
	 * اضافه کردن یک جعبۀ متنی به نمای اصلی
	 * @param text متن جعبه
	 * @param clickable جعبه قابل کلیک باشد یا خیر
	 * @return جعبۀ متنی ایجاد شده
	 */
	private TextView CreateTextView(String text, boolean clickable){
		return CreateTextView(text, clickable, -1, -1, false);
	}
	/**
	 * اضافه کردن یک جعبۀ متنی به نمای اصلی
	 * @param text متن جعبه
	 * @param clickable جعبه قابل کلیک باشد یا خیر
	 * @param selectionStart آغاز قسمتی از متن که باید رنگ متفاوتی داشته باشد
	 * @param selectionLength طول قسمتی از متن که باید رنگ متفاوتی داشته باشد
	 * @param highlightBackground زمینه متن متفاوت رنگ شود یا رنگ رویی
	 * @return جعبۀ متنی ایجاد شده
	 */
	private TextView CreateTextView(String text, boolean clickable, int selectionStart, int selectionLength, Boolean highlightBackground){
		TextView tv = new TextView(_Context);		
		tv.setGravity(android.view.Gravity.CENTER);
		tv.setClickable(clickable);
		setTextViewText(tv, text, selectionStart, selectionLength, highlightBackground);		
		tv.setTypeface(_Typeface);
		tv.setTextSize(_TextFontSize);
		tv.setPadding(DEF_PADDING, DEF_PADDING, DEF_PADDING, DEF_PADDING);
		tv.setHorizontallyScrolling(false);
		LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, (float)1);
		tv.setLayoutParams(lp);
		return tv;
	}

	/**
	 * تعریف این فرایند در یک متد جداگانه در جستجو و برجسته سازی به دردمان می خورد 
	 */
	private void setTextViewText(TextView tv, String text, int selectionStart,
			int selectionLength, Boolean highlightBackground) {
		if(selectionStart >= 0 && selectionLength > 0){			
			tv.setText(text, TextView.BufferType.SPANNABLE);
			Spannable WordToSpan = (Spannable)tv.getText();
			WordToSpan.setSpan(highlightBackground ? new BackgroundColorSpan(COLOR_BACKHI) : new ForegroundColorSpan(COLOR_FOREHI), selectionStart, selectionStart + selectionLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);			
			tv.setText(WordToSpan);
		}
		else{
			tv.setText(text);
		}
	}

	/**
	 * ایجاد محلی برای یک ردیف کنترل
	 * @return
	 */
	private LinearLayout CreateControlLine(){
		LinearLayout ll = new LinearLayout(_Context);
		ll.setGravity(android.view.Gravity.CENTER);
		ll.setPadding(DEF_PADDING, DEF_PADDING, DEF_PADDING, DEF_PADDING);
		LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, (float)1);
		ll.setLayoutParams(lp);	
		return ll;
		
	}
	/**
	 * اضافه کردن کنترل (جعبه متنی) به نمای اصلی
	 * @param child کنترلی که قرار است اضافه شود
	 */
	private void AddView(View child){	
		_ParentView.addView(child);	
	}

	/**
	 * بخش فعلی
	 */
	public int _CurCatId;
	/**
	 * شعر فعلی
	 */
	public int _CurPoemId;
	
	/**	 
	 * @return امکان برگشت به عنوان قبلی وجود دارد یا خیر
	 */
	public Boolean getCanGoBackInHistory(){
		return !_history.empty();
	}
	/**
	 * اضافه کردن صفحۀ فعلی به تاریخچه
	 */
	private void UpdateHistory(){
			_history.push(new GanjoorBrowsingHistory(_CurCatId, _CurPoemId, 0, null));
	}
	
	/**
	 * برگشت به عقب در تاریخچه
	 * @return false if history is empty
	 */
	public Boolean GoBackInHistory(){
		if(getCanGoBackInHistory()){
			GanjoorBrowsingHistory back = _history.pop();
			GoToItem(back._PoemID, back._CatID, false);
			return true;
		}
		return false;
	}
	/**
	 * 
	 * @return آیا در صفحۀ فهرست شاعران هستیم یا خیر
	 */
	public Boolean getIsOnHome(){
		return _CurCatId == 0;
	}
	
	/**
	 * نمایش شعر بعدی
	 * @return در صورتی که عملیات معنی داشته باشد true
	 */
	public Boolean GoNext(){
		GanjoorPoem nextPoem = getNextPoem();
		if(nextPoem != null){
			ShowPoem(nextPoem, true);
			return true;
		}
		GanjoorCat nextCat = getNextCatForNoneRoots();
		if(nextCat != null){
			ShowCat(nextCat, true);
			return true;
		}
		
		GanjoorPoet nextPoet = getNextPoet();
		if(nextPoet != null){
			ShowPoet(nextPoet, true);
			return true;
		}
		return false;
	}
	/**
	 * 
	 * @return آیا بعدی معنی دارد؟
	 */
	public Boolean getCanNext(){
		if(getNextPoem() != null){
			return true;
		}
		if(getNextCatForNoneRoots() != null){
			return true;
		}
		if(getNextPoet() != null){
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @return شعر بعدی شعر جاری در بخش یا null بر می گرداند
	 */
	private GanjoorPoem getNextPoem(){
		if(_CurPoemId != 0){
			return _DbBrowser.getNextPoem(_CurPoemId, _CurCatId);
			}
		return null;
	}
	/**
	 * 
	 * @return شاعر بعدی را برای بخشهای روی ریشه بر می گرداند
	 */
	private GanjoorPoet getNextPoet(){
		GanjoorCat cat = _DbBrowser.getCat(_CurCatId);
		if(cat != null){
			GanjoorPoet poet = _DbBrowser.getPoet(cat._PoetID);
			if(poet != null){
				return _DbBrowser.getNextPoet(poet);
				}
		}
		return null;
	}
	/**
	 * 
	 * @return بخش بعدی برای بخشهایی که در ریشه قرار تدارند
	 */
	private GanjoorCat getNextCatForNoneRoots(){
		if(_CurCatId != 0){
			GanjoorCat cat = _DbBrowser.getCat(_CurCatId);
			if(cat != null){
					if(cat._ParentID != 0){						
						return _DbBrowser.getNextCat(cat);
					}
				}
			}
		return null;
	}
	
	
	/**
	 * نمایش شعر قبلی
	 * @return در صورتی که عملیات معنی داشته باشد true
	 */
	public Boolean GoPrev(){
		GanjoorPoem prevPoem = getPrevPoem();
		if(prevPoem != null){
			ShowPoem(prevPoem, true);
			return true;
		}		
		GanjoorCat prevCat = getPrevCatForNoneRoots();
		if(prevCat != null){
			ShowCat(prevCat, true);
			return true;
		}
		
		GanjoorPoet prevPoet = getPrevPoet();
		if(prevPoet != null){
			ShowPoet(prevPoet, true);
			return true;
		}		
		return false;
	}
	
	/**
	 * 
	 * @return آیا قبلی معنی دارد؟
	 */
	public Boolean getCanPrev(){
		if(getPrevPoem() != null){
			return true;
		}
		if(getPrevCatForNoneRoots() != null){
			return true;
		}
		if(getPrevPoet() != null){
			return true;
		}
		return false;
	}
	
	
	/**
	 * 
	 * @return شعر قبلی شعر جاری در بخش یا null بر می گرداند
	 */	
	private GanjoorPoem getPrevPoem(){
		if(_CurPoemId != 0){
			return _DbBrowser.getPrevPoem(_CurPoemId, _CurCatId);
			}
		return null;		
	}
	
	/**
	 * 
	 * @return شاعر قبلی را برای بخشهای روی ریشه بر می گرداند
	 */
	private GanjoorPoet getPrevPoet(){
		if(_CurPoemId == 0){
			if(_CurCatId != 0){
				GanjoorCat cat = _DbBrowser.getCat(_CurCatId);
				if(cat != null){
					GanjoorPoet poet = _DbBrowser.getPoet(cat._PoetID);
					if(poet != null){
						return _DbBrowser.getPrevPoet(poet);
					}
				}
			}
		}
		return null;
	}
	/**
	 * 
	 * @return بخش قبلی برای بخشهایی که در ریشه قرار تدارند
	 */
	private GanjoorCat getPrevCatForNoneRoots(){
		if(_CurPoemId == 0){
			if(_CurCatId != 0){
				GanjoorCat cat = _DbBrowser.getCat(_CurCatId);
				if(cat != null){
						if(cat._ParentID != 0){						
							return _DbBrowser.getPrevCat(cat);
						}
					}
				}
			}		
		return null;
	}
	
	/**
	 * تغییر آیتم جاری
	 * @param PoemId شناسۀ رکورد شعر
	 * @param CatId شناسۀ رکورد بخش
	 * @param AddToHistory به تاریخچه اضافه شود یا خیر
	 */
	public void GoToItem(int PoemId, int CatId, Boolean AddToHistory){
        if(CatId == 0){
        	ListPoets(AddToHistory);
        }
        else
        	if(PoemId != 0){
        		GanjoorPoem poem = _DbBrowser.getPoem(PoemId);
        		if(poem != null){
        		ShowPoem(poem, AddToHistory);
        		}
        		else{
        			ListPoets(AddToHistory);
        		}
        			
        	}
        	else{
        		GanjoorCat Cat = _DbBrowser.getCat(CatId);
        		if(Cat == null){
        			ListPoets(AddToHistory);
        		}
        		else{
        			if(Cat._ParentID == 0){
        				ShowPoet(_DbBrowser.getPoet(Cat._PoetID), AddToHistory);
        			}
        			else{
        				ShowCat(Cat, AddToHistory);
        			}        				
        		}
        	}  	
		
	}
	
	/**
	 * رابط رویداد تغییر صفحۀ جاری
	 * @author Hamid Reza
	 *
	 */
	public interface NavigationTracker{
		void onCurrentItemChanged();
		}
	/**
	 * رویداد تغییر صفحۀ جاری
	 */
	private NavigationTracker _tracker = null;
	/**
	 * مقداردهی رویداد تغییر صفحۀ جاری
	 * @param tracker
	 */
	public void setOnCurrentItemChanged(NavigationTracker tracker){
		_tracker = tracker;
	}
	
	
	/**
	 * 
	 * @return سلسله مراتب بخشهای آیتم جاری
	 */
	public List<GanjoorCat> getCurItemParentsHierarchy(){
		Stack<GanjoorCat> rev = new Stack<GanjoorCat>();
		int CatId = _CurCatId;
		while(CatId != 0){
			GanjoorCat Cat = _DbBrowser.getCat(CatId);
			if(Cat == null){
				break;
			}
			else{
				rev.push(Cat);
				CatId = Cat._ParentID;
			}
		}
		LinkedList<GanjoorCat> hierarchy = new LinkedList<GanjoorCat>();
		while(!rev.empty()){
			hierarchy.add(rev.pop());
		}		
		return hierarchy;
	}
	/**
	 * 
	 * @author Hamid Reza
	 * رابط رویداد تقاضای جستجو برای مسیر دیتابیس
	 */
	public interface RequestSearchForDbCommand{
		void onRequestSearchForDbCommand();
		}
	/**
	 * رویداد تقاضای جستجو برای مسیر دیتابیس
	 */
	private RequestSearchForDbCommand _searchcmd = null;
	/**
	 * مقداردهی رویداد تقاضای جستجو برای مسیر دیتابیس
	 * @param tracker
	 */
	public void setOnRequestSearchForDbCommand(RequestSearchForDbCommand searchcmd){
		_searchcmd = searchcmd;
	}
	/**
	 * تنظیم مسیر پیشنهادی دیتابیس از بیرون
	 * @param suggestedPath
	 */
	public void setSuggestedDbPath(String suggestedPath){
		_txtCurSelectedPath.setText(suggestedPath);
	}
	
	/**
	 * کادر متنی تنظیم شده برای مسیر دیتابیس
	 */
	private EditText _txtCurSelectedPath;
	/**
	 * نمایش رابط کاربری پیکربندی دیتابیس در زمان عدم وجود فایل
	 */
	private void ShowConfigureDbPath(){
		Boolean bSetTypefaceAndSize = true;
		if(_DbBrowser.getDatabaseFileExists()){
			NotifyMessage("فایل پایگاه داده ها وجود دارد اما امکان اتصال به آن وجود ندارد.");
		}
		else{
			NotifyMessage("فایل پایگاه داده ها در مسیر تعیین شده وجود ندارد.");	
		}
		
		
		//ردیف کنترلهای تغییر مسیر
		LinearLayout llCurrentPath = CreateControlLine();
		AddView(llCurrentPath);
		
		
		LayoutParams lp;
		
		
		
		//مسیر جاری
		_txtCurSelectedPath = new EditText(_Context);
		_txtCurSelectedPath.setGravity(android.view.Gravity.LEFT);
		_txtCurSelectedPath.setClickable(false);
		_txtCurSelectedPath.setText(AppSettings.getDatabasePath());
		if(bSetTypefaceAndSize){
			_txtCurSelectedPath.setTypeface(_Typeface);
			_txtCurSelectedPath.setTextSize(_TextFontSize);
		}
		_txtCurSelectedPath.setPadding(DEF_PADDING, DEF_PADDING, DEF_PADDING, DEF_PADDING);
		lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, (float)1);
		_txtCurSelectedPath.setLayoutParams(lp);
		llCurrentPath.addView(_txtCurSelectedPath);
	
		//برچسب ganjoor.s3db
		TextView tvDbName = new TextView(_Context);		
		tvDbName.setGravity(android.view.Gravity.LEFT);
		tvDbName.setClickable(false);
		tvDbName.setText("/ganjoor.s3db");
		if(bSetTypefaceAndSize){		
			tvDbName.setTypeface(_Typeface);
			tvDbName.setTextSize(_TextFontSize);
		}
		tvDbName.setPadding(DEF_PADDING, DEF_PADDING, DEF_PADDING, DEF_PADDING);
		tvDbName.setTextColor(Color.GRAY);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, (float)0);
		tvDbName.setLayoutParams(lp);
		llCurrentPath.addView(tvDbName);
		
		//برچسب مسیر فعلی
		TextView tvCurPath = new TextView(_Context);		
		tvCurPath.setGravity(android.view.Gravity.RIGHT);
		tvCurPath.setClickable(false);
		tvCurPath.setText("مسیر فعلی:");
		if(bSetTypefaceAndSize){
			tvCurPath.setTypeface(_Typeface);
			tvCurPath.setTextSize(_TextFontSize);
		}
		tvCurPath.setPadding(DEF_PADDING, DEF_PADDING, DEF_PADDING, DEF_PADDING);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, (float)0);
		tvCurPath.setLayoutParams(lp);
		llCurrentPath.addView(tvCurPath);
		
		//دکمۀ جستجوی خودکار
		Button btnFindDbPath = new Button(_Context);
		btnFindDbPath.setGravity(android.view.Gravity.CENTER);
		if(bSetTypefaceAndSize){		
			btnFindDbPath.setTypeface(_Typeface);
			btnFindDbPath.setTextSize(_TextFontSize);
		}
		btnFindDbPath.setPadding(DEF_PADDING, DEF_PADDING, DEF_PADDING, DEF_PADDING);
		lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, (float)1);
		btnFindDbPath.setLayoutParams(lp);	
		btnFindDbPath.setText("جستجوی خودکار");
		
		btnFindDbPath.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(_searchcmd != null){
					_searchcmd.onRequestSearchForDbCommand();
				}
				
			}
		});
		if(_searchcmd != null){
			AddView(btnFindDbPath);
		}	
		
		//دکمۀ ذخیره و تلاش مجدد
		Button btnSaveDbPath = new Button(_Context);
		btnSaveDbPath.setGravity(android.view.Gravity.CENTER);
		if(bSetTypefaceAndSize){		
			btnSaveDbPath.setTypeface(_Typeface);
			btnSaveDbPath.setTextSize(_TextFontSize);
		}
		btnSaveDbPath.setPadding(DEF_PADDING, DEF_PADDING, DEF_PADDING, DEF_PADDING);
		lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, (float)1);
		btnSaveDbPath.setLayoutParams(lp);	
		btnSaveDbPath.setText("ذخیره و تلاش مجدد");
		btnSaveDbPath.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(_txtCurSelectedPath != null){
					String dbPath = _txtCurSelectedPath.getText().toString();
					String fullDbPath = new File(dbPath, "ganjoor.s3db").getPath();
					AppSettings.setDatabasePath(dbPath);
					_DbBrowser.OpenDatbase(fullDbPath);
					ListPoets(true);
				}
				
			}
		});
		AddView(btnSaveDbPath);
		
		
		//دکمۀ ذخیره و ایجاد بانک خالی
		Button btnCreateDb = new Button(_Context);
		btnCreateDb.setGravity(android.view.Gravity.CENTER);
		if(bSetTypefaceAndSize){
			btnCreateDb.setTypeface(_Typeface);
			btnCreateDb.setTextSize(_TextFontSize);
		}
		btnCreateDb.setPadding(DEF_PADDING, DEF_PADDING, DEF_PADDING, DEF_PADDING);
		lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, (float)1);
		btnCreateDb.setLayoutParams(lp);	
		btnCreateDb.setText("ذخیره و ایجاد بانک خالی");
		btnCreateDb.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(_txtCurSelectedPath != null){
					String dbPath = _txtCurSelectedPath.getText().toString();
					AppSettings.setDatabasePath(dbPath);
					

					String newDbPath = new File(dbPath, "ganjoor.s3db").getPath();
					if(GanjoorDbBrowser.CreateNewPoemDatabase(newDbPath, true)){
						_DbBrowser.OpenDatbase(newDbPath);
						ListPoets(true);
					}
					else{
						AddView(CreateTextView("تلاش برای ایجاد بانک خالی موفقیت آمیز نبود.", false));
					}
					
				}
				
			}
		});
		//btnCreateDb.setVisibility(View.GONE); //فعلاً
		AddView(btnCreateDb);
		
		//راهنمای مختصر نگارش آلفا
		TextView tvLittleHelp = CreateTextView(
				"برای دستیابی به فایل مورد نیاز، لازم است یکی از برنامه های رایگان و بازمتن «گنجور رومیزی» "
				+
				"یا «ساغر» را نصب کرده باشید. "
				+
				"گنجور رومیزی را از نشانی "
				+
				"\n\r"				
				+
				"http://ganjoor.sourceforge.net "
				+
				"\n\r"				
				+
				"و ساغر را از نشانی "
				+
				"\n\r"				
				+
				" http://pozh.org/saaghar "
				+
				"\n\r"				
				+
				"می توانید دریافت کنید.\n\r"
				+
				"نصاب هر دو برنامه در هنگام نصب مسیر کپی فایل مورد نیاز را سؤال می کند و شما هم می توانید بعداً از طریق "
				+
				"پنجره های پیکربندی این برنامه ها مسیر فایل را دوباره پیدا کنید. "
				+
				"کپی فایل مورد نیاز روی گوشی یا تبلت و تنظیم صحیح مسیر مشکل را حل می کند.\n\r"
				+
				"توجه داشته باشید که گنجور اندروید همانند سایر متعلقات گنجور رایگان است و "
				+
				"و نگارش جاری یک نگارش پیش نمایش و ابتدایی است. جهت کسب اطلاعات بیشتر به وبلاگ «تازه های گنجور» به نشانی "
				+
				"\n\r"				
				+
				"http://blog.ganjoor.net "
				+
				"\n\r"				
				+
				" یا تنها صفحۀ فیس بوک رسمی گنجور به نشانی "
				+
				"\n\r"				
				+				
				"http://www.facebook.com/ganjoor "
				+
				"\n\r"				
				+
				" مراجعه و ایرادات احتمالی را به ایمیل "
				+
				"\n\r"				
				+
				" ganjoor@ganjoor.net "
				+
				"\n\r"				
				+
				"گزارش کنید."
				,false);
		AddView(tvLittleHelp);
	}
	
	/**
	 * اندازۀ فونت پیش فرض
	 */
	private float _TextFontSize = AppSettings.DEF_TEXT_SIZE;

	/**
	 * اندازۀ فونت پیش فرض
	 */	
	public float getFontSize(){
		return _TextFontSize;
	}	
	
	/**
	 * تغییر اندازۀ فونت
	 * @param fTextFontSize اندازۀ فونت
	 */
	public void setFontSize(float fTextFontSize){
		if(_TextFontSize == fTextFontSize)
			return;
		
		_TextFontSize = fTextFontSize;
		if(getIsOnHome()){
			ListPoets(false);
		}
		else{
			ListPoets(true);
		}
	}
	
	public final static int SEARCH_SMALLWORD_MAXVERSELIMIT	= 100;
	public final static int SEARCH_SMALLWORD_LENGTH			= 1;
	public final static int SEARCH_INCOMPLETE				= -2;
	public final static int SEARCH_EMPTYTERM				= -1;
	
	/**
	 * برجسته سازی متن 
	 * @param term متن
	 * @param scrollView کنترل اسکرول جهت لغزیدن به محل متن پیدا شده
	 * @return تعداد آیتمهای یافته شده
	 */
	public int doHighlightTerm(String term, ScrollView scrollView){
		undoHighlight();
		if(term.isEmpty()){
			return SEARCH_EMPTYTERM;
		}
		int ctlCount = _ParentView.getChildCount();
		Boolean breakOnFirst = 
			(term.length() <= SEARCH_SMALLWORD_LENGTH)
			&&
			(ctlCount > SEARCH_SMALLWORD_MAXVERSELIMIT); 
		_highlightSet = new LinkedList<TextView>();
		for(int ctlIndex = 0; ctlIndex < ctlCount ; ctlIndex++){
			TextView tv = (TextView)_ParentView.getChildAt(ctlIndex);
			if(tv != null){
				String txt = tv.getText().toString();
				if(txt.indexOf(term) != -1){
					_highlightSet.add(tv);	
					setTextViewText(tv, txt, txt.indexOf(term), term.length(), true);
					if(breakOnFirst){
						scrollView.scrollTo(0, tv.getTop());
						return SEARCH_INCOMPLETE;
					}
				}
			}
		}
		int nCount = _highlightSet.size();
		if(nCount == 0){
			_highlightSet = null;
			_curHighlightIndex = -1;
		}
		else{
			_curHighlightIndex = 0;
			scrollView.scrollTo(0, _highlightSet.get(_curHighlightIndex).getTop());
		}
		return nCount;
	}	
	
	/**
	 * لیست مصرعهایی که حاوی متن برجسته شده هستند
	 */
	private List<TextView> _highlightSet = null;
	
	/**
	 * اندیس متن برجسته شده
	 */
	private int _curHighlightIndex = -1;
	
	/**
	 * برگرداندن وضعیت کادرهای متنی برجسته شده
	 * @return true if succeeds
	 */
	public Boolean undoHighlight(){
		if(_highlightSet == null)
			return false;
		for (TextView tv : _highlightSet) {
			tv.setText(tv.getText().toString(), TextView.BufferType.NORMAL);
		}
		_highlightSet = null;
		_curHighlightIndex = -1;
		return true;
	}
	

	/**
	 * 
	 * @param bNext بعدی یا قبلی
	 * @return آیا می تواند به موقعیت بعدی یا قبلی برجسته شده بلغزد
	 */
	public Boolean canScrollToNextHightlight(Boolean bNext){
		if(_highlightSet == null){
			return false;
		}
		if(_curHighlightIndex == -1){
			return false;
		}
		if(bNext){
			return (_curHighlightIndex + 1) < _highlightSet.size();
		}
		else{
			return (_highlightSet.size() > 1) && (_curHighlightIndex  != 0);
		}
	}
	/**
	 * لغزش به موقیت مورد بعدی یا قبلی برجسته شده
	 * @param bNext
	 * @param scrollView
	 * @return
	 */
	public Boolean doScrollToNextHighlight(Boolean bNext, ScrollView scrollView){
		if(!canScrollToNextHightlight(bNext)){
			return false;		
		}

		if(bNext)
			_curHighlightIndex++;
		else
			_curHighlightIndex--;
		scrollView.scrollTo(0, _highlightSet.get(_curHighlightIndex).getTop());
		return false;
		
	}
	/**
	 * 
	 * @return تعداد موارد همسان با متن جستجوی کاربر در صفحۀ جاری را بر می گرداند
	 */
	public int getHighlightsCount(){
		if(_highlightSet == null){
			return 0;
		}
		return _highlightSet.size();
	}
	/**
	 * ترتیب مورد فعلی برجسته سازی شده را با شروع از 1 بر می گرداند
	 * @return
	 */
	public int getCurrentHighlightOrder(){
		return _curHighlightIndex + 1;
	}
	

}
