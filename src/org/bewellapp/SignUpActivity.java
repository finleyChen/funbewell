package org.bewellapp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import org.bewellapp.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SignUpActivity extends Activity {

	private String TAG = "Sign Up";
	private String username;
	private String password;
	private String password2;
	private String email;
	private String errorMessage;
	private ProgressDialog progressDialog;
	
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 == 0) {
				// registration failed
				errorMessage = "Registration failed. Please try again later.\n" + errorMessage;
				progressDialog.dismiss();
				Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
			} else {
				// registration successful
				SharedPreferences.Editor spe = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).edit();
				spe.putString(Ml_Toolkit_Application.SP_USERNAME, email);
				spe.putString(Ml_Toolkit_Application.SP_PASSWORD, password);
				spe.putBoolean(Ml_Toolkit_Application.SP_ISREGISTERED, true);
				spe.commit();
				progressDialog.dismiss();
				Toast.makeText(getApplicationContext(), "Registration Successful!\nThanks!", Toast.LENGTH_LONG).show();
			}
			finish();
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sign_up);
		final EditText usernameText = (EditText)findViewById(R.id.SignUpUserNameET);
		final EditText passwordText = (EditText)findViewById(R.id.SignUpPasswordET);
		final EditText passwordText2 = (EditText)findViewById(R.id.SignUpPasswordET2);
		final EditText emailText = (EditText)findViewById(R.id.SignUpEmailET);

		Button btn = (Button)findViewById(R.id.SignUp);
		btn.setOnClickListener(new View.OnClickListener(){


			@Override
			public void onClick(View v){

				username = usernameText.getText().toString().trim();
				password = passwordText.getText().toString().trim();
				password2 = passwordText2.getText().toString().trim();
				email = emailText.getText().toString().trim();

				//if username is empty
				if (username.equals(""))
				{
					Toast.makeText(getApplicationContext(), "Name can not be empty!", Toast.LENGTH_LONG).show();
					return;
				}

				//if password is empty
				if (password.equals(""))
				{
					Toast.makeText(getApplicationContext(), "Password can not be empty!", Toast.LENGTH_LONG).show();
					return;
				}

				//if passwords don't match
				if (!password.equalsIgnoreCase(password2))
				{
					Toast.makeText(getApplicationContext(), "Passwords do not match!", Toast.LENGTH_LONG).show();
					return;
				}

				//if email is empty
				if (email.equals(""))
				{
					Toast.makeText(getApplicationContext(), "Email address can not be empty!", Toast.LENGTH_LONG).show();
					return;
				}

				Log.d(TAG, "username is " + username + " password is " + password + " email is" + email);
				progressDialog = ProgressDialog.show(SignUpActivity.this, "", "Registering, please wait...");
				
				Thread registerThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						errorMessage = "";
						boolean result = sendRegistration();
						Message m = handler.obtainMessage();
						if (result)
						{
							// registration successful
							m.arg1 = 1;
						}
						else
						{
							// registration failed
							m.arg1 = 0;
						}
						handler.sendMessage(m);
					}
				});
				registerThread.start();
			}
		});

	}


	/* returns true on successful registration */
	private boolean sendRegistration()
	{

		/* get the web service address */
		String service = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF,
				MODE_PRIVATE).getString(Ml_Toolkit_Application.SP_WEB_REGISTER_URL, Ml_Toolkit_Application.DEFAULT_SP_REGISTER_URL);
		Log.d(TAG, "Service is " + service);
		JSONObject holder = new JSONObject();
		try {
			holder.put("email", email);
			holder.put("password", password);
			holder.put("name", username);
		} catch (JSONException e) {
			// Should never happen
			e.printStackTrace();
		}
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(service);
		StringEntity se = null;
		try {
			se = new StringEntity(holder.toString());
		} catch (UnsupportedEncodingException e) {
			return false;
		}
		httpPost.setEntity(se);
		httpPost.setHeader("Accept", "application/json");
		httpPost.setHeader("Content-type", "application/json");
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String response;
		try {
			response = httpClient.execute(httpPost, responseHandler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			return false;
		}
		JSONObject result;
		try {
			result = new JSONObject(response);
		} catch (JSONException e) {
			/* server response is not a JSON object: something went wrong */
			return false;
		}
		
		Log.i(TAG, "Server response:" + result.toString());
		try {
			if (result.getString("result").equals("SUCCESS")) {
				return true;
			} else {
				if (result.getString("result").equals("ERROR")){
					errorMessage = result.getString("message");
				}
				
				return false;
			}
		} catch (Exception e) {
			/* malformed server response */
			return false;
		}
	}



}
