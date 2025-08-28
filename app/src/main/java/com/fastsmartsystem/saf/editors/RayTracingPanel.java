package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.adapters.*;
import com.fastsmartsystem.saf.*;
import com.forcex.rte.*;
import com.forcex.gfx3d.*;
import com.forcex.gfx3d.effect.*;
import com.forcex.utils.*;
import com.forcex.*;
import com.forcex.app.threading.*;
import com.forcex.core.gpu.*;
import com.forcex.core.*;
import com.forcex.rte.objects.*;
import com.forcex.math.*;
import com.forcex.rte.utils.*;
import java.util.*;
import com.forcex.gfx3d.shapes.*;
import java.io.*;
import java.util.regex.*;

public class RayTracingPanel extends PanelFragment {
	Layout main;
	EditText etThreads,etTileSize,etScreenWidth,etScreenHeight;
	Button btnRender;
	String hdri_sky = "none";
	Color world_color = new Color(190,190,190);
	float strength_sky = 0.2f;
	RenderView renderview;
	RTExecutionView rt_execution;
	int width,height;
	int max_width = 0;
	int max_height = 0;
	TextView tvProgress;
	ProgressBar pbProgress;
	
	public RayTracingPanel(RenderView view) {
		renderview = view;
		main = Zmdl.lay(0.25f, false);
		TextView tvLabel = new TextView(Zmdl.gdf());
		tvLabel.setAlignment(Layout.CENTER);
		tvLabel.setTextSize(0.06f);
		tvLabel.setText("ForceX RT Engine");
		main.add(tvLabel);
		Button worldColor = new Button("World Color", Zmdl.gdf(), 0.15f, 0.05f);
		worldColor.setAlignment(Layout.CENTER);
		worldColor.setMarginTop(0.02f);
		worldColor.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					showSkyColor();
				}
			});
		main.add(worldColor);
		Layout threads = Zmdl.lay(true);
		threads.setMarginLeft(0.01f);
		threads.setMarginTop(0.02f);
		TextView tvThreads = new TextView(Zmdl.gdf());
		tvThreads.setAlignment(Layout.CENTER);
		tvThreads.setTextSize(0.05f);
		tvThreads.setText("Threads: ");
		threads.add(tvThreads);
		etThreads = new EditText(Zmdl.ctx(),0.08f,0.05f,0.05f);
		etThreads.setAlignment(Layout.CENTER);
		etThreads.setText("1");
		etThreads.setNumbersMode(true);
		etThreads.setMarginLeft(0.01f);
		threads.add(etThreads);
		main.add(threads);
		Layout tilesize = Zmdl.lay(true);
		tilesize.setMarginLeft(0.01f);
		tilesize.setMarginTop(0.02f);
		TextView tvTileSize = new TextView(Zmdl.gdf());
		tvTileSize.setAlignment(Layout.CENTER);
		tvTileSize.setTextSize(0.05f);
		tvTileSize.setText("Tiles Size: ");
		tilesize.add(tvTileSize);
		etTileSize = new EditText(Zmdl.ctx(),0.08f,0.05f,0.05f);
		etTileSize.setAlignment(Layout.CENTER);
		etTileSize.setText("64");
		etTileSize.setNumbersMode(true);
		etTileSize.setMarginLeft(0.01f);
		tilesize.add(etTileSize);
		main.add(tilesize);
		Layout screenWidth = Zmdl.lay(true);
		screenWidth.setMarginLeft(0.01f);
		screenWidth.setMarginTop(0.02f);
		TextView tvScreenWidth = new TextView(Zmdl.gdf());
		tvScreenWidth.setAlignment(Layout.CENTER);
		tvScreenWidth.setTextSize(0.05f);
		tvScreenWidth.setText("Screen Width: ");
		screenWidth.add(tvScreenWidth);
		etScreenWidth = new EditText(Zmdl.ctx(),0.1f,0.05f,0.05f);
		etScreenWidth.setAlignment(Layout.CENTER);
		etScreenWidth.setNumbersMode(true);
		etScreenWidth.setMarginLeft(0.01f);
		screenWidth.add(etScreenWidth);
		main.add(screenWidth);
		Layout screenHeight = Zmdl.lay(true);
		screenHeight.setMarginLeft(0.01f);
		screenHeight.setMarginTop(0.02f);
		TextView tvScreenHeight = new TextView(Zmdl.gdf());
		tvScreenHeight.setAlignment(Layout.CENTER);
		tvScreenHeight.setTextSize(0.05f);
		tvScreenHeight.setText("Screen Height: ");
		screenHeight.add(tvScreenHeight);
		etScreenHeight = new EditText(Zmdl.ctx(),0.1f,0.05f,0.05f);
		etScreenHeight.setAlignment(Layout.CENTER);
		etScreenHeight.setNumbersMode(true);
		etScreenHeight.setMarginLeft(0.01f);
		screenHeight.add(etScreenHeight);
		main.add(screenHeight);
		tvProgress = new TextView(Zmdl.gdf());
		tvProgress.setAlignment(Layout.CENTER);
		tvProgress.setTextSize(0.04f);
		tvProgress.setVisibility(View.INVISIBLE);
		tvProgress.setText("Progressing");
		main.add(tvProgress);
		pbProgress = new ProgressBar(0.25f,0.015f);
		pbProgress.setIndeterminate(true);
		pbProgress.setUseBorder(false);
		pbProgress.setVisibility(View.INVISIBLE);
		main.add(pbProgress);
		btnRender = new Button("Render", Zmdl.gdf(), 0.1f, 0.05f);
		btnRender.setAlignment(Layout.CENTER);
		btnRender.setMarginTop(0.02f);
		btnRender.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					Zmdl.adtsk(new Task(){
							@Override
							public boolean execute() {
								processRender();
								return true;
							}
						});
				}
			});
		main.add(btnRender);
		Button btnClose = new Button(Zmdl.gt("close"), Zmdl.gdf(), 0.1f, 0.05f);
		btnClose.setAlignment(Layout.CENTER);
		btnClose.setMarginTop(0.02f);
		btnClose.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					dispose();
					Zmdl.ep().pick_object = true;
					Zmdl.app().panel.dismiss();
				}
			});
		main.add(btnClose);
		etScreenWidth.setOnEditTextListener(new EditText.onEditTextListener(){
				@Override
				public void onChange(View view, String text, boolean deleting) {
					etScreenWidth.setEdgeMultiColor(210,25,25,245,20,20);
					if(text.isEmpty() || text.contains(".") || text.contains("-")){
						return;
					}
					try{
						width = Integer.parseInt(text);
						if(width >= 256){
							etScreenWidth.setEdgeMultiColor(7,128,255,220,220,220);
							adaptView();
						}
					}catch(Exception e){
					}
				}

				@Override
				public void onEnter(View view, String text) {
					// TODO: Implement this method
				}
		});
		etScreenHeight.setOnEditTextListener(new EditText.onEditTextListener(){
				@Override
				public void onChange(View view, String text, boolean deleting) {
					etScreenHeight.setEdgeMultiColor(210,25,25,245,20,20);
					if(text.isEmpty() || text.contains(".") || text.contains("-")){
						return;
					}
					try{
						height = Integer.parseInt(text);
						if(height >= 256){
							etScreenHeight.setEdgeMultiColor(7,128,255,220,220,220);
							adaptView();
						}
					}catch(Exception e){
					}
				}

				@Override
				public void onEnter(View view, String text) {
					// TODO: Implement this method
				}
			});
		etThreads.setOnEditTextListener(new EditText.onEditTextListener(){
				@Override
				public void onChange(View view, String text, boolean deleting) {
					etThreads.setEdgeMultiColor(210,25,25,245,20,20);
					if(text.isEmpty() || text.contains("0") || text.contains("-")|| text.contains(".")){
						return;
					}
					if(Integer.parseInt(text) > getNumCores()){
						etThreads.setText(getNumCores() + "");
					}
					etThreads.setEdgeMultiColor(7,128,255,220,220,220);
				}

				@Override
				public void onEnter(View view, String text) {
					// TODO: Implement this method
				}
			});
		etTileSize.setOnEditTextListener(new EditText.onEditTextListener(){
				@Override
				public void onChange(View view, String text, boolean deleting) {
					etTileSize.setEdgeMultiColor(210,25,25,245,20,20);
					if(text.isEmpty() || text.contains("0") || text.contains("-")|| text.contains(".")){
						return;
					}
					if(Integer.parseInt(text) > 512){
						return;
					}
					etTileSize.setEdgeMultiColor(7,128,255,220,220,220);
				}

				@Override
				public void onEnter(View view, String text) {
					// TODO: Implement this method
				}
			});
		max_width = (int)(renderview.getWidth() * FX.gpu.getWidth());
		max_height = (int)(renderview.getHeight() * FX.gpu.getHeight());
		rt_execution = new RTExecutionView();
		rt_execution.setVisibility(View.INVISIBLE);
		rt_execution.setRelativePosition(view.relative.x,view.relative.y);
		Zmdl.ctx().addUntouchableView(rt_execution);
	}

	public void requestShow() {
		width = max_width;
		height = max_height;
		rt_execution.createTexture();
		etScreenWidth.setText(width + "");
		etScreenHeight.setText(height + "");
		etScreenWidth.setEdgeMultiColor(7,128,255,220,220,220);
		etScreenHeight.setEdgeMultiColor(7,128,255,220,220,220);
		Zmdl.ep().pick_object = false;
		Zmdl.apl(main);
	}
	
	private void showSkyColor() {
		Layout color_lay = new Layout(Zmdl.ctx());
		final ColorPicker picker = new ColorPicker(0.3f,0.25f);
		picker.setColor(world_color);
		picker.setMarginTop(0.01f);
		picker.setOnColorPickListener(new ColorPicker.OnColorPickListener(){
				@Override
				public void pick(int color){
					world_color.set(color);
				}
			});
		color_lay.add(picker);
		final TextView tvHDRIPath = new TextView(Zmdl.gdf());
		tvHDRIPath.setTextSize(0.05f);
		tvHDRIPath.setText("HDRI: "+hdri_sky);
		tvHDRIPath.setAnimationScroll(true);
		tvHDRIPath.setConstraintWidth(0.3f);
		color_lay.add(tvHDRIPath);
		Button btnPickHDRI = new Button("HDRI", Zmdl.gdf(), 0.1f, 0.05f);
		btnPickHDRI.setAlignment(Layout.CENTER);
		btnPickHDRI.setMarginTop(0.02f);
		final Dialog diag = new Dialog(color_lay);
		diag.setTitle("World Sky");
		btnPickHDRI.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					diag.hide();
					FileDialog.create(Zmdl.ctx(), "Select HDRI", Zmdl.fp().getCurrentPath(), ".png", new FileDialog.OnResultListener(){
							@Override
							public boolean tryCancel(short id) {
								diag.show();
								return true;
							}
							@Override
							public void open(short id, String path) {
								diag.show();
								hdri_sky = path;
								tvHDRIPath.setText("HDRI: "+hdri_sky);
							}
						},Zmdl.app().lang,1);
				}
			});
		color_lay.add(btnPickHDRI);
		diag.show();
	}
	
	private void adaptView() {
		float w = (float)width / FX.gpu.getWidth();
		float h = (float)height / FX.gpu.getHeight();
		if(width > max_width && height <= max_height){
			w = (float)max_width / FX.gpu.getWidth();
			h = (float)height / FX.gpu.getHeight() * ((float)max_width / width);
		}else if(width <= max_width && height > max_height){
			w = (float)width / FX.gpu.getWidth() * ((float)max_height / height);
			h = (float)max_height / FX.gpu.getHeight();
		}else if(width > max_width && height > max_height){
			if(width == height) {
				w = (float)max_height / FX.gpu.getWidth();
				h = (float)max_height / FX.gpu.getHeight();
			}else if(width > height) {
				w = (float)max_width / FX.gpu.getWidth();
				h = (float)max_height / FX.gpu.getHeight() * ((float)max_width / width);
			}else if(width < height) {
				w = (float)max_width / FX.gpu.getWidth() * ((float)max_height / height);
				h = (float)max_height / FX.gpu.getHeight();
			}
		}
		renderview.setDimens(w,h);
		Zmdl.rp().getCamera().setAspectRatio(
			width > height ? ((float)width / height) : ((float)height / width));
	}
	
	public void processRender() {
		int numThreads = -1,tilesize = -1;
		try{
			numThreads = Integer.parseInt(etThreads.getText());
			tilesize = Integer.parseInt(etTileSize.getText());
			if(numThreads <= 0 || tilesize <= 0){
				return;
			}
		}catch(Exception e){
			Toast.error("Invalid Parameters",4);
			return;
		}
		pbProgress.setVisibility(View.VISIBLE);
		Zmdl.rp().getCamera().update();
		Zmdl.app().setRenderControlVisible(false);
		RTScene rt_scene = new RTScene(Zmdl.rp().getLight(),Zmdl.rp().getCamera());
		rt_scene.setColorSky(world_color.r,world_color.g,world_color.b);
		if(!hdri_sky.contains("none")) {
			rt_scene.loadSky(hdri_sky);
		}
		rt_scene.sky_intensity = strength_sky;
		ModelObject box = new ModelObject(new Box(4,4,4));
		box.setPosition(0,0,0);
		RTObject o = new RTMesh(box.getMesh(),box.getTransform());
		rt_scene.add(o);
		RTMaterial mat1 = o.getMaterial(0);
		mat1.color = new RTColor(0.5f,0.3f,0);
		mat1.roughness = 0.1f;
		mat1.emission = 0.8f;
		RTEngine engine = new RTEngine(rt_scene,width,height);
		engine.setMultiThread(numThreads);
		engine.setTileSize(tilesize);
		rt_execution.setVisibility(View.VISIBLE);
		rt_execution.setWidth(renderview.getWidth());
		rt_execution.setHeight(renderview.getHeight());
		engine.updateTiles();
		rt_execution.setup(engine,numThreads,tilesize);
		engine.execute();
	}
	
	
	private void normalizeRender(){
		renderview.setDimens(0.75f,0.865f);
		renderview.setRelativePosition(0.25f,-0.135f);
		Zmdl.rp().getCamera().setAspectRatio((0.75f * FX.gpu.getWidth()) / (0.865f * FX.gpu.getHeight()));
	}

	byte update_state = 0;
	byte state = -1;

	public void dispose() {
		normalizeRender();
	}

	@Override
	public boolean isShowing(){
		return Zmdl.tlay(main);
	}

	@Override
	public void close() {
		if (isShowing()) {
			Zmdl.app().panel.dismiss();
			dispose();
		}
	}
	
	private int getNumCores() {
		class CpuFilter implements FileFilter {
			@Override
			public boolean accept(File pathname) {
				if(Pattern.matches("cpu[0-9]+", pathname.getName())) {
					return true;
				}
				return false;
			}      
		}
		
		try {
			File dir = new File("/sys/devices/system/cpu/");
			File[] files = dir.listFiles(new CpuFilter());
			return files.length;
		} catch(Exception e) {
			return 1;
		}
	}
	
	private class RTExecutionView extends View implements RTEngine.OnRenderingListener {
		int texture = -1;
		Color color;
		ArrayList<CoreView> cores = new ArrayList<>();
		float coresWidth,coresHeight;
		boolean finished = false;
		boolean executing = false;
		RTEngine engine;
		
		private class CoreView {
			public Color color = new Color();
			public Vector2f pos = new Vector2f();
		}
		
		public RTExecutionView() {
			color = new Color(0,0,0,0);
		}
		
		public void createTexture() {
			if(texture == -1){
				texture = Texture.genTextureWhite();
			}
		}
		
		public void setup(RTEngine engine,int threads,int tiles_size){
			this.engine = engine;
			cores.clear();
			for(int i = 0;i < threads;i++) {
				CoreView v = new CoreView();
				v.color.set(
					(int)(Math.random() * 255f),
					(int)(Math.random() * 255f),
					(int)(Math.random() * 255f),
					255);
				cores.add(v);
			}
			engine.setRenderingListener(this);
			finished = false; 
			coresWidth = width / (engine.getWidth() / tiles_size);
			coresHeight = height / (engine.getHeight() / tiles_size);
		}

		@Override
		public void onDraw(Drawer drawer) {
			drawer.setScale(extent.x,extent.y);
			drawer.renderQuad(local,color,texture);
			if(executing) {
				float startx = (local.x - width) + coresWidth;
				float starty = (local.y + height) - coresHeight;
				for(int i = 0;i < engine.getNumCores();i++) {
					cores.get(i).pos.set(
						startx + coresWidth * engine.getCore(i).currentX * 2f,
						starty - coresHeight * engine.getCore(i).currentY * 2f);
				}
			}
			for(CoreView c : cores) {
				if(finished){
					break;
				}
				drawer.setScale(coresWidth,coresHeight);
				drawer.renderLineQuad(c.pos,c.color);
			}
		}
		
		@Override
		public void updateFrameBuffer(final RTEngine engine) {
			tvProgress.setVisibility(View.VISIBLE);
			executing = true;
			final byte[] rgba = engine.getFrameBuffer().getTexture();
			FX.gpu.queueTask(new Task(){
					@Override
					public boolean execute() {
						tvProgress.setText(engine.getActualProgress()+"/"+engine.getAllNumTiles());
						FX.gl.glBindTexture(GL.GL_TEXTURE_2D,texture);
						FX.gl.glTexImage2D(GL.GL_TEXTURE_2D,engine.getWidth(),engine.getHeight(),GL.GL_TEXTURE_RGBA,rgba);
						FX.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
						FX.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
						FX.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
						FX.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
						color.set(0,255,255,255);
						return true;
					}
				});
			FX.gpu.waitEmptyQueue();
		}

		@Override
		public void finish(RTEngine engine) {
			tvProgress.setVisibility(View.VISIBLE);
			executing = false;
			finished = true;
			Toast.info("Render terminado",4f);
			pbProgress.setVisibility(View.INVISIBLE);
			
		}
	}
}
