package org.traxnet.shadingzen.core;

import android.opengl.Matrix;
import org.traxnet.shadingzen.math.Matrix4;
import org.traxnet.shadingzen.math.Vector3;
import org.traxnet.shadingzen.math.Vector4;

/*
 * Basic camera class. 
 * 
 * By convention:
 * 	+y coord is UP
 *  +z coord is FORWARD, inside the screen
 *  +x coord is RIGHT 
 */
public class Camera extends Actor {
	protected Vector3 _dir = new Vector3(0.f, 0.f, 1.f);
    protected Vector3 _up = new Vector3(0.f, 1.f, 0.f);
    protected Vector3 _right  = new Vector3(1.f, 0.f, 0.f);

    float [] frustum = new float[16];
	
	// Variables to calculate view frustum volume
	protected float _fov, _aspect, _near, _far;
	protected float _viewportHeight, _viewportWidth;
	
	protected Matrix4 _projectionMatrix = null;
	protected Matrix4 _orthoProjectionMatrix = null;
	protected Matrix4 _viewMatrix = null;
    protected Matrix4 _cachedViewProjectionMatrix = null;
	
	protected boolean _isTrackingAnActor = false;
	protected Actor _trackedActor = null;
    protected boolean _mvpIsDirty = true;
	
	float [] viewmatrxi_data = new float[16];
	
	public Camera(){
		_viewMatrix = Matrix4.identity();
		_position = new Vector3();
		_dir = new Vector3();
        _cachedViewProjectionMatrix = Matrix4.identity();
	}
	public void setValues(Vector3 position, float fov, float aspect, float near, float far){
		float vfov = (float) ((float)2*Math.atan( Math.tan(fov/2.0)/(aspect) ));
		// this is the real fov:
		_fov = (float) ((float)2*Math.atan( Math.tan(vfov/2.0f)*aspect ));
		
		_position = position;
		//_fov = fov;
		_aspect = aspect;
		_near = near;
		_far = far;
		_dir = new Vector3(0.f, 0.f, 1.f);
	}
	
	public void setViewportSize(float width, float height){
		_viewportHeight = height;
		_viewportWidth = width;
		
		buildOrthoProjectionMatrix();
	}
	
	public final float getViewportWidth(){
		return _viewportWidth;
	}
	
	public final float getViewportHeight(){
		return _viewportHeight;
	}
	
	public void setAspect(float fov, float aspect){
		float vfov = (float) ((float)2*Math.atan( Math.tan(fov/2.0)/(aspect) ));
		// this is the real fov:
		_fov = (float) ((float)2*Math.atan( Math.tan(vfov/2.0f)*aspect ));
		_aspect = aspect;
        _mvpIsDirty = true;
	}
	
	public final Vector3 getDirection(){
		return _dir;
	}

    /** Sets the camera view _temp_dir without performing a normalization */
    public void setDirUnsafe(float x, float y, float z){
        _dir.x = x;
        _dir.y = y;
        _dir.z = z;
        _mvpIsDirty = true;
    }
	public void setDirection(Vector3 dir){
		_dir.set(dir);
        _dir.normalizeNoCopy();
        _mvpIsDirty = true;
	}
	public void setDirection(float x, float y, float z){
        _dir.set(x, y, z);
        _dir.normalizeNoCopy();
        _mvpIsDirty = true;
	}

    @Override
    public void setPosition(float x, float y, float z) {
        super.setPosition(x, y, z);

        _mvpIsDirty = true;
    }

    @Override
    public void setPosition(Vector3 v) {
        setPosition(v.x, v.y, v.z);
    }

    public void setCameraUp(float x, float y, float z){
        _up.x = x;
        _up.y = y;
        _up.z = z;
        _mvpIsDirty = true;
    }

    public final Vector3 getCameraUp(){ return _up; }

    public final Vector3 getCameraRight(){ return _up.cross(_dir).normalize(); }
	
	/***
	 * Returns the View matrix for this camera.
	 * We pass this to the shaders as an openGL SL uniform.
	 * @return View matrix for this camera.
	 */
	public final Matrix4 getViewMatrix(){
		Matrix.setLookAtM(_viewMatrix.getAsArray(), 0, _position.getX(), _position.getY(), _position.getZ(), _position.getX()+_dir.getX(), _position.getY()+_dir.getY(), _position.getZ()+_dir.getZ(), _up.x, _up.y, _up.z);
		return _viewMatrix;
	}
	
	
	/***
	 * Returns the Projection matrix for this camera
	 * We pass this to the shaders as an openGL SL uniform.
	 * @return  Projection matrix for this camera.
	 */
	public final Matrix4 getProjectionMatrix(){
		if(null == _projectionMatrix)
			_projectionMatrix = buildProjectionMatrix(_projectionMatrix);
		
		return _projectionMatrix;
		
	}

    public final Matrix4 getViewProjectionMatrix(){
        if(_mvpIsDirty){

            Matrix4 projectionMatrix = getProjectionMatrix();
            Matrix4 viewMatrix = getViewMatrix();

            Matrix.multiplyMM(_cachedViewProjectionMatrix.getAsArray(), 0, projectionMatrix.getAsArray(), 0, viewMatrix.getAsArray(), 0);
            _mvpIsDirty = false;
        }

        return _cachedViewProjectionMatrix;
    }
	
	/*
	 * Builds a projection matrix with the given parameters
	 * 
	 * See Eq 3.78 at Real-Time Rendering by Tomas Moller and Eric Haines
	 */
	private Matrix4 buildProjectionMatrix(Matrix4 matrix){
		
		float xmin, xmax, ymin, ymax;

		float doublenear, one_deltay, one_deltaz, one_deltax;
		
		xmax = _near * (float)Math.tan(_fov*0.5f);
		xmin = -xmax;
		
		ymax = xmax / _aspect;
		ymin = -ymax;
		
		doublenear = _near*2.f;
		one_deltax = 1.f / (xmax - xmin);
		one_deltay = 1.f / (ymax - ymin);
		one_deltaz = 1.f / (_far - _near);
		
		frustum[0] = doublenear * one_deltax;
		frustum[1] = 0.f;
		frustum[2] = 0.f;
		frustum[3] = 0.f;
		frustum[4] = 0.f;
		frustum[5] = doublenear * one_deltay;
		frustum[6] = 0.f;
		frustum[7] = 0.f;
		frustum[8] = (xmax + xmin) * one_deltax;
		frustum[9] = (ymax + ymin) * one_deltay;
		frustum[10] = -(_far + _near) * one_deltaz;
		frustum[11] = -1.f;
		frustum[12] = 0.f;
		frustum[13] = 0.f;
		frustum[14] = -(_far * doublenear) * one_deltaz;
		frustum[15] = 0.f;

        if(null == matrix)
            return new Matrix4(frustum);

        matrix.set(frustum);
		
		return matrix;
	}
	
	void buildOrthoProjectionMatrix(){
		int viewport_h = (int)_viewportHeight;
		int viewport_w = (int)_viewportWidth;
		
		// Calcuate view aspect factor to normalize everything to a 800x480 screen
		float view_factor_x = (float)viewport_w/(float)480;
		float view_factor_y = (float)viewport_h/(float)800;
		
		
		float [] ortho_matrix2 = {
				view_factor_x*2.f/viewport_w, 		   	0.f, 	0.f, 0.f,
                0.f, view_factor_y*2.f/viewport_h, 		0.f, 	0.f,
                0.f, 					  			 	0.f, 	1.f, 0.f,
                -1.f, 					  				-1.f, 	1.f, 1.f
		};
		
		this._orthoProjectionMatrix = new Matrix4(ortho_matrix2);
	}
	
	public final Matrix4 getOrthoProjectionMatrix(){
		return this._orthoProjectionMatrix;
	}
	
	/***
	 * Compute a ray direction in world-space given a pair of coordinates in viewport space (for example an screen touch coordinate).
	 * @param x Point X in screen/viewport space
	 * @param y Point Y in screen/viewport space
	 
	  	normalised_x = 2 * mouse_x / win_width - 1
		normalised_y = 1 - 2 * mouse_y / win_height
		// note the y pos is inverted, so +y is at the top of the screen
		
		unviewMat = (projectionMat * modelViewMat).inverse()
		
		near_point = unviewMat * Vec(normalised_x, normalised_y, 0, 1)
		camera_pos = ray_origin = modelViewMat.inverse().col(4)
		ray_dir = near_point - camera_pos
	 
	 */
	float mvp_f[] = new float[16];
	float [] inv_mvp_f = new float[16];
	
