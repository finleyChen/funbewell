package org.bewellapp.MomentarySampling;

import java.sql.Date;
import java.text.SimpleDateFormat;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.Storage.ML_toolkit_object;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.bewellapp.R;

public class momentary_list extends Activity {

	private Cursor c;

	ListView l1;

	private Ml_Toolkit_Application appState;
	private final int rateNotification = 1000*60*1;
	private Handler mHandler = new Handler();

	public static int how_many_hours = 6;

	private static class EfficientAdapter extends CursorAdapter {

		private Cursor mCursor;
		private Context mContext;
		private final LayoutInflater mInflater;



		public EfficientAdapter(Context context, Cursor cursor) {
			//super(context, c);
			super(context, cursor, true);
			mInflater = LayoutInflater.from(context);
			mContext = context;
			mCursor = cursor;
		}

		//public int getCount() {
		//	return country.length;
		//}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			//Log.i("MomentarySampling"," Pos " + position);
			if (!mCursor.moveToPosition(position)) {
				throw new IllegalStateException("couldn't move cursor to position " + position);
			}
			View v;
			if (convertView == null) {
				v = newView(mContext, mCursor, parent);
			} else {
				//convertView.setBackgroundColor((position & 1) == 1 ? Color.BLACK  : 0xff222222);
				v = convertView;
			}
			bindView(v, mContext, mCursor);
			v.setBackgroundColor((position & 1) == 1 ? Color.BLACK  : 0xff222222);
			return v;
		}
		/*	
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.list_item, null);
				holder = new ViewHolder();
				holder.text = (TextView) convertView
				.findViewById(R.id.TextView01);
				holder.text2 = (TextView) convertView
				.findViewById(R.id.TextView02);
				holder.text3 = (TextView) convertView
				.findViewById(R.id.TextView03);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			convertView.setBackgroundColor((position & 1) == 1 ? Color.BLACK  : 0xff222222);


			holder.text.setText(curr[position]);
			holder.text2.setText(country[position] + '\n' + country[position]);
			holder.text3.setText(country[position]);
			holder.text2.setBackgroundColor(Color.RED);
			holder.text3.setBackgroundColor(Color.GREEN);

			return convertView;
		}*/


		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// TODO Auto-generated method stub
			TextView t = (TextView) view.findViewById(R.id.momentary_TextView01);
			//DateFormat df = new SimpleDateFormat("h:mm a"); 
			Date stress_time = new Date(cursor.getLong(cursor.getColumnIndex("DATE_time")));
			SimpleDateFormat df = new SimpleDateFormat("h:mma");

			t.setText(df.format(stress_time));



			t = (TextView) view.findViewById(R.id.momentary_TextView02);
			t.setText(cursor.getString(cursor.getColumnIndex("Label_value_")));
			if(t.getText().equals("Available")){
				t.setText("Avail\nable");
				t.setBackgroundColor(Color.argb(128, 0, 0, 128));}
			else{
				t.setBackgroundColor(Color.argb(128, 128, 0, 0));
				t.setText("Not\nAvailable");
			}

			t = (TextView) view.findViewById(R.id.momentary_TextView03);
			t.setText(cursor.getString(cursor.getColumnIndex("Inferred_status_")));
			if(t.getText().equals("Available")){
				t.setText("Avail\nable");
				t.setBackgroundColor(Color.argb(128, 0, 0, 128));}
			else{
				t.setBackgroundColor(Color.argb(128, 128, 0, 0));
				t.setText("Not\nAvailable");
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			// TODO Auto-generated method stub
			final View view = mInflater.inflate(R.layout.list_item, parent, false);
			return view;
		}

