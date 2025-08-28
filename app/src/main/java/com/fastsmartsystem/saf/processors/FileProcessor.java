package com.fastsmartsystem.saf.processors;
import com.fastsmartsystem.saf.*;
import com.fastsmartsystem.saf.adapters.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.loaders.*;
import com.forcex.*;
import com.forcex.app.threading.*;
import com.forcex.core.gpu.*;
import com.forcex.gfx3d.*;
import com.forcex.gtasdk.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.forcex.utils.*;
import java.io.*;
import java.util.*;
import android.os.*;
import android.util.Log;

import com.forcex.core.*;

public class FileProcessor extends PanelFragment implements ListView.OnItemClickListener,FileDialog.OnResultListener {
	Layout main;
	ListView menu, menu2;
	boolean export = false;
	OutputStream android_output_stream;
	
	public static void copy_temp(InputStream is, String path) {
		try
		{
			FileOutputStream os = new FileOutputStream(path);
			byte[] buffer = new byte[is.available()];
			is.read(buffer);
			os.write(buffer);
			is.close();
			os.close();
		}
		catch (Exception e)
		{
			Toast.info(e.toString(), 10);
		}
	}
	
	public void copy_out(String path) {
		try
		{
			FileInputStream is = new FileInputStream(path);
			byte[] buffer = new byte[is.available()];
			is.read(buffer);
			android_output_stream.write(buffer);
			is.close();
			android_output_stream.close();
			android_output_stream = null;
			new File(path).delete();
		}
		catch (Exception e)
		{
			Toast.info(e.toString(), 10);
		}
	}
	
	public FileProcessor(){
		main = Zmdl.lay(false);
		MenuAdapter adapter = new MenuAdapter();
		adapter.add(Texture.load("zmdl/folderzmdl.png"),Zmdl.gt("open"));
		adapter.add(Texture.load("zmdl/save.png"),Zmdl.gt("save"));
		adapter.add(Texture.load("zmdl/import.png"),Zmdl.gt("import"));
		adapter.add(Texture.load("zmdl/export.png"),Zmdl.gt("export"));
		adapter.add(Texture.load("zmdl/close.png"),Zmdl.gt("close"));
		adapter.add(Texture.load("zmdl/exit.png"),Zmdl.gt("exit"));
		menu = new ListView(0.25f,0.6f,adapter);
		menu.setOnItemClickListener(this);
		MenuAdapter adapter2 = new MenuAdapter();
		adapter2.add(Texture.load("zmdl/dff.png"),"DFF");
		adapter2.add(Texture.load("zmdl/obj.png"),"OBJ");
		adapter2.add(Texture.load("zmdl/3ds.png"),"3DS");
		menu2 = new ListView(0.25f,0.5f, adapter2);
		menu2.setOnItemClickListener(this);
		menu2.setVisibility(View.GONE);
		menu.setInterlinedColor(210,210,210,210);
		menu2.setInterlinedColor(210,210,210,210);
		main.add(menu2);
		main.add(menu);
	}
	
	public void requestShow(){
		if(!Zmdl.app().panel.isShowing() || Zmdl.app().panel.isShowing() && Zmdl.app().panel.getContent().getId() != main.getId()){
			Zmdl.apl(main);
		}
		menu.setVisibility(View.VISIBLE);
		menu2.setVisibility(View.GONE);
	}

	@Override
	public boolean tryCancel(short id){
		return true;
	}

	@Override
	public void open(short id, String path) {
		switch(id){
			case 0x12:
				Zmdl.adtsk(new LoadDFF(path));
				break;
			case 0x13:
				importOBJ(path);
				break;
			case 0x14:
				Zmdl.adtsk(new LoadGeneric(path,true,-1));
				break;
			case 0x15:
				Zmdl.adtsk(new SaveDFF(path + "/"));
				break;
			case 0x16:
				saveOBJ(path+"/");
				break;
			case 0x17:
				Zmdl.adtsk(new LoadZFile(path));
				break;
			case 0x18:
				Zmdl.adtsk(new SaveZFile(path+"/"));
				break;
		}
	}
	
