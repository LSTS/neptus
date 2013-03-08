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
 * Feb 25, 2010
 * $Id:: StringProperty.java 9615 2012-12-30 23:08:28Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.util.conf;

import java.beans.PropertyEditor;

import pt.up.fe.dceg.neptus.gui.editor.LongStringPropertyEditor;
import pt.up.fe.dceg.neptus.plugins.PropertyType;

/**
 * @author zp
 *
 */
public class StringProperty implements PropertyType {

	private String value = "";
	
	public StringProperty(String value) {
		this.value = value;
	}
	
	@Override
	public void fromString(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}

	@Override
	public Class<? extends PropertyEditor> getPropertyEditor() {
		return LongStringPropertyEditor.class;
	}

}
