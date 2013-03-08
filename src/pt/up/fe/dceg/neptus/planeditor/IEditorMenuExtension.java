/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2010/04/25
 * $Id:: IEditorMenuExtension.java 9615 2012-12-30 23:08:28Z pdias              $:
 */
package pt.up.fe.dceg.neptus.planeditor;

import java.util.Collection;

import javax.swing.JMenuItem;

import pt.up.fe.dceg.neptus.types.coord.LocationType;


/**
 * @author ZP
 *
 */
public interface IEditorMenuExtension {

	public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source);  
	
}
