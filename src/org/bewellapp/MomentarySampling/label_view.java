package org.bewellapp.MomentarySampling;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.bewellapp.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TimePicker;
import android.widget.Toast;

public class label_view extends Activity {
	/** Called when the activity is first created. */

	private long rowId; 
	private TextView t_time;// = (TextView) findViewById(R.id.time_of_occurance);
	private TextView t_start_time;// = (TextView) findViewById(R.id.start_stress_time);
	private TextView t_end_time;// = (TextView) findViewById(R.id.end_stress_time);

	private Button b_start_custom;// = (Button) findViewById(R.id.start_custom);
	private Button b_start_30;// = (Button) findViewById(R.id.start_30);
	private Button b_start_10;// = (Button) findViewById(R.id.start_10);
	private Button b_start_1;// = (Button) findViewById(R.id.start_1);

	private Button b_end_custom;// = (Button) findViewById(R.id.start_custom);
	private Button b_end_30;// = (Button) findViewById(R.id.start_30);
	private Button b_end_10;// = (Button) findViewById(R.id.start_10);
	private Button b_end_1;// = (Button) findViewById(R.id.start_1);

	private CheckBox rb_stress_level_table_0;// = (CheckBox) findViewById(R.id.rb_stress_level_0);
	private CheckBox rb_stress_level_table_1;// = (CheckBox) findViewById(R.id.rb_stress_level_1);
	private CheckBox rb_stress_level_table_2;// = (CheckBox) findViewById(R.id.rb_stress_level_2);
	private CheckBox rb_stress_level_table_3;// = (CheckBox) findViewById(R.id.rb_stress_level_3);
	private CheckBox rb_stress_level_table_4;// = (CheckBox) findViewById(R.id.rb_stress_level_4);
	private CheckBox rb_stress_level_table_5;// = (CheckBox) findViewById(R.id.rb_stress_level_5);
	private CheckBox rb_stress_level_table_6;// = (CheckBox) findViewById(R.id.rb_stress_level_6);
	private CheckBox rb_stress_level_table_7;// = (CheckBox) findViewById(R.id.rb_stress_level_7);

	private CheckBox rb_stress_location_work;
	private CheckBox rb_stress_location_home;
	private CheckBox rb_stress_location_others;

	private Button b_confirm;
	private Button b_cancel;

	Date start_stress_time;
	Date end_stress_time;
	Date stress_time;

	private long stress_time_unix;
	private long start_stress_time_unix;
	private long end_stress_time_unix;


	private boolean isCustomStart = true;

	private static final int TIME_DIALOG_ID = 0;

	private boolean stress_level_selected =false;
	private boolean stress_location_selected =false;

	private int stress_level;
	private String stress_location = "";

	private boolean update_data_available = false;
	//private long stress_level_old;

	//MyMomentarySamplingDBAdapter db_adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.momentarysampling_response);







