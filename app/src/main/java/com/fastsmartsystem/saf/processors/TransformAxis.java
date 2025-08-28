package com.fastsmartsystem.saf.processors;
import com.forcex.gfx3d.shapes.*;
import com.forcex.gfx3d.*;
import com.forcex.collision.*;
import com.forcex.math.*;
import com.forcex.gfx3d.shader.*;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.gui.*;

public class TransformAxis extends ModelObject {
	ModelObject coneX,coneY,coneZ,boxX,boxY,boxZ;
	BoundingBox axisX,axisY,axisZ;
	public boolean show_long_axis;
	
	public TransformAxis() {
		super(new Dummy());
		coneX = new ModelObject(new DummyCone());
		coneY = new ModelObject(new DummyCone());
		coneZ = new ModelObject(new DummyCone());
		boxX = new ModelObject(new DummyBox(100f,0.6f,0.6f));
		boxY = new ModelObject(new DummyBox(0.6f,100f,0.6f));
		boxZ = new ModelObject(new DummyBox(0.6f,0.6f,100f));
		coneX.setRotation(0,0,90);
		coneY.setRotation(0,0,-180);
		coneZ.setRotation(-90,0,0);
		coneX.getMesh().getPart(0).material.color.set(255,0,0);
		coneY.getMesh().getPart(0).material.color.set(0,255,0);
		coneZ.getMesh().getPart(0).material.color.set(0,0,255);
		boxX.getMesh().getPart(0).material.color.set(255,0,0,50);
		boxY.getMesh().getPart(0).material.color.set(0,255,0,50);
		boxZ.getMesh().getPart(0).material.color.set(0,0,255,50);
		axisX = new BoundingBox();
		axisY = new BoundingBox(); 
		axisZ = new BoundingBox(); 
		axisX.extent.set(0.5f,0.08f,0.08f); axisX.center.set(0.7f,0,0);
		axisY.extent.set(0.08f,0.5f,0.08f); axisY.center.set(0,0.7f,0);
		axisZ.extent.set(0.08f,0.08f,0.5f); axisZ.center.set(0,0,0.7f);
	}
	
	public int testRay(Ray ray) {
		if((isVisible() || boxX.isVisible()) && axisX.intersectRay(getPosition(),ray)){
			return 0;
		}
		if((isVisible() || boxY.isVisible()) && axisY.intersectRay(getPosition(),ray)){
			return 1;
		}
		if((isVisible() || boxZ.isVisible()) &&  axisZ.intersectRay(getPosition(),ray)){
			return 2;
		}
		return -1;
	}
	
	public void setShowPointer(int axis) {
		boxX.setVisible(false);
		boxY.setVisible(false);
		boxZ.setVisible(false);
		show_long_axis = (axis != -1);
		setVisible(!show_long_axis);
		if(axis == -1){
			axisX.extent.set(0.5f,0.15f,0.15f); axisX.center.set(0.65f,0,0);
			axisY.extent.set(0.15f,0.5f,0.15f); axisY.center.set(0,0.65f,0);
			axisZ.extent.set(0.15f,0.15f,0.5f); axisZ.center.set(0,0,0.65f);
		}else{
			switch(axis){
				case 0:
					boxX.setVisible(true);
					axisX.extent.set(100f,0.6f,0.6f); axisX.center.set(0,0,0);
					break;
				case 1:
					boxY.setVisible(true);
					axisY.extent.set(0.6f,100f,0.6f); axisY.center.set(0,0,0);
					break;
				case 2:
					boxZ.setVisible(true);
					axisZ.extent.set(0.6f,0.6f,100f); axisZ.center.set(0,0,0);
					break;
			}
			
		}
	}

	@Override
	public void update() {
		Vector3f pos = getPosition();
		coneX.setPosition(pos.x + 1,pos.y,pos.z);
		coneY.setPosition(pos.x,pos.y + 1,pos.z);
		coneZ.setPosition(pos.x,pos.y,pos.z + 1);
		boxX.setPosition(pos.x,pos.y,pos.z);
		boxY.setPosition(pos.x,pos.y,pos.z);
		boxZ.setPosition(pos.x,pos.y,pos.z);
		super.update();
	}
	
	private static class Dummy extends Axis {
		public Dummy() {
			super();
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
	
	private static class DummyCone extends Cone {
		public DummyCone() {
			super(8,8,0.04f,0.1f,true);
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
	
	private static class DummyBox extends Box {
		public DummyBox(float x,float y,float z) {
			super(x,y,z);
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
