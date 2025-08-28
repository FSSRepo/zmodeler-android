package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.*;
import com.forcex.core.gpu.*;
import com.forcex.*;

public class SettingPanel extends PanelFragment
{
	Layout main;
	TextView tv_settings,tvSensibility;
	ToggleButton help,smooth_cam;
	ProgressBar pSens;
	float temp = 0;
	
	public SettingPanel(){
		main = Zmdl.lay(0.25f,false);
		tv_settings = new TextView(Zmdl.gdf());
		tv_settings.setTextSize(0.06f);
		tv_settings.setAlignment(Layout.CENTER);
		tv_settings.setMarginBottom(0.01f);
		tv_settings.setText(Zmdl.gt("settings"));
		main.add(tv_settings);
		help = new ToggleButton(Zmdl.gt("show_help_tips"),Zmdl.gdf(),0.2f,0.045f);
		help.setMarginTop(0.02f);
		help.setAlignment(Layout.CENTER);
		main.add(help);
		smooth_cam = new ToggleButton(Zmdl.gt("smooth_cam"),Zmdl.gdf(),0.2f,0.045f);
		smooth_cam.setMarginTop(0.02f);
		smooth_cam.setAlignment(Layout.CENTER);
		smooth_cam.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					Zmdl.rp().getCamera().setSmoothMovement(z);
					pSens.setVisibility(z ? View.VISIBLE : View.GONE);
					tvSensibility.setVisibility(z ? View.VISIBLE : View.GONE);
				}
		});
		main.add(smooth_cam);
		tvSensibility = new TextView(Zmdl.gdf());
		tvSensibility.setTextSize(0.045f); tvSensibility.setAlignment(Layout.CENTER);
		main.add(tvSensibility);
		pSens = new ProgressBar(0.2f,0.05f);
		pSens.setAlignment(Layout.CENTER); pSens.useSeekBar(true);
		pSens.setOnSeekListener(new ProgressBar.onSeekListener(){
				@Override
				public void seek(int id, float progress) {
					tvSensibility.setText(Zmdl.gt("sensibility_s",progress * 0.03f));
				}
				@Override
				public void finish(float final_progress) {
					Zmdl.sf().sensibility_cam = final_progress * 0.3f;
				}
		});
		pSens.setMarginTop(0.01f);
		main.add(pSens);
		help.setMarginBottom(0.01f);
		ToggleButton debug = new ToggleButton("DEBUG",Zmdl.gdf(),0.2f,0.045f);
		debug.setMarginTop(0.02f);
		debug.setAlignment(Layout.CENTER);
		debug.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					Zmdl.app().debug.setDebugging(z);
				}
			});
		main.add(debug);
		Layout main4 = Zmdl.lay(true);
		main4.setMarginTop(0.02f);
		Button btnAccept = new Button(Zmdl.gt("save"),Zmdl.gdf(),0.12f,0.04f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					Zmdl.sf().help = help.isToggled();
					Zmdl.sf().smooth_camera = smooth_cam.isToggled();
					Zmdl.sf().save();
					close();
				}
			});
		main4.add(btnAccept);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.12f,0.04f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					Zmdl.rp().getCamera().setSmoothMovement(Zmdl.sf().smooth_camera);
					Zmdl.sf().sensibility_cam = temp;
					close();
				}
			});
		btnCancel.setMarginLeft(0.01f);
		main4.setAlignment(Layout.CENTER);
		main4.add(btnCancel);
		main.add(main4);
	}

	public boolean showingThis(){
		return Zmdl.tlay(main);
	}

	public void requestShow(){
		help.setToggle(Zmdl.sf().help);
		smooth_cam.setToggle(Zmdl.sf().smooth_camera);
		temp = Zmdl.sf().sensibility_cam + 0.01f;
		pSens.setProgress(temp / 0.3f);
		pSens.setVisibility(Zmdl.sf().smooth_camera ? View.VISIBLE : View.GONE);
		tvSensibility.setVisibility(Zmdl.sf().smooth_camera ? View.VISIBLE : View.GONE);
		tvSensibility.setText(Zmdl.gt("sensibility_s",pSens.getProgress() * 0.03f));
		Zmdl.apl(main);
	}
	
	@Override
	public boolean isShowing() {
		return Zmdl.tlay(main);
	}

	@Override
	public void close() {
		if(isShowing()){
			Zmdl.app().panel.dismiss();
		}
	}
}
