/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Dec 16, 2012
 */
package pt.up.fe.dceg.neptus.plugins.mra3d;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.AbstractWireframeable;
import org.jzy3d.plot3d.primitives.ISortableDraw;
import org.jzy3d.plot3d.rendering.view.Camera;
import org.jzy3d.plot3d.text.align.Halign;
import org.jzy3d.plot3d.text.align.Valign;
import org.jzy3d.plot3d.text.renderers.TextBitmapRenderer;

/**
 * @author zp
 *
 */
public class Marker3d extends AbstractWireframeable implements ISortableDraw {

    protected String label;
    protected Coord3d center;
    protected Color color;
    
    public Marker3d(String text, Coord3d pos, java.awt.Color color) {
        this.label = text;
        this.center = pos;
        this.color = new Color(color);        
    }
    
    @Override
    public void draw(GL2 gl, GLU glu, Camera cam) {
        if(transform!=null)
            transform.execute(gl);
        
        gl.glPointSize(3.0f);
        
        gl.glBegin(GL.GL_POINTS);
        gl.glColor4f(color.r, color.g, color.b, color.a);
        gl.glVertex3f(center.x, center.y, center.z);
        gl.glEnd();
        
        TextBitmapRenderer txt = new TextBitmapRenderer();
        txt.drawText(gl, glu, cam, label, center, Halign.LEFT, Valign.CENTER, color);
    }

    @Override
    public double getDistance(Camera camera) {
        return center.distance(camera.getEye());        
    }
    
    @Override
    public double getShortestDistance(Camera camera) {
        return getDistance(camera);
    }
    
    @Override
    public double getLongestDistance(Camera camera) {
        return getDistance(camera);
    }
}
