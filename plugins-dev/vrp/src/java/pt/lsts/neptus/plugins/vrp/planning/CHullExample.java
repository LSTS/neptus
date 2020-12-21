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
 * Author: Rui Gonçalves
 * 2010/04/14
 */
package pt.lsts.neptus.plugins.vrp.planning;

/**
 * @author Rui Gonçalves
 *
 */


import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Point2d;

class DrawPanel extends JPanel {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public void paintComponent(Graphics g) {
	super.paintComponent(g);
	Graphics2D g2 = (Graphics2D) g;
	int size = 60;
	int rad = 4;
	Random r = new Random();
	ArrayList<Point2d> array = new ArrayList<Point2d>(size);
	for (int i = 0; i < size; i++) {
	    double x = r.nextInt(350) + 15.23453;
	    double y = r.nextInt(350) + 15.345345;
	    array.add(new Point2d(x,y));
	    //System.err.println("POINT:" +array.get(i));
	    g2.draw(new Ellipse2D.Double(x-2,y-2,rad,rad));
	}
	Collections.sort(array, new Comparator<Point2d>() {
		public int compare (Point2d pt1, Point2d pt2) {
		    double r = pt1.x - pt2.x;
		    if (r != 0)
		    {
		    	if(r<0) return -1;
		    	else return 1;
		    }
			else
			{
				if((pt1.y - pt2.y)<0) return -1;
				else return 1; 
			}
		}
	    });
	//System.err.println("-------------------");
	//for (int i = 0; i < size; i++)
	// System.err.println("POINT:" +array.get(i));
	ArrayList<Point2d> hull = CHull.cHull(array);
	//System.err.println("-------Array------------");
	//for (int i = 0; i < hull.size(); i++)
	//	 System.err.println("POINTHull:" +hull.get(i));
	//System.err.println("---------Passsouuuu ---------");
	
	
	Iterator<Point2d> itr = hull.iterator();
	Point2d prev = itr.next();
	Point2d curr = null;
	while (itr.hasNext()) {
	    curr = itr.next();
	    g2.drawLine((int)prev.x, (int)prev.y, (int)curr.x, (int)curr.y);
	    prev = curr;
	}
	curr = hull.get(0);
	g2.drawLine((int)prev.x, (int)prev.y, (int)curr.x,(int) curr.y);
	
	Point2d base=hull.get(0);
	hull=CHull.resizePath(4,hull, array, hull.get(0));
	
	itr = hull.iterator();
	prev = itr.next();
	curr = null;
	g2.setColor(Color.RED);
	 g2.draw(new Ellipse2D.Double(base.x,base.y,rad,rad));
	while (itr.hasNext()) {
	    curr = itr.next();
	    g2.drawLine((int)prev.x, (int)prev.y, (int)curr.x,(int) curr.y);
	    prev = curr;
	}
	curr = hull.get(0);
	g2.drawLine((int)prev.x, (int)prev.y, (int)curr.x, (int)curr.y);
	
    }
}

public class CHullExample extends JFrame {
    /**
	 * 
	 */
	private static final long serialVersionUID = -3268652136882271864L;

	public CHullExample() {
	setSize(400,400);
	setLocation(100,100);
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	DrawPanel dp = new DrawPanel();
	Container cp = this.getContentPane();
	cp.add(dp);
    }

    public static void main(String[] args) {
	new CHullExample().setVisible(true);
    }
}