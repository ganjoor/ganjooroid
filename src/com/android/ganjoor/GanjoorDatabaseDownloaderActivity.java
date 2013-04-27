/**
 * 
 */
package com.android.ganjoor;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * کلاس رابط کاربری دریافت مجموعه ها از اینترنت
 * @author Hamid Reza
 */
public class GanjoorDatabaseDownloaderActivity extends Activity {
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);  	
    	 
    	super.onCreate(savedInstanceState);
    	
    	_TextFontSize = AppSettings.getGanjoorViewFontSize();
    	
    	
     
        // آماده سازی والد اصلی تمام کنترلها
    	LinearLayout mainView = new LinearLayout(this);
        mainView.setOrientation( 1/*VERTICAL*/);       
        setContentView(mainView);
        
        //آماده سازی نوار عنوان        
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.dwnldtitlebar);
        TextView tvTitle = (TextView)findViewById(R.id.txtDownloadTitle);
        tvTitle.setTypeface(AppSettings.getApplicationTypeface());
        tvTitle.setTextSize(_TextFontSize);        
        
        
        //ادامۀ آماده سازی سلسله مراتب کنترلهای والد
        _ScrollView = new ScrollView(this);
        mainView.addView(_ScrollView);        
        
        //کنترلهای بلوک شاعر روی این کنترل سوار میشوند
        _contentView = new LinearLayout(this);
        _contentView.setOrientation(1/*VERTICAL*/);        
        
        _ScrollView.addView(_contentView);
        
        //اتصال به دیتابیس
        _DbBrowser = new GanjoorDbBrowser(this);
        
        //آیا می توانیم از داونلود منیجر اندروید استفاده کنیم؟
        _IsDownloadManagerAvailable = isDownloadManagerAvailable() && AppSettings.getUseAndroidDownloadManager();
        
        if(_IsDownloadManagerAvailable){
            _DownloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
            //رجیستر کردن شنونده رویداد پایان دریافت
            registerReceiver(onDownloadComplete,
            		new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
        
        _downloadedGdbs = new LinkedList<String>();
        
        _scheduledDownloads = new LinkedList<String>();
        
          
        //اگر قبلا لیست دریافت شده نمایشش بدهیم
        listLists(true);
        
        //دریافت لیست مجموعه ها      
        showDialog(ID_DLG_DOWNLOADXMLLISTS); 
     }
    /**
     * آیا می توانیم از سرویس داونلود منیجر اندرویید استفاده کنیم
     * این به ورژن اندرویید بستگی دارد
     */
    private Boolean _IsDownloadManagerAvailable;
    
    /**
     * نمونۀ downloadmanager
     */
    private DownloadManager _DownloadManager;
    
    /**
     * اضافه کردن داونلود حدید
     * @param url مسیر
     * @param title عنوان
     * @param description شرح
     * @param destinationPath مسیر خروجی
     */
	public void addDownload(String url, String title, String description, String destinationPath){
		Uri uri = Uri.parse(url);
		_scheduledDownloads.add(title);
		Request request = new Request(uri);
		request.setTitle(title);
		request.setDescription(description);
		request.setDestinationUri(Uri.fromFile(new File(destinationPath)));		
		
		try{			
			_DownloadManager.enqueue(request);
			
			Toast.makeText(getApplicationContext(), String.format("در حال دریافت %s", title), Toast.LENGTH_SHORT).show();		
		}
		catch(Exception e){
			Toast.makeText(getApplicationContext(), String.format("خطا در شروع دریافت %s", title), Toast.LENGTH_SHORT).show();
		}
		
	}
	/**
	 * در هنگام خروج باید شنوندۀ کامل شدن داونلودها را آنرجیستر کنیم
	 */
	@Override
	public void onDestroy(){
		super.onDestroy();
		_DbBrowser.CloseDatabase();
		if(_IsDownloadManagerAvailable){
		unregisterReceiver(onDownloadComplete);
		}
	}
	
	/**
	 * کامل شدن دریافت مجموعه ها
	 */
	private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
		
			Cursor cursor = _DownloadManager.query(new DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL));
			if(cursor.moveToFirst()){
				do{

					String gdbName = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
					Boolean scheduled = false;
					for(String scheduledDownload : _scheduledDownloads){
						if(scheduledDownload.equals(gdbName)){
							scheduled = true;
							break;
						}
							
					}
					Boolean alreadyInstalled = false;					
					for(String installedGdbName : _downloadedGdbs){
						if(installedGdbName.equals(gdbName)){
							alreadyInstalled = true;
							break;
						}						
					}
					if(scheduled && !alreadyInstalled){
						_downloadedGdbs.add(gdbName);

		    			File installingFile = new File(Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))).getPath());

						/*		    			
	            		if(_DbBrowser.ImportGdb(installingFile.getPath() )){
	            			Toast.makeText(getApplicationContext(), String.format("نصب مجموعۀ '%s' موفقیت آمیز بود.", gdbName), Toast.LENGTH_SHORT).show();			        			
	            		}
	            		else{            			
	            			Toast.makeText(getApplicationContext(), String.format("نصب مجموعۀ '%s' با خطا مواجه شد.", gdbName), Toast.LENGTH_LONG).show();			        			
	            		}			        		
	            		installingFile.delete();
		    			*/
		    			_InstallingQueue.add(new GDBInstallingItem(gdbName, installingFile));
		    			//ProcessInstallingQueue();
		    			Toast.makeText(getApplicationContext(), String.format("تلاش برای نصب '%s'", gdbName), Toast.LENGTH_SHORT).show();
		    			ProcessDownloadListThread th = new ProcessDownloadListThread(_ProcessHandler);
		    			th.start();
		    			
		    			
		    				    			
						
					}
				}while(cursor.moveToNext());
				
			}
			/*
			cursor = _DownloadManager.query(new DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_FAILED));			
			if(cursor.moveToFirst()){
				do{
					String gdbName = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
					Boolean scheduled = false;
					for(String scheduledDownload : _scheduledDownloads){
						if(scheduledDownload.equals(gdbName)){
							scheduled = true;
							break;
						}
							
					}
					if(scheduled){
						_downloadedGdbs.add(gdbName);
						Toast.makeText(getApplicationContext(), String.format("دریافت مجموعۀ '%s' با خطا مواجه شد.", gdbName), Toast.LENGTH_LONG).show();				
						
					}
				}while(cursor.moveToNext());
				
			}
			*/
			
		}
	};
	/**
	 * اطلاعات موارد دریافت شده برای قرار دادن در صف نصب
	 * @author Hamid Reza
	 *
	 */
	private class GDBInstallingItem{
		public GDBInstallingItem(String name, File file){
			_Name = name;
			_File = file;
		}
		public String _Name;
		public File _File;
	}
	
	protected void ProcessInstallingQueue(){
		if(_Installing){
			return;
		}
		_Installing = true;
		while(_InstallingQueue.size() > 0){
			
    		try{	        
    			File installingFile = _InstallingQueue.get(0)._File;
    			String gdbName = _InstallingQueue.get(0)._Name;
    			_InstallingQueue.remove(0);
   			
        		if(_DbBrowser.ImportGdb(installingFile.getPath() )){
        			listLists(false);
        			Toast.makeText(getApplicationContext(), String.format("نصب مجموعۀ '%s' موفقیت آمیز بود.", gdbName), Toast.LENGTH_SHORT).show();			        			
        		}
        		else{       			
        			Toast.makeText(getApplicationContext(), String.format("نصب مجموعۀ '%s' با خطا مواجه شد.", gdbName), Toast.LENGTH_LONG).show();			        			
        		}			        		
        		installingFile.delete();
	        		
        		}
    			finally{
    				
    			}

		}

		_Installing = false;
	}
	
	/**
	 * صف نصب
	 */
	private List<GDBInstallingItem> _InstallingQueue = new LinkedList<GDBInstallingItem>();
	/**
	 * در حال نصب هست یا خیر
	 */
	private Boolean _Installing = false;
	/**
	 * پردازش صف مجموعه های دریافت شده آماده نصب
	 */
	
	/**
	 * این لیست را برای فیلتر کردن داونلودهایی که نصبشان کرده ایم از آنها که نصب نشده اند نگه می داریم
	 */
	private List<String> _downloadedGdbs;

	/**
	 * این لیست را برای حل مشکلی نگه می داریم
	 * مشکل این است که وقتی قرار است ما لیست داونلودها را بگیریم سرویس گویا به ما لیست داونلودهایی را که در اجرای قبلی
	 * گذاشته بودیم هم می دهد، به همین خاطر لیست داونلودهایی که خودمان و در همین جلسه اضافه شان کردیم را در لیست نگه می داریم
	 */
	private List<String> _scheduledDownloads;
	
	
    
    
    /**
     * پدر تمام کنترلها
     */
    private LinearLayout _contentView;
    
    /**
     * متغیر کار با دیتابیس برنامه در پنجرۀ اصلی ساخته شده و به نمونۀ کلاس
     * GanjoorView
     * ارسال می شود
     * 
     */
    private GanjoorDbBrowser _DbBrowser;    
    
    /**
     * وقتی لیست را رفرش می کنیم اسکرول را به بالا بر می گردانیم
     */
    ScrollView _ScrollView;    
    /**
     * اندازۀ فونت متون
     */
    private float _TextFontSize;
    
    /**
     * حاشیۀ اطراف کنترلها
     */
    private static final int DEF_PADDING = 5;
    
    /**
     * نمایش لیست مجموعه های قابل دریافت
     * @param list
     */
    protected void showGDBList(GDBList list){
    	_contentView.removeAllViews();
    	_ScrollView.scrollTo(0, 0);
    	if(list != null){
    		Boolean addedBreaker = false;
    		Boolean hasInstalledItems = false;
    		//مجموعه های داونلود شده
	    	for (GDBInfo Item : list._Items) {
	    		Boolean installed = false;
	    		for(String gdbName : _downloadedGdbs){
	    			if(Item._CatName.equals(gdbName)){
	    				installed = true;
	    			}
	    		}
	    		if(installed){
	    			hasInstalledItems = true;
	    			if(!addedBreaker){
	    				
						LinearLayout llItem = new LinearLayout(this);
						llItem.setOrientation( 1/*VERTICAL*/);
						llItem.addView(createBreakerTextView("دریافت شده ها", GanjoorView.COLOR_BREAK));		
						_contentView.addView(llItem);   				
	    				addedBreaker = true;
	    			}
					LinearLayout llItem = new LinearLayout(this);
					llItem.setOrientation( 1/*VERTICAL*/);
					llItem.addView(createGDBItemTextView(Item, true));		
					_contentView.addView(llItem);
	    		}
			}
			if(hasInstalledItems){
				
				LinearLayout llItem = new LinearLayout(this);
				llItem.setOrientation( 1/*VERTICAL*/);
				llItem.addView(createBreakerTextView("دریافت نشده ها", GanjoorView.COLOR_BREAK));		
				_contentView.addView(llItem);   				
				addedBreaker = true;
			}	    	
    		//مجموعه های باقیمانده
	    	for (GDBInfo Item : list._Items) {
	    		Boolean installed = false;
	    		for(String gdbName : _downloadedGdbs){
	    			if(Item._CatName.equals(gdbName)){
	    				installed = true;
	    			}	    			
	    		}
	    		for(String gdbName : _scheduledDownloads){
	    			if(Item._CatName.equals(gdbName)){
	    				installed = true;
	    			}	    			
	    		}	    		
	    		if(!installed){
				LinearLayout llItem = new LinearLayout(this);
				llItem.setOrientation( 1/*VERTICAL*/);			
				llItem.addView(createGDBItemTextView(Item, false));
				_contentView.addView(llItem);
	    		}
			}    	
    	}
    	
    }

    /**
     * ایجاد کنترل نمایش نام مجموعه که کلیک بر روی آن باعث شروع دریافت می شود
     * @param Item مشخصات GDB
     * @return کنترل ایجاد شده
     */
	private TextView createGDBItemTextView(GDBInfo Item, Boolean installed) {
		TextView tv = new TextView(this);		
		tv.setGravity(android.view.Gravity.CENTER);	
		tv.setTypeface(AppSettings.getApplicationTypeface());
		tv.setTextSize(_TextFontSize);
		tv.setPadding(DEF_PADDING, DEF_PADDING, DEF_PADDING, DEF_PADDING);
		tv.setHorizontallyScrolling(false);
		LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, (float)1);
		tv.setLayoutParams(lp);
		String gdbName = Item._CatName;
		if(installed){
			tv.setTextColor(GanjoorView.COLOR_TITLE);
		}
		else{
			if(Item._FileSizeInByte != 0){
				gdbName = gdbName + String.format(" (%d کیلوبایت)", Item._FileSizeInByte / 1024);
			}
			tv.setClickable(true);
			tv.setTag(Item );
			tv.setTextColor(GanjoorView.COLOR_LINKCAT);
			tv.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					onDownloadItemClick(v);					
				}
			});
		}
		tv.setText(gdbName);		
		return tv;
	}
	/**
	 * ایجاد جداکنندۀ نصب شده ها و نشده ها
	 * @param tvText
	 * @param color
	 * @return
	 */
	private TextView createBreakerTextView(String tvText, int color) {
		TextView tv = new TextView(this);		
		tv.setGravity(android.view.Gravity.CENTER);	
		tv.setTypeface(AppSettings.getApplicationTypeface());
		tv.setTextSize(_TextFontSize);
		tv.setPadding(DEF_PADDING, DEF_PADDING, DEF_PADDING, DEF_PADDING);
		tv.setHorizontallyScrolling(false);
		LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, (float)1);
		tv.setLayoutParams(lp);
		tv.setText(tvText);
		tv.setTextColor(color);		
		return tv;
	}
	
	/**
	 * 
	 * @return آیا می توانیم از داونلود منیجر اندرویید استفاده کنیم
	 */
	public boolean isDownloadManagerAvailable(){
		try{
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD){
				return false;
			}
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);			
		
			intent.setClassName("com.android.providers.downloads.ui", "com.android.providers.downloads.ui.DownloadList");
			List<ResolveInfo> list = this.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);			
			return list.size() > 0;
		}catch(Exception e){
			return false;
		}
	}	
	/**
	 * کلیک روی هر مجموعه برای دریافت
	 * @param v
	 */
	private void onDownloadItemClick(View v) {
		GDBInfo tag = (GDBInfo) v.getTag();
		if(tag != null){
			v.setVisibility(View.GONE);
			if(_IsDownloadManagerAvailable){
			addDownload(tag._DownloadUrl, tag._CatName, "مجموعه شعر گنجور",
					new File(AppSettings.getDownloadPath(), new File(tag._DownloadUrl).getName()).toString());
			}
			else{			
			
			_GDBUrl = tag._DownloadUrl;
			_GDBName = tag._CatName;
			showDialog(ID_DLG_DOWNLOADGDB);
			}
			
		}
	}						
	
    
    
    
	/**
	 * ثابتهای دیالوگها
	 */
	
	//دیالوگ دریافت لیست مجموعه ها
	private static final int ID_DLG_DOWNLOADXMLLISTS 	= 1;
	
	//دیالو گ اعلام خطای دریافت لیست مجموعه ها
	private static final int ID_DLG_DOWNLOADFAILED 	= 2;
	
	//دیالوگ دریافت GDB خاص
	private static final int ID_DLG_DOWNLOADGDB 	= 3;
	
	//اعلان دریافت و نصب موفقیت آمیز مجموعه
	private static final int ID_DLG_INSTALLGDBSUCCEEDED 	= 4;
	
	//اعلان خطای نصب مجموعه
	private static final int ID_DLG_INSTALLGDBFAILED 	= 5;
	
	//در صورت زده شدن کلید برگضت و خالی نبودن صف داونلود اخطار می دهیم
	private static final int ID_DLG_CONFIRMNOTEXIT = 6;
	
	//نمایش پنجرۀ پیکربندی داونلود منیجر
	private static final int ID_DLG_DOWNLOADMNGROPTION = 7;
	

	

	/**
	 * ایجاد دیالوگها
	 */
	protected Dialog onCreateDialog(int id){
		switch(id){
	      case ID_DLG_DOWNLOADXMLLISTS:{	//دیالوگ دریافت لیست مجموعه ها
	            _DownloadXmlListsProgress = new ProgressDialog(this);
	            _DownloadXmlListsProgress.setMessage("در حال دریافت فهرست مجموعه ها\n لطفاً صبور باشید");
	            _DownloadXmlListsProgress.setCancelable(true);
	            _DownloadXmlListsProgress.setOnCancelListener(new OnCancelListener() {
					
					@Override
					public void onCancel(DialogInterface dialog) {
						listLists(false);
						
					}
				});
	            return _DownloadXmlListsProgress;
	      }
	      case ID_DLG_DOWNLOADGDB:{
	    	  _DownloadGDBProgress = new ProgressDialog(this);	    	 
	    	   _DownloadGDBProgress.setMessage(String.format("در حال دریافت %s", _GDBName));
	    	   _DownloadGDBProgress.setCancelable(false);
	    	   return _DownloadGDBProgress;
	      }
	      case ID_DLG_DOWNLOADFAILED:
	      case ID_DLG_INSTALLGDBSUCCEEDED:
	      case ID_DLG_INSTALLGDBFAILED:
	      {//دیالو گ اعلام خطای دریافت لیست مجموعه ها
				AlertDialog.Builder abuilder = new AlertDialog.Builder(this);
				String strMsg = "";
				switch(id){				
			      case ID_DLG_DOWNLOADFAILED:
			    	  strMsg = "دریافت از اینترنت با خطا مواجه شد.";
			    	  break;
			      case ID_DLG_INSTALLGDBSUCCEEDED:
			    	  strMsg = String.format("نصب مجموعۀ '%s' موفقیت آمیز بود.", _GDBName);
			    	  break;
			      case ID_DLG_INSTALLGDBFAILED:
			    	  strMsg = String.format("نصب مجموعۀ '%s' با خطا مواجه شد.", _GDBName);
			    	  break;
				}
				abuilder.setMessage(strMsg);
				abuilder.setCancelable(true);
				return abuilder.create();
	      }	
	      case ID_DLG_CONFIRMNOTEXIT:
	      {
	    	  AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	  builder.setMessage("در صورت خروج مجموعه های در حال دریافت امکان نصب نخواهند داشت.\n" +
	    	  		"از خروج اطمینان دارید؟")
	    	         .setCancelable(false)
	    	         .setPositiveButton("بله", new DialogInterface.OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							GanjoorDatabaseDownloaderActivity.this.finish();
							
						}
					})
	    	         .setNegativeButton("خیر", new DialogInterface.OnClickListener() {
	    	             public void onClick(DialogInterface dialog, int id) {
	    	                  dialog.cancel();
	    	             }
	    	         });
	    	  return builder.create();	    	  
	      }
	      
			case ID_DLG_DOWNLOADMNGROPTION:{//تنظیم داونلود منیجر
				final int IDX_USE 		= 0;
				final int IDX_DOTUNSE 	= 1;
				final CharSequence[] items = {"بله", "خیر"};

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("استفاده از مدیر دریافت اندروید");
				
				builder.setItems(items, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	Boolean bUseDownloadManager = _IsDownloadManagerAvailable;
				    	switch(item){
				    	case IDX_USE:
				    		bUseDownloadManager = true;
				    		break;
				    	case IDX_DOTUNSE:
				    		bUseDownloadManager = false;
				    		break;
				    	}
				    	
				    	if(bUseDownloadManager != _IsDownloadManagerAvailable){
				    		if(bUseDownloadManager){
				    			if(isDownloadManagerAvailable()){
				    				_IsDownloadManagerAvailable = true;
					    			AppSettings.setUseAndroidDownloadManager(true);				    				
				    				
				    			}else
				    			{
				    				Toast.makeText(getApplicationContext(), "مدیر دریافت اندروید روی گوشی شما قابل استفاده نیست", Toast.LENGTH_SHORT).show();
				    			}
				    		}
				    		else{
				    			_IsDownloadManagerAvailable = false;
				    			AppSettings.setUseAndroidDownloadManager(false);
				    		}
				    	}
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
        case ID_DLG_DOWNLOADXMLLISTS:
            _DownloadXmlListsThread = new DownloadXmlListsProgressThread(_DownloadXmlListsHandler);            
            _DownloadXmlListsThread.start();
            break;
        case ID_DLG_DOWNLOADGDB:
        	if(_DownloadGDBProgress != null){
        		_DownloadGDBProgress.setMessage(String.format("در حال دریافت %s", _GDBName));
        	}
        	_DownloadGDBThread = new DownloadGDBProgressThread(_DownloadGDBHandler, _GDBUrl);
        	_DownloadGDBThread.start();
        	break;
    }	
	
    }
    
    
   
    
    /**
     *دریافت لیست مجموعه ها را در یک ریسمان جدا انجام می دهیم
     *کدها و متغیرهای زیر مربوط به همین کار هستند 
     */
    final Handler _DownloadXmlListsHandler = new Handler() {
        public void handleMessage(Message msg) {
        	Bundle msgData = msg.getData();
        	String result = msgData.getString("result");
        	if(result == null){
        			showDialog(ID_DLG_DOWNLOADFAILED);
        	}
        	else{        		
                
        		listLists(false);        		
        	}
        }

    };
    
    final Handler _ProcessHandler = new Handler(){
    	public void handleMessage(Message msg){
        		Bundle msgData = msg.getData();
        		String result = msgData.getString("result");
    		
    			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();    		
        		listLists(false);        		
    	}
    	
    };
    
    /**
     * نام فایلهای مجموعه های پیش فرض
     * 
     */
	private final static String[] _Lists = new String[]{
		"androidgdbs.xml"
		/*
		"sitegdbs.xml",
        "newgdbs.xml",        
        "programgdbs.xml"*/
	};
	
	private GDBList _MixedList = null;
	/**
	 * نمایش لیستها
	 */
	private void listLists(Boolean refreshLists) {
		
		if(refreshLists || (_MixedList == null)){
		List<GDBList> lists = new LinkedList<GDBList>();       		
		
		
		for(int i=0; i<_Lists.length; i++){
			String xmlFile = _Lists[i];
		    File f = new File(AppSettings.getDownloadPath(), xmlFile);
		    GDBList list = null;
		    try {
		    	list = GDBList.Build(i, f.toString(), _DbBrowser);
		    	if(list != null){
		    	lists.add(list);
		    	}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}        			
		}
		_MixedList = GDBList.Mix(lists); 
		}
		
		showGDBList(_MixedList);
	}
	
    private class ProcessDownloadListThread extends Thread {
        Handler mHandler;
       
        ProcessDownloadListThread(Handler h) {
            mHandler = h;
        }
       
        public void run() {
        	ProcessInstallingQueue();
        	mHandler.sendMessage(mHandler.obtainMessage()); 
        }       
        
    	private void ProcessInstallingQueue(){
    		if(_Installing){
    			return;
    		}
    		_Installing = true;
    		while(_InstallingQueue.size() > 0){
    			
        		try{	        
        			File installingFile = _InstallingQueue.get(0)._File;
        			String gdbName = _InstallingQueue.get(0)._Name;
        			_InstallingQueue.remove(0);
       			
            		if(_DbBrowser.ImportGdb(installingFile.getPath() )){
                        Message msg = mHandler.obtainMessage();
                        Bundle pathString = new Bundle();
                        pathString.putString("result", String.format("نصب مجموعۀ '%s' موفقیت آمیز بود.", gdbName));
                        msg.setData(pathString);
                        mHandler.sendMessage(msg);       		
            			
            			//Toast.makeText(getApplicationContext(), String.format("نصب مجموعۀ '%s' موفقیت آمیز بود.", gdbName), Toast.LENGTH_SHORT).show();			        			
            		}
            		else{
                        Message msg = mHandler.obtainMessage();
                        Bundle pathString = new Bundle();
                        pathString.putString("result", String.format("نصب مجموعۀ '%s' با خطا مواجه شد.", gdbName));
                        msg.setData(pathString);
                        mHandler.sendMessage(msg);       		
            			
            			//Toast.makeText(getApplicationContext(), String.format("نصب مجموعۀ '%s' با خطا مواجه شد.", gdbName), Toast.LENGTH_LONG).show();			        			
            		}			        		
            		installingFile.delete();
		        		
            		}
        			finally{
        				
        			}

    		}

    		_Installing = false;
    	}
        
       
    }
    

    /**
     * کلاس ریسمان دریافت لیست مجموعه ها 
     *
     */
    private class DownloadXmlListsProgressThread extends Thread {
        Handler mHandler;
       
        DownloadXmlListsProgressThread(Handler h) {
            mHandler = h;
        }
       
        public void run() {
       	
            Boolean bResult = doDownloadLists();           
            dismissDialog(ID_DLG_DOWNLOADXMLLISTS);
            if(!bResult){
                mHandler.sendMessage(mHandler.obtainMessage());  		
            	
            	
            }
        }       
       
        private Boolean doDownloadLists(){
        	if(GanjoorListReceiver.DownloadLists()){
                Message msg = mHandler.obtainMessage();
                Bundle pathString = new Bundle();
                pathString.putString("result", "true");
                msg.setData(pathString);
                mHandler.sendMessage(msg);       		
        		
        		return true;
        	}
        	return false;        	 
        }
    }
	
	/**
	 * ریسمان دریافت فهرستهای مجموعه ها
	 */
    DownloadXmlListsProgressThread _DownloadXmlListsThread;
    /**
     * پنجرۀ نمایش وضعیت در حال دریافت فهرست مجموعه ها
     */
    ProgressDialog _DownloadXmlListsProgress;	    

    
    
    
    /**
     *دریافت هر مجموعه را در یک رشتۀ جدا انجام می دهیم
     *کدها و متغیرهای زیر مربوط به همین کار هستند 
     */
    final Handler _DownloadGDBHandler = new Handler() {
        public void handleMessage(Message msg) {
        	Bundle msgData = msg.getData();
        	String result = msgData.getString("result");
        	if(result == null){
        			showDialog(ID_DLG_DOWNLOADFAILED);
        	}
        	else{   
        		try{
        		if(_DbBrowser.ImportGdb(new File(AppSettings.getDownloadPath(), new File(_GDBUrl).getName()).toString())){
        			showDialog(ID_DLG_INSTALLGDBSUCCEEDED);
        			_downloadedGdbs.add(_GDBName);
        			listLists(false);
        		}
        		else{
        			showDialog(ID_DLG_INSTALLGDBFAILED);
        		}
        		}
        		catch(Exception exp){
        			
        		}
        	}
        }
    };
    

    /**
     * کلاس رشتۀ دریافت مجموعه 
     *
     */
    private class DownloadGDBProgressThread extends Thread {
    	
        Handler mHandler;
        String urlFile;
       
        DownloadGDBProgressThread(Handler h, String url) {
            mHandler = h;
            urlFile = url;
        }
       
        public void run() {
       	
            Boolean bResult = doDownload();           
            dismissDialog(ID_DLG_DOWNLOADGDB);
            if(!bResult){
                mHandler.sendMessage(mHandler.obtainMessage());    		
            	
            	
            }
        }       
       
        private Boolean doDownload(){
        	if(SyncDownloader.Download(urlFile)){
                Message msg = mHandler.obtainMessage();
                Bundle pathString = new Bundle();
                pathString.putString("result", "true");
                msg.setData(pathString);
                mHandler.sendMessage(msg);       		
        		
        		return true;
        	}
        	return false;        	 
        }
    }
	
	/**
	 * ریسمان دریافت مجموعه
	 */
    DownloadGDBProgressThread _DownloadGDBThread;
    /**
     * پنجرۀ نمایش وضعیت در حال دریافت مجموعه
     */
    ProgressDialog _DownloadGDBProgress;
    
    
    /**
     * نام مجموعۀ انتخاب شده
     */
    String _GDBName = "";
    
    /**
     * نشانی دریافت مجموعۀ انتخاب شده
     */
    String _GDBUrl = "";
    
    /**
     * کار با دکمۀ برگشت و منوی اندروید
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
    	switch(keyCode){
    		//کار با دکمۀ برگشت اندروید
    		case KeyEvent.KEYCODE_BACK:
    			if(_downloadedGdbs.size() < _scheduledDownloads.size()){
        			setResult(RESULT_CANCELED);
        			showDialog(ID_DLG_CONFIRMNOTEXIT);
        			return false;
    			}
    			break;
        		//کار با دکمۀ منوی اندروید
    		case KeyEvent.KEYCODE_MENU:
    			showDialog(ID_DLG_DOWNLOADMNGROPTION);
    			return true;    			
    	}
    	return super.onKeyDown(keyCode, event);
    }
    
    
    
    
    
}
