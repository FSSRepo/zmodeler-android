package com.fastsmartsystem.saf;
import android.content.pm.*;
import android.os.*;
import com.forcex.*;
import com.forcex.io.*;
import com.forcex.utils.*;
import java.io.*;

import java.util.*;
import com.forcex.gui.widgets.*;
import com.forcex.app.threading.*;
import com.forcex.gui.*;

public class DebugInfo
{
	public static String info(ZModelerActivity app,boolean includeDump) {
		String info = "\nDebug Information:\n";
		try{
			PackageInfo inf = app.getPackageManager().getPackageInfo(app.getPackageName(),0);
			info += "App Version: "+inf.versionName+"\n";
			info += "Package: "+inf.packageName+"\n";
			info += "Android API: "+Build.VERSION.SDK_INT+"\n";
			info += "Device: "+Build.DEVICE+"\n";
			info += "GPU: "+FX.gpu.getGPUModel()+"\n";
			info += "OpenGL Version: "+FX.gpu.getOpenGLVersion()+"\n";
		}catch(Exception e){
			Logger.log(e);
		}
		return info;
	}
	
	ArrayList<String> debug = new ArrayList<>();
	boolean debugging = false;
	TextView tv;
	ImageView iv;
	
	public DebugInfo(){
		tv = new TextView(Zmdl.gdf());
		tv.setText("{|230,200,34:=Debug Printer|}\n");
		tv.setTextSize(0.04f);
		tv.setConstraintWidth(0.25f);
		tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		tv.setRelativePosition(0.75f,-0.6f);
		tv.setTextColor(255,255,255);
		tv.setVisibility(View.INVISIBLE);
		iv = new ImageView(-1,0.25f,0.4f);
		iv.setRelativePosition(0.75f,-0.6f);
		iv.setMixColor(new Color(0,0,0,90));
		iv.setVisibility(View.INVISIBLE);
		Zmdl.ctx().addUntouchableView(iv);
		Zmdl.ctx().addUntouchableView(tv);
	}
	
	public void setDebugging(boolean z){
		debugging = z;
		tv.setVisibility(z ? View.VISIBLE: View.INVISIBLE);
		iv.setVisibility(z ? View.VISIBLE: View.INVISIBLE);
	}
	
	public void back() {
		if(!debugging){
			return;
		}
		if(debug.size() > 1){
			debug.remove(debug.size() - 1);
		}
	}
	
	public void print(String text) {
		if(!debugging){
			return;
		}
		if(debug.size() == 9){
			debug.remove(0);
		}
		debug.add(text);
		String txt = "";
		for(String t : debug){
			txt += t + "\n";
		}
		tv.setText(txt);
	}
	
}
