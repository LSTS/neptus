/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.graph;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import pt.up.fe.dceg.neptus.gui.PropertiesProvider;

public interface NeptusGraphElement<O> extends PropertiesProvider {	
	
	public O getUserObject();
	
	public void setUserObject(O userObject);
	
	public String getElementName();
	
	public String getID();
	
	public void setID(String id);
	
	public void paint(Graphics2D g, NeptusGraph<?, ?> graph);
	
	public boolean containsPoint(Point2D point);
	
	public void setSelected(boolean selected);
	
	public boolean isSelected();
	
	public void cleanup();
}
