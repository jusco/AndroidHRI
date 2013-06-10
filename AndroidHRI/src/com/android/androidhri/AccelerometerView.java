/*
 * Copyright (C) 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.androidhri;

import com.android.androidhri.R;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;


import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UiView for controlling the cellbot with a Accelerator scheme layout.
 * 
 * @author css@google.com (Charles Spirakis)
 * @author keertip@google.com (Keerti Parthasarathy)
 */
public class AccelerometerView extends Activity {
	SensorManager mSensorManager = null;

	Sensor mSensorAccelerometer = null;

	SensorEventListener mSensorListener = null;

	AccelerometerTranslator mTranslator = null;

	PressHereView mPressHereImage = null;
	
	BluetoothAdapt mBluetoothAdapt;


	Context mContext = null;

	private boolean isDrawer = true;

	private class PressHereView extends ImageButton {
		// We are treating this as a shared variable. This class is the writer
		// and other classes are the reader. Since it can change
		// asynchronously, enforce get/set atomicity.
		public AtomicBoolean pressed = new AtomicBoolean(false);

		public PressHereView(Context context) {
			super(context);
			setImageDrawable(context.getResources().getDrawable(
					R.drawable.accelerometer));
			this.setBackgroundColor(Color.TRANSPARENT);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			int what = event.getAction();

			if (what == MotionEvent.ACTION_UP) {
				pressed.set(false);
			} else if (what == MotionEvent.ACTION_DOWN) {
				
				pressed.set(true);
			}
			return super.onTouchEvent(event);
		}
	}

	private class AccelerometerListener implements SensorEventListener {
		public static final long MAX_SAMPLE_RATE_MS = 10;

		private boolean mWasPressed = false;

		long mLastTimestampMs;

		private PressHereView mPress;

		private AccelerometerTranslator mAcceleratorTranslator;

		public AccelerometerListener(PressHereView phv,
				AccelerometerTranslator act) {
			super();
			mPress = phv;
			mAcceleratorTranslator = act;
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// Don't think we care...
		}

		public void onSensorChanged(SensorEvent se) {
			// We only care about the sensor events if the button is being
			// pressed. Want to only read the value once during this event as
			// mPress.pressed can change asynchronously and what we read now may
			// be different
			// than what we read later.
			boolean pressed = mPress.pressed.get();
			if (pressed) {
				// Now see if this is a transition to a new pressed state or
				// if it is part of a held pressed state.
				if (mWasPressed) {
					// Continuing a press...
					// Put in the MAX_SAMPLE_RATE_MS to allow others to get
					// processing time and as
					// a simple form of event filtering. This may not make sense
					// for the android. If
					// this app is the front app, let it use 100% of the cpu
					// reading sensors if it wants?
					long currentTimestampMs = se.timestamp / 1000000;
					long deltaTimestampMs = currentTimestampMs
							- mLastTimestampMs;
					if (deltaTimestampMs > MAX_SAMPLE_RATE_MS) {
						// When there is a change...
						AccelerometerTranslator.SensorData s = new AccelerometerTranslator.SensorData(
								currentTimestampMs, se.values[0], se.values[1],
								se.values[2]);
						mAcceleratorTranslator.movement(s);
					}
					mLastTimestampMs = currentTimestampMs;
				} else {
					// Transition from not-pressed to pressed.
					// Capture the values to use as a reference.
					AccelerometerTranslator.SensorData s = new AccelerometerTranslator.SensorData(
							se.timestamp / 1000000, se.values[0], se.values[1],
							se.values[2]);
					mAcceleratorTranslator.setBaseline(s);
				}
			} else {
				// transition from pressed to not-pressed
				if (mWasPressed) {
					mAcceleratorTranslator.stop();
				}
			}
			mWasPressed = pressed;
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.remote_drawer_with_directional_control_container);
		
		mBluetoothAdapt = new BluetoothAdapt(this);
		mPressHereImage = new PressHereView(this);

//		LayoutParams params = new LayoutParams(width, height);
//		setLayoutParams(params);
//
//		LayoutInflater inflate = LayoutInflater.from(this);
//
//		FrameLayout frameLayout = (FrameLayout) inflate
//				.inflate(
//						R.layout.remote_drawer_with_directional_control_container,
//						null);
//		addView(frameLayout);

		

		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.directionalController_container);
		linearLayout.addView(mPressHereImage);

		
		mSensorManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);
		mSensorAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


		mTranslator = new AccelerometerTranslator(this,mBluetoothAdapt,
				AccelerometerListener.MAX_SAMPLE_RATE_MS);
		mSensorListener = new AccelerometerListener(mPressHereImage,
				mTranslator);
		mSensorManager.registerListener(mSensorListener, mSensorAccelerometer,
				SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.comm_modality, menu);
		return true;
	}
	
	public void onSelect(View v){
		mBluetoothAdapt.sendData("d");
		Intent intent=new Intent(v.getContext(),CommReceiver.class);
    	startActivity(intent);
    	finish();
	}
	
    public void onReset(View v){
    	mBluetoothAdapt.sendData("c");
    	Intent intent = new Intent(v.getContext(),CommReceiver.class);
    	startActivity(intent);
    	finish();
    }
	
	protected void onPause(){
		super.onPause();
		mBluetoothAdapt.closeConnect();
	}
	
	
}
