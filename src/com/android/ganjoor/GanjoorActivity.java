package com.android.ganjoor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import com.android.ganjoor.GanjoorView.NavigationTracker;
import com.android.ganjoor.GanjoorView.RequestSearchForDbCommand;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

/**
 * فهرست تغییرات:

 *
 *  شمارۀ ویرایش: 0.74
 *  - ایجاد ایندکسهای ناموجود جهت افزایش کارایی برنامه 
 *
 
 * 
 *
 *  شمارۀ ویرایش: 0.73
 *  - رفع ایراد خطای نصب مجموعه ها روی اندرویید با زبان پیش فرض فارسی (اعداد فارسی در کوئریهای SQL) 
 *
 
 *  شمارۀ ویرایش: 0.72
 *  - آیکون برنامه با آیکون ارسالی توسط setarvan.blogspot.de/2013/03/blog-post.html جایگزین شد 
 * 
 *  شمارۀ ویرایش: 0.7
 *  - عرضه شده در ؟
 *  - کلاسهای مقدماتی پردازش لیستهای مجموعه های قابل دریافت افزوده و آزمایش شد
 *  -
 * شمارهٔ ویرایش: ۰.۶
 * عرضه شده در ؟
 * - ایراد باقی ماندن اسکرولبار عمودی در نقطهٔ صفحهٔ قبل حل شد.
 * - رویداد دکمهٔ منوی اندرویید موقتاً با نمایش پنجرهٔ انتخاب اندازهٔ فونت پاسخگویی شد.
 *  
 * شمارهٔ ویرایش : ۰.۵
 * اولین ویرایش
 * عرضه شده در شهریور 91
 *
   
 */


/**
 * نقطۀ ورود به برنامه
 * @author Hamid Reza
 */
