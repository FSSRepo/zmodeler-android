package com.fastsmartsystem.saf.processors;
import java.util.*;
import com.forcex.gfx3d.*;
import com.forcex.gfx3d.shapes.*;
import com.forcex.anim.*;
import com.forcex.collision.*;
import com.forcex.math.*;
import com.forcex.*;
import com.forcex.core.*;

public class SkeletonObject extends ModelObject {
	BoundingBox bound;
	public SkeletonNode attach;
	public boolean selected = false;
	
	public SkeletonObject(){
		super();
		bound = new BoundingBox();
		bound.max = new Vector3f(0.02f);
		bound.min = new Vector3f(-0.02f);
		bound.calculateExtents();
		setMesh(new SpecialBox());
	}
	
	public boolean intersect(Ray ray){
		return bound.intersectRay(getPosition(),ray);
	}

	@Override
	public void update() {
		super.update();
		getMesh().useGlobalColor = selected;
	}
	
	private static class SpecialBox extends Box {
		public SpecialBox() {
			super(0.02f,0.02f,0.02f);
			getPart(0).material.color.set(
				(short)(Math.max(Math.random(),0.2f) * 255),
				(short)(Math.max(Math.random(),0.1f) * 255),
				(short)(Math.max(Math.random(),0.2f) * 255)
			);
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