	private void importOBJ(final String path){
		final ArrayList<OBJStream.OBJItem> items = OBJStream.getObjectList(path);
		Log.i("OBJSTREAM", path);
		if(items.size() >= 1) {
			FX.gpu.queueTask(() -> {
				Layout lay = Zmdl.lay(false);
				MenuAdapter mn = new MenuAdapter();
				int geom_ic = Texture.load("zmdl/geom_ic.png");
				for(int i = 0;i < items.size();i++) {
					mn.add(geom_ic,items.get(i).name);
				}
				ListView lv = new ListView(0.24f,0.24f, mn);
				lv.setApplyAspectRatio(true);
				lay.add(lv);
				final Dialog diag = new Dialog(lay);
				lv.setOnItemClickListener(new ListView.OnItemClickListener(){
					@Override
					public void onItemClick(ListView view, Object item, short position, boolean longclick) {
						diag.dismiss();
						Zmdl.adtsk(new LoadGeneric(path,false,items.get(position).lineIndex));
						items.clear();
					}
				});
				lay.add(lv);
				diag.setTitle(Zmdl.gt("select"));
				diag.setThisPriority();
				diag.show();
				return true;
			});
		}else{
			Zmdl.adtsk(new LoadGeneric(path,false,0));
		}
	}
	
	@Override
	public void onItemClick(ListView view, Object item, short position, boolean longclick) {
		if(menu == view) {
			switch(position){
				case 0:
					createFileDialog(Zmdl.gt("select")+" DFF",".dff",0x12);
					Zmdl.app().panel.dismiss();
					break;
				case 1:
					if(!Zmdl.im().hasCurrentInstance()){
						Zmdl.app().panel.dismiss();
						break;
					}
					FileDialog.create(Zmdl.ctx(),Zmdl.gt("select_folder"),getCurrentPath(),this,Zmdl.app().lang,0x15);
					Zmdl.app().panel.dismiss();
					break;
				case 2:
					menu.setVisibility(View.GONE);
					menu2.setVisibility(View.VISIBLE);
					export = false;
					break;
				case 3:
					if(!Zmdl.im().hasCurrentInstance()){
						Zmdl.app().panel.dismiss();
						break;
					}
					menu.setVisibility(View.GONE);
					menu2.setVisibility(View.VISIBLE);
					export = true;
					break;
				case 4:
					Zmdl.im().closeInstance();
					Zmdl.app().panel.dismiss();
					break;
				case 5:
					FX.device.destroy();
					break;
			}
		}else{
			switch(position){
				case 0:
					if(!export){
						createFileDialog(Zmdl.gt("select")+" DFF",".dff",0x12);
					}else{
						createSaveFileDialog(Zmdl.gt("select_folder"),".dff",0x15);
					}
					break;
				case 1:
					if(!export){
						createFileDialog(Zmdl.gt("select")+" OBJ",".obj",0x13);
					}else{
						createSaveFileDialog(Zmdl.gt("select_folder"),".obj",0x16);
					}
					break;
				case 2:
					if(!export){
						createFileDialog(Zmdl.gt("select")+" 3DS",".3ds",0x14);
					}else{
						Toast.error(Zmdl.gt("n_impl"),5f);
					}
					break;
			}
			Zmdl.app().panel.dismiss();
		}
	}
	