public class GanjoorActivity extends Activity implements SwipeInterface{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);    	
        super.onCreate(savedInstanceState);
        
        //اتصال به دیتابیس
        _DbBrowser = new GanjoorDbBrowser(this);
        
        
        //فونت نمایش متون
        AppSettings.setApplicationTypeface(Typeface.createFromAsset(getAssets(), "irsans.ttf"));
        _TextFontSize = AppSettings.getGanjoorViewFontSize();
        
        // آماده سازی والد اصلی تمام کنترلها
        _mainView = new LinearLayout(this);
        _mainView.setOrientation(1/*VERTICAL*/);       
        setContentView(_mainView);

        
        //آماده سازی نوار عنوان        
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);      
        setTitleBarItemsTypeface(AppSettings.getApplicationTypeface());
        

        //ادامۀ آماده سازی سلسله مراتب کنترلهای والد
        _scrollView = new ScrollView(this);
        _mainView.addView(_scrollView);        
        
        //کنترلهای نمایش متن و شعر روی این کنترل سوار میشوند
        LinearLayout contentView = new LinearLayout(this);
        contentView.setOrientation(1/*VERTICAL*/);        
        
        _scrollView.addView(contentView);    
        
        
        //فعال کردن حرکات لمسی راست به چپ و چپ به راست
        ActivitySwipeDetector swipe = new ActivitySwipeDetector(this);
        contentView.setOnTouchListener(swipe);
        _mainView.setOnTouchListener(swipe);
        
        //نمایش دهندۀ اطلاعات دیتابیس
        _GanjoorView = new GanjoorView(this, _DbBrowser, contentView, AppSettings.getApplicationTypeface(), _TextFontSize);
        
        //رویداد تغییر صفحه (شعر، بخش و ...) جاری 
        _GanjoorView.setOnCurrentItemChanged(new NavigationTracker() {
			
			@Override
			public void onCurrentItemChanged() {
				
				hidePopupMenu();
				//اگر پنل جستجو و برجسته سازی را داریم نمایش می دهیم حذفش می کنیم
		    	if(_FindView != null){
		    		undoSearch();	
		    	}		    	
				// موقعیت اسکرولبار را ریست می کنیم
				_scrollView.scrollTo(0, 0);
				
		        //دکمۀ جستجو را وقتی به دیتابیس وصل نیستیم نمایش نمی دهیم
		        Button btnSearch = (Button)findViewById(R.id.btnSearch);
		        if(btnSearch != null){		        	
		        	btnSearch.setVisibility(_DbBrowser.getIsConnected() ? View.VISIBLE : View.GONE);
		        }
				
				
				showCatsHierarchy();
				//اگر در صفحۀ خانه (و احتمالاً بعدتر در صفحات نشانه ها و جستجو) باشیم عنوان برنامه را نشان می دهیم وگرنه سلسله مراتب بخشها را
				TextView tvAppTitle = (TextView)findViewById(R.id.txtAppTitle);
				if(tvAppTitle != null){
					tvAppTitle.setVisibility(_GanjoorView.getIsOnHome() ? View.VISIBLE : View.GONE);
				}
			}
		});
        
        //رویداد تقاضای جستجو برای مسیر پایگاه داده ها
        _GanjoorView.setOnRequestSearchForDbCommand(new RequestSearchForDbCommand() {
			
			@Override
			public void onRequestSearchForDbCommand() {
				// TODO Auto-generated method stub
				showDialog(ID_DLG_AUTOFINDDBPATH);
			}
		});       
       
       
        //نمایش آخرین عنوان نمایش داده شده
        initFirstPage();		       
        
    }    

    /**
     * مدیریت اسکرول به عهدۀ این کنترل است
     */
    ScrollView _scrollView;
    /**
     * متغیر کار با دیتابیس برنامه در پنجرۀ اصلی ساخته شده و به نمونۀ کلاس
     * GanjoorView
     * ارسال می شود
     * 
     */
    private GanjoorDbBrowser _DbBrowser;
    /**
     * نمایش اشعار، بخشها و ... توسط این نمونه انجام می شود
     */
    private GanjoorView _GanjoorView;
    
    /**
     * اندازۀ پیش فرض فونتها
     */
    private float _TextFontSize;
    
  
    /**
     * پدر تمام کنترلها: رفرنسی از آن را برای درچ و حذف پنل جستجو نیاز داریم
     */
    private LinearLayout _mainView; 
    
    /**
     * کار با دکمۀ برگشت و منوی اندروید
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
    	switch(keyCode){
    		//کار با دکمۀ برگشت اندروید
    		case KeyEvent.KEYCODE_BACK:
        		if(_GanjoorView.getCanGoBackInHistory()){
        			doBack();        			
        			setResult(RESULT_CANCELED);
        			return false;
        		}
        		else{
        			doExit();
          		}    			
    			break;
    		//کار با دکمۀ منوی اندروید
    		case KeyEvent.KEYCODE_MENU:
    			showPopupMenu();
    			return true;
    	}
    	return super.onKeyDown(keyCode, event);
    }
    /**
     * ذخیرۀ مقدار شعر و بخش جاری جهت بازیابی در اجرای بعدی
     */
    private void SaveCurrentItems(){
		AppSettings.setLastCatIdVisited(_GanjoorView._CurCatId);
		AppSettings.setLastPoemIdVisited(_GanjoorView._CurPoemId);    	
    }
    /**
     * پیکربندی
     */
    private void doOptions(){
    	hidePopupMenu();
    	showDialog(ID_DLG_CONFIGVIEWFONT);    	
    }
    
    /**
     * پنل جستجو و برجسته سازی 
     */
    private LinearLayout _FindView = null;    
    
    /**
     * جستجو و برجسته سازی
     * تکنیک استفاده شده درج یک پنل در بالا و نقطۀ صفر پنل والد است
     */
    private void doSearch(){
    	//اگر داریم نمایشش می دهیم یعنی کاربر تصمیم گرفته دیگر نبیندش
    	if(_FindView != null){
    		undoSearch();
    		return;    		
    	}
    	
    	//پنل اصلی جستجو و برجسته سازی
		_FindView = new LinearLayout(this);
		_FindView.setGravity(android.view.Gravity.CENTER);
		_FindView.setPadding(5, 5, 5, 5);
		LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, (float)0);
		_FindView.setLayoutParams(lp);
		_FindView.setBackgroundColor(Color.DKGRAY);
		
		//ردیف بالا
		LinearLayout topView = new LinearLayout(this);
		topView.setOrientation(0/*HORIZONTAL*/);
		topView.setLayoutParams(lp);
		_FindView.addView(topView);
		
		
		
		//متن جستجو
		EditText edtTerm = new EditText(this);
		edtTerm.setText("");
		edtTerm.setTypeface(AppSettings.getApplicationTypeface());
		edtTerm.setTextSize(20);
		edtTerm.setPadding(5, 5, 5, 5);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, (float)0.8);
		edtTerm.setLayoutParams(lp);
		edtTerm.setFocusableInTouchMode(true);
		edtTerm.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				int nCount = _GanjoorView.doHighlightTerm(s.toString(), _scrollView);
				switch(nCount){
				case 0:
					getFindViewNotifier().setVisibility(View.VISIBLE);
					getFindViewNotifier().setText("یافت نشد.");
					getFindViewEditor().setBackgroundColor(Color.RED);
					getFindNextButton().setVisibility(View.GONE);
					getFindPrevButton().setVisibility(View.GONE);
					break;
				case GanjoorView.SEARCH_EMPTYTERM:
					getFindViewNotifier().setVisibility(View.GONE);
					getFindViewNotifier().setText("");
					getFindViewEditor().setBackgroundColor(Color.WHITE);
					getFindNextButton().setVisibility(View.GONE);
					getFindPrevButton().setVisibility(View.GONE);
					break;
				case GanjoorView.SEARCH_INCOMPLETE:
					getFindViewNotifier().setVisibility(View.VISIBLE);
					getFindViewNotifier().setText("؟  مورد");
					getFindViewEditor().setBackgroundColor(Color.WHITE);
					getFindNextButton().setVisibility(View.GONE);
					getFindPrevButton().setVisibility(View.GONE);
					break;
				default:
					getFindViewNotifier().setVisibility(View.VISIBLE);
					getFindViewEditor().setBackgroundColor(Color.WHITE);
					updateFindPanel();
					break;
				
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// هیچی
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				//هیچی
				
			}
		});
		
		
		//نمایشگر تعداد آیتمهای یافت شده
		TextView tvCount = new TextView(this);
		tvCount.setText("");
		tvCount.setTypeface(AppSettings.getApplicationTypeface());
		tvCount.setTextSize(20);
		tvCount.setPadding(5, 5, 5, 5);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, (float)0.2);
		tvCount.setLayoutParams(lp);
		tvCount.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.RIGHT);
		tvCount.setVisibility(View.GONE);
		
		
		//دکمۀ بعدی
		Button btnNext = new Button(this);
		btnNext.setText("بعدی");
		btnNext.setTextSize(20);
		btnNext.setPadding(5, 5, 5, 5);	
		btnNext.setWidth(70);
		btnNext.setGravity(android.view.Gravity.CENTER);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, (float)0);
		btnNext.setLayoutParams(lp);	
		btnNext.setTypeface(AppSettings.getApplicationTypeface());
		btnNext.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_GanjoorView.doScrollToNextHighlight(true, _scrollView);
				updateFindPanel();
				
			}
		});
		btnNext.setVisibility(View.GONE);
		
		//دکمۀ قبلی
		Button btnPrev = new Button(this);
		btnPrev.setText("قبلی");
		btnPrev.setTextSize(20);
		btnPrev.setPadding(5, 5, 5, 5);
		btnPrev.setWidth(70);
		btnPrev.setGravity(android.view.Gravity.CENTER);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, (float)0);
		btnPrev.setLayoutParams(lp);	
		btnPrev.setTypeface(AppSettings.getApplicationTypeface());
		btnPrev.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_GanjoorView.doScrollToNextHighlight(false, _scrollView);
				updateFindPanel();
				
			}
		});
		btnPrev.setVisibility(View.GONE);		
		
		topView.addView(btnNext);
		topView.addView(btnPrev);		
		topView.addView(tvCount);
		topView.addView(edtTerm);
		
		_mainView.addView(_FindView, 0);
		
		//کنترلها آماده اند، فقط مانده دادن فوکوس و ظاهر کردن صفحه کلید
		edtTerm.requestFocus();		
		InputMethodManager imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(edtTerm, 0);
		

	}
    
    /**
     * ترتیب مورد فعلی در یافته ها در صفحۀ جاری را نمایش می دهد
     */
	private void updateFindPanel() {
		getFindViewNotifier().setText(String.format("مورد %s از %s", PersianNumberFormat.Format(String.valueOf(_GanjoorView.getCurrentHighlightOrder())), PersianNumberFormat.Format(String.valueOf(_GanjoorView.getHighlightsCount())) ));
		getFindNextButton().setVisibility(_GanjoorView.canScrollToNextHightlight(true)? View.VISIBLE : View.GONE);
		getFindPrevButton().setVisibility(_GanjoorView.canScrollToNextHightlight(false)? View.VISIBLE : View.GONE);
	}
    
    /**
     * حذف پنل جستجو و مخفی کردن صفحه کلید در صورت نیاز
     */
	private void undoSearch() {
		_GanjoorView.undoHighlight();
		EditText edtTerm = getFindViewEditor();
		if(edtTerm == null)
			return;
		InputMethodManager imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(edtTerm.getWindowToken(), 0); 		
		_mainView.removeView(_FindView);    		
		_FindView = null;		
	}
	
	/**
	 * 
	 * @return کادر متنی پنل جستجو و برجسته سازی
	 */
    private EditText getFindViewEditor(){
    	if(_FindView == null)
    		return null;
    	return (EditText)((LinearLayout)_FindView.getChildAt(0)).getChildAt(3);
    }
    /**
     * 
     * @return برچسب اطلاع وضعیت پنل جستجو و برجسته سازی
     */
    private TextView getFindViewNotifier(){
    	if(_FindView == null)
    		return null;
    	return (TextView)((LinearLayout)_FindView.getChildAt(0)).getChildAt(2);
    }
    /**
     * 
     * @return دکمۀ بعدی پنل جستجو و برجسته سازی
     */
    private Button getFindNextButton(){
    	if(_FindView == null)
    		return null;
    	return (Button)((LinearLayout)_FindView.getChildAt(0)).getChildAt(0);
    }    
    /**
     * 
     * @return دکمۀ قبلی پنل جستجو و برجسته سازی
     */
    private Button getFindPrevButton(){
    	if(_FindView == null)
    		return null;
    	return (Button)((LinearLayout)_FindView.getChildAt(0)).getChildAt(1);
    }        
    
	
    /**
     * خروج از برنامه
     */
    private void doExit(){
		SaveCurrentItems();
		_DbBrowser.CloseDatabase();
		setResult(-1);
		finish();   	
    }
    /**
     * حذف شاعر
     */
    private void doDeletePoet(){
    	showDialog(ID_DLG_CONFIRMDELETEPOET);
    }
    
    /**
     * تنظیم فونت جعبه های متنی نوار عنوان
     * @param tf فونت
     */
    private void setTitleBarItemsTypeface(Typeface tf){
    	//نوار عنوان
    	setTypeface ((TextView)findViewById(R.id.txtAppTitle), tf);
    	setTypeface ((TextView)findViewById(R.id.txtCat1), tf);
    	setTypeface ((TextView)findViewById(R.id.txtSep1), tf);
    	setTypeface ((TextView)findViewById(R.id.txtCat2), tf);
    	setTypeface ((TextView)findViewById(R.id.txtSep2), tf);
    	setTypeface ((TextView)findViewById(R.id.txtCat3), tf);
    	setTypeface ((TextView)findViewById(R.id.txtSep3), tf);
    	setTypeface ((TextView)findViewById(R.id.txtCat4), tf);
    	
 
    }
    
    /**
     * تنظیم فونت جعبۀ متنی
     * @param tv جعبۀ متنی
     * @param tf فونت
     */
    private void setTypeface(TextView tv, Typeface tf){
        if(tv != null){
        	tv.setTypeface(tf);
        	tv.setTextSize(_TextFontSize);
        	}  	
    }
    
    /**
     * نمایش سلسله مراتب بخشها روی نوار عنوان
     */
    private void showCatsHierarchy(){
    	List<GanjoorCat> cats = _GanjoorView.getCurItemParentsHierarchy();
    	
    	Boolean showCat = (cats.size() > 3);
    	TextView tv = (TextView)findViewById(R.id.txtCat4);    	
    	if(tv != null){
    		tv.setVisibility(showCat ? View.VISIBLE : View.GONE);
    		if(showCat){
    			GanjoorCat cat = cats.get(3);
    			tv.setText(cat._Text);
    			tv.setTag(cat);   			
    		}
			tv = (TextView)findViewById(R.id.txtSep3);    		
			if(tv != null){
				tv.setVisibility(showCat ? View.VISIBLE : View.GONE);
			}    		
    	}
    	
    	showCat = (cats.size() > 2);
    	tv = (TextView)findViewById(R.id.txtCat3);    	
    	if(tv != null){
    		tv.setVisibility(showCat ? View.VISIBLE : View.GONE);
    		if(showCat){
    			GanjoorCat cat = cats.get(2);
    			tv.setText(cat._Text);
    			tv.setTag(cat);   			
    		}
			tv = (TextView)findViewById(R.id.txtSep2);    		
			if(tv != null){
				tv.setVisibility(showCat ? View.VISIBLE : View.GONE);
			}    		
    	}
    	
    	showCat = (cats.size() > 1);
    	tv = (TextView)findViewById(R.id.txtCat2);    	
    	if(tv != null){
    		tv.setVisibility(showCat ? View.VISIBLE : View.GONE);
    		if(showCat){
    			GanjoorCat cat = cats.get(1);
    			tv.setText(cat._Text);
    			tv.setTag(cat);   			
    		}
			tv = (TextView)findViewById(R.id.txtSep1);    		
			if(tv != null){
				tv.setVisibility(showCat ? View.VISIBLE : View.GONE);
			}    		
    	}
    	
    	showCat = (cats.size() > 0);
    	tv = (TextView)findViewById(R.id.txtCat1);    	
    	if(tv != null){
    		tv.setVisibility(showCat ? View.VISIBLE : View.GONE);
    		if(showCat){
    			GanjoorCat cat = cats.get(0);
    			tv.setText(cat._Text);
    			tv.setTag(cat);   			
    		}   		
    	}   	
    }
    
    /**
     * نمایش صفحۀ اول
     */
    private void initFirstPage(){
        _GanjoorView.GoToItem(
        		AppSettings.getLastPoemIdVisited(),
        		AppSettings.getLastCatIdVisited(),
        		false);
    }
    /**
     * رویداد نمایش منو
     */
    public void onMenuClick(View v){
    	showPopupMenu();
    }
    
    /**
     * رویداد کلیک دکمۀ حذف شاعر
     * @param v
     */
    public void onDeleteClick(View v){
    	hidePopupMenu();
    	doDeletePoet();
    }
    
    /**
     * رویداد کلیک دکمۀ خروج 
     */
	public void onExitClick(View v) {
		hidePopupMenu();
		doExit();
	}
	/**
	 * رویداد کلیک دکمۀ جستجو روی نوار عنوان
	 */
	public void onSearchClick(View v){
		doSearch();
	}
    /**
     * رویداد کلیک دکمۀ پیکربندی
     */	
	public void onNextClick(View v) {
		hidePopupMenu();
		doNext();
	}
    /**
     * رویداد کلیک دکمۀ پیکربندی
     */	
	public void onPrevClick(View v) {
		hidePopupMenu();
		doPrev();
	}
	
    /**
     * رویداد کلیک دکمۀ پیکربندی
     */	
	public void onOptionsClick(View v) {
		doOptions();
	}
    /**
     * رویداد کلیک دکمۀ خانه
     */
	public void onHomeClick(View v) {
		hidePopupMenu();
		if(!_GanjoorView.getIsOnHome()){
			_GanjoorView.ListPoets(true);
		}		
	}
	
	/**
	 * دریافت مجموعه ها 
	 */
	public void onDownloadClick(View v){
		hidePopupMenu();
		showDownloadWindow();

	}	
	/**
	 * معرفی برنامه در منو 
	 */
	public void onAboutClick(View v){
		hidePopupMenu();
		showDialog(ID_DLG_ABOUT);
	}

	/**
	 * رویداد کلیک روی لینکهای روی نوار عنوان
	 * @param v
	 */
	public void onTitleLinkClick(View v) {
		GanjoorCat Cat = (GanjoorCat)v.getTag();
		if(Cat != null){
			if(Cat._ParentID == 0){
				//در این حالت نه بخش که باید صفحۀ اطلاعات شاعر نمایش داده شود
				_GanjoorView.ShowPoet(_DbBrowser.getPoet(Cat._PoetID), true);
			}
			else{
				_GanjoorView.ShowCat(Cat, true);
			}
		}
	}
	
	/**
	 * برای نمایش منو از این متغیر استفاده می کنیم
	 */
	PopupWindow _popupMenu = null;
	
	/**
	 * نمایش یا جمع کردن منو
	 */
	private void showPopupMenu(){
		if(_popupMenu == null){
			LayoutInflater inflater = getLayoutInflater();
			View vMenu = inflater.inflate(R.layout.menu, null);
			
			Typeface typeface = AppSettings.getApplicationTypeface();
			setTypeface((TextView)vMenu.findViewById(R.id.tvHome) , typeface);
			setTypeface((TextView)vMenu.findViewById(R.id.tvDelete) , typeface);
			setTypeface((TextView)vMenu.findViewById(R.id.tvOptions) , typeface);
			setTypeface((TextView)vMenu.findViewById(R.id.tvDownload) , typeface);
			setTypeface((TextView)vMenu.findViewById(R.id.tvAbout) , typeface);
			setTypeface((TextView)vMenu.findViewById(R.id.tvExit) , typeface);
			
			int nItemCount = 6;
			if(_GanjoorView.getIsOnHome()){
				vMenu.findViewById(R.id.mnuHome).setVisibility(View.GONE);				
				nItemCount--;
				vMenu.findViewById(R.id.mnuDelete).setVisibility(View.GONE);				
				nItemCount--;				
			}
				
	
			
			
		
			_popupMenu = new PopupWindow(vMenu, 280, nItemCount * 48, false);
			_popupMenu.setHeight(_popupMenu.getMaxAvailableHeight(findViewById(R.id.btnMenu)));
			
			
			
		}
		if(_popupMenu.isShowing())
			hidePopupMenu();
		else
			_popupMenu.showAsDropDown(findViewById(R.id.btnMenu));
	}
	/**
	 * جمع کردن منو
	 */
	private void hidePopupMenu(){
		if(_popupMenu != null){
			_popupMenu.dismiss();
			_popupMenu = null;
		}
	}
	
	/**
	 * حرکت لمسی چپ به راست : ترجمه به عنوان قبلی می شود 
	 */
	@Override
	public void onSwipeLeftToRight(View v){
		doNext();
	}
	/**
	 * عنوان قبلی نمایش داده شود
	 */
	protected void doPrev() {
		if(!_GanjoorView.getIsOnHome()){
		
			_mainView.startAnimation(getPrevAnimation());
			_GanjoorView.GoPrev();
		}
	}
	

	/**
	 * حرکت لمسی راست به چپ : ترجمه به عنوان بعدی می شود 
	 */	
	@Override
	public void onSwipeRightToLeft(View v){
		doPrev();
	}
	/**
	 * عنوان بعدی نمایش داده شود
	 */
	protected void doNext() {
		if(!_GanjoorView.getIsOnHome()){
			_mainView.startAnimation(getNextAnimation());			
			_GanjoorView.GoNext();
		}
	}
	/**
	 * برگشت در تاریخچه
	 */
	private void doBack(){
		_mainView.startAnimation(getBackAnimation());
		_GanjoorView.GoBackInHistory();
	}
	/**
	 * انیمیشنها
	 */
	private Animation _nextAny = null;

	private Animation getNextAnimation(){
		if(_nextAny == null){
			_nextAny = AnimationUtils.loadAnimation(this, R.anim.next);
		}
		return _nextAny;
	}
	
	private Animation _prevAny = null;
	
	private Animation getPrevAnimation(){
		if(_prevAny == null){
			_prevAny = AnimationUtils.loadAnimation(this, R.anim.prev);
		}
		return _prevAny;
	}
	
	private Animation _backAny = null;
	
	private Animation getBackAnimation(){
		if(_backAny == null){
			_backAny = AnimationUtils.loadAnimation(this, R.anim.back);
		}
		return _backAny;
	}
	
	private static final int DOWNLOAD_GDB = 1;
	
	/**
	 * نمایش پنجرۀ دریافت مجموعه ها
	 */
	private void showDownloadWindow(){
		_DbBrowser.CloseDatabase();
		Intent intent = new Intent(this, GanjoorDatabaseDownloaderActivity.class);
		startActivityForResult(intent, DOWNLOAD_GDB);
	}
	/**
	 * بسته شدن دریافت مجموعه ها باید باعث رفرش شدن لیست شاعران شود
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (/*resultCode == Activity.RESULT_OK &&*/ requestCode == DOWNLOAD_GDB) {
	    	_DbBrowser.OpenDatbase();
	    	_GanjoorView.ListPoets(!_GanjoorView.getIsOnHome());
	    }
	}	
	
	/**
	 * ثابتهای دیالوگها
	 */
	//دیالوگ تنظیم اندازۀ فونت
	private static final int ID_DLG_CONFIGVIEWFONT 	= 0;
	
	//دیالوگ جستجوی خودکار مسیر پایگاه داده ها
	private static final int ID_DLG_AUTOFINDDBPATH 	= 1;
	
	//دیالو گ اعلام بی نتیجه بودن جستجوی خودکار مسیر پایگاه داده ها
	private static final int ID_DLG_AUTOFINDFAILED 	= 2;
	
	//دیالوگ معرفی
	private static final int ID_DLG_ABOUT			= 3;
	
	//دریافت تأییدیه حذف شاعر
	private static final int ID_DLG_CONFIRMDELETEPOET	=	4;
	

	/**
	 * ایجاد دیالوگها
	 */
	protected Dialog onCreateDialog(int id){
		switch(id){
		case ID_DLG_CONFIGVIEWFONT:{////دیالوگ تنظیم اندازۀ فونت
			final int IDX_SMALLFONT 	= 0;
			final int IDX_MIDDLEFONT 	= 1;
			final int IDX_MIDLRGFONT 	= 2;
			final int IDX_LARGEFONT 	= 3;
			final CharSequence[] items = {"کوچک", "متوسط", "بزرگ", "به اندازه کافی بزرگ"};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("انتخاب اندازه قلم");
			
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	float fFontSize = AppSettings.DEF_TEXT_SIZE;
			    	switch(item){
			    	case IDX_SMALLFONT:
			    		fFontSize = AppSettings.SMALL_TEXT_SIZE;
			    		break;
			    	case IDX_MIDDLEFONT:
			    		fFontSize = AppSettings.MIDDLE_TEXT_SIZE;
			    		break;
			    	case IDX_MIDLRGFONT:
			    		fFontSize = AppSettings.MIDLRG_TEXT_SIZE;
			    		break;			    		
			    	case IDX_LARGEFONT:
			    		fFontSize = AppSettings.LARGE_TEXT_SIZE;
			    		break;
			    	}
			    	
			    	if(_GanjoorView.getFontSize() != fFontSize){
			    		AppSettings.setGanjoorViewFontSize(fFontSize);
			    		_TextFontSize = fFontSize;
			    		setTitleBarItemsTypeface(AppSettings.getApplicationTypeface());			    		
			    		Toast.makeText(getApplicationContext(), "نمایش مجدد فهرست شاعران", Toast.LENGTH_SHORT).show();
			    		_GanjoorView.setFontSize(fFontSize);
			    	}
			    	else{
			    		Toast.makeText(getApplicationContext(), "انداز ه فونت را تغییر ندادید.", Toast.LENGTH_SHORT).show();
			    	}
			    }
			});
			return builder.create();
		}
	      case ID_DLG_AUTOFINDDBPATH:{//دیالوگ جستجوی خودکار مسیر پایگاه داده ها
	            _SearhcForDbProgressDialog = new ProgressDialog(this);
	            return _SearhcForDbProgressDialog;
	      }
	      case ID_DLG_AUTOFINDFAILED:{//دیالو گ اعلام بی نتیجه بودن جستجوی خودکار مسیر پایگاه داده ها
				AlertDialog.Builder abuilder = new AlertDialog.Builder(this);
				abuilder.setMessage("جستجوی خودکار نتیجه ای در بر نداشت.");
				abuilder.setCancelable(true);
				return abuilder.create();
	      }
	      case ID_DLG_ABOUT:{ //معرفی
	    	  	try {
					PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					Dialog aboutDlg = new Dialog(this);
					aboutDlg.setContentView(R.layout.about);
					aboutDlg.setTitle("");
					aboutDlg.setCancelable(true);
					aboutDlg.setCanceledOnTouchOutside(true);
					TextView tvAppNameVersion = (TextView) aboutDlg.findViewById(R.id.appVersion);
					setTypeface(tvAppNameVersion, AppSettings.getApplicationTypeface());
					tvAppNameVersion.setTextColor(Color.GREEN);
					tvAppNameVersion.setText(getString(pInfo.applicationInfo.labelRes) + " اندروید ویرایش " + PersianNumberFormat.Format(String.valueOf(pInfo.versionName)));
					TextView tvQuickHelp = (TextView) aboutDlg.findViewById(R.id.quickHelp);
					setTypeface(tvQuickHelp, AppSettings.getApplicationTypeface());
					tvQuickHelp.setText(
							"نرم افزار رایگان مرور اشعار برای اندروید\n"+
							"جهت کسب اطلاعات بیشتر به این نشانی مراجعه کنید:\n"+
							"http://blog.ganjoor.net\n"+
							"نظرات خود را به این ایمیل ارسال کنید:\n"+
							"ganjoor@ganjoor.net\n"+
							"صفحۀ فیسبوک گنجور:\n"+
							"http://www.facebook.com/ganjoor"
							);					
					return aboutDlg;	    	  
					
				} catch (NameNotFoundException e) {
					return null;
				}
	      }
	      
	      case ID_DLG_CONFIRMDELETEPOET:
	      {
	    	  GanjoorPoet poet = _DbBrowser.getPoet(_DbBrowser.getCat(_GanjoorView._CurCatId)._PoetID);	    	  
	    	  if(poet == null)
	    		  return null;
	    	  
	    	  AlertDialog.Builder builder = new AlertDialog.Builder(this);

	    	  builder.setMessage(String.format("از حذف آثار '%s' اطمینان دارید؟", poet._Name))
	    	         .setCancelable(false)
	    	         .setPositiveButton("بله", new DialogInterface.OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							_DbBrowser.DeletePoet(_DbBrowser.getPoet(_DbBrowser.getCat(_GanjoorView._CurCatId)._PoetID));
							_GanjoorView.ListPoets(true);
							
						}
					})
	    	         .setNegativeButton("خیر", new DialogInterface.OnClickListener() {
	    	             public void onClick(DialogInterface dialog, int id) {
	    	                  dialog.cancel();
	    	             }
	    	         });
	    	  return builder.create();	    	  
	      }
	      
		
			
		default:
			return null;			
		}
	}

	/**
	 * آماده سازی دیالوگها
	 */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
        case ID_DLG_AUTOFINDDBPATH:
            _SearhcForDbThread = new SearchForDbPathProgressThread(_SearchForDbPathHandler);
            _SearhcForDbThread.start();
    }	
	
    }
	
    /**
     *thread  جستجوی خودکار برای پایگاه داده ها را ما در یک
     *جداگانه انجام می دهیم
     *کدها و متغیرهای زیر مربوط به همین کار هستند 
     */
    // Define the Handler that receives messages from the thread and update the progress
    final Handler _SearchForDbPathHandler = new Handler() {
        public void handleMessage(Message msg) {
        	Bundle msgData = msg.getData();
        	String finalPath = msgData.getString("result");
        	if(finalPath != null){
        		_GanjoorView.setSuggestedDbPath(finalPath);
        	}
        	else{
        		String curPath = msgData.getString("path");
        		if(curPath != null){
        			_SearhcForDbProgressDialog.setMessage(curPath);
        		}
        		else{
        			showDialog(ID_DLG_AUTOFINDFAILED);
        		}
        		
        	}
        }
    };

    /** Nested class that performs progress calculations (counting) */
    private class SearchForDbPathProgressThread extends Thread {
        Handler mHandler;
       
        SearchForDbPathProgressThread(Handler h) {
            mHandler = h;
        }
       
        public void run() {
            Boolean bResult = doSearchPath("/");           
            dismissDialog(ID_DLG_AUTOFINDDBPATH);
            if(!bResult){
                mHandler.sendMessage(mHandler.obtainMessage());    		
            	
            	
            }
        }
       
       
        private Boolean doSearchPath(String sPath){
        	if(new File(sPath, "ganjoor.s3db").exists()){
                Message msg = mHandler.obtainMessage();
                Bundle pathString = new Bundle();
                pathString.putString("result", sPath);
                msg.setData(pathString);
                mHandler.sendMessage(msg);        		
        		return true;
        	}
        	if(sPath.equals("/proc")){
        		return false;
        	}
        	if(sPath.equals("/sys")){
        		return false;
        	}
          	
            Message msg = mHandler.obtainMessage();
            Bundle pathString = new Bundle();
            pathString.putString("path", sPath);
            msg.setData(pathString);
            mHandler.sendMessage(msg);
            FilenameFilter filterDir = new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String filename) {
					// TODO Auto-generated method stub
					return new File(dir, filename).isDirectory();
				}
			};
			String[] list;
			try{			
			list = new File(sPath).list(filterDir);
			}
			catch(Exception exp){
				list = null;				
			}
			
			if(list != null){				
			for (String dirName : list) {
				if(doSearchPath(new File(sPath, dirName).toString())){
					return true;
				}
			}
			}
        	return false;        	
        }
    }
	
	
    SearchForDbPathProgressThread _SearhcForDbThread;
    ProgressDialog _SearhcForDbProgressDialog;	    
    
}