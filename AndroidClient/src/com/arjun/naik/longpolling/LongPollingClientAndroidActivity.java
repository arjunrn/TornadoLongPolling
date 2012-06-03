package com.arjun.naik.longpolling;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

public class LongPollingClientAndroidActivity extends Activity {
	protected static final String TAG = "LongPollingClient";
	String mPushURL = "http://192.168.1.21:8000/long-poll/";
	TextView messageTV;
	
	Thread longPollThread;
	Handler longPollHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			Bundle payLoad = m.getData();
			String messageString = payLoad.getString("message");
			try {
				JSONObject longPollJSON = new JSONObject(messageString);
				String messageBody = longPollJSON.getString("body");
				messageTV.setText(messageBody);
			} catch (JSONException e) {
				Log.e(TAG, "JSON parse error with long polling message");
				e.printStackTrace();
			}
			Log.d(TAG, "Message String : " + messageString);
			longPollThread = new Thread(longPollingRunnable);
			longPollThread.start();
		}
	};

	Runnable longPollingRunnable = new Runnable() {

		public void run() {
			DefaultHttpClient defHttpClient = new DefaultHttpClient();
			HttpParams params = defHttpClient.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 15000);

			HttpGet httpGet = new HttpGet(mPushURL);
			try {
				Log.i(TAG,
						"Executing GET(PUSH) request "
								+ httpGet.getRequestLine());

				HttpResponse httpResponse = defHttpClient.execute(httpGet);
				HttpEntity entity = httpResponse.getEntity();
				String msgBodyString = EntityUtils.toString(entity);

				Log.i(TAG, String.valueOf(httpResponse.getProtocolVersion()));
				Log.i(TAG, msgBodyString);

				Bundle msgBundle = new Bundle();
				msgBundle.putString("message", msgBodyString);
				Message msg = new Message();
				msg.setData(msgBundle);
				longPollHandler.sendMessage(msg);

			} catch (ClientProtocolException e) {
				Log.e(TAG, "ClientProtocolException");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(TAG, "IOException");
				e.printStackTrace();
			}

		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		messageTV = (TextView) findViewById(R.id.message);
	}

	@Override
	public void onStart() {
		super.onStart();
		longPollThread = new Thread(longPollingRunnable);
		longPollThread.start();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (longPollThread != null) {
			if (longPollThread.isAlive()) {
				longPollThread.interrupt();
			}
		}
	}

}