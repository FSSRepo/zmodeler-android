package com.fastsmartsystem.saf.processors;
import com.forcex.app.threading.*;
import com.forcex.gfx3d.*;
import com.forcex.anim.*;
import com.forcex.math.*;
import com.forcex.core.gpu.*;
import com.forcex.*;

public class CPUAnimation implements Task {
	boolean stop = false;
	Mesh mesh;
	Animator anim;
	Matrix3f[] boneMatricesNormal;
	float[] temp_vert;
	float[] temp_norm;
	float[] dst_v;
	float[] dst_n = null;
	Vector3f v = new Vector3f();
	Vector3f n = new Vector3f();
	
	public CPUAnimation(Animator anim,Mesh mesh) {
		this.mesh = mesh;
		this.anim = anim;

		temp_vert = mesh.getVertexData().vertices;
		temp_norm = mesh.getVertexData().normals;
		dst_v = new float[mesh.getVertexInfo().vertexCount * 3];
		if(mesh.getVertexInfo().hasNormals()){
			dst_n = new float[mesh.getVertexInfo().vertexCount * 3];
		}
		boneMatricesNormal = new Matrix3f[anim.getBoneMatrices().length];
	}
	
	@Override
	public boolean execute() {
		if(!stop) {
			anim.updateBonesMatrices();
			int vcount = mesh.getVertexInfo().vertexCount;
			Matrix4f[] boneMatrices = anim.getBoneMatrices();
			for(int i = 0;i < boneMatrices.length;i++){
				boneMatricesNormal[i] = boneMatrices[i].getUpperLeft().invert().transpose();
			}
			VertexData data = mesh.getVertexData();
			for(int i = 0, j = 0,k = 0;i < vcount;i++,j += 3,k += 4){
				v.set(temp_vert[j],temp_vert[j+1],temp_vert[j+2]);
				Vector3f vs = boneMatrices[data.bone_indices[k]].mult(data.bone_weights[k]).mult(v);
				vs.addLocal(boneMatrices[data.bone_indices[k + 1]].mult(data.bone_weights[k + 1]).mult(v));
				vs.addLocal(boneMatrices[data.bone_indices[k + 2]].mult(data.bone_weights[k + 2]).mult(v));
				vs.addLocal(boneMatrices[data.bone_indices[k + 3]].mult(data.bone_weights[k + 3]).mult(v));

				dst_v[j] = vs.x;
				dst_v[j+1] = vs.y;
				dst_v[j+2] = vs.z;

				if(dst_n != null) {
					n.set(temp_norm,j);
					Vector3f ns = boneMatricesNormal[data.bone_indices[k]].mult(data.bone_weights[k]).mult(n);
					ns.addLocal(boneMatricesNormal[data.bone_indices[k + 1]].mult(data.bone_weights[k + 1]).mult(n));
					ns.addLocal(boneMatricesNormal[data.bone_indices[k + 2]].mult(data.bone_weights[k + 2]).mult(n));
					ns.addLocal(boneMatricesNormal[data.bone_indices[k + 3]].mult(data.bone_weights[k + 3]).mult(n));

					dst_n[j] = ns.x;
					dst_n[j + 1] = ns.y;
					dst_n[j + 2] = ns.z;
				}
			}
			FX.gpu.queueTask(() -> {
				mesh.setVertices(dst_v);
				if(dst_n != null){
					mesh.setNormals(dst_n);
				}
				return true;
			});
			return false;
		} else {
			FX.gpu.queueTask(() -> {
				mesh.setVertices(temp_vert);
				if(dst_n != null) {
					mesh.setNormals(temp_norm);
				}
				return true;
			});
			return true;
		}
	}
	
	public void stop() {
		stop = true;
	}
}
