package com.fastsmartsystem.saf.adapters;
import com.forcex.gui.widgets.*;
import com.forcex.gui.*;
import com.fastsmartsystem.saf.*;

public class MenuAdapter extends ListAdapter<MenuItem> {
	TextView tv_item;
	ImageView ic_item;
	
	public MenuAdapter(){
		super(Zmdl.ctx());
	}
	
	public void add(int texture,String text){
		super.add(new MenuItem(texture,text));
	}
	
	@Override
	protected void createView(Layout container) {
		container.setOrientation(Layout.HORIZONTAL);
		ic_item = new ImageView(-1,0.05f / getContext().getAspectRatio(),0.05f);
		ic_item.setMarginTop(0.02f);
		ic_item.setMarginLeft(0.01f);
		tv_item = new TextView(getContext().default_font);
		tv_item.setTextSize(0.055f);
		tv_item.setMarginTop(0.02f);
		tv_item.setMarginLeft(0.01f);
		tv_item.setMarginBottom(0.02f);
		container.add(ic_item);
		container.add(tv_item);
	}

	@Override
	protected void updateView(MenuItem item, short position, Layout container) {
		ic_item.setTexture(item.icon);
		tv_item.setText(item.text);
	}
}
