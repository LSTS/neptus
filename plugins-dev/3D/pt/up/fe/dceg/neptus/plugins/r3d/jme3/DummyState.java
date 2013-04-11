package pt.up.fe.dceg.neptus.plugins.r3d.jme3;

import java.awt.EventQueue;

import pt.up.fe.dceg.neptus.NeptusLog;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

public class DummyState extends AbstractAppState {

    Geometry geom;
    int count;

    Camera cam;

    /*
     * (non-Javadoc)
     * 
     * @see com.jme3.app.state.AbstractAppState#initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application)
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        // TODO Auto-generated method stub
        super.initialize(stateManager, app);
        NeptusLog.pub().info("<###>simpleInitApp EvtDispatchThread? " + EventQueue.isDispatchThread() + " Thread id:"
                + Thread.currentThread().getId());
        Box b = new Box(Vector3f.ZERO, 1, 1, 1); // create cube shape at the origin
        geom = new Geometry("Box", b); // create cube geometry from the shape
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md"); // create a simple
                                                                                                 // material
        mat.setColor("Color", ColorRGBA.Blue); // set color of material to blue
        geom.setMaterial(mat); // set the cube's material
        ((SimpleApplication) app).getRootNode().attachChild(geom); // make the cube appear in the scene
        cam = ((SimpleApplication) app).getCamera();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.jme3.app.state.AbstractAppState#update(float)
     */
    @Override
    public void update(float tpf) {
        // TODO Auto-generated method stub
        super.update(tpf);
        if (count > 1000) {
            NeptusLog.pub().info("<###>update EvtDispatchThread? " + EventQueue.isDispatchThread() + " Thread id:"
                    + Thread.currentThread().getId());
            System.out.println(geom.checkCulling(cam));
            count = 0;
        }
        count++;
    }

}