		//Read more: http://getablogger.blogspot.com/2008/01/android-pass-data-to-activity.html#ixzz1Fhp6uozi

	}

	@Override
	public void onResume()
	{
		super.onResume();
		//group objects in the activity
		iniitilize_UI();


		Bundle extras = getIntent().getExtras(); 
		Log.i("MomentarySampling", ""+extras);
		if(extras !=null)
		{
			stress_time_unix = extras.getLong("DATE_time");
			stress_time = new Date(stress_time_unix);
			//SimpleDateFormat df = new SimpleDateFormat("HH:mma");
			t_time.setText(format_time(stress_time));

			start_stress_time_unix = extras.getLong("Start_time");
			start_stress_time = new Date(start_stress_time_unix);
			//SimpleDateFormat df_s = new SimpleDateFormat("HH:mm a");
			t_start_time.setText(format_time(start_stress_time));

			end_stress_time_unix = extras.getLong("End_time");
			end_stress_time = new Date(end_stress_time_unix);
			//SimpleDateFormat df_e = new SimpleDateFormat("HH:mm a");
			t_end_time.setText(format_time(end_stress_time));

			stress_level = extras.getInt("Stress_level");
			stress_location = extras.getString("location_");
			//end_stress_time = new Date(end_stress_time_unix);

			Log.i("MomentarySamling","stress_level " + stress_level + stress_location);

			updateStressLevelandLocation();

			rowId = extras.getLong("rowId");

			//db_adapter = (MyMomentarySamplingDBAdapter)extras.getSerializable("db_adapter");



		}
	}



	private String format_time(Date stress_time2) {
		// TODO Auto-generated method stub
		/*int hourOfDay = stress_time2.getHours();
		int minutes = stress_time2.getMinutes();

		String AA = "AM";
		//int mul = 0;
		if(hourOfDay > 11){
			AA = "PM";
			hourOfDay = hourOfDay%12;	
		}

		if(hourOfDay == 0)  hourOfDay = 12;
		return ""+hourOfDay+":"+minutes+AA;*/
		//Calendar c = Calendar.getInstance();
		//c.setTime(stress_time2);
		SimpleDateFormat df_e = new SimpleDateFormat("h:mm a");
		return df_e.format(stress_time2);

	}



	private void iniitilize_UI() {
		// TODO Auto-generated method stub
		t_time = (TextView) findViewById(R.id.time_of_occurance);
		t_start_time = (TextView) findViewById(R.id.start_stress_time);
		t_end_time = (TextView) findViewById(R.id.end_stress_time);

		b_start_custom = (Button) findViewById(R.id.start_custom);
		b_start_custom.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				isCustomStart = true;
				showDialog(TIME_DIALOG_ID);
			}
		});

		b_start_30 = (Button) findViewById(R.id.start_30);
		b_start_30.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//isCustomStart = true;
				//showDialog(TIME_DIALOG_ID);
				start_stress_time_unix = start_stress_time_unix - 30*60*1000; 
				start_stress_time = new Date(start_stress_time_unix);
				t_start_time.setText(format_time(start_stress_time));
			}
		});


		b_start_10 = (Button) findViewById(R.id.start_10);
		b_start_10.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//isCustomStart = true;
				//showDialog(TIME_DIALOG_ID);
				start_stress_time_unix = start_stress_time_unix - 10*60*1000; 
				start_stress_time = new Date(start_stress_time_unix);
				t_start_time.setText(format_time(start_stress_time));
			}
		});

		b_start_1 = (Button) findViewById(R.id.start_1);
		b_start_1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//isCustomStart = true;
				//showDialog(TIME_DIALOG_ID);
				start_stress_time_unix = start_stress_time_unix - 1*60*1000; 
				start_stress_time = new Date(start_stress_time_unix);
				t_start_time.setText(format_time(start_stress_time));
			}
		});

		b_end_custom = (Button) findViewById(R.id.end_custom);
		b_end_custom.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				isCustomStart = false;
				showDialog(TIME_DIALOG_ID);
			}
		});

		b_end_30 = (Button) findViewById(R.id.end_30);
		b_end_30.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//isCustomStart = true;
				//showDialog(TIME_DIALOG_ID);
				end_stress_time_unix = end_stress_time_unix + 30*60*1000; 
				end_stress_time = new Date(end_stress_time_unix);
				t_end_time.setText(format_time(end_stress_time));
			}
		});


		b_end_10 = (Button) findViewById(R.id.end_10);
		b_end_10.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//isCustomStart = true;
				//showDialog(TIME_DIALOG_ID);
				end_stress_time_unix = end_stress_time_unix + 10*60*1000; 
				end_stress_time = new Date(end_stress_time_unix);
				t_end_time.setText(format_time(end_stress_time));
			}
		});

		b_end_1 = (Button) findViewById(R.id.end_1);
		b_end_1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//isCustomStart = true;
				//showDialog(TIME_DIALOG_ID);
				end_stress_time_unix = end_stress_time_unix + 1*60*1000; 
				end_stress_time = new Date(end_stress_time_unix);
				t_end_time.setText(format_time(end_stress_time));
			}
		});

		rb_stress_level_table_0 = (CheckBox) findViewById(R.id.rb_stress_level_0);
		//if(stress_level == 0)
		//rb_stress_level_table_0.setChecked(true);
		rb_stress_level_table_0.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							stress_level_selected = true;
							stress_level = 0;
							rb_stress_level_table_1.setChecked(false);
							rb_stress_level_table_2.setChecked(false);
							rb_stress_level_table_3.setChecked(false);
							rb_stress_level_table_4.setChecked(false);
							rb_stress_level_table_5.setChecked(false);
							rb_stress_level_table_6.setChecked(false);
							rb_stress_level_table_7.setChecked(false);	
						} 
					}
				});

		rb_stress_level_table_1 = (CheckBox) findViewById(R.id.rb_stress_level_1);
		//if(stress_level == 1)
		//rb_stress_level_table_1.setChecked(true);
		rb_stress_level_table_1.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							stress_level_selected = true;
							stress_level = 1;
							rb_stress_level_table_0.setChecked(false);
							rb_stress_level_table_2.setChecked(false);
							rb_stress_level_table_3.setChecked(false);
							rb_stress_level_table_4.setChecked(false);
							rb_stress_level_table_5.setChecked(false);
							rb_stress_level_table_6.setChecked(false);
							rb_stress_level_table_7.setChecked(false);	
						} 
					}
				});

		rb_stress_level_table_2 = (CheckBox) findViewById(R.id.rb_stress_level_2);
		//if(stress_level == 2)
		//rb_stress_level_table_2.setChecked(true);
		rb_stress_level_table_2.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							stress_level_selected = true;
							stress_level = 2;
							rb_stress_level_table_1.setChecked(false);
							rb_stress_level_table_0.setChecked(false);
							rb_stress_level_table_3.setChecked(false);
							rb_stress_level_table_4.setChecked(false);
							rb_stress_level_table_5.setChecked(false);
							rb_stress_level_table_6.setChecked(false);
							rb_stress_level_table_7.setChecked(false);	
						} 
					}
				});

		rb_stress_level_table_3 = (CheckBox) findViewById(R.id.rb_stress_level_3);
		//if(stress_level == 3)
		//rb_stress_level_table_3.setChecked(true);
		rb_stress_level_table_3.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							stress_level_selected = true;
							stress_level = 3;
							rb_stress_level_table_1.setChecked(false);
							rb_stress_level_table_2.setChecked(false);
							rb_stress_level_table_0.setChecked(false);
							rb_stress_level_table_4.setChecked(false);
							rb_stress_level_table_5.setChecked(false);
							rb_stress_level_table_6.setChecked(false);
							rb_stress_level_table_7.setChecked(false);	
						} 
					}
				});

		rb_stress_level_table_4 = (CheckBox) findViewById(R.id.rb_stress_level_4);
		//if(stress_level == 4)
		//rb_stress_level_table_4.setChecked(true);
		rb_stress_level_table_4.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							stress_level_selected = true;
							stress_level = 4;
							rb_stress_level_table_1.setChecked(false);
							rb_stress_level_table_2.setChecked(false);
							rb_stress_level_table_3.setChecked(false);
							rb_stress_level_table_0.setChecked(false);
							rb_stress_level_table_5.setChecked(false);
							rb_stress_level_table_6.setChecked(false);
							rb_stress_level_table_7.setChecked(false);	
						} 
					}
				});

		rb_stress_level_table_5 = (CheckBox) findViewById(R.id.rb_stress_level_5);
		//if(stress_level == 5)
		//rb_stress_level_table_5.setChecked(true);
		rb_stress_level_table_5.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							stress_level_selected = true;
							stress_level = 5;
							rb_stress_level_table_1.setChecked(false);
							rb_stress_level_table_2.setChecked(false);
							rb_stress_level_table_3.setChecked(false);
							rb_stress_level_table_4.setChecked(false);
							rb_stress_level_table_0.setChecked(false);
							rb_stress_level_table_6.setChecked(false);
							rb_stress_level_table_7.setChecked(false);	
						} 
					}
				});

		rb_stress_level_table_6 = (CheckBox) findViewById(R.id.rb_stress_level_6);
		//if(stress_level == 6)
		//rb_stress_level_table_6.setChecked(true);
		rb_stress_level_table_6.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							stress_level_selected = true;
							stress_level = 6;
							rb_stress_level_table_1.setChecked(false);
							rb_stress_level_table_2.setChecked(false);
							rb_stress_level_table_3.setChecked(false);
							rb_stress_level_table_4.setChecked(false);
							rb_stress_level_table_5.setChecked(false);
							rb_stress_level_table_0.setChecked(false);
							rb_stress_level_table_7.setChecked(false);	
						} 
					}
				});

		rb_stress_level_table_7 = (CheckBox) findViewById(R.id.rb_stress_level_7);
		//if(stress_level == 7)
		//rb_stress_level_table_7.setChecked(true);
		rb_stress_level_table_7.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							stress_level_selected = true;
							stress_level = 7;
							rb_stress_level_table_1.setChecked(false);
							rb_stress_level_table_2.setChecked(false);
							rb_stress_level_table_3.setChecked(false);
							rb_stress_level_table_4.setChecked(false);
							rb_stress_level_table_5.setChecked(false);
							rb_stress_level_table_6.setChecked(false);
							rb_stress_level_table_0.setChecked(false);	
						} 
					}
				});



		rb_stress_location_work = (CheckBox) findViewById(R.id.rb_stress_location_work);
		rb_stress_location_work.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							stress_location_selected = true;
							stress_location = "work";
							rb_stress_location_home.setChecked(false);
							rb_stress_location_others.setChecked(false);
						} 
					}
				});

		rb_stress_location_home = (CheckBox) findViewById(R.id.rb_stress_location_home);
		rb_stress_location_home.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							stress_location_selected = true;
							stress_location = "home";
							rb_stress_location_work.setChecked(false);
							rb_stress_location_others.setChecked(false);
						} 
					}
				});

		rb_stress_location_others = (CheckBox) findViewById(R.id.rb_stress_location_others);
		rb_stress_location_others.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							stress_location_selected = true;
							stress_location = "others";
							rb_stress_location_home.setChecked(false);
							rb_stress_location_work.setChecked(false);
						} 
					}
				});

		b_confirm = (Button) findViewById(R.id.b_confirm);
		b_confirm.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//isCustomStart = true;
				//showDialog(TIME_DIALOG_ID);
				//end_stress_time_unix = end_stress_time_unix + 30*60*1000; 
				//end_stress_time = new Date(end_stress_time_unix);
				//t_end_time.setText(format_time(end_stress_time));

				if(stress_level_selected == false)
					Toast.makeText(label_view.this, "Please select the level of stress", Toast.LENGTH_SHORT).show();

				else if(stress_location_selected == false && stress_level != 0)
					Toast.makeText(label_view.this, "Please select the location of stress", Toast.LENGTH_SHORT).show();

				else{
					Toast.makeText(label_view.this, "Thanks for labeling", Toast.LENGTH_SHORT).show();

					//update the database
					//db_adapter.updateEntryMomertarySampling(stress_time_unix, start_stress_time_unix, end_stress_time_unix, 
					//stress_level, stress_location, "Available", "not_available", rowId);
					update_data_available = true;

					finish();
				}

			}
		});



		b_cancel = (Button) findViewById(R.id.b_cancel);
		b_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(label_view.this, "Labeling cancelled", Toast.LENGTH_SHORT).show();
				finish();
			}
		});

		b_start_custom.setText(" < ");



	}


	private TimePickerDialog.OnTimeSetListener mTimeSetListener =
		new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			//Toast.makeText(second_view.this, "Time is="+hourOfDay+":"+minute, Toast.LENGTH_SHORT).show();
			String AA = "AM";
			int new_original_hourOfDay_dtp = hourOfDay;
			int prev_hoursOfDay;
			int prev_minutes;
			Calendar calendar = GregorianCalendar.getInstance();
			//int mul = 0;
			if(hourOfDay > 11){
				AA = "PM";
				hourOfDay = hourOfDay%12;	
			}
			if(hourOfDay == 0)  hourOfDay = 12;

			if(!isCustomStart){
				calendar.setTime(new Date(stress_time_unix));
				prev_hoursOfDay = calendar.get(Calendar.HOUR_OF_DAY); 
				prev_minutes = calendar.get(Calendar.MINUTE); 

				Log.i("MomentarySamplingggg", "SampleTime "+ prev_hoursOfDay + ":" 
						+ prev_minutes + "  " + "End " +  new_original_hourOfDay_dtp + ":" + minute);

				int temp_original_hourOfDay_dtp = new_original_hourOfDay_dtp;
				if(new_original_hourOfDay_dtp < prev_hoursOfDay) 
					temp_original_hourOfDay_dtp = new_original_hourOfDay_dtp + 24;

				if(temp_original_hourOfDay_dtp - prev_hoursOfDay > 3)
				{
					Toast.makeText(label_view.this, "Time Change is more than 3 hours \n value discarded", Toast.LENGTH_SHORT).show();
					return;
				}


				calendar.add(Calendar.HOUR_OF_DAY, temp_original_hourOfDay_dtp - prev_hoursOfDay);

				int temp_minute = minute;
				//if(minute < prev_minutes) 
				//	temp_minute = minute + 60;
				calendar.add(Calendar.MINUTE, temp_minute  - prev_minutes);
				if(calendar.getTimeInMillis() < stress_time_unix)
				{
					Toast.makeText(label_view.this, "Stress ended before trigger point \nValud discarded", Toast.LENGTH_SHORT).show();
					return;
				}

				end_stress_time = calendar.getTime();
				end_stress_time_unix = calendar.getTimeInMillis();

				t_end_time.setText(format_time(end_stress_time));
				//t_end_time.setText(""+end_stress_time.getHours()+":"+end_stress_time.getMinutes()+AA);
			}
			if(isCustomStart){
				calendar.setTime(new Date(stress_time_unix));
				prev_hoursOfDay = calendar.get(Calendar.HOUR_OF_DAY); 
				prev_minutes = calendar.get(Calendar.MINUTE); 

				int temp_original_hourOfDay_dtp = new_original_hourOfDay_dtp;
				if(new_original_hourOfDay_dtp > prev_hoursOfDay) 
					temp_original_hourOfDay_dtp = new_original_hourOfDay_dtp - 24;

				if(temp_original_hourOfDay_dtp - prev_hoursOfDay < -3)
				{
					Toast.makeText(label_view.this, "Time Change is more than 3 hours \n value discarded " + (temp_original_hourOfDay_dtp - prev_hoursOfDay) ,
							Toast.LENGTH_SHORT).show();
					return;
				}


				calendar.add(Calendar.HOUR_OF_DAY, temp_original_hourOfDay_dtp - prev_hoursOfDay);

				int temp_minute = minute;
				//if(minute > prev_minutes) 
				//temp_minute = minute - 60;
				calendar.add(Calendar.MINUTE, temp_minute - prev_minutes);
				if(calendar.getTimeInMillis() > stress_time_unix)
				{
					//Toast.makeText(label_view.this, "Stress started after trigger point \nValud discarded", Toast.LENGTH_SHORT).show();
					return;
				}
				
				
				start_stress_time = calendar.getTime();
				start_stress_time_unix = calendar.getTimeInMillis();

				//SimpleDateFormat df_e = new SimpleDateFormat("HH:mm a");
				//t_start_time.setText(""+df_e.format(start_stress_time));
				t_start_time.setText(format_time(start_stress_time));
			}
		}
	};
	@Override
	protected void onPrepareDialog(int id,final Dialog dialog) {
		int hours;
		int minutes;

		Log.i("MomentarySampling", " called ");
		switch (id) {
		case TIME_DIALOG_ID:
			if(isCustomStart)
			{
				hours = start_stress_time.getHours(); minutes = start_stress_time.getMinutes();
			}
			else{
				hours = end_stress_time.getHours(); minutes = end_stress_time.getMinutes();
			}


			Log.i("MomentarySampling", " " + hours + ":" + minutes);
			//return new TimePickerDialog(this,mTimeSetListener, hours, minutes, false);

			((TimePickerDialog) dialog).updateTime(hours, minutes);

		}
		//return null;
	}

	protected Dialog onCreateDialog(int id) {
		int hours;
		int minutes;

		Log.i("MomentarySampling", " called ");
		switch (id) {
		case TIME_DIALOG_ID:
			if(isCustomStart)
			{
				hours = start_stress_time.getHours(); minutes = start_stress_time.getMinutes();
			}
			else{
				hours = end_stress_time.getHours(); minutes = end_stress_time.getMinutes();
			}


			Log.i("MomentarySampling", " " + hours + ":" + minutes);
			return new TimePickerDialog(this,mTimeSetListener, hours, minutes, false);

			//((TimePickerDialog) dialog).updateTime(hours, minutes);

		}
		return null;
	}


	@Override
	public void finish(){
		if(update_data_available)
		{
			Intent intent = getIntent();
			intent.putExtra("rowId", rowId);
			intent.putExtra("DATE_time", stress_time_unix);
			intent.putExtra("Start_time", start_stress_time_unix);			
			intent.putExtra("End_time", end_stress_time_unix);
			intent.putExtra("Stress_level", stress_level);
			intent.putExtra("Stress_location", stress_location);
			intent.putExtra("Label_value_", "Available");
			intent.putExtra("Inferred_status_", "Not\nAvailable");
			setResult(RESULT_OK,intent);
			super.finish();
		}
		else{
			setResult(RESULT_CANCELED,null);
			super.finish();
		}
	}

	public void updateStressLevelandLocation()
	{
		if(stress_level == 0)
			rb_stress_level_table_0.setChecked(true);
		if(stress_level == 1)
			rb_stress_level_table_1.setChecked(true);
		if(stress_level == 2)
			rb_stress_level_table_2.setChecked(true);
		if(stress_level == 3)
			rb_stress_level_table_3.setChecked(true);
		if(stress_level == 4)
			rb_stress_level_table_4.setChecked(true);
		if(stress_level == 5)
			rb_stress_level_table_5.setChecked(true);
		if(stress_level == 6)
			rb_stress_level_table_6.setChecked(true);
		if(stress_level == 7)
			rb_stress_level_table_7.setChecked(true);


		//stress location
		if(stress_location.equals("work"))
			rb_stress_location_work.setChecked(true);
		if(stress_location.equals("home"))
			rb_stress_location_home.setChecked(true);
		if(stress_location.equals("others"))
			rb_stress_location_others.setChecked(true);

	}

}
