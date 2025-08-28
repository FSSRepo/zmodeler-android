package com.fastsmartsystem.saf.adapters;
import com.forcex.gui.widgets.*;
import com.forcex.gui.*;
import com.fastsmartsystem.saf.*;

public class TextAdapter extends ListAdapter<String>
{
	TextView tv_item;

	public TextAdapter(){
		super(Zmdl.ctx());
	}

	@Override
	protected void createView(Layout container) {
		container.setOrientation(Layout.HORIZONTAL);
		tv_item = new TextView(UIContext.default_font);
		tv_item.setTextSize(0.04f);
		tv_item.setMarginTop(0.01f);
		tv_item.setMarginLeft(0.01f);
		tv_item.setMarginBottom(0.01f);
		container.add(tv_item);
	}

	@Override
	protected void updateView(String item, short position, Layout container) {
		tv_item.setText(item);
	}
	
}
