package com.fastsmartsystem.saf;
import com.forcex.gui.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.app.threading.*;
import com.forcex.utils.*;
import java.io.*;
import com.forcex.*;

public class Zmdl {
	private static ZModelerActivity.ZModelerApp app;
	private static int id_gen = 0x68618;
	
	public static Font gdf(){
		return app.ctx.default_font;
	}
	
	public static FileProcessor fp(){
		return app.file_proc;
	}
	
	public static void ns(){
		if(Zmdl.im().hasCurrentInstance()){
			Zmdl.inst().setSaveState(true);
		}
	}
	
	public static ZObject go(int kh){
		return app.getRenderProcessor().getObject(kh);
	}
	
	public static ZObject gos(){
		return app.getRenderProcessor().getSelected();
	}
	
	public static EditorProcessor ep(){
		return app.getEditorProcessor();
	}
	
	public static RenderProcessor rp(){
		return app.getRenderProcessor();
	}
	
	public static InstanceManager im() {
		return app.getInstanceManager();
	}
	
	public static TreeItemProcessor tip(){
		return app.tree_proc;
	}
	
	public static AddProcessor ap(){
		return app.add_proc;
	}
	
	public static ZModelerActivity.ZModelerApp app(){
		return app;
	}
	
	public static void svo(ZObject except,boolean z){
		app.getRenderProcessor().setVisibleOneObject(except,z);
	}
	
	public static boolean hs(){
		return app.getRenderProcessor().hasSelected();
	}
	
	public static String gt(String id,Object... data){
		return app.lang.get(id,data);
	}
	
	public static Layout lay(boolean hor){
		Layout layout = new Layout(Zmdl.ctx());
		layout.setId(id_gen);
		layout.setToWrapContent();
		layout.setOrientation(hor ? Layout.HORIZONTAL : Layout.VERTICAL);
		id_gen ++;
		return layout;
	}
	
	public static Layout lay(float width,boolean hor){
		Layout layout = new Layout(Zmdl.ctx());
		layout.setId(id_gen);
		layout.setToWrapContent();
		layout.setWidth(width);
		layout.setUseWidthCustom(true);
		layout.setOrientation(hor ? Layout.HORIZONTAL : Layout.VERTICAL);
		id_gen ++;
		return layout;
	}
	
	public static void adtsk(Task task){
		app.tasks.addTask(task);
	}
	
	public static boolean tlay(Layout main){
		return app.panel.isShowing() && app.panel.getContent().getId() == main.getId();
	}
	
	public static void apl(Layout main){
		app.panel.showWithContent(main);
	}
	
	public static UndoManager um(){
		return app.undo_manager;
	}
	
	public static UIContext ctx(){
		return app.ctx;
	}
	
	public static SettingFile sf(){
		return app.setting_file;
	}
	
	public static ZInstance inst(){
		return app.getInstanceManager().getCurrentInstance();
	}
	
	static void init(ZModelerActivity.ZModelerApp application){
		app = application;
		String[] ls = new File(FX.fs.homeDirectory).list();
		if(ls != null) {
			for(String s : ls) {
				if(s.endsWith(".col")) {
					new File(FX.fs.homeDirectory + s).delete();
				}
			}
		}
		ls = null;
	}
	
	static void destroy(){
		app = null;
	}
} 