	private void saveOBJ(final String path){
		if(!Zmdl.im().hasCurrentInstance()){
			return;
		}
		final ZInstance inst = Zmdl.inst();
		if(inst.type == 1){
			// select geometry
			Layout lay = Zmdl.lay(false);
			MenuAdapter mn = new MenuAdapter();
			int geom_ic = Texture.load("zmdl/geom_ic.png");
			for(int i = 0;i < inst.model_hashes.size();i++) {
				mn.add(geom_ic,inst.model_hashes.get(i).name);
			}
			ListView lv = new ListView(0.24f,0.24f,mn);
			lv.setApplyAspectRatio(true);
			lay.add(lv);
			final Dialog diag = new Dialog(lay);
			lv.setOnItemClickListener((view, item, position, longclick) -> {
				diag.dismiss();
				MenuItem mn1 = (MenuItem)item;
				ZObject o = Zmdl.go(inst.model_hashes.get(position).model_hash);
				showExportOBJOptions(path,o.getMesh(), mn1.text);
			});
			lay.add(lv);
			diag.setTitle(Zmdl.gt("select"));
			diag.setThisPriority();
			diag.show();
		}else if(inst.type == 2 || inst.type == 3){
			showExportOBJOptions(path,((ZObject)inst.obj).getMesh(),inst.name);
		}
	}
	
	public String getCurrentPath() {
		if(Zmdl.im().hasCurrentInstance()){
			return Zmdl.inst().path;
		}
		return FX.fs.homeDirectory;
	}

