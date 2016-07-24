package org.bewellapp.ServiceControllers.UploadingLib;

import java.io.File;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class UploadService extends IntentService{

	//public static final String UPLOAD_FILES = "org.bewellapp.ServiceControllers.UploadingLib.UploadService";

	//public UploadService(String name) {
		//super("worker");
		// TODO Auto-generated constructor stub
		//postData();
	//}

	
	private static final String TAG = "XY_QUEUE";
	
	
	public String UploadFileLocation;
	public UploadService() {
		super("Upload Service");
		// TODO Auto-generated constructor stub
	}


	@Override
	public void onCreate() { 
        super.onCreate(); 
	} 
	
	
	//@Override	
	//protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		//postData();
	//}
	
	private void postData(String db_path) {  
		// Create a new HttpClient and Post Header  
		HttpClient httpclient = new DefaultHttpClient();
		httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);	
		HttpPost httppost = new HttpPost("http://www.cs.dartmouth.edu/~mrshuva/all_php/helloworld.php");  


		File file = new File(db_path);
		MultipartEntity mpEntity = new MultipartEntity();
		ContentBody cbFile = new FileBody(file, "binary/octet-stream");
		mpEntity.addPart("userfile", cbFile);
		httppost.setEntity(mpEntity);
		String str="";
		try {  

			str = str + " " + "executing request " + httppost.getRequestLine();
			//System.out.println("executing request " + httppost.getRequestLine());
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity resEntity = response.getEntity();



			//System.out.println(response.getStatusLine());
			if (resEntity != null) {
				//System.out.println(EntityUtils.toString(resEntity));
				str = str + "\n" + EntityUtils.toString(resEntity);	
			}
			if (resEntity != null) {
				resEntity.consumeContent();
			}
			httpclient.getConnectionManager().shutdown();
		}
		catch(Exception e)
		{
			str = str + " "+e.toString();			
		}
		Log.d(TAG, "Problem uploading " + str);
		/*
	    try {  
			// Add your data  
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
			nameValuePairs.add(new BasicNameValuePair("fname", "mash"));  
			nameValuePairs.add(new BasicNameValuePair("age", "25"));  
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  

			// Execute HTTP Post Request  
			HttpResponse response = httpclient.execute(httppost);  
			str="Sucessful\n";

		} catch (ClientProtocolException e) {  
			// TODO Auto-generated catch block  
			str = "from here " + e.toString();
		} catch (IOException e) {  
			// TODO Auto-generated catch block  
			str = "from there " + e.toString();
		}  */


		//return str;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		//;
		Log.d(TAG, "Starting Uploadd" + intent.getStringExtra("dbpath"));
		postData(intent.getStringExtra("dbpath"));
	}  

}


