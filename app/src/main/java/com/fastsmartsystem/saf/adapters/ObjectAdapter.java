package com.fastsmartsystem.saf.adapters;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.*;
import com.forcex.gui.*;

public class ObjectAdapter extends ListAdapter<ObjectItem> {
	TextView tv_item;
	ImageView ic_item;
	float size = 0.045f;
	
	public ObjectAdapter(){
		super(Zmdl.ctx());
	}
	
	public void setTextSize(float sz){
		size = sz;
	}

	@Override
	protected void createView(Layout container) {
		container.setOrientation(Layout.HORIZONTAL);
		ic_item = new ImageView(-1,0.05f / getContext().getAspectRatio(),0.05f);
		ic_item.setMarginTop(0.02f);
		ic_item.setMarginLeft(0.01f);
		tv_item = new TextView(UIContext.default_font);
		tv_item.setTextSize(size);
		tv_item.setMarginTop(0.02f);
		tv_item.setMarginLeft(0.01f);
		tv_item.setMarginBottom(0.02f);
		container.add(ic_item);
		container.add(tv_item);
	}

	@Override
	protected void updateView(ObjectItem item, short position, Layout container) {
		ic_item.setTexture(item.icon);
		if(item.text.length() > 11){
			tv_item.setText(item.text.substring(0,10)+"...");
		}else{
			tv_item.setText(item.text);
		}
	}
}
