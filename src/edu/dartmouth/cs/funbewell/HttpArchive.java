/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland. 
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * Funf is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.dartmouth.cs.funbewell;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.bewellapp.Ml_Toolkit_Application;
import org.json.JSONObject;

import edu.mit.media.funf.storage.RemoteArchive;

import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import static edu.mit.media.funf.Utils.TAG;

/**
 * Archives a file to the url specified using POST HTTP method.
 * 
 * NOTE: not complete or tested
 * 
 */
public class HttpArchive implements RemoteArchive {

    SharedPreferences sp;

    private String uploadUrl;
    private String mimeType;

    public HttpArchive(SharedPreferences _sp) {
	uploadUrl="";
	sp = _sp;
    }

    Ml_Toolkit_Application appState;
    public HttpArchive(SharedPreferences _sp, Ml_Toolkit_Application _appState) {
    	uploadUrl="";
    	sp = _sp;
    	appState = _appState;
        }
    
    public String getUrl() {
	if (!sp.getBoolean(Ml_Toolkit_Application.SP_ISREGISTERED, false)) {
	    return "";
	}
	String bewelluploadUrl = sp.getString(
		Ml_Toolkit_Application.SP_WEB_UPLOAD_DB_URL,
		Ml_Toolkit_Application.DEFAULT_SP_UPLOAD_DB_URL);
	uploadUrl = String.format("%s/%s/%s", bewelluploadUrl,
		getUsername(), getPassword());

	return uploadUrl;
    }

    private String getUsername() {
	return sp.getString(Ml_Toolkit_Application.SP_USERNAME,
		"john@sample.com");
    }

    private String getPassword() {
	return sp.getString(Ml_Toolkit_Application.SP_PASSWORD, "password");
    }

    /*
    public HttpArchive(final String uploadUrl, final String mimeType) {
	this.uploadUrl = uploadUrl;
	this.mimeType = mimeType;
    }
    */
    public String getId() {
	return getUrl();
    }

    public boolean add(File file) {
	getUrl();
	
	if (appState.infoObj.charging_state == 0) {
		return false;
	}
	return isValidUrl(uploadUrl) ? uploadFileApache(file, uploadUrl) : false;
    }

    public static boolean isValidUrl(String url) {
	//Log.d(TAG, "Validating url");
	boolean isValidUrl = false;
	if (url != null && !url.trim().equals("")) {
	    try {
		Uri test = Uri.parse(url);
		isValidUrl = test.getScheme() != null
			&& test.getScheme().startsWith("http")
			&& test.getHost() != null
			&& !test.getHost().trim().equals("");
	    } catch (Exception e) {
		Log.d(TAG, "Not valid", e);
	    }
	}
	//Log.d(TAG, "Valid url? " + isValidUrl);
	return isValidUrl;
    }

    public static boolean uploadFileApache(File file, String uploadurl) {
	boolean isSuccess = true;
	
	HttpClient httpclient = new HttpClientLooseSSL();
	HttpPost httpPost = new HttpPost(uploadurl);
	FileBody bin = new FileBody(file);
	String filename = file.getName();
	try {
		StringBody comment = new StringBody("Filename: " + filename);

		MultipartEntity reqEntity = new MultipartEntity();
		reqEntity.addPart("data", bin);
		reqEntity.addPart("comment", comment);
		httpPost.setEntity(reqEntity);

		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String response = httpclient.execute(httpPost, responseHandler);
		JSONObject result = new JSONObject(response);
		String retString = result.getString("result");
		if(retString.equals("SUCCESS")) {
		    isSuccess = true;
		} else {
		    isSuccess = false;
		}
		Log.e(TAG, retString);
	} catch (Exception e) {
		e.printStackTrace();
		isSuccess = false;
	}
	
	return isSuccess;
    }
    /**
     * Copied (and slightly modified) from Friends and Family
     * 
     * @param file
     * @param uploadurl
     * @return
     */
    public static boolean uploadFile(File file, String uploadurl) {
	HttpURLConnection conn = null;
	DataOutputStream dos = null;
	DataInputStream inStream = null;

	String lineEnd = "\r\n";
	String twoHyphens = "--";
	String boundary = "*****";

	int bytesRead, bytesAvailable, bufferSize;
	byte[] buffer;
	int maxBufferSize = 1024; // old old value 1024*1024 old value 64*1024

	boolean isSuccess = true;
	try {
	    // ------------------ CLIENT REQUEST
	    FileInputStream fileInputStream = null;
	    // Log.i("FNF","UploadService Runnable: 1");
	    try {
		fileInputStream = new FileInputStream(file);
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
		Log.e(TAG, "file not found");
	    }
	    // open a URL connection to the Servlet
	    URL url = new URL(uploadurl);
	    // Open a HTTP connection to the URL
	    conn = (HttpURLConnection) url.openConnection();
	    // Allow Inputs
	    conn.setDoInput(true);
	    // Allow Outputs
	    conn.setDoOutput(true);
	    // Don't use a cached copy.
	    conn.setUseCaches(false);
	    // set timeout
	    conn.setConnectTimeout(60000);
	    conn.setReadTimeout(60000);
	    // Use a post method.
	    conn.setRequestMethod("POST");
	    conn.setRequestProperty("Connection", "Keep-Alive");
	    conn.setRequestProperty("Content-Type",
		    "multipart/form-data;boundary=" + boundary);

	    dos = new DataOutputStream(conn.getOutputStream());
	    dos.writeBytes(twoHyphens + boundary + lineEnd);
	    dos.writeBytes("Content-Disposition: form-data; name=\"data\";filename=\""
		    + file.getName() + "\"" + lineEnd);
	    dos.writeBytes(lineEnd);

	    // Log.i("FNF","UploadService Runnable:Headers are written");

	    // create a buffer of maximum size
	    bytesAvailable = fileInputStream.available();
	    bufferSize = Math.min(bytesAvailable, maxBufferSize);
	    buffer = new byte[bufferSize];
	    int totalSize = 0;

	    // read file and write it into form...
	    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
	    while (bytesRead > 0) {
		totalSize += bufferSize;
		if (totalSize > 6 * 1024 * 1024) {
		    dos.flush();
		    dos.close();
		    conn.disconnect();
		    System.gc();
		    conn.connect();
		    dos = new DataOutputStream(conn.getOutputStream());
		    dos.writeBytes(twoHyphens + boundary + lineEnd);
		    dos.writeBytes("Content-Disposition: form-data; name=\"data\";filename=\""
			    + file.getName() + "\"" + lineEnd);
		    dos.writeBytes(lineEnd);
		}
		dos.write(buffer, 0, bufferSize);
		bytesAvailable = fileInputStream.available();
		bufferSize = Math.min(bytesAvailable, maxBufferSize);
		bytesRead = fileInputStream.read(buffer, 0, bufferSize);
	    }

	    // send multipart form data necesssary after file data...
	    dos.writeBytes(lineEnd);
	    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

	    // close streams
	    // Log.i("FNF","UploadService Runnable:File is written");
	    fileInputStream.close();
	    dos.flush();
	    dos.close();
	} catch (Exception e) {
	    Log.e("FNF", "UploadService Runnable:Client Request error", e);
	    isSuccess = false;
	}

	// ------------------ read the SERVER RESPONSE
	try {
	    if (conn.getResponseCode() != 200) {
		isSuccess = false;
	    }
	} catch (IOException e) {
	    Log.e("FNF", "Connection error", e);
	    isSuccess = false;
	}

	return isSuccess;
    }
}
