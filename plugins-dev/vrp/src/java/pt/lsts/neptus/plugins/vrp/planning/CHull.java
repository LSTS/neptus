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
 * 2010/04/15
 */
package pt.lsts.neptus.plugins.vrp.planning;

/**
 * @author Rui Gonçalves
 *
 */
import java.util.ArrayList;

import javax.vecmath.Point2d;


class CHull {
    
    //Returns the determinant of the point matrix
    //This determinant tells how far p3 is from vector p1p2 and on which side it is
    static double distance(Point2d p1, Point2d p2, Point2d p3) {
    	//if(p1==null || p2==null || p3==null) return 0;
    	//System.err.println("p1:" +p1 + "\np2:"+p2);
	double x1 = p1.x;
	double x2 = p2.x;
	double x3 = p3.x;
	double y1 = p1.y;
	double y2 = p2.y;
	double y3 = p3.y;
	//System.err.println("RETURN:"+ (x1*y2 + x3*y1 + x2*y3 - x3*y2 - x2*y1 - x1*y3));
	return x1*y2 + x3*y1 + x2*y3 - x3*y2 - x2*y1 - x1*y3;
    }

    //Returns the points of convex hull in the correct order
    static ArrayList<Point2d> cHull(ArrayList<Point2d> array) {
	int size = array.size();
	if (size < 2)
	    return null;
	Point2d l = array.get(0);
	Point2d r = array.get(size - 1);
	//System.err.println("LLLLLLLLLLLLLLLLLLLLLLLL"+l+"\nRRRRRRRRRRRRRRRRRRRRR"+r);
	ArrayList<Point2d> path = new ArrayList<Point2d>();
	path.add(l);
	cHull(array, l, r, path,50);
	path.add(r);
	cHull(array, r, l, path,50);
	return path;
    }

    static void cHull(ArrayList<Point2d> points, Point2d l, Point2d r, ArrayList<Point2d> path,int i) {
    	if(i==0) return; // prevenir deathlock
    //	System.err.println("LLLLLLLLLLLLLLLLLLLLLLLL"+l+"\nRRRRRRRRRRRRRRRRRRRRR"+r);
   // 	System.err.println("SSSSSSSSSSSSIZE:"+points.size());
	if (points.size() < 3)
	    return;
	double maxDist = 0.0;
	double tmp;
	Point2d p = null;
	for (Point2d pt : points) {
	    if (pt != l && pt != r) {
		tmp = distance(l, r, pt);
		if (tmp > maxDist) {
		    maxDist = tmp;
		    p = pt;
		}
		
	//	if(p==null) p=pt;
		
		
	    }
	}
	//System.err.println("PPPPPPPPPPPPPPPPPP"+p);
	if(p==null) return;
	ArrayList<Point2d> left = new ArrayList<Point2d>();
	ArrayList<Point2d> right = new ArrayList<Point2d>();
	left.add(l);
	right.add(p);
	for (Point2d pt : points) {
	    if (distance(l, p, pt) > 0)
		left.add(pt);
	    else if (distance(p, r, pt) > 0)
		right.add(pt);
	}
	left.add(p);
	right.add(r);
	cHull(left, l, p, path,i-1);
	path.add(p);
	cHull(right, p, r, path,i-1);
    }
    
    static ArrayList<Point2d> resizePath(int n,ArrayList<Point2d> path, ArrayList<Point2d> points,Point2d fixed) {
    	int size=path.size();
    	
    	
    	if(path.size()>n)
    	{
    		
    		while (size>n)
    		{
    			//double min=path.get(0).distanceSq(path.get(1))+
    			//			path.get(0).distanceSq(path.get(path.size()-1));

    			double incaux=0;
    			for (int x=1;x<path.size(); x++)
    			{
   					if(x==path.size()-1)
   						incaux+=path.get(x).distanceSquared(path.get(0));
   					else
   						incaux+=path.get(x).distanceSquared(path.get(x+1));
    			}
    			
    			double maxaux=incaux;
    			int index=0;
    			
    			if(path.get(0)==fixed)
    			{
    				maxaux=0;
    			}
    			
    			for (int i = 1 ; i<path.size() ; i++)
    			{
    				
    				incaux=0;
    				for (int x=0;x<path.size(); x++)
    					if (x!=i)
    					{
    						if(x==path.size()-1)
    							incaux+=path.get(x).distanceSquared(path.get(0));
    						else
    							incaux+=path.get(x).distanceSquared(path.get(x+1));
    					}
    				
    				if(incaux>maxaux)
    				{
    					if(path.get(i)!=fixed)
    					{
    						maxaux=incaux;
    						index=i;
    					}
    				}
    			}
    			//NeptusLog.pub().info("<###>ciclo");
    			path.remove(index);
    			size=path.size();
    		}
    	}
    	/*if(path.size()<n)
    	{
    		while (size<n)
    		{
    		
    		}
    	}*/
    	return path;
    }
    
}