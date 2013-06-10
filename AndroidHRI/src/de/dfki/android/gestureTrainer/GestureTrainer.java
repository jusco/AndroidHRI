/*
 * GestureTrainer.java
 *
 * Created: 18.08.2011
 *
 * Copyright (C) 2011 Robert Nesselrath
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package de.dfki.android.gestureTrainer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.androidhri.AccelerometerView;
import com.android.androidhri.BluetoothAdapt;
import com.android.androidhri.CommModality;
import com.android.androidhri.CommReceiver;
import com.android.androidhri.R;
import de.dfki.ccaal.gestures.IGestureRecognitionListener;
import de.dfki.ccaal.gestures.IGestureRecognitionService;
import de.dfki.ccaal.gestures.classifier.Distribution;

public class GestureTrainer extends Activity {

	IGestureRecognitionService recognitionService;
	String activeTrainingSet;
	BluetoothAdapt mBluetoothAdapt;
	Context mContext;
	
	public void onReset(View v){
    	mBluetoothAdapt.sendData("c");
    	Intent intent = new Intent(v.getContext(),CommReceiver.class);
    	startActivity(intent);
    	finish();
    }
	
	public void handleFinishing(){
		Intent intent=new Intent(this,AccelerometerView.class);
    	startActivity(intent);
    	finish();
	}
	
	public void handleFinishingAlt(){
		Intent intent=new Intent(this,CommReceiver.class);
    	startActivity(intent);
    	finish();
	}

	private final ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			recognitionService = IGestureRecognitionService.Stub.asInterface(service);
			try {
				recognitionService.startClassificationMode(activeTrainingSet);
				recognitionService.registerListener(IGestureRecognitionListener.Stub.asInterface(gestureListenerStub));
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			recognitionService = null;
		}
	};

	IBinder gestureListenerStub = new IGestureRecognitionListener.Stub() {

		@Override
		public void onGestureLearned(String gestureName) throws RemoteException {
			//Toast.makeText(GestureTrainer.this, String.format("Gesture %s learned", gestureName), Toast.LENGTH_SHORT).show();
			System.err.println("Gesture %s learned");
		}

		@Override
		public void onTrainingSetDeleted(String trainingSet) throws RemoteException {
			//Toast.makeText(GestureTrainer.this, String.format("Training set %s deleted", trainingSet), Toast.LENGTH_SHORT).show();
			System.err.println(String.format("Training set %s deleted", trainingSet));
		}

		@Override
		public void onGestureRecognized(final Distribution distribution) throws RemoteException {
			String output;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(GestureTrainer.this, String.format("%s: %f", distribution.getBestMatch(), distribution.getBestDistance()), Toast.LENGTH_LONG).show();
					if(distribution.getBestDistance() > 6){
						mBluetoothAdapt.sendData(distribution.getBestMatch());
						if (distribution.getBestMatch().equals("1") ||distribution.getBestMatch().equals("2"))
							handleFinishingAlt();
						else
							handleFinishing();
					}
					else{
						mBluetoothAdapt.sendData("0");
						handleFinishingAlt();
					}
					System.err.println(String.format("%s: %f", distribution.getBestMatch(), distribution.getBestDistance()));
				}
			});
			//Try going to accelerometerview
			
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		final TextView activeTrainingSetText = (TextView) findViewById(R.id.activeTrainingSet);
		final EditText trainingSetText = (EditText) findViewById(R.id.trainingSetName);
		final EditText editText = (EditText) findViewById(R.id.gestureName);
		activeTrainingSet = editText.getText().toString();
		final Button startTrainButton = (Button) findViewById(R.id.trainButton);
		final Button deleteTrainingSetButton = (Button) findViewById(R.id.deleteTrainingSetButton);
		final Button changeTrainingSetButton = (Button) findViewById(R.id.startNewSetButton);
		final SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar1);
		seekBar.setVisibility(View.INVISIBLE);
		seekBar.setMax(20);
		seekBar.setProgress(20);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

				try {
					recognitionService.setThreshold(progress / 10.0f);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

		startTrainButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (recognitionService != null) {
					try {
						if (!recognitionService.isLearning()) {
							startTrainButton.setText("Stop Training");
							editText.setEnabled(false);
							deleteTrainingSetButton.setEnabled(false);
							changeTrainingSetButton.setEnabled(false);
							trainingSetText.setEnabled(false);
							recognitionService.startLearnMode(activeTrainingSet, editText.getText().toString());
						} else {
							startTrainButton.setText("Start Training");
							editText.setEnabled(true);
							deleteTrainingSetButton.setEnabled(true);
							changeTrainingSetButton.setEnabled(true);
							trainingSetText.setEnabled(true);
							recognitionService.stopLearnMode();
						}
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		changeTrainingSetButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				activeTrainingSet = trainingSetText.getText().toString();
				activeTrainingSetText.setText(activeTrainingSet);

				if (recognitionService != null) {
					try {
						recognitionService.startClassificationMode(activeTrainingSet);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});

		deleteTrainingSetButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				AlertDialog.Builder builder = new AlertDialog.Builder(GestureTrainer.this);
				builder.setMessage("You really want to delete the training set?").setCancelable(true).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (recognitionService != null) {
							try {
								recognitionService.deleteTrainingSet(activeTrainingSet);
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				builder.create().show();
			}
		});
		mBluetoothAdapt = new BluetoothAdapt(this);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.edit_gestures:
			Intent editGesturesIntent = new Intent().setClass(this, GestureOverview.class);
			editGesturesIntent.putExtra("trainingSetName", activeTrainingSet);
			startActivity(editGesturesIntent);
			return true;

		default:
			return false;
		}
	}

	@Override
	protected void onPause() {
		try {
			recognitionService.unregisterListener(IGestureRecognitionListener.Stub.asInterface(gestureListenerStub));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		recognitionService = null;
		unbindService(serviceConnection);
		mBluetoothAdapt.closeConnect();
		super.onPause();
	}

	@Override
	protected void onResume() {
		Intent bindIntent = new Intent("de.dfki.ccaal.gestures.GESTURE_RECOGNIZER");
		bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		super.onResume();
	}
	
	
	
}