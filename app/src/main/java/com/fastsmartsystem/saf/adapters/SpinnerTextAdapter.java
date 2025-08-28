package com.fastsmartsystem.saf.adapters;
import com.forcex.gui.widgets.*;
import com.forcex.gui.*;
import com.fastsmartsystem.saf.*;

public class SpinnerTextAdapter extends ListAdapter<Spinner.SpinnerItem>
{

	TextView tv_item;

	public SpinnerTextAdapter(){
		super(Zmdl.ctx());
	}
	
	public void add(String item) {
		super.add(new Spinner.SpinnerItem(item));
	}

	@Override
	protected void createView(Layout container) {
		container.setOrientation(Layout.HORIZONTAL);
		tv_item = new TextView(getContext().default_font);
		tv_item.setTextSize(0.04f);
		tv_item.setMarginTop(0.01f);
		tv_item.setMarginLeft(0.01f);
		tv_item.setMarginBottom(0.01f);
		container.add(tv_item);
	}

	@Override
	protected void updateView(Spinner.SpinnerItem item, short position, Layout container) {
		tv_item.setText(item.text);
	}
	
}
