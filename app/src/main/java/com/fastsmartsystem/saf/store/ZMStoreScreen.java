package com.fastsmartsystem.saf.store;
import android.app.*;
import android.os.*;
import com.fastsmartsystem.saf.*;
import com.forcex.app.threading.*;
import android.content.*;
import android.view.*;

public class ZMStoreScreen extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.store_splash);
		Zmdl.adtsk(new Task(){
				@Override
				public boolean execute() {
					try {
						Thread.sleep(2400);
					} catch (InterruptedException e)
					{}
					if(!isFinishing()){
						startOther();
					}
					return true;
				}
		});
	}
	
	private void startOther(){
		runOnUiThread(new Runnable(){
				@Override
				public void run() {
					Intent i = new Intent(getApplicationContext(),ZMStoreActivity.class);
					startActivity(i);
					finish();
				}
			});
	}
}
