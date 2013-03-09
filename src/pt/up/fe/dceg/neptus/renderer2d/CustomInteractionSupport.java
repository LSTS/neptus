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
 * May 13, 2010
 */
package pt.up.fe.dceg.neptus.renderer2d;

import java.util.Collection;

/**
 * @author zp
 *
 */
public interface CustomInteractionSupport {

	public void addInteraction(StateRendererInteraction interaction);	
	public void removeInteraction(StateRendererInteraction interaction);	
	public void setActiveInteraction(StateRendererInteraction interaction);	
	public StateRendererInteraction getActiveInteraction();		
	public Collection<StateRendererInteraction> getInteractionModes();
	
}