		//@Override
		/*public void bindView(View view, Context context, Cursor cursor) {
			// TODO Auto-generated method stub

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			// TODO Auto-generated method stub
			return null;
		}*/
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.momentary_main);
		//setListAdapter(new EfficientAdapter(this));

		appState = (Ml_Toolkit_Application) getApplicationContext();

		//insert some elements in the database
		//insertElementsMomentarySampling();




		String   table = MyMomentarySamplingDBAdapter.MomentarySamples_TABLE_NAME;
		String[] columns = {MyMomentarySamplingDBAdapter.MomentarySamples_ID, MyMomentarySamplingDBAdapter.DATE_time,MyMomentarySamplingDBAdapter.Start_time,
				MyMomentarySamplingDBAdapter.End_time,MyMomentarySamplingDBAdapter.Stress_level, "LOCATION_", MyMomentarySamplingDBAdapter.Label_value_,
				MyMomentarySamplingDBAdapter.Inferred_status_};

		//String[] columns = {MyMomentarySamplingDBAdapter.DATE_time, MyMomentarySamplingDBAdapter.Label_value_,
		//		MyMomentarySamplingDBAdapter.Inferred_status_};

		// THE XML DEFINED VIEWS WHICH THE DATA WILL BE BOUND TO
		//int[] to = new int[] { R.id.TextView01, R.id.TextView02,  R.id.TextView03};


		c = appState.ms_db_adapter.getHandle().query(table, columns, 
				MyMomentarySamplingDBAdapter.DATE_time + ">" + (System.currentTimeMillis() - how_many_hours *60*60*1000), 
				null, null, null, MyMomentarySamplingDBAdapter.DATE_time + " DESC");
		startManagingCursor(c);

		/*
		//access data from cursor
		int timeColumn = c.getColumnIndex("DATE_time");
		int labelColumn = c.getColumnIndex("Label_value_");


		if (c != null) {

			c.moveToFirst();
			if (c.isFirst()) {
				int i = 0;

				do {
					i++;

					long time_Value = c.getLong(timeColumn);
					//int age = c.getInt(labelColumn);

					String ageColumName = c.getString(labelColumn);


					//results.add("" + i + ": " + firstName 
					//		+ " (" + ageColumName + ": " + age + ")");
					Log.i("MomentarySampling", " " + ageColumName + "---" + time_Value);
					c.moveToNext();

				} while (!c.isLast());
			}
		}
		Log.i("MomentarySampling", " Nothing " + c.getCount());
		 */

		//c.close();


		l1 = (ListView) findViewById(R.id.ListView01);
		// CREATE THE ADAPTER USING THE CURSOR POINTING TO THE DESIRED DATA AS WELL AS THE LAYOUT INFORMATION
		//SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this, R.layout.list_item, c, columns, to);

		/*
		ColorDrawable divcolor = new ColorDrawable(Color.DKGRAY);
		l1.setDivider(divcolor);
		l1.setDividerHeight(2);
		//View v =  R.layout.list_item;
		//private LayoutInflater mInflater;
		LayoutInflater mInflater = LayoutInflater.from(this);
		View convertView = mInflater.inflate(R.layout.list_item, null);
		convertView = mInflater.inflate(R.layout.list_item, null);
		ViewHolder holder;
		holder = new ViewHolder();
		holder.text = (TextView) convertView
		.findViewById(R.id.TextView01);
		holder.text2 = (TextView) convertView
		.findViewById(R.id.TextView02);
		holder.text3 = (TextView) convertView
		.findViewById(R.id.TextView03);
		holder.text.setText("ll");
		holder.text2.setText("ll");
		holder.text3.setText("ll");
		holder.text2.setBackgroundColor(Color.RED);
		holder.text3.setBackgroundColor(Color.GREEN);

		convertView.setTag(holder);
		convertView.setBackgroundColor((position & 1) == 1 ? Color.BLACK  : 0xff222222);

		l1.addHeaderView(convertView);*/

		//insert some elements in the database
		//insertElementsMomentarySampling();


		//l1.setAdapter(mAdapter);
		//l1.setAdapter(new EfficientAdapter(this));
		l1.setAdapter(new EfficientAdapter(this,c));
		l1.setOnItemClickListener(new ListView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
				//Toast.makeText(getBaseContext(), "You clciked "+country[arg2], Toast.LENGTH_LONG).show();
				Log.i("MomentarySampling"," Pos " + position + " rowId " + id);

				//MyMomentarySamplingDBAdapter.MomentarySamples_ID, MyMomentarySamplingDBAdapter.DATE_time,MyMomentarySamplingDBAdapter.Start_time,
				//MyMomentarySamplingDBAdapter.End_time,MyMomentarySamplingDBAdapter.Stress_level, MyMomentarySamplingDBAdapter.Label_value_,
				//MyMomentarySamplingDBAdapter.Inferred_status_

				Intent intent = new Intent();
				//intent.putExtra("sensor_status", sensor_status);
				intent.setAction("org.bewellapp.MomentarySampling.label_view");
				//intent.putExtra("", value)

				c.moveToPosition(position);

				int tColumn = c.getColumnIndex(MyMomentarySamplingDBAdapter.MomentarySamples_ID);
				long id_ = c.getLong(tColumn);
				intent.putExtra("rowId", id_);

				tColumn = c.getColumnIndex("DATE_time");
				long time_ = c.getLong(tColumn);
				intent.putExtra("DATE_time", time_);

				tColumn = c.getColumnIndex("Start_time");
				long start_time_ = c.getLong(tColumn);
				intent.putExtra("Start_time", start_time_);

				tColumn = c.getColumnIndex("End_time");
				long end_time_ = c.getLong(tColumn);
				intent.putExtra("End_time", end_time_);

				tColumn = c.getColumnIndex("Stress_level");
				int Stress_level = c.getInt(tColumn);
				intent.putExtra("Stress_level", Stress_level);

				tColumn = c.getColumnIndex("LOCATION_");
				String location_ = c.getString(tColumn);
				intent.putExtra("location_", location_);

				tColumn = c.getColumnIndex("Label_value_");
				String Label_value_= c.getString(tColumn);
				intent.putExtra("Label_value_", Label_value_);

				tColumn = c.getColumnIndex("Inferred_status_");
				String Inferred_status_ = c.getString(tColumn);
				intent.putExtra("Inferred_status_", Inferred_status_);

				//tColumn = c.getColumnIndex("Inferred_status_");
				//String Inferred_status_ = c.getString(tColumn);
				//intent.putExtra("db_adapter", db_adapter);




				startActivityForResult(intent,1);

			}

		});
		l1.setTextFilterEnabled(true);


		//start an update timer to change if there is any 
		//mHandler.removeCallbacks(mUpdateTimeTask);
		//mHandler.postDelayed(mUpdateTimeTask, rateNotification);

	}

	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			//appState.location_text = "Scanning";
			//myLocationManager.getLocation(LocationService.this, locationResult );

			//put_sample_in_db();

			//updateNotificationArea();
			//mHandler.postAtTime(this,1000*60*1);
			//((EfficientAdapter)l1.getAdapter()).notifyDataSetChanged();
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, rateNotification);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*60*10);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*30);
		}
	};

	private void insertElementsMomentarySampling() {
		// TODO Auto-generated method stub
		//db_adapter = new MyMomentarySamplingDBAdapter(this,  "momemtary_sampling2.dbr_ms");	
		appState.ms_db_adapter.open();


		for(int i = 0; i<10; i++)
		{
			appState.ms_db_adapter.insertEntryMomertarySampling(System.currentTimeMillis() + i*60*60*1000, System.currentTimeMillis() + i*60*60*1000, 
					System.currentTimeMillis() + i*60*60*1000, i%7, "work","not_available","not_available");
		}
		appState.ms_db_adapter.close();

	}

	static class ViewHolder {
		TextView text;
		TextView text2;
		TextView text3;
	}

	/*protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Object o = this.getListAdapter().getItem(position);
		//String pen = o.toString();
		Toast.makeText(this, "You have chosen the position: " + " " + position, Toast.LENGTH_LONG).show();
	}*/


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode==RESULT_OK){
			//String msg = data.getStringExtra("returnedData");
			//textView.setText(msg);
			long end_stress_time_unix, stress_time_unix, rowId, start_stress_time_unix;
			int stress_level;
			String Label_value_, Inferred_status_,stress_location;
			rowId = data.getLongExtra("rowId", 0);

			stress_time_unix = data.getLongExtra("DATE_time",0);
			start_stress_time_unix = data.getLongExtra("Start_time",0);			
			end_stress_time_unix = data.getLongExtra("End_time", 0);
			stress_level = data.getIntExtra("Stress_level", 0);
			Label_value_ = data.getStringExtra("Label_value_");
			Inferred_status_ = data.getStringExtra("Inferred_status_");
			stress_location = data.getStringExtra("Stress_location");


			appState.ms_db_adapter.updateEntryMomertarySampling(stress_time_unix, start_stress_time_unix, end_stress_time_unix, 
					stress_level, stress_location, "Available", "not_available", rowId);
			MomentarySamplingService.labelCountAvailable = MyMomentarySamplingDBAdapter.getAvailableCount(appState.ms_db_adapter);


			String inferred_status = "rowID=" + rowId + ",sample_time=" + stress_time_unix + ",stress_start_time=" + start_stress_time_unix +
			",stress_end_time=" + end_stress_time_unix + ",stress_level=" + stress_level + ",location=" + stress_location 
			+ ",label_stress=" + "Available" +
			",inferred_stress=" + "not_available";

			ML_toolkit_object momentary_sample_point = appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset,
					12, true ,inferred_status);
			appState.ML_toolkit_buffer.insert(momentary_sample_point);


			Log.i("MomentarySamplinggg", "Update " + stress_location);

			//c.requery();
			((EfficientAdapter)l1.getAdapter()).notifyDataSetChanged();
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(0, Menu.FIRST + 1, 1, "Refresh");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {

		case Menu.FIRST + 1: { //upload status
			Toast.makeText(this, "Refreshed",
					Toast.LENGTH_SHORT).show();
			c.requery();
			((EfficientAdapter)l1.getAdapter()).notifyDataSetChanged();
			break;
		}
		}
		return false;


	}
}




