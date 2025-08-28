package com.fastsmartsystem.saf;
import com.forcex.gfx3d.*;
import com.forcex.collision.*;
import com.forcex.math.*;
import com.forcex.gfx3d.shapes.*;
import com.forcex.gui.*;
import com.forcex.gfx3d.shader.*;
import com.forcex.core.*;
import com.forcex.*;
import com.forcex.utils.*;
import java.util.*;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.core.gpu.*;

public class ZObject extends ModelObject{
	BoundingBox bound_box;
	BoundingMesh bound_mesh;
	public ModelObject bound_wire, object_wire, object_label, origin;
	public boolean drawLimits,wireFrame,show_label,draw_weights,keep_selector = false;
	public boolean selected = false;
	public boolean hasSelector = false;
	ArrayList<Short> current_alpha = new ArrayList<>();
	public boolean draw_origin = false;
	CPUAnimation cpu_anim;
	
	public ZObject(){
		super();
		bound_box = new BoundingBox();
		bound_mesh = new BoundingMesh();
		bound_wire = new ModelObject(new WireBox(4,4,4));
		origin = new ModelObject(new Origin());
	}

	public ZObject(Mesh mesh){
		super(mesh);
		bound_box = new BoundingBox();
		bound_mesh = new BoundingMesh();
		mesh.global_color.set(240,240,35);
		bound_wire = new ModelObject(new WireBox(4,4,4));
		origin = new ModelObject(new Origin());
	}

	@Override
	public void setMesh(Mesh mesh){
		super.setMesh(mesh);
		mesh.global_color.set(240,240,35);
	}
	
	public CPUAnimation setupCpuAnim() {
		cpu_anim = new CPUAnimation(getAnimator(),getMesh());
		return cpu_anim;
	}
	
	public void clearCpuAnim(){
		if(cpu_anim == null){
			return;
		}
		cpu_anim.stop();
		cpu_anim = null;
	}
	
	public void restoreAlpha(){
		if(current_alpha.size() == 0){
			return;
		}
		int i = 0;
		for(MeshPart p : getMesh().getParts().list){
			p.material.color.a = current_alpha.get(i);
			i++;
		}
		current_alpha.clear();
	}
	
	public void setUseAlpha(int index,int alpha){
		for(int i = 0;i < current_alpha.size();i++){
			if(index != i){
				getMesh().getPart(i).material.color.a = (short)alpha;
			}else{
				getMesh().getPart(i).material.color.a = current_alpha.get(i);
			}
		}
	}
	
	public void saveAlpha(){
		if(current_alpha.size() > 0){
			return;
		}
		for(MeshPart p : getMesh().getParts().list){
			current_alpha.add((short)p.material.color.a);
		}
	}
	
	public void setShowLabel(boolean z){
		show_label = z;
		if(z && object_label == null){
			object_label = new ModelObject(new Text(getName(),Zmdl.gdf(),0.2f));
			object_label.setTransform(getTransform());
		}else if(!z && object_label != null){
			object_label.delete();
			object_label = null;
		}
	}

	public void calculateBounds(){
		BoundingBox.create(bound_box,getMesh().getVertexData().vertices);
		bound_box.calculateExtents();
		((WireBox)bound_wire.getMesh()).update(bound_box.extent.x,bound_box.extent.y,bound_box.extent.z);
		if(getMesh().getPrimitiveType() != GL.GL_TRIANGLE_STRIP){
			bound_mesh.compute(getMesh().getVertexData().vertices,getMesh().getParts().list,getTransform());
		}else{
			Toast.error("Error: We couldn't\nconvert to triangle list.",3f);
		}
	}
	
	public boolean rayTest(Ray ray){
		if(bound_box.intersectRay(getPosition(),ray)){
			if(getMesh().getPrimitiveType() == GL.GL_TRIANGLES){
				return bound_mesh.rayTest(ray);
			}
		}
		return false;
	}

	public BoundingBox getBound(){
		return bound_box;
	}
	
	public void setSplitShow(int index){
		int i  = 0;
		for(MeshPart part : getMesh().getParts().list){
			part.visible = i == index || index == -1;
			i ++;
		}
	}
	
	@Override
	public void update(){
		super.update();
		getMesh().useGlobalColor = selected;
		if(drawLimits){
			bound_wire.getMesh().getPart(0).material.color = getMesh().getPart(0).material.color;
			bound_wire.setTransform(getTransform().clone());
			bound_wire.getTransform().data[Matrix4f.M03] += bound_box.center.x;
			bound_wire.getTransform().data[Matrix4f.M13] += bound_box.center.y;
			bound_wire.getTransform().data[Matrix4f.M23] += bound_box.center.z;
		}
		origin.setPosition(getPosition());
		if(wireFrame && object_wire == null){
			if(getMesh().getPrimitiveType() == GL.GL_TRIANGLE_STRIP){
				Toast.error("Error: We couldn't\nconvert to triangle list.",3f);
				wireFrame = false;
			}else{
				object_wire = new ModelObject(new WireFrameObject(getMesh()));
			}
		}else if(!wireFrame && !keep_selector && object_wire != null){
			object_wire.delete();
			object_wire = null;
		}
		if(wireFrame){
			object_wire.setTransform(getTransform());
		}
	}

	@Override
	public void render(DefaultShader shader){
		if(!wireFrame){
			super.render(shader);
		}
	}

	@Override
	public void delete() {
		if(getMesh() == null){
			return;
		}
		if(bound_wire != null){
			bound_wire.delete();
			bound_wire = null;
		}
		if(origin != null){
			origin.delete();
			origin = null;
		}
		Zmdl.app().getTextureManager().removeMesh(getMesh());
		super.delete();
	}
	
	private class Origin extends Sphere
	{
		public Origin(){
			super(0.03f,10,10);
			getPart(0).material.color.set(255,245,20);
		}
		
		@Override
		public void preRender() {
			FX.gl.glDisable(GL.GL_DEPTH_TEST);
		}
		
		@Override
		public void postRender() {
			FX.gl.glEnable(GL.GL_DEPTH_TEST);
		}
	}
}
