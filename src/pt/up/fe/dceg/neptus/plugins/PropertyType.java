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
 * 2009/05/28
 */
package pt.up.fe.dceg.neptus.plugins;

import java.beans.PropertyEditor;

/**
 * @author zp
 *
 */
public interface PropertyType {
	
	public String toString();
	public void fromString(String value);
	
	public Class<? extends PropertyEditor> getPropertyEditor();
	
}
