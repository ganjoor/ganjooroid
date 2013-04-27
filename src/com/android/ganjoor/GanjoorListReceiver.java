/**
 * 
 */
package com.android.ganjoor;

/**
 * @author Hamid Reza
 * کلاس دریافت فهرست مجمموعه های قابل دریافت از گنجور
 */
public class GanjoorListReceiver {
	public static Boolean DownloadLists(){
		for (String ListUrl : _UrlList) {
			if(!SyncDownloader.Download(ListUrl)){
				return false;
			}
		}
		return true;		
	}
	

	
	/**
	 * فهرستهای مجموعه ها
	 */
	private final static String[] _UrlList = new String[]{
		"http://i.ganjoor.net/android/androidgdbs.xml"
	};
	/*
	private final static String[] _UrlList = new String[]{
            "http://ganjoor.sourceforge.net/newgdbs.xml",
            "http://ganjoor.sourceforge.net/sitegdbs.xml",
            "http://ganjoor.sourceforge.net/programgdbs.xml"			
	};*/

}