	public static void showExportOBJOptions(final String path,final Mesh obj,final String name){
		final OBJStream.OBJParams params = new OBJStream.OBJParams();
		Layout lay = Zmdl.lay(false);
		ToggleButton btnIMat = new ToggleButton(Zmdl.gt("include_materials"),Zmdl.gdf(),0.3f,0.05f);
		btnIMat.setToggle(true);
		params.export_materials = true;
		btnIMat.setTextSize(0.045f);
		btnIMat.setOnToggleListener((v, z) -> params.export_materials = z);
		ToggleButton btnICre = new ToggleButton(Zmdl.gt("include_creator"),Zmdl.gdf(),0.3f,0.05f);
		btnICre.setToggle(false);
		btnICre.setTextSize(0.045f);
		btnICre.setMarginTop(0.02f);
		final EditText etCreator = new EditText(Zmdl.ctx(),0.3f,0.05f,0.05f);
		etCreator.setVisibility(View.GONE);
		final EditText etOBJName = new EditText(Zmdl.ctx(),0.3f,0.05f,0.05f);
		etOBJName.setText(name);
		etOBJName.setHint(Zmdl.gt("obj_name"));
		etOBJName.setMarginTop(0.02f);
		btnICre.setOnToggleListener((v, z) -> {
			params.export_creator = z;
			etCreator.setVisibility(z ? View.VISIBLE : View.GONE);
		});
		Button btnOK = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.1f,0.045f);
		btnOK.setTextSize(0.045f);
		btnOK.setMarginTop(0.02f);
		etCreator.setHint(Zmdl.gt("creator"));
		etCreator.setMarginTop(0.02f);
		btnOK.setAlignment(Layout.CENTER);
		lay.add(btnIMat); lay.add(btnICre); lay.add(etCreator); lay.add(etOBJName); lay.add(btnOK);
		final Dialog diag = new Dialog(lay);
		diag.setTitle(Zmdl.gt("export_options")+" ("+name+")");
		diag.setThisPriority();
		btnOK.setOnClickListener(view -> {
			params.creator = etCreator.getText();
			diag.dismiss();
			Zmdl.adtsk(new SaveOBJ(obj,etOBJName.getText(),path,params));
		});
		diag.show(0,0.5f);
	}
	
	private class LoadDFF implements Task, OnDFFStreamingListener {
		String path;
		ArrayList<String> err_stack = new ArrayList<String>();
		boolean notified = false;
		
		public LoadDFF(String path){
			this.path = path;
		}
		
		@Override
		public boolean execute() {
			DFFSDK result = DFFStream.readDFF(path,this,Zmdl.app().lang, FX.fs.homeDirectory);
			Zmdl.app().getProgressScreen().show();
			notified = false;
			if(result != null) {
				try{
					result.getFrameRoot().rotation.identity();
					final ZInstance inst = new ZInstance();
					inst.name = result.name;
					inst.setObject(result, 1);
					inst.id = Zmdl.im().genID();
					inst.path = new File(path).getParent();
					for(int i = 0;i < result.geometryCount;i++){
						onStreamPrint(Zmdl.gt("loading")+" "+i+"/"+result.geometryCount);
						onStreamProgress(100f * ((float)i/result.geometryCount));
						ZObject obj = (ZObject)result.getObject(new ZObject(),i,false);
						if(obj == null){
							onStreamError(Zmdl.gt("model_damaged"),true);
							Zmdl.app().getProgressScreen().dismiss();
							return true;
						}
						Zmdl.app().getTextureManager().update(obj.getMesh(),inst.path);
						obj.setID(Zmdl.im().genID());
						if(inst.getModelHash(obj.getName()) == -1){
							inst.addHash(obj.getName(),obj.getID());
						}else if(!notified){
							Toast.error(Zmdl.gt("critical_error_mn"),10f);
							Toast.setCancellable(false);
							notified = true;
						}
						result.getFrame(result.geom.get(i).frameIdx).model_id = obj.getID();
						if(obj.getName().endsWith("_vlo")){
							obj.setVisible(false);
						}
						result.geom.get(i).model_id = obj.getID();
						Zmdl.rp().queue.add(obj);
					}
					if(result.isSkin()){
						inst.skeleton = result.getSkeleton(result.getFrameRoot());
					}
					inst.root = setTreeNodes(result,result.getFrameRoot());
					inst.error_stack = err_stack;
					Zmdl.im().add(inst);
					Zmdl.im().setInstanceCurrent(inst);
					Zmdl.rp().rewind();
					Toast.info(Zmdl.gt("loaded"),3f);
				}catch(Exception e){
					Logger.log(e);
					FX.gpu.queueTask(() -> {
						Zmdl.app().sendEmailDialog(true);
						return true;
					});
				}
			}
			Zmdl.app().getProgressScreen().dismiss();
			return true;
		}
		
		@Override
		public void onStreamPrint(final String log) {
		}

		@Override
		public void onStreamProgress(final float progress) {
			Zmdl.app().getProgressScreen().setProgress(progress);
		}

		@Override
		public void onStreamError(String err, boolean stop) {
			if(stop){
				Toast.error(err,40f);
				Toast.setCancellable(false);
				FX.gpu.queueTask(() -> {
					Zmdl.app().sendEmailDialog(true);
					return true;
				});
			}else{
				if(!err_stack.contains(err)){
					Toast.warning(err,5f);
					Toast.setCancellable(false);
					err_stack.add(err);
				}
			}
		}
	}
	
	private class LoadZFile implements Task {
		String path;
		
		public LoadZFile(String path){
			this.path = path;
		}

		@Override
		public boolean execute() {
			ZContainer sdk = ZFileStream.read(path);
			if(sdk != null) {
				Toast.info(Zmdl.gt("finished"),4f);
			}else{
				Toast.error(Zmdl.gt("zpk_fail"),4f);
			}
			return true;
		}
	}
	
	private void createFileDialog(String title,final String ext,final int id) {
		if(FX.device.getAndroidVersion() < Build.VERSION_CODES.P) {
			FileDialog diag = FileDialog.create(Zmdl.ctx(),title, getCurrentPath(), ext, this, Zmdl.app().lang, id);
			diag.addExtensionIcon(".dff",Texture.load("zmdl/dff.png"));
			diag.addExtensionIcon(".obj",Texture.load("zmdl/obj.png"));
			diag.addExtensionIcon(".3ds",Texture.load("zmdl/3ds.png"));
		} else {
			FX.device.invokeFileChooser(true, title, "", new SystemDevice.OnAndroidFileStream() {
					@Override
					public void open(InputStream is,String name) {
						if(!name.endsWith(ext)) {
							Toast.error("El formato debe ser " + ext,4);
							return;
						}
						copy_temp(is, FX.fs.homeDirectory + name);
						FileProcessor.this.open((short)id, FX.fs.homeDirectory + name);
					}
					
					@Override
					public void save(OutputStream os) {
						
					}
			});
		}
	}
	
	private void createSaveFileDialog(String title,final String ext,final int id){
		if(FX.device.getAndroidVersion() < Build.VERSION_CODES.P) {
			FileDialog.create(Zmdl.ctx(),title, getCurrentPath(), this, Zmdl.app().lang, id);
		} else {
			FX.device.invokeFileChooser(false, title, Zmdl.inst().name + ext, new SystemDevice.OnAndroidFileStream() {
					@Override
					public void open(InputStream is,String name) {
						
					}
					@Override
					public void save(OutputStream os) {
						android_output_stream = os;
						FileProcessor.this.open((short)id, FX.fs.homeDirectory);
					}
				});
		}
	}
	public static Znode setTreeNodes(DFFSDK dff,DFFFrame frame){
		Znode root = new Znode();
		root.name = frame.name;
		root.model_kh = frame.model_id;
		root.isGeometry = frame.geoAttach != -1;
		root.frame_idx = (short)dff.indexOfFrame(frame.name);
		root.geo_idx = (short)dff.indexOfGeometry(frame.name);
		for(DFFFrame node : frame.children){
			root.addChild(setTreeNodes(dff,node));
		}
		return root;
	}
	
	private class LoadGeneric implements Task,onLoadListener {
		String path;
		boolean is3ds;
		int obj_line;
		
		public LoadGeneric(String path,boolean is3ds,int obj_line){
			this.path = path;
			this.is3ds = is3ds;
			this.obj_line = obj_line;
		}

		@Override
		public boolean execute() {
			Zmdl.app().getProgressScreen().show();
			String name = new File(path).getName().replace(is3ds ? ".3ds":".obj","");
			Object result = null;
			if(is3ds){
				result = new Load3DS().read(path,this,Zmdl.app().lang);
			}else{
				result = new OBJStream().read(path,this,obj_line,Zmdl.app().lang);
			}
			if(result != null){
				ZObject obj = null;
				if(is3ds) {
					obj = (ZObject)result;
				} else {
					obj = new ZObject((Mesh)result);
				}
				ZInstance inst = new ZInstance();
				inst.name = name;
				inst.path = new File(path).getParent() + "/";
				obj.setName(name);
				Zmdl.app().getTextureManager().update(obj.getMesh(),inst.path);
				inst.id = Zmdl.im().genID();
				Zmdl.im().add(inst);
				inst.setObject(obj,is3ds ? 3 : 2);
				obj.setID(Zmdl.im().genID());
				inst.addHash(name,obj.getID());
				Zmdl.rp().queue.add(obj);
				Znode root = new Znode();
				root.name = inst.name;
				root.isGeometry = true;
				root.geo_idx = 0;
				root.frame_idx = 0;
				root.model_kh = obj.getID();
				inst.root = root;
				Zmdl.im().setInstanceCurrent(inst);
				Toast.info(Zmdl.gt("loaded"),3f);
				Zmdl.rp().rewind();
			}
			Zmdl.app().getProgressScreen().dismiss();
			return true;
		}

		@Override
		public void progress(float progress) {
			Zmdl.app().getProgressScreen().setProgress(progress);
		}

		@Override
		public void error(String err) {
			Toast.error(err,5f);
		}
	}
	
	private static class SaveOBJ implements Task,onLoadListener {
		private String path;
		private Mesh inst;
		private String name;
		OBJStream.OBJParams params;
		
		public SaveOBJ(Mesh inst,String name,String path,OBJStream.OBJParams params){
			this.path = path;
			this.inst = inst;
			this.name = name;
			this.params = params;
		}

		@Override
		public boolean execute() {
			if(inst != null){
				Zmdl.app().getProgressScreen().show();
				Toast.info(Zmdl.gt("exporting"),3f);
				if(OBJStream.write(path,this,Zmdl.app().lang,inst,name,params)){
					Toast.info(Zmdl.gt("saved"),3f);
					Zmdl.inst().setSaveState(false);
				}
				Zmdl.app().getProgressScreen().dismiss();
				if(Zmdl.fp().android_output_stream != null) {
					Zmdl.fp().copy_out(path + name + ".obj");
				}
			}
			return true;
		}

		@Override
		public void progress(float progress) {
			Zmdl.app().getProgressScreen().setProgress(progress);
		}
		
		@Override
		public void error(String err) {
			Toast.error(err,8f);
		}
	}
	
	public static class SaveDFF implements Task,OnDFFStreamingListener {
		private String path;
		private DFFSDK dff;
		
		public SaveDFF(String path){
			this.path = path;
		}
		
		public SaveDFF(String path,DFFSDK dff){
			this(path);
			this.dff = dff;
		}

		@Override
		public boolean execute() {
			if(!Zmdl.im().hasCurrentInstance()){
				return true;
			}
			ZInstance inst = Zmdl.inst();
			Zmdl.app().getProgressScreen().show();
			if(dff == null){
				if(inst.type == 1){
					Toast.info(Zmdl.gt("exporting"),3f);
					if(DFFStream.saveDFF((DFFSDK)inst.obj,path + inst.name + ".dff",this,Zmdl.app().lang)){
						inst.setSaveState(false);
						Toast.info(Zmdl.gt("saved"),3f);
						if(Zmdl.fp().android_output_stream != null){
							Zmdl.fp().copy_out(path + inst.name + ".dff");
						}
					}
				}else{
					Toast.error(Zmdl.gt("no_dff_export"),3f);
				}
			}else{
				Toast.info(Zmdl.gt("exporting"),3f);
				if(DFFStream.saveDFF(dff,path + dff.name + ".dff",this,Zmdl.app().lang)){
					Toast.info(Zmdl.gt("saved"),3f);
					if(Zmdl.fp().android_output_stream != null) {
						Zmdl.fp().copy_out(path + dff.name + ".dff");
					}
				}
			}
			Zmdl.app().getProgressScreen().dismiss();
			return true;
		}

		@Override
		public void onStreamProgress(float progress){
			Zmdl.app().getProgressScreen().setProgress(progress);
		}

		@Override
		public void onStreamError(String err,boolean stop) {
			Toast.error(err,8f);
			FX.gpu.queueTask(new Task(){
					@Override
					public boolean execute() {
						Zmdl.app().sendEmailDialog(true);
						return true;
					}
				});
		}

		@Override
		public void onStreamPrint(final String log) {
			
		}
	}
	
	private class SaveZFile implements Task,OnDFFStreamingListener {
		private String path;

		public SaveZFile(String path){
			this.path = path;
		}

		@Override
		public boolean execute() {
			if(!Zmdl.im().hasCurrentInstance()){
				return true;
			}
			try{
				ZInstance inst = Zmdl.inst();
				if(inst.type == 1){
					Toast.info(Zmdl.gt("exporting"),3f);
					if(DFFStream.saveDFF((DFFSDK)inst.obj,path + inst.name + ".dff",this,Zmdl.app().lang)){
						ZFileStream.write(path + inst.name + ".zpk",inst.name,inst.root,path + inst.name + ".dff",false);
						Toast.info(Zmdl.gt("saved"),3f);
					}
				}else if(inst.type == 3){

				}else{
					Toast.error(Zmdl.gt("not_compatible"),3f);
				}
			}catch(Exception e){
				Logger.log(e);
				FX.gpu.queueTask(new Task(){
						@Override
						public boolean execute() {
							Zmdl.app().sendEmailDialog(true);
							return true;
						}
					});
			}
			return true;
		}

		@Override
		public void onStreamProgress(float progress){

		}

		@Override
		public void onStreamError(String err,boolean stop) {
			Toast.error(err,8f);
		}

		@Override
		public void onStreamPrint(final String log) {

		}
	}
	@Override
	public boolean isShowing() {
		return 
			Zmdl.app().panel.isShowing() && 
			Zmdl.app().panel.getContent().getId() == main.getId();
	}

	@Override
	public void close() {
		if(isShowing()){
			Zmdl.app().panel.dismiss();
		}
	}
}
