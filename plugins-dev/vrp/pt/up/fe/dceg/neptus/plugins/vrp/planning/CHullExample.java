/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Rui Gonçalves
 * 2010/04/14
 */
package pt.up.fe.dceg.neptus.plugins.vrp.planning;

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