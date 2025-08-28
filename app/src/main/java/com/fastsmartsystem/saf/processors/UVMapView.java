package com.fastsmartsystem.saf.processors;
import com.forcex.*;
import com.forcex.app.*;
import com.forcex.core.*;
import com.forcex.gfx3d.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.forcex.math.*;
import com.forcex.utils.*;

public class UVMapView extends View implements QuadSelector.OnSelectionListener {
	int texture = -1;
	Color background_color,texture_color;
	int edges_idxcnt, triangles_idxcnt, grid_idxcount;
	int vbo = -1, ibo_lines = -1, ibo_triangles = -1,
	grid_vbo = -1, grid_ibo = -1, part = -1;
	float texture_aspect = 1f;
	Vector2f offset = new Vector2f();
	float scale = 1f;
	boolean move,zoom,select;
	Mesh mesh;
	short 
	grid_width = 40,
	grid_height = 33;
	Color grid_color,
	triangle_no_selected,
	triangle_selected,struct_lines;
	TextView tvText;
	QuadSelector scr;
	public boolean[] state_triangles,state_vertices;
	float[] tex_coord;
	boolean reset_representation = false;
	boolean unselected = false;
	OnUVListener listener;
	
	public UVMapView(){
		background_color = new Color(Color.GREY);
		texture_color = new Color(Color.WHITE);
		struct_lines = new Color(Color.WHITE);
		triangle_no_selected = new Color(210,210,210,100);
		triangle_selected = new Color(230,240,80,100);
		grid_color = new Color(180,180,180,50);
		scr = new QuadSelector(0.375f,0.865f);
		scr.setListener(this);
		setFullScreen(false);
	}
	
	public void setAspectTexture(int width,int height) {
		this.texture_aspect = width > height ? ((float)width / height) : ((float)height / width);
	}

	public void setTexture(int texid){
		texture = texid;
	}
	
	public void setMesh(Mesh mesh) {
		this.mesh = mesh;
	}
	
	public void setMode(int mode){
		move = mode == 1;
		zoom = mode == 2;
		select = mode == 3;
	}
	
	public void setUnselecting(boolean z) {
		unselected = z;
	}
	
	public void setListener(OnUVListener listener) {
		this.listener = listener;
	}
	
	public void reset(){
		scale = 1f;
		offset.set(0,0);
	}

	boolean first = true;
	float ox,oy;
	
	@Override
	public void onTouch(float x, float y, byte type) {
		if(first){
				ox = x;
				oy = y;
				first = false;
			}
			if(select){
				scr.onTouch(x,y,type);
			}
			if(!select && type == EventType.TOUCH_DRAGGING){
				if(move){
					offset.x += -(ox - x) * 0.5f;
					offset.y += -(oy - y) * 0.5f;
				}else if(zoom){
					scale += -(oy - y) * 0.5f;
					if(scale < 0.1f){
						scale = 0.1f;
					}
				}
			}
			ox = x;
			oy = y;
	}
	
	public void setMeshPart(int index){
		this.part = index;
	}

	@Override
	protected boolean testTouch(float x, float y) {
		return isVisible() && GameUtils.testRect(x,y,local,extent.x,extent.y);
	}
	
	public void setFullScreen(boolean z){
		if(z){
			setWidth(1f);
			setHeight(1f);
			scr.setWidth(1f);
			scr.setHeight(1f);
			setRelativePosition(0,0);
		}else{
			setWidth(0.375f);
			setHeight(0.865f);
			scr.setWidth(0.375f);
			scr.setHeight(0.865f);
			setRelativePosition(-0.125f,-0.135f);
		}
		reset();
	}

	@Override
	public void click(Vector2f center) {
		
	}
	
