/**
 * 
 */
package com.android.ganjoor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.os.AsyncTask;



/**
 * کلاس دریافت فایل
 *
 */
public class DownloadFile extends AsyncTask<String, Integer, String>{
	
	public interface DownloadCompleteEvent{
		void onDownloadCompleted(String result);
		}

	private DownloadCompleteEvent _dwnldCompleted = null;
	public void setOnDownloadCompleted(DownloadCompleteEvent dwnldCompleted){
		_dwnldCompleted = dwnldCompleted;
	}
	@Override
	protected void onCancelled (){
		super.onCancelled();
	}
	
	
	@Override
	protected void onPreExecute(){
		super.onPreExecute();
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress){
		super.onProgressUpdate(progress);
	}
	
	@Override
	protected void onPostExecute(String result){
		super.onPostExecute(result);
		if(_dwnldCompleted != null){
			_dwnldCompleted.onDownloadCompleted(result);
		}
	}
	
	@Override
	protected String doInBackground(String... sUrl ){
		try{
			URL url = new URL(sUrl[0]);
			URLConnection connection = url.openConnection();
			int fileLength = connection.getContentLength();
			InputStream input = new BufferedInputStream(url.openStream());
			String DownloadPath = AppSettings.getDownloadPath();
			File downloadPath = new File(DownloadPath);		
			if(!downloadPath.exists()){
			try{			
				downloadPath.mkdirs();
			}
			catch(SecurityException e){
				return null;
			}	
			}
			String fileName = url.getFile();
			if(fileName.isEmpty()){
				return null;
			}
			String fileFileName = new File(downloadPath, fileName).toString();
			OutputStream output = new FileOutputStream(fileFileName);
			byte data[] = new byte[1024];
			int count;
			int total = 0;
			while((count = input.read(data)) != -1){
				total += count;
				if(fileLength != 0)
					publishProgress((int)total * 100 / fileLength);
				output.write(data, 0, count);
			}
			output.flush();
			output.close();
			input.close();
			return fileFileName;
		}
		catch(Exception e){
			return null;
		}
	}
	
}
