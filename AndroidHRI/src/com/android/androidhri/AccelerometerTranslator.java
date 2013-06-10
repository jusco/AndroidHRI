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


import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Handle the translation from accelerometer sensing to actual robot movement.
 * 
 * @author css@google.com (Charles Spirakis)
 */
public class AccelerometerTranslator {
	/**
	 * Simple data class to let us only hold onto the sensor values we care
	 * about.
	 */
	public static class SensorData {

		public long timestampMs;

		public float accelX;

		public float accelY;

		public float accelZ;

		SensorData(long ts, float x, float y, float z) {
			timestampMs = ts;
			accelX = x;
			accelY = y;
			accelZ = z;
		}
	}

	private static final float IDLE_SPEED_THRESHOLD_MMPS = 60;

	private static final double FIVE_DEGREES_IN_RADIANS = 0.0872664626;

	private Context mContext;


	private SensorData mInitialValues;

	private double mPressTanYZ;

	private double mPressTanXZ;

	private SensorData mLastValues;

	private long mLastRobotUpdateMs;

	private Vibrator mVibe;

	private long mMaxRateMs;

	// add units - currently millimeters/second.
	private float mSpeedX;

	private float mSpeedY;

	private float mSpeedZ;

	private int scount = 0;

	private int mcount = 0;
	
	private BluetoothAdapt mBluetoothAdapt;
	
	public AccelerometerTranslator(Context ct, BluetoothAdapt mBluetoothAdapt, long maxRateMs) {
		mContext = ct;
		mMaxRateMs = maxRateMs;
		this.mBluetoothAdapt = mBluetoothAdapt;
		mVibe = (Vibrator) ct.getSystemService(Context.VIBRATOR_SERVICE);
	}

	public void stop() {
		//Do something with this
	}

	/**
	 * Translate movement of the phone into movement of the robot. Currently we
	 * compute the instantaneous speed the of the device (in millimeters/second)
	 * based on the last two accelerometer values. If the absolute speed is
	 * below a threshold, then the assumption is that the accelerometer is
	 * mostly measuring values relative to gravity and we can treat the
	 * accelerometer values as an indication of rotation. If we integrated the
	 * speed over time, we could (in theory) determine relative position from
	 * the starting location and that may be interesting for some form of robot
	 * control (vs. using orientation). This would also (in theory) allow for
	 * 3-d control (moving the device up relative to the original location would
	 * translate into an up command to the robot). In this case, the orientation
	 * of the phone may not be relevant (only care about 3-d position for
	 * control) or it could indicate roll/pitch to the device. But that is
	 * something for later...
	 * 
	 * @param currentValues
	 *            The current accelerometer readings.
	 */
	public void movement(SensorData currentValues) {
		long deltaRobotUpdateMs = currentValues.timestampMs
				- mLastRobotUpdateMs;

		// Because we are looking at instantaneous speed, only bother checking
		// for work if the robot is capable of getting another input.

		// For the moment, assume the robot can accept a command every 100ms.
		if (deltaRobotUpdateMs >= 100) {
			// If the z accerometer is negative, it means the phone is facing
			// down.
			// For the moment, let the user know that the phone orientation is
			// "out of bounds" by vibrating the phone.
			if (currentValues.accelZ < 0) {
				// We get information every mMaxRateMs, so vibrate
				// until the next update.
				// mVibe.vibrate(mMaxRateMs);
				//mVibe.vibrate(40);
				mLastValues = currentValues;
				return;
			}

			long instantDeltaTimeMs = currentValues.timestampMs
					- mLastValues.timestampMs;
			float instantDeltaX = currentValues.accelX - mLastValues.accelX;
			float instantDeltaY = currentValues.accelY - mLastValues.accelY;
			float instantDeltaZ = currentValues.accelZ - mLastValues.accelZ;

			// acceleration is m/s^2 so multiple by seconds to get to m/s
			// Since we actually have milliseconds, divide by 1000 to get
			// seconds and to get millimeters/second, multiple by 1000.
			// Net result, just multiply the two values together to get
			// millimeters/second velocity.
			mSpeedX = instantDeltaX * instantDeltaTimeMs;
			mSpeedY = instantDeltaY * instantDeltaTimeMs;
			mSpeedZ = instantDeltaZ * instantDeltaTimeMs;

			if ((Math.abs(mSpeedX) < IDLE_SPEED_THRESHOLD_MMPS)
					&& (Math.abs(mSpeedY) < IDLE_SPEED_THRESHOLD_MMPS)) {
				long relativeTimeMs = currentValues.timestampMs
						- mInitialValues.timestampMs;

				/**
				 * As long as the phone display is facing up, the atan XZ and
				 * atan YZ values correspond to orientation's roll and pitch.
				 * Once the phone display is upside down (and the z
				 * accelerometer value becomes negative), the XZ/YZ and
				 * roll/pitch values differ. There is a break that happens when
				 * z becomes negative. The code above blocks that out, so we
				 * only need to deal with values from -90 to 0 to 90 (as values
				 * above 90 means the phone is at least partially upside down).
				 */

				// This is the current angle (in radians).
				double currentTanYZ = Math.atan(currentValues.accelY
						/ -currentValues.accelZ);
				double currentTanXZ = Math.atan(currentValues.accelX
						/ -currentValues.accelZ);

				/**
				 * The difference between the two angles. If the pitch is
				 * increasing, that means speed the robot up. If the pitch is
				 * decreasing, that means slow the robot down. A positive pitch
				 * means the robot should be moving forward and a negative pitch
				 * means the robot should move backwards. If the roll is
				 * increasing, that means speed up the robots turn to the right
				 * (aka slow the robots turn to the left). If the roll is
				 * decreasing, that means slow the robots turn to the left (aka
				 * speed up the robots turn to the right). For roll 0 should
				 * indicate straight movement and negative values should
				 * indicate some form of left turn while positive roll values
				 * should indicate some form of right turn.
				 */
				// Relative angle (in radians) from start
				double relPitch = currentTanYZ - mPressTanYZ;
				double relRoll = currentTanXZ - mPressTanXZ;

				int speed = convertRadiansToSpeed(relPitch);
				int turn = convertRadiansToTurn(relRoll, relPitch >= 0);
		
				
				//TODO 
				//Send turn and speed data
//				String output = null;
//				if ((turn > -30  && turn < 0) ||(turn>=0 && turn < 30))
//					output= "q"; 
//				else if ( turn >=30 && turn <=160)
//					output= "w"; 
//				else if ((turn > 160 && turn <=180) || (turn >= -180 && turn < -160))
//					output= "e"; 
//				else if (turn >= -160 && turn <=-30)
//					output= "r"; 
//				else
//					output = "g";	
				
				//Under this scheme we make all angles positive to account for unsigned byte
				turn +=180 ;
				turn = turn/3; // So it fits within a byte
				
				
				byte b =(byte)turn;
				byte [] data = {b};
//				mBluetoothAdapt.sendData(Integer.toString(turn));
				//Then we send the Byte
				if (turn!=100) //if turn equals 100 then we have selected
					mBluetoothAdapt.sendBytes(data);
				mcount = mcount + 1;
				if (mcount > 5) {
					//Logging

					mcount = 0;
				}

				// Only update the lastUpdate if we actually sent a command to
				// the robot.
				mLastRobotUpdateMs = currentValues.timestampMs;
			}

			scount = scount + 1;
			if (scount > 10) {
				System.out.println("deltaTs: " + instantDeltaTimeMs);
				System.out.println( " deltax: " + instantDeltaX);
				System.out.println( " deltay: " + instantDeltaY);
				System.out.println( " deltaz: " + instantDeltaZ);
				System.out.println( " speedx: " + mSpeedX);
				System.out.println( " speedy: " + mSpeedY);
				System.out.println( " speedz: " + mSpeedZ);
				System.out.println( " valx: " + currentValues.accelX);
				System.out.println( " valy: " + currentValues.accelY);
				System.out.println( " valz: " + currentValues.accelZ);

				scount = 0;
			}
		}

		mLastValues = currentValues;
	}

	public void setBaseline(SensorData initial) {
		mInitialValues = initial;
		mLastValues = initial;

		// This is the angle (in radians) at which the button was
		// pressed.
		mPressTanYZ = Math.atan(mInitialValues.accelY / -mInitialValues.accelZ);
		mPressTanXZ = Math.atan(mInitialValues.accelX / -mInitialValues.accelZ);

		System.out.println( "initx: " + initial.accelX);
		System.out.println( "inity: " + initial.accelY);
		System.out.println( "initz: " + initial.accelZ);
	}

	/*
	 * Assuming a typical user holds the phone at a 30 degree angle (for pitch)
	 * and assuming we want to max out acceleration at 85 degrees, that means we
	 * have about 50 degrees of motion that scales from 0 to 100% of motor
	 * control. Let's define: 0-5 as fidget and stopped (speed 0) 5-10 as slow
	 * (linear 0 - 10%) 10-35 as regular (linear 10-70%) 35-50 as fast (linear
	 * 70-100%) 50 + max
	 * 
	 * @param deltaRadians angle to use in radians
	 */
	protected int convertRadiansToSpeed(double angleRadians) {
		double absAngle = Math.abs(angleRadians);
		double delta;
		int result;

		if (absAngle < FIVE_DEGREES_IN_RADIANS) {
			// 0-5 degrees
			return 0;
		} else if (absAngle < (FIVE_DEGREES_IN_RADIANS * 2)) {
			// 5-10 degrees
			delta = (absAngle - FIVE_DEGREES_IN_RADIANS)
					/ FIVE_DEGREES_IN_RADIANS;
			result = (int) (delta * 10);
		} else if (absAngle < (FIVE_DEGREES_IN_RADIANS * 7)) {
			// 10-35 degrees
			delta = (absAngle - (FIVE_DEGREES_IN_RADIANS * 2))
					/ (FIVE_DEGREES_IN_RADIANS * 5);
			result = (int) (delta * 60) + 10;
		} else if (absAngle < (FIVE_DEGREES_IN_RADIANS * 10)) {
			// 35-50 degrees
			delta = (absAngle - (FIVE_DEGREES_IN_RADIANS * 7))
					/ (FIVE_DEGREES_IN_RADIANS * 3);
			result = (int) (delta * 30) + 70;
		} else {
			// 50+ degrees
			result = 100;
			mVibe.vibrate(40);
		}

		return result;
	}

	/*
	 * Assuming a typical user holds the phone level (for roll) and assuming we
	 * want to max out acceleration at 85 degrees, that means we have about 85
	 * degrees of motion that scales from 0 to 180 degrees of motor turn. Let's
	 * define 0-5 as fidget and stopped (speed 0) 5-10 as slow (linear 0 - 10%)
	 * 10-40 as regular (linear 10-70%) 40-85 as fast (linear 70-100%) 85 + max
	 * 
	 * @param deltaRadians angle to use in radians
	 */
	protected int convertRadiansToTurn(double angleRadians, boolean goForward) {
		double absAngle = Math.abs(angleRadians);
		double delta;
		double percentage;

		if (absAngle < FIVE_DEGREES_IN_RADIANS) {
			// 0-5 degrees
			percentage = 0;
		} else if (absAngle < (FIVE_DEGREES_IN_RADIANS * 2)) {
			// 5-10 degrees
			delta = (absAngle - FIVE_DEGREES_IN_RADIANS)
					/ FIVE_DEGREES_IN_RADIANS;
			percentage = (delta * 10);
		} else if (absAngle < (FIVE_DEGREES_IN_RADIANS * 8)) {
			// 10-40 degrees
			delta = (absAngle - (FIVE_DEGREES_IN_RADIANS * 2))
					/ (FIVE_DEGREES_IN_RADIANS * 6);
			percentage = (delta * 60) + 10;
		} else if (absAngle < (FIVE_DEGREES_IN_RADIANS * 17)) {
			// 40-85 degrees
			delta = (absAngle - (FIVE_DEGREES_IN_RADIANS * 8))
					/ (FIVE_DEGREES_IN_RADIANS * 9);
			percentage = (delta * 30) + 70;
		} else {
			// 85+ degrees
			percentage = 100;
			mVibe.vibrate(40);
		}

		// Convert % of turn into absolute 0 - 90.
		int result = (int) (((percentage * 90) / 100) + 0.5);

		if (goForward) {
			if (angleRadians < 0) {
				result = 0 - result;
			}
		} else {
			if (angleRadians < 0) {
				result = -180 + result;
			} else {
				result = 180 - result;
			}
		}

		return result;
	}
}