	@Override
	public void selecting(Vector2f center, float width, float height) {
		Vector2f uv_center = local.add(offset);
		float UVscaleX = extent.x * scale;
		float UVscaleY = scale * extent.x * context.getAspectRatio() * texture_aspect;
		MeshPart p = mesh.getPart(part);
		for(int i = 0;i < p.index.length;i += 3) {
			int idx1 = (p.index[i] & 0xffff);
			int idx2 = (p.index[i+1] & 0xffff);
			int idx3 = (p.index[i+2] & 0xffff);
			boolean v1 = SelectorWrapper.testRect(tex_coord[idx1*7] * UVscaleX + uv_center.x,tex_coord[idx1*7 + 1] * UVscaleY + uv_center.y,center,width,height);
			boolean v2 = SelectorWrapper.testRect(tex_coord[idx2*7] * UVscaleX + uv_center.x,tex_coord[idx2*7 + 1] * UVscaleY + uv_center.y,center,width,height);
			boolean v3 = SelectorWrapper.testRect(tex_coord[idx3*7] * UVscaleX + uv_center.x,tex_coord[idx3*7 + 1] * UVscaleY + uv_center.y,center,width,height);
			if(v1) {
				state_vertices[idx1] = !unselected;
				tex_coord[idx1 * 7 + 4] = state_vertices[idx1] ? 1f : 1f;
				tex_coord[idx1 * 7 + 5] = state_vertices[idx1] ? 0.7f : 1f;
				tex_coord[idx1 * 7 + 6] = state_vertices[idx1] ? 0.08f : 1f;
			}
			if(v2) {
				state_vertices[idx2] = !unselected;
				tex_coord[idx2 * 7 + 4] = state_vertices[idx2] ? 1f : 1f;
				tex_coord[idx2 * 7 + 5] = state_vertices[idx2] ? 0.7f : 1f;
				tex_coord[idx2 * 7 + 6] = state_vertices[idx2] ? 0.08f : 1f;
			}
			if(v3) {
				state_vertices[idx3] = !unselected;
				tex_coord[idx3 * 7 + 4] = state_vertices[idx3] ? 1f : 1f;
				tex_coord[idx3 * 7 + 5] = state_vertices[idx3] ? 0.7f : 1f;
				tex_coord[idx3 * 7 + 6] = state_vertices[idx3] ? 0.08f : 1f;
			}
			if(v1 && v2 && v3){
				state_triangles[i / 3] = !unselected;
			}
		}
		listener.select();
		Drawer.updateBuffer(vbo,tex_coord);
	}

	@Override
	public void onCreate(Drawer drawer) {
		float[] vertexs = new float[28 + ((grid_width - 1) * 14) + ((grid_height - 1) * 14)];
		short[] indices = new short[8 + ((grid_width - 1) * 2) + ((grid_height - 1) * 2)];
		grid_idxcount = indices.length;
		put(vertexs,0,-extent.x,extent.y);
		put(vertexs,1,-extent.x,-extent.y);
		put(vertexs,2,extent.x,-extent.y);
		put(vertexs,3,extent.x,extent.y);
		int i = 0;
		for(i = 0;i < 4;i++){
			if(i != 3){
				put(indices,i,i,i+1);
			}else{
				put(indices,i,i,0);
			}
		}
		float cell_width = (extent.x * 2f) / grid_width;
		float cell_height = (extent.y * 2f) / grid_height;
		int vec = 4;
		for(short x = 1;x < grid_height;x++){
			float offset_height = (extent.y - (cell_height * x));
			put(vertexs,vec + 0,-extent.x, offset_height);
			put(vertexs,vec + 1, extent.x, offset_height);
			put(indices, i, vec, vec + 1);i++;
			vec += 2;
		}
		float offset_x = -extent.x;
		for(short y = 1;y < grid_width;y++){
			offset_x += cell_width;
			put(vertexs,vec + 0,offset_x,-extent.y);
			put(vertexs,vec + 1,offset_x, extent.y);
			put(indices, i, vec, vec + 1);i++;
			vec += 2;
		}
		grid_vbo = drawer.createBuffer(vertexs,false,false);
		grid_ibo = drawer.createBuffer(indices,true,false);
		vertexs = null;
		indices = null;
		tvText = new TextView(UIContext.default_font);
		tvText.setTextSize(0.03f);
		tvText.getTextColor().set(230,230,230);
	}

	private void put(float[] a,int vec,float x,float y){
		a[vec * 7] = x;
		a[vec * 7 + 1] = y;
		a[vec * 7 + 2] = 0;
		a[vec * 7 + 3] = 0;
		a[vec * 7 + 4] = 1;
		a[vec * 7 + 5] = 1;
		a[vec * 7 + 6] = 1;
	}

	private void put(short[] a,int vec,int sidx,int eidx){
		a[vec * 2] = (short)sidx;
		a[vec * 2 + 1] = (short)eidx;
	}
	
