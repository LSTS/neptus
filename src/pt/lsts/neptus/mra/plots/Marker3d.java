/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Dec 16, 2012
 */
package pt.lsts.neptus.mra.plots;

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
        this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
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
