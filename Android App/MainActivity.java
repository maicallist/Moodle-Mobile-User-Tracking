package com.example.moodle2020;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	
	private Button btn_rcdimg;
	private Button btn_exit;
	private Button btn_Ins_FF;
	private TextView tv_ffstate;
	private TextView tv_text;
	
	private static Integer imgNum = 0;
	private static String imgNumStr = "";
	private boolean switchState = false;
	private boolean ff_Ins = false;
	private String startbtn = "Start";
	
	private boolean screenshot = false;
	
	Timer timer = new Timer();
	//------delete------
	private TextView test;

	//-----------------------Android Timer---------------------------
	final Handler handler = new Handler(){  
		public void handleMessage(Message msg) {  
			switch (msg.what) {      
			case 1: 
				File image = new File("/mnt/sdcard/USQ_IMAGE");
				if (!image.exists()) {
					image.mkdir();
				}
				long size = 0;
				if(image.exists()){
					File[] sub = image.listFiles();
					for(int i=0 ; i<sub.length ; i++) {
						size = size + sub[i].length();
					}
					size = size / 1048576;
				}
				if(screenshot == true && size < 200){
					incre();
					try {
						RunAsRoot();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}      
			super.handleMessage(msg);  
		}    
	}; 
	//-----------------------------------------------------------------
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		float fv = (float) 4.03;
		if(fv>2.3){
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads()
				.detectDiskWrites()
				.detectAll() // 这里可以替换为detectAll() 就包括了磁盘读写和网络I/O
				.penaltyLog() //打印logcat，当然也可以定位到dropbox，通过文件保存相应的log
				.build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects() //探测SQLite数据库操作
				.penaltyLog() //打印logcat
				.penaltyDeath()
				.build()); 
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		
		btn_rcdimg = (Button)findViewById(R.id.btn_rcdimg);
		btn_exit = (Button)findViewById(R.id.btn_exit);
		btn_Ins_FF = (Button)findViewById(R.id.btn_Ins_FF);
		tv_ffstate = (TextView)findViewById(R.id.tv_ffstate);
		tv_text = (TextView)findViewById(R.id.tv_text);
		//--------------------
		test = (TextView)findViewById(R.id.test);
		
		//read root file
		//File[] filess = new File("/data").listFiles();
		//if(filess == null)Toast.makeText(this, "data file read failed.", Toast.LENGTH_LONG).show();else Toast.makeText(this, "data file read.", Toast.LENGTH_LONG).show();
		try{
			Process process = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(process.getOutputStream());

			String cmd = "/system/bin/screencap -p /mnt/sdcard/USQ_test_image.png\n";
			os.writeBytes(cmd);
			os.writeBytes("exit\n");
			os.flush();
			os.close();
			process.waitFor();
		} catch (Exception e){
			//do nothing
		}
		File testFile = new File("/mnt/sdcard/USQ_test_image.png");
		if(testFile.exists()) {
			Toast.makeText(this, "Root Permission Confirmed.", Toast.LENGTH_LONG).show();
			if(screenshot == false)
				enableshot();
		} else if(!testFile.exists()) {
			Toast.makeText(this, "Root Permission Denied, screenshot function not provided.", Toast.LENGTH_LONG).show();
			test.setVisibility(View.INVISIBLE);
			tv_text.setVisibility(View.INVISIBLE);
		}
		testFile.delete();
		//clear last zip
		clearZip();
		//check FF Ext
		checkExt();
		//set image number
		readImg();
		//start screen shot
		btn_rcdimg.setOnClickListener(new Button.OnClickListener(){
			public void onClick(View v) {
				if(startbtn.equals("Start")){
					startbtn = "Pause";
					btn_rcdimg.setText(startbtn);
				}else if(startbtn.equals("Pause")){
					startbtn = "Start";
					btn_rcdimg.setText(startbtn);
				}
				if(switchState == false){
					mTimerTask task = null;
					task = new mTimerTask();
					timer = new Timer(true);
					timer.schedule(task,1000, 5000);
					switchState = true;
				}else if(switchState == true){
					timer.cancel();
					switchState = false;
				}
			}
		});
		
		btn_exit.setOnClickListener(new Button.OnClickListener(){
			public void onClick(View v) {
				File file = new File(Environment.getExternalStorageDirectory(), "USQ_IMAGE");
				if(file.exists()){
					//zip
					try {
						ZipFolder(Environment.getExternalStorageDirectory().toString() +"/USQ_IMAGE",Environment.getExternalStorageDirectory().toString()+"/USQZip.zip");
					} catch (Exception e) {
						e.printStackTrace();
					}
					//send email
					//sendMail();
					//delete image folder
					File files[] = file.listFiles();
					if(files.length != 0){
						for (int i=0; i < files.length; i++){
							files[i].delete();
						}
					}
					file.delete();
					//delete zip
					//File mzip = new File(Environment.getExternalStorageDirectory()+"/USQZip.zip");
					//mzip.delete();
					//File ff = new File(Environment.getExternalStorageDirectory() +"/FF_Extension/URLRecorder.xpi");
					//ff.delete();
					//File fa = new File(Environment.getExternalStorageDirectory() + "/FF_ExtensionURLRecorder.xpi");
					//fa.delete();
					//File ft = new File(Environment.getExternalStorageDirectory() + "/FF_Extension");
					//ft.delete();
				}
				//kill process
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		});
		//----------Install ff ext --------------------------------
		btn_Ins_FF.setOnClickListener(new Button.OnClickListener(){
			public void onClick(View v) {
				tv_ffstate.setText("Downloading Firefox Extension...");
				download();
				checkExt();
				if(ff_Ins == true){
					tv_ffstate.setText("Download complete");
				}else if(ff_Ins == false){
					tv_ffstate.setText("Download File Not Found");
				}
				try{
				startActivity(openFile());
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			
		});
		//-------reset ff ext---------------------------------------
		tv_ffstate.setOnTouchListener(new OnTouchListener(){
			public boolean onTouch(View arg0, MotionEvent arg1) {
				File ff = new File("/mnt/sdcard/FF_Extension/URLRecorder.xpi");
				if(ff.exists()) {
					clearFF();
				}
				return false;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	//-------shell cmd to shoot----------
	
	 public void RunAsRoot() throws Exception{

		Process process = Runtime.getRuntime().exec("su");
		DataOutputStream os = new DataOutputStream(process.getOutputStream());
		
		imgNumStr = imgNum.toString();
		String cmd = "/system/bin/screencap -p /mnt/sdcard/USQ_IMAGE/image_" + imgNumStr + ".png\n";
		os.writeBytes(cmd);
		os.writeBytes("exit\n");
		os.flush();
		os.close();
		process.waitFor();
	 }

	//-----------zip-------------------------
	public static void ZipFolder(String srcFileString, String zipFileString)throws Exception{
		ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(zipFileString));
		File file = new File(srcFileString); 
		ZipFiles(file.getParent()+java.io.File.separator, file.getName(), outZip);
		outZip.finish();  
        outZip.close();
	}
	public static void ZipFiles(String folderString, String fileString, ZipOutputStream zipOutputSteam)throws Exception{
		if(zipOutputSteam == null)  
            return;
		File file = new File(folderString+fileString);
		if (file.isFile()) {  
			  
            ZipEntry zipEntry =  new ZipEntry(fileString);  
            FileInputStream inputStream = new FileInputStream(file);  
            zipOutputSteam.putNextEntry(zipEntry);  
              
            int len;  
            byte[] buffer = new byte[4096];  
              
            while((len=inputStream.read(buffer)) != -1){  
                zipOutputSteam.write(buffer, 0, len);  
            }  
              
            zipOutputSteam.closeEntry();  
        }else{
        	String fileList[] = file.list();
        	if (fileList.length <= 0) {  
                ZipEntry zipEntry =  new ZipEntry(fileString+java.io.File.separator);
                zipOutputSteam.putNextEntry(zipEntry);  
                zipOutputSteam.closeEntry();                  
            }  
        	for (int i = 0; i < fileList.length; i++) {  
                ZipFiles(folderString, fileString+java.io.File.separator+fileList[i], zipOutputSteam);  
            }
        }
	}
	//---------------send mail---------------------------------------------
	public void sendMail(){
		Intent intent = new Intent(Intent.ACTION_SEND);
		String[] tos = {"maicallist@gmail.com"};
		intent.putExtra(intent.EXTRA_EMAIL, tos);
		intent.putExtra(intent.EXTRA_SUBJECT, "USQ_Project");
		intent.putExtra(intent.EXTRA_TEXT, "content");
		intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///mnt/sdcard/USQZip.zip"));
		intent.setType("text/html");
		startActivity(Intent.createChooser(intent, "Choose client"));
	}
	//------download xpi-----------------------
	public void download(){
		//current Firefox Extension storage address
		//strUrl may change if need 
		//https://dl.dropboxusercontent.com/u/65645759/url_recorder.xpi
		String strUrl = "https://dl.dropboxusercontent.com/u/65645759/url_recorder.xpi";
		File tmpFile = new File(Environment.getExternalStorageDirectory() + "/FF_Extension");
        if (!tmpFile.exists()) {
                tmpFile.mkdir();
        }
		File file = new File(Environment.getExternalStorageDirectory() + "/FF_Extension/URLRecorder.xpi");
		URL url;
		try {
			url = new URL(strUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();
			FileOutputStream fos = new FileOutputStream(file);
			byte[] buf = new byte[256];
			conn.connect();
			double count = 0;
			if (conn.getResponseCode() >= 400) {
				Toast.makeText(MainActivity.this, "Timeout", Toast.LENGTH_SHORT).show();
			}else {
				while (count <= 100) {
					if (is != null) {
						int numRead = is.read(buf);
						if (numRead <= 0) {
							break;
						}else{
							fos.write(buf, 0, numRead);
						}
					}else {
						break;
					}
				}
			}
			conn.disconnect();
			fos.flush();
			fos.close();
			is.close();
		}catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(MainActivity.this, "Download Interrupted", Toast.LENGTH_LONG).show();
		}
	}
	
	//-----------------------Check Ext---------------------
	public void checkExt(){
		File file = new File(Environment.getExternalStorageDirectory() +"/FF_Extension/URLRecorder.xpi");
		if (!file.exists() || file.length() == 0){
			ff_Ins = false;
			tv_ffstate.setText("Firefox Extension Not Found");
			btn_Ins_FF.setVisibility(View.VISIBLE);
		}else{
			ff_Ins = true;
			tv_ffstate.setText("Firefox Extension Found");
			btn_Ins_FF.setVisibility(View.INVISIBLE);
		}
	}
	//--------------open xpi
	public Intent openFile(){
		String path = Environment.getExternalStorageDirectory() + "/FF_Extension/URLRecorder.xpi";
		File file = new File(path);  
        if(!file.exists()) return null;  

        String end=file.getName().substring(file.getName().lastIndexOf(".") + 1,file.getName().length()).toLowerCase();   
        if(end.equals("xpi")){
        	return getTextFileIntent(path, false);
        	
        }else{
        	return getAllIntent(path);
        }
	}
	public static Intent getAllIntent( String param ) {  
        Intent intent = new Intent();    
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);    
        intent.setAction(android.content.Intent.ACTION_VIEW);    
        Uri uri = Uri.fromFile(new File(param ));  
        intent.setDataAndType(uri,"*/*");   
        return intent;  
    }
	public Intent getTextFileIntent( String param, boolean paramBoolean){     
        Intent intent = new Intent("android.intent.action.VIEW");     
        intent.addCategory("android.intent.category.DEFAULT");     
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);     
        if (paramBoolean){     
            Uri uri1 = Uri.parse(param );     
            intent.setDataAndType(uri1, "text/plain");     
        }else{     
            Uri uri2 = Uri.fromFile(new File(param ));     
            intent.setDataAndType(uri2, "text/plain");     
        }     
        return intent;     
    }
	//------------increment screen shot
	public void incre(){
		imgNum++;
		test.setText(imgNum.toString());
	}
	//-----------read current image number when second time open app
	public void readImg(){
		File file = new File(Environment.getExternalStorageDirectory(), "USQ_IMAGE");
		boolean txtest = false;
		if(!file.exists()){
			imgNum = 0;
		}else{
			File files[] = file.listFiles();
			for(int i=0 ; i<files.length ; i++){
				if(files[i].getName().equals("USQRecorders.txt")){
					txtest = true;
				}
			}
			if(files.length != 0 && txtest == false){
				imgNum = files.length;
				test.setText(imgNum.toString());
				btn_rcdimg.setVisibility(View.INVISIBLE);
			}else if(files.length != 0 && txtest == true){
				imgNum = files.length-1;
				test.setText(imgNum.toString());
				btn_rcdimg.setVisibility(View.INVISIBLE);
			}else if(files.length == 0){
				imgNum = 0;
			}else if(files.length == 1 && txtest == true){
				imgNum = 0;
			}
		}
	}
	//---------clear last zip
	public void clearZip(){
		File mzip = new File(Environment.getExternalStorageDirectory()+"/USQZip.zip");
		mzip.delete();
		File file = new File(Environment.getExternalStorageDirectory(), "USQ_IMAGE");
		if(file.exists()){
			File files[] = file.listFiles();
			if(files.length == 0){
				file.delete();
			}
		}
	}
	//-------clear ff ext---------
	public void clearFF() {
		//final EditText input = new EditText(this);
    	//input.setFocusable(true);
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Redownload FireFox Ext?").setIcon(R.drawable.ic_launcher).setNegativeButton("Cancel",null);//.setView(input)
    	builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
    		public void onClick(DialogInterface dialog, int which){
    			File ff = new File(Environment.getExternalStorageDirectory() +"/FF_Extension/URLRecorder.xpi");
				ff.delete();
				File ft = new File(Environment.getExternalStorageDirectory() + "/FF_Extension");
				ft.delete();
				download();
				checkExt();
				startActivity(openFile());
    		}
    	});
    	builder.show();
	}
	
	//-----enablescreenshot---
	public void enableshot(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Do you wang to enable screenshot function?").setIcon(R.drawable.ic_launcher).setNegativeButton("Cancel",null);//.setView(input)
    	builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
    		public void onClick(DialogInterface dialog, int which){
    			screenshot = true;
    		}
    	});
    	builder.show();
	}
	class mTimerTask extends TimerTask{
		public void run(){
			Message message = new Message();
			message.what = 1;      
			handler.sendMessage(message);
		}
	}
}