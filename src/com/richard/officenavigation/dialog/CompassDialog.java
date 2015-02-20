package com.richard.officenavigation.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.richard.officenavigation.R;
import com.richard.officenavigation.constants.C;

public class CompassDialog extends Dialog implements
		android.view.View.OnClickListener, SensorEventListener {
	private String mTitle;
	private onConfirmOrientationListener mListener;
	private TextView mTvTitle, mTvHeading;
	private ImageView mIvCompass;
	private boolean mCreated;

	private SensorManager mSensorManager;
	private float[] aValues = new float[3];
	private float[] mValues = new float[3];
	private int rotation;

	private float currentDegree = 0f;
	private int mCurOri;

	public CompassDialog(Context context) {
		super(context);
	}

	public static CompassDialog newInstance(Context context, String title,
			onConfirmOrientationListener listener) {
		CompassDialog d = new CompassDialog(context);
		d.mTitle = title;
		d.mListener = listener;
		return d;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		View view = View.inflate(getContext(), R.layout.dialog_compass, null);
		setContentView(view);
		mTvTitle = (TextView) view.findViewById(R.id.title);
		mTvTitle.setText(mTitle);
		mTvHeading = (TextView) view.findViewById(R.id.tv_heading);
		mIvCompass = (ImageView) view.findViewById(R.id.iv_compass);
		view.findViewById(R.id.btn_confirm).setOnClickListener(this);
		view.findViewById(R.id.btn_cancel).setOnClickListener(this);

		mSensorManager = (SensorManager) getContext().getSystemService(
				Context.SENSOR_SERVICE);
		WindowManager wm = (WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		rotation = display.getRotation();
		updateOrientation(new float[] { 0, 0, 0 });
		mCreated = true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (((ContextThemeWrapper) getContext()).getResources()
				.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			Point outSize = new Point();
			WindowManager wm = (WindowManager) getContext().getSystemService(
					Context.WINDOW_SERVICE);
			wm.getDefaultDisplay().getSize(outSize);
			getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
					outSize.y / 2);
		}
		Sensor aSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor mSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorManager.registerListener(this, aSensor,
				SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mSensor,
				SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	protected void onStop() {
		super.onStop();
		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mValues = event.values;
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			aValues = event.values;
		updateOrientation(calculateOrientation());
	}

	private float[] calculateOrientation() {
		float[] values = new float[3];
		float[] inR = new float[9];
		float[] outR = new float[9];
		// Determine the rotation matrix
		SensorManager.getRotationMatrix(inR, null, aValues, mValues);
		// Remap the coordinates based on the natural device orientation.
		int x_axis = SensorManager.AXIS_X;
		int y_axis = SensorManager.AXIS_Y;
		switch (rotation) {
		case (Surface.ROTATION_90):
			x_axis = SensorManager.AXIS_Y;
			y_axis = SensorManager.AXIS_MINUS_X;
			break;
		case (Surface.ROTATION_180):
			y_axis = SensorManager.AXIS_MINUS_Y;
			break;
		case (Surface.ROTATION_270):
			x_axis = SensorManager.AXIS_MINUS_Y;
			y_axis = SensorManager.AXIS_X;
			break;
		default:
			break;
		}
		SensorManager.remapCoordinateSystem(inR, x_axis, y_axis, outR);
		// Obtain the current, corrected orientation.
		SensorManager.getOrientation(outR, values);
		// Convert from Radians to Degrees.
		values[0] = (float) Math.toDegrees(values[0]);
		values[1] = (float) Math.toDegrees(values[1]);
		values[2] = (float) Math.toDegrees(values[2]);
		return values;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	private void updateOrientation(float[] values) {
		float degree = Math.round(values[0]);
		if (-degree != currentDegree) {
			mTvHeading.setText(getContext()
					.getString(R.string.subtitle_heading)
					+ degreeToOrientation(degree));

			// create a rotation animation (reverse turn degree degrees)
			RotateAnimation ra = new RotateAnimation(currentDegree, -degree,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);

			// how long the animation will take place
			ra.setDuration(210);

			// set the animation after the end of the reservation status
			ra.setFillAfter(true);

			// Start the animation
			mIvCompass.startAnimation(ra);
			currentDegree = -degree;
		}
	}

	private String degreeToOrientation(float degree) {
		int oriStringId = 0;
		if (degree >= -22.5 && degree < 22.5) {
			oriStringId = R.string.ori_north;
			mCurOri = C.map.ORIENT_NORTH;
		} else if (degree >= 22.5 && degree < 67.5) {
			oriStringId = R.string.ori_northeast;
			mCurOri = C.map.ORIENT_NORTHEAST;
		} else if (degree >= 67.5 && degree <= 112.5) {
			oriStringId = R.string.ori_east;
			mCurOri = C.map.ORIENT_EAST;
		} else if (degree >= 112.5 && degree < 157.5) {
			oriStringId = R.string.ori_sortheast;
			mCurOri = C.map.ORIENT_SORTHEAST;
		} else if ((degree >= 157.5 && degree <= 180) || (degree) >= -180
				&& degree < -157.5) {
			oriStringId = R.string.ori_sorth;
			mCurOri = C.map.ORIENT_SORTH;
		} else if (degree >= -157.5 && degree < -112.5) {
			oriStringId = R.string.ori_sorthwest;
			mCurOri = C.map.ORIENT_SORTHWEST;
		} else if (degree >= -112.5 && degree < -67.5) {
			oriStringId = R.string.ori_west;
			mCurOri = C.map.ORIENT_WEST;
		} else if (degree >= -67.5 && degree < -22.5) {
			oriStringId = R.string.ori_northwest;
			mCurOri = C.map.ORIENT_NORTHWEST;
		}
		return getContext().getString(oriStringId);
	}

	public void setTitle(String text) {
		if (mCreated)
			mTvTitle.setText(text);
		else
			mTitle = text;
	}

	public interface onConfirmOrientationListener {
		void onConfirmOrientation(int orientation);
	}

	public void setOnConfirmSettingListener(
			onConfirmOrientationListener listener) {
		mListener = listener;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_confirm:
			handleConfirm();
			break;
		case R.id.btn_cancel:
			dismiss();
			break;
		}
	}

	private void handleConfirm() {
		if (mListener != null) {
			mListener.onConfirmOrientation(mCurOri);
		}
		dismiss();
	}
}
