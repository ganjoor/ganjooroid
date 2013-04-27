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

/**
 * @author Hamid Reza
 *
 */
public class SyncDownloader {
	public static Boolean Download(String dwnldUrl){
		try{
			URL url = new URL(dwnldUrl);
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
			String fileName = new File(dwnldUrl).getName();
			if(fileName.isEmpty()){
				return null;
			}
			String filePath = new File(downloadPath, fileName).toString();
			OutputStream output = new FileOutputStream(filePath);
			byte data[] = new byte[1024];
			int count;
			int total = 0;
			while((count = input.read(data)) != -1){
				total += count;
				output.write(data, 0, count);
			}
			output.flush();
			output.close();
			input.close();
			return true;
		}
		catch(Exception e){
			return false;
		}
		
	}
}
