package com.android.androidhri;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;

public class CommModality extends Activity {
	BluetoothAdapt mBluetoothAdapt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_comm_modality);
		mBluetoothAdapt = new BluetoothAdapt(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.comm_modality, menu);
		return true;
	}
	
	public void onFaceMe(View v) {
		v.setBackgroundColor(Color.BLUE);
		mBluetoothAdapt.sendData("f");
		Intent intent=new Intent(v.getContext(),CommReceiver.class);
    	startActivity(intent);
    	finish();
	}
	
	public void onFollowMe(View v){
		v.setBackgroundColor(Color.BLUE);
		mBluetoothAdapt.sendData("F"); 	
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
