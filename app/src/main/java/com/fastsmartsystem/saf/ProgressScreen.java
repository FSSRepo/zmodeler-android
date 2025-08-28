package com.fastsmartsystem.saf;
import com.forcex.gui.widgets.*;
import com.forcex.gui.*;
import com.forcex.utils.*;

public class ProgressScreen {
	ProgressCircle progress_indicator;
	TextView tvText;
	ImageView ivBackground;
	
	public ProgressScreen(UIContext ctx){
		progress_indicator = new ProgressCircle(0.3f,0.95f,360);
		progress_indicator.setApplyAspectRatio(true);
		progress_indicator.setSpeedPerCycle(300);
		tvText = new TextView(ctx.default_font);
		tvText.setText("Prueba");
		tvText.setTextColor(255,255,255);
		tvText.setTextSize(0.06f);
		ivBackground = new ImageView(-1,1f,1f);
		ivBackground.setMixColor(new Color(0,0,0,80));
		ctx.addUntouchableView(ivBackground);
		ctx.addUntouchableView(tvText);
		ctx.addUntouchableView(progress_indicator);
		dismiss();
	}
	
	public boolean isVisible(){
		return progress_indicator.isVisible();
	}
	
	public void show(){
		progress_indicator.setVisibility(View.VISIBLE);
		ivBackground.setVisibility(View.VISIBLE);
		tvText.setVisibility(View.VISIBLE);
	}
	
	public void setIndeterminate(boolean z){
		progress_indicator.setIndeterminate(z);
	}
	
	float progress;
	
	public void setProgress(float progress){
		progress_indicator.setProgress(progress);
		this.progress = progress;
	}
	
	public void update() {
		if(isVisible()) {
			tvText.setText(String.format("Processing:\n%.2f",progress)+"%");
		}
	}
	
	public void dismiss(){
		progress_indicator.setVisibility(View.INVISIBLE);
		ivBackground.setVisibility(View.INVISIBLE);
		tvText.setVisibility(View.INVISIBLE);
	}
}