	public final Vector3 rayDirectionForViewportCoordinate(float x, float y){
		float normalized_x = 2.f * x / _viewportWidth - 1.f;
		float normalized_y = 1 - 2.f * y / _viewportHeight;
		

		Matrix.multiplyMM(mvp_f, 0, this.getProjectionMatrix().getAsArray(), 0, this.getViewMatrix().getAsArray(), 0);	
		Matrix.invertM(inv_mvp_f, 0, mvp_f, 0);
		
		Matrix4 inv_mvp = new Matrix4(inv_mvp_f);
		Vector4 near_point = inv_mvp.mul(new Vector4(normalized_x, normalized_y, 0 , 1));
		
		float w = 1.f/near_point.getW();
		Vector3 point = new Vector3(near_point.getX()*w, near_point.getY()*w, near_point.getZ()*w);
		point.subNoCopy(_position);
		point.normalizeNoCopy();
		return point;
	}

    /** Transform a point into screen space using the view and projection matrices
     *
     * @return The x,y values of the point in screen space (-1,1)
     */
    public void projectPointScreenSpace(float [] ret, float [] point){
        Matrix4 mvp = getViewProjectionMatrix();

        point[3] = 1.f;
        Matrix.multiplyMV(ret, 0, mvp.getAsArray(), 0, point, 0);
        ret[0] /= ret[3];
        ret[1] /= ret[3];

        //Vector4 screen_space = mvp.mul(new Vector4(x, y, z, 1.f));
        //return new Vector2(screen_space.x/screen_space.w, screen_space.y/screen_space.w);
    }

    /** Transform a point into screen space using the view and projection matrices.
     * The point is then further transformed by the VIEWPORT matrix as follow:
     *
     * [w/2   0   w/2+x]
     * [ 0   h/2  h/2+y]
     * [ 0    0     1  ]
     *
     * @return The x,y values of the point in viewport space (0,width)x(0,height)
     */
    public void projectPointViewportSpace(float [] ret, float[] point){
         projectPointScreenSpace(ret, point);
        ret[0] = (ret[0]+1)*_viewportWidth*0.5f;
        ret[1] = (ret[1]+1)*_viewportHeight*0.5f;
    }
	
	@Override
	public void onDraw(RenderService renderer) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onLoad() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onUnload() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onUpdate(float deltaTime) {
		
		if(_isTrackingAnActor){
			this._dir.set(this._trackedActor.getPosition().sub(_position));
			//this._dir.sub(_position);
			this._dir.normalizeNoCopy();
            _mvpIsDirty = true;
		}
	}
	
	/*public Matrix4 getViewMatrix(){
	float [] matrix = new float[16];
	
	float PI = 3.1415926535898f;
	
	float sy = FloatMath.sin(_yaw+PI*0.5f);
	float cy = FloatMath.cos(_yaw+PI*0.5f);
	float sp = FloatMath.sin(_pitch-PI*0.5f);
	float cp = FloatMath.cos(_pitch-PI*0.5f);
	float sr = FloatMath.sin(_roll);
	float cr = FloatMath.cos(_roll);
	
	matrix[0] = cr*cy + sr*sp*sy;
	matrix[1] = cp*sy;
	matrix[2] = -sr*cy + cr*sp*sy;
	matrix[3] = 0.f;
	
	matrix[4] = -cr*sy + sr*sp*cy;
	matrix[5] = cp*cy;
	matrix[6] = sr*sy + cr*sp*cy;
	matrix[7] = 0.f;
	
	matrix[8] = sr*cp;
	matrix[9] = -sp;
	matrix[10] = cr*cp;
	matrix[11] = 0.f;
	
	matrix[12] = _position.getX();
	matrix[13] = _position.getY();
	matrix[14] = _position.getZ();
	matrix[15] = 1.f;
		
	return new Matrix4(matrix);
}*/
	
	public void trackActor(Actor tracked_actor){
		this._trackedActor = tracked_actor;
		this._isTrackingAnActor = true;
	}
	
	public void untrackActor(){
		this._trackedActor = null;
		this._isTrackingAnActor = false;
	}
}
