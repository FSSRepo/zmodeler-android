package com.fastsmartsystem.saf.processors;
import android.os.Build;
import android.util.Log;

import com.fastsmartsystem.saf.*;
import com.fastsmartsystem.saf.adapters.*;
import com.forcex.*;
import com.forcex.app.threading.*;
import com.forcex.core.*;
import com.forcex.core.gpu.*;
import com.forcex.gfx3d.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.forcex.utils.*;
import java.io.*;
import java.util.*;

public class TextureManager {
	ArrayList<TextureInfo> textures;

	public TextureManager(){
		textures = new ArrayList<>();
	}
	
	public int update(Mesh mesh,String parent) {
		int textures_loaded = 0;
		for(MeshPart part : mesh.getParts().list) {
			final Material mat = part.material;
			if(mat.textureName.length() > 0) {
				File img_file = new File(parent + "/" + mat.textureName + ".png");
				if(!img_file.exists()){
					img_file = new File("gtacache/"+mat.textureName+".png");
					if(!img_file.exists()){
						img_file = null;
					}
				}
				if(img_file != null) {
					int temp_texture = get(mat.textureName);								
					if(temp_texture != -1 || !img_file.exists()){
						mat.diffuseTexture = temp_texture;
					}else{
						final Image img = new Image(img_file.getAbsolutePath());
						final String path = img_file.getAbsolutePath();
						try {
							FX.gpu.queueTask(new Task(){
									@Override
									public boolean execute() {
										mat.diffuseTexture = Texture.load(img.width,img.height,img.getBuffer(),true);
										add(mat.textureName, path, mat.diffuseTexture,img);												return true;
									}
								});
							FX.gpu.waitEmptyQueue();
							img.clear();
							textures_loaded++;
						} catch (Exception e) {}
					}
				}
			}
		}
		return textures_loaded;
	}

	public void add(String name,String path,int id,Image img){
		TextureInfo inf = new TextureInfo();
		inf.name = name;
		inf.glId = id;
		inf.size = (int)new File(path).length();
		inf.width = (short)img.width;
		inf.height = (short)img.height;
		inf.path = path;
		textures.add(inf);
	}
	
	public boolean exist(String name){
		for(TextureInfo tex :textures){
			if(tex.name.equals(name)){
				return true;
			}
		}
		return false;
	}

	public int get(String name){
		if(exist(name)){
			for(TextureInfo t : textures){
				if(t.name.equals(name)){
					return t.glId;
				}
			}
		}
		return -1;
	}
	
	public TextureInfo getInfo(String name){
		if(exist(name)){
			for(TextureInfo t : textures){
				if(t.name.equals(name)){
					return t;
				}
			}
		}
		return null;
	}
	
	public void removeMesh(Mesh mesh){
		for(MeshPart p : mesh.getParts().list){
			if(exist(p.material.textureName)){
				p.material.diffuseTexture = -1;
			}
		}
	}

	public void remove(int index){
		textures.remove(index);
	}
	
	boolean updateTexture = true;
	TextureInfo current;
	Dialog driver;
	TextView tvInfo;
	
	public void showDriver(final OnResultListener result) {
		if(driver != null) {
			return;
		}
		updateTexture = true;
		current = null;
		Zmdl.adtsk(new Task(){
				@Override
				public boolean execute() {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
					if (current != null && new File(current.path).exists()) {
						int new_size = (int)new File(current.path).length();
						if(new_size != current.size){
							current.size = new_size;
							final Image img = new Image(current.path);
							if(FX.device.getAndroidVersion() >= Build.VERSION_CODES.P) {
								new File(current.path).delete();
							}
							current.width = (short)img.width;
							current.height = (short)img.height;
							FX.gpu.queueTask(new Task(){
									@Override
									public boolean execute() {
										FX.gl.glBindTexture(GL.GL_TEXTURE_2D,current.glId);
										FX.gl.glTexImage2D(GL.GL_TEXTURE_2D,img.width,img.height,GL.GL_TEXTURE_RGBA,img.getRGBAImage());
										img.clear();
										FX.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
										FX.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
										FX.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
										FX.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
										tvInfo.setText(Zmdl.gt("texture_info",current.name,current.width,current.height));
										return true;
									}
								});
							FX.gpu.waitEmptyQueue();
						}
					}
					return !updateTexture;
				}
		});
		Layout main = Zmdl.lay(0.5f,false);
		Layout lay1 = Zmdl.lay(true);
		Layout lay2 = Zmdl.lay(false);
		Layout lay3 = Zmdl.lay(false);
		final Filter filter = new Filter(new Filter.OnFilteringListener(){
				@Override
				public boolean filter(Object item, Object[] args) {
					String s = ((MenuItem)item).text;
					return s.contains((String)args[0]);
				}
			});
		final int tex_ic = Texture.load("zmdl/texture.png");
		Button btnAdd = new Button(Zmdl.gt("add"),Zmdl.gdf(),0.1f,0.05f);
		main.add(btnAdd);
		MenuAdapter mn = new MenuAdapter();
		for(TextureInfo t : textures){
			mn.add(tex_ic,t.name);
		}
		mn.setFilter(filter);
		EditText etFilter = new EditText(Zmdl.ctx(),0.24f,0.05f,0.05f);
		etFilter.setHint(Zmdl.gt("search")); etFilter.setMarginBottom(0.02f);
		etFilter.setOnEditTextListener(new EditText.onEditTextListener(){
				@Override
				public void onChange(View view, String text, boolean deleting) {
					filter.filter(false,text);
				}
				@Override
				public void onEnter(View view, String text){}
			});
		lay2.add(etFilter);
		tvInfo = new TextView(Zmdl.gdf());
		tvInfo.setTextSize(0.04f);
		tvInfo.setConstraintWidth(0.25f);
		tvInfo.setText("Select any texture to see details"); tvInfo.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		ListView lv = new ListView(0.25f,0.3f,mn);
		lv.setApplyAspectRatio(true);
		final ImageView ivTexture = new ImageView(-1,0.24f,0.24f);
		ivTexture.setApplyAspectRatio(true);
		lay3.add(ivTexture);
		lv.setOnItemClickListener(new ListView.OnItemClickListener(){
				@Override
				public void onItemClick(ListView view, Object item, short position, boolean longclick) {
					current = textures.get(position);
					ivTexture.setTexture(current.glId);
					tvInfo.setText(Zmdl.gt("texture_info",current.name,current.width,current.height));
				}
			});
		lay3.add(tvInfo);
		lay3.setMarginLeft(0.01f);
		lay2.add(lv);
		lay1.add(lay2);
		lay1.add(lay3);
		main.add(lay1);
		btnAdd.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					driver.hide();
					if(FX.device.getAndroidVersion() < Build.VERSION_CODES.P) {
						FileDialog.create(Zmdl.ctx(), Zmdl.gt("select_image"), Zmdl.fp().getCurrentPath(), ".png", new FileDialog.OnResultListener(){
							@Override
							public boolean tryCancel(short id) {
								driver.show();
								return true;
							}

							@Override
							public void open(short id, String path) {
								driver.show();
								loadFromPath(path);
								filter.add(new MenuItem(tex_ic, current.name));
								ivTexture.setTexture(current.glId);
							}
						},Zmdl.app().lang,0x450);
					}
					else {
						FX.device.invokeFileChooser(true, Zmdl.gt("select_image"), "", new SystemDevice.OnAndroidFileStream() {
							@Override
							public void open(InputStream is,String name) {
								Log.i("TexturePath", FX.fs.homeDirectory + name);
//
								if(!name.endsWith(".png")) {
									Toast.error("Debe ser una imagen png",4);
									driver.show();
									return;
								}
								FileProcessor.copy_temp(is, FX.fs.homeDirectory + name);
								loadFromPath(FX.fs.homeDirectory + name);
								filter.add(new MenuItem(tex_ic, current.name));
								ivTexture.setTexture(current.glId);
								driver.show();
							}

							@Override
							public void save(OutputStream os) {

							}
						});
					}
				}
			});
		driver = new Dialog(main);
		driver.setTitle(result!= null ? Zmdl.gt("select_textur") : Zmdl.gt("texture_manager"));
		driver.setIcon(tex_ic);
		driver.setOnDismissListener(new Dialog.OnDimissListener(){
				@Override
				public boolean dismiss() {
					if(result != null){
						result.select(false,null);
					}
					ivTexture.setTexture(-1);
					updateTexture = false;
					driver = null;
					tvInfo = null;
					return true;
				}
			});
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.12f,0.05f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(result != null){
						if(current == null){
							result.select(false,null);
						}else{
							result.select(true,current);
						}
					}
					ivTexture.setTexture(-1);
					updateTexture = false;
					driver.dismiss();
					driver = null;
					tvInfo = null;
				}
			});
		btnAccept.setAlignment(Layout.CENTER);
		main.add(btnAccept);
		driver.show();
	}

	private void loadFromPath(String path) {
		TextureInfo inf = new TextureInfo();
		File file = new File(path);
		inf.name = file.getName().substring(0, file.getName().indexOf(".png"));
		inf.glId = Texture.genTextureWhite();
		inf.path = path;
		inf.size = 13;
		textures.add(inf);
		current = textures.get(textures.size() - 1);
		tvInfo.setText(Zmdl.gt("texture_info", current.name, current.width, current.height));
	}
	
	public static interface OnResultListener{
		void select(boolean success,TextureInfo result);
	}
	
	public static class TextureInfo {
		public String name = "";
		public int glId = -1;
		public int size = 0;
		public short width = 0;
		public short height = 0;
		public String path = "";
	}
}
