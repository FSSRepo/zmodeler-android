package com.fastsmartsystem.saf.processors;
import com.forcex.gui.widgets.*;
import com.forcex.math.*;
import com.fastsmartsystem.saf.*;
import com.forcex.utils.*;

public class SphereInfluencer extends ProgressCircle {
	Vector2f position;
	float w,h;
	
	public SphereInfluencer() {
		super(0.2f,0.96f,100);
		setApplyAspectRatio(true);
		setIndeterminate(true);
		setProgress(20);
		setIndeterminate(true);
		setSpeedPerCycle(360);
		position = new Vector2f();
	}
	
	public void begin(float x,float y) {
		position.set(x,y);
		w = getWidth() / Zmdl.rp().getView().getWidth();
		h = (getHeight() * getContext().getAspectRatio()) / Zmdl.rp().getView().getHeight();
	}
	
	public boolean testRect(float x,float y){
		if(x < -1.0f || x > 1.0f || y < -1.0f || y > 1.0f) {
			return false;
		}
		return 
			x >= (position.x - w) && 
			x <= (position.x + w) &&  
			y >= (position.y - h) && 
			y <= (position.y + h);
	}
}