	@Override
	public void onDraw(Drawer drawer) {
		if(reset_representation){
			if(vbo != -1){
				FX.gl.glDeleteBuffer(ibo_lines);
				FX.gl.glDeleteBuffer(ibo_triangles);
				FX.gl.glDeleteBuffer(vbo);
			}
			process();
			vbo = drawer.createBuffer(tex_coord,false,true);
			ibo_lines = drawer.createBuffer(processIndex(),true,false);
			ibo_triangles = drawer.createBuffer(mesh.getPart(part).index,true,false);
			reset_representation = false;
		}
		drawer.scissorArea(local.x,local.y,extent.x,extent.y);
		drawer.setScale(extent.x,extent.y);
		drawer.renderQuad(local,background_color,-1);
		drawer.setScale(1,1);
		drawer.freeRender(grid_vbo,local,grid_color,-1);
		scr.setRelativePosition(relative.x,relative.y);
		FX.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, grid_ibo);
		FX.gl.glDrawElements(GL.GL_LINES, grid_idxcount);
		Vector2f uv_center = local.add(offset);
		float UVscaleX = extent.x * scale;
		float UVscaleY = scale * extent.x * context.getAspectRatio() * texture_aspect;
		if(mesh != null){
			drawer.setScale(UVscaleX,UVscaleY);
			drawer.renderQuad(uv_center,texture_color,texture);
			drawer.freeRender(vbo,uv_center,triangle_no_selected,-1);
			FX.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo_triangles);
			FX.gl.glDrawElements(GL.GL_TRIANGLES,triangles_idxcnt);
			drawer.shader.setSpriteColor(struct_lines);
			FX.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo_lines);
			FX.gl.glDrawElements(GL.GL_LINES,edges_idxcnt);
		}else{
			drawer.setScale(UVscaleX,UVscaleY);
			drawer.renderQuad(uv_center,texture_color,-1);
		}
		tvText.setText("0,0");
		tvText.local.set(uv_center.x - (tvText.getWidth() + UVscaleX),uv_center.y + UVscaleY);
		tvText.onDraw(drawer);
		tvText.setText("1,1");
		tvText.local.set(uv_center.x + (tvText.getWidth() + UVscaleX),uv_center.y - UVscaleY);
		tvText.onDraw(drawer);
		tvText.setText("UV Mapping ("+getTrianglesSelected()+")");
		tvText.local.set(local.x - (extent.x - tvText.getWidth()),local.y - (extent.y - tvText.getHeight()));
		tvText.onDraw(drawer);
		scr.onDraw(drawer);
		drawer.finishScissor();
	}
		
	private int getTrianglesSelected(){
		int num = 0;
		for(boolean z : state_triangles){
			if(z){
				num++;
			}
		}
		return num;
	}
	
	public void dispose(){
		mesh = null;
		tex_coord = null;
		state_triangles = null;
		state_vertices = null;
		reset_representation = true;
		part = -1;
	}
	
	private void process() {
		if(tex_coord == null){
			tex_coord = new float[mesh.getVertexInfo().vertexCount * 7];
			state_vertices = new boolean[mesh.getVertexInfo().vertexCount];
		}
		int ofs = 0;
		for(int i = 0;i < tex_coord.length;i += 7){
			tex_coord[i] = 2 * mesh.getVertexData().uvs[ofs] - 1;
			tex_coord[i + 1] = -(2 * mesh.getVertexData().uvs[ofs + 1] - 1);
			tex_coord[i + 2] = tex_coord[i + 3] = 0;
			tex_coord[i + 4] = 1f;
			tex_coord[i + 5] = 1f;
			tex_coord[i + 6] = 1f;
			ofs += 2;
		}
	}
	
	private short[] processIndex() {
		MeshPart mesh_part = mesh.getPart(part);
		short[] indices = new short[mesh_part.index.length * 2];
		int triangles = (mesh_part.index.length / 3);
		state_triangles = new boolean[triangles];
		for(int i = 0;i < triangles;i ++) {
			int line = i * 6,triangle = i * 3;
			indices[line + 0] = mesh_part.index[triangle + 0];
			indices[line + 1] = mesh_part.index[triangle + 1];
			indices[line + 2] = mesh_part.index[triangle + 1];
			indices[line + 3] = mesh_part.index[triangle + 2];
			indices[line + 4] = mesh_part.index[triangle + 2];
			indices[line + 5] = mesh_part.index[triangle + 0];
		}
		edges_idxcnt = indices.length;
		triangles_idxcnt = mesh_part.index.length;
		return indices;
	}
}
