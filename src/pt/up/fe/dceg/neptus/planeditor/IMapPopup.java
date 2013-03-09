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
 */
package pt.up.fe.dceg.neptus.planeditor;

import java.util.Collection;

import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;

/**
 * @author ZP
 *
 */
public interface IMapPopup {

	public boolean addMenuExtension(IEditorMenuExtension extension);
	public boolean removeMenuExtension(IEditorMenuExtension extension);
	public Collection<IEditorMenuExtension> getMenuExtensions();
	public StateRenderer2D getRenderer();
	
}
