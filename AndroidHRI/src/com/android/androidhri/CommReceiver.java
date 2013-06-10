package com.android.androidhri;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class CommReceiver extends Activity {
	BluetoothAdapt mBluetoothAdapt;
	Button btn;
	Button btn2;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comm_receiver);
        btn=(Button)findViewById(R.id.button3);
        btn.setEnabled(false);
        btn2=(Button)findViewById(R.id.button4); 
        btn2.setEnabled(false);
        mBluetoothAdapt = new BluetoothAdapt(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.comm_receiver, menu);
        return true;
    }
    
    public void onSelectedRobot(View v){
    	v.setBackgroundColor(Color.BLUE);
    	btn.setEnabled(true);
    	btn2.setEnabled(true);
    	mBluetoothAdapt.sendData("r");      				
    }
    
    
    public void onSelectedSoldier(View v){
    	v.setBackgroundColor(Color.BLUE);
    	btn.setEnabled(true);
    	btn2.setEnabled(true);
    	mBluetoothAdapt.sendData("s");    	
   	}
    
    
    public void onSelectedSound(View v){
    	v.setBackgroundColor(Color.BLUE);    	
    	Intent intent=new Intent(v.getContext(),CommModality.class);
    	startActivity(intent);
    	finish();
    }
    
    public void onSelectedGesture(View v){
    	v.setBackgroundColor(Color.BLUE);  	
    	Intent intent=new Intent(v.getContext(),de.dfki.android.gestureTrainer.GestureTrainer.class);
    	startActivity(intent);
    	finish();
    }
    
    public void onReset(View v){
    	mBluetoothAdapt.sendData("c");
    	Intent intent = new Intent(v.getContext(),CommReceiver.class);
    	startActivity(intent);
    	finish();
    }
    
    public void onQuit(View v){
    	System.exit(0);
    }
    
    protected void onPause(){
		super.onPause();
		mBluetoothAdapt.closeConnect();
	}
    
}
