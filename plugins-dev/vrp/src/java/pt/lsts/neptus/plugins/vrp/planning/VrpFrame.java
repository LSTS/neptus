/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Rui Gonçalves
 * 2010/04/20
 */
package pt.lsts.neptus.plugins.vrp.planning;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Point2d;

import pt.lsts.neptus.NeptusLog;


/**
 * @author Rui Gonçalves
 *
 */

public class VrpFrame extends JFrame {
    /**
	 * 
	 */
	private static final long serialVersionUID = -3268652136882271864L;

	
	
	public VrpFrame() {
	super();
	setSize(400,400);
	setLocation(100,100);
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
	int size=70;
	Vector<Point2d> array = new Vector<Point2d>(size);
	
	
	double x = 20.23453;
    double y = 20.345345;
    
    Point2d depot=new Point2d(x,y);
    
	for (int i = 0; i < size; i++) {
		Random r = new Random();
	    x = r.nextInt(350) + 15.23453;
	    y = r.nextInt(350) + 15.345345;
	    array.add(new Point2d(x,y));
	}
	
	long time = System.nanoTime();
	Vector<Vector<Point2d>> paths=VrpManager.computePathsSingleDepot(depot,array, 5);
	long time2 = System.nanoTime();
	
	NeptusLog.pub().info("<###>Time:"+((double)(time2-time)*1E-9d));
	
	
	VrpDrawPanel dp = new VrpDrawPanel(depot,array,paths);
	Container cp = this.getContentPane();
	cp.add(dp);
    }
	
	  public class VrpDrawPanel extends JPanel {
		  /**
         * 
         */
        private static final long serialVersionUID = 1L;
        Color[] colors = {Color.black, Color.green, Color.yellow, Color.red, Color.blue,Color.gray, Color.orange,Color.pink};
	    	Point2d depot;
	    	Vector<Point2d> array;
	    	Vector<Vector<Point2d>> paths;
	    	
	    	public VrpDrawPanel(Point2d d, Vector<Point2d> a,Vector<Vector<Point2d>> p)
	    	{
	    		super();
	    		depot=d;
	    		array=a;
	    		paths=p;
	    	}
	    	
	        public void paintComponent(Graphics g) {
	    	super.paintComponent(g);
	    	
	    	int rad=4;
	    	
	    	Graphics2D g2 = (Graphics2D) g;
	    	
	    	
	    	
	    	for(Point2d p:array)
	    	{
	    		g2.draw(new Ellipse2D.Double(p.x-2,p.y-2,rad,rad));
	    	}
	    	
	    	int i=0;
	    	for(Vector<Point2d> path:paths)
	    	{
	    		g2.setColor(colors[i++]);
	    		Iterator<Point2d> itr = path.iterator();
	    		Point2d prev = itr.next();
	    		g2.drawLine((int)depot.x, (int)depot.y, (int)prev.x, (int)prev.y);
	    		Point2d curr = null;
	    		while (itr.hasNext()) {
	    			curr = itr.next();
	    			g2.drawLine((int)prev.x, (int)prev.y, (int)curr.x, (int)curr.y);
	    			prev = curr;
	    		}
	    	}
	        }

	    }

	
	
public static void main(String[] args) {
	
	VrpFrame frame=new VrpFrame();
	frame.setVisible(true);
	
	
}
}