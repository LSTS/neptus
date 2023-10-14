/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 2010/04/15
 */
package pt.lsts.neptus.plugins.vrp.planning;

import javax.vecmath.Point2d;

import drasys.or.DoubleI;
import drasys.or.geom.rect2.Point;
import drasys.or.geom.rect2.PointI;


/**
 * @author Rui Gonçalves
 *
 */

class Customer extends Point implements DoubleI
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	double load;
 
	PointI screenPoint;
    Point2d point2d;
    
    Customer(double ld, PointI projectedPt, PointI screenPt)
    {
        super(projectedPt);
        load = ld;
        screenPoint = screenPt;
    }
    
    Customer(double ld, double xx,double yy, PointI screenPt)
    {
        super(xx,yy);
        load = ld;
        screenPoint = screenPt;
       
    }
    
    
    Customer(double ld, Point2d p2d)
    {
        super(p2d.x,p2d.y);
        load = ld;
        screenPoint = null;
       point2d=p2d;
    }
    
    public void setLoad(double load) {
		this.load = load;
	}
    
    public double doubleValue(){return load;}
}

