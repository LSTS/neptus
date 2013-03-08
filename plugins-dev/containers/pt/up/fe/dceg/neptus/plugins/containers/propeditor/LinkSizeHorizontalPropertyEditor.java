/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 5 de Out de 2010
 * $Id:: LinkSizeHorizontalPropertyEditor.java 9615 2012-12-30 23:08:28Z pdias  $:
 */
package pt.up.fe.dceg.neptus.plugins.containers.propeditor;

import pt.up.fe.dceg.neptus.i18n.I18n;

/**
 * @author pdias
 *
 */
public class LinkSizeHorizontalPropertyEditor extends HorizontalGroupPropertyEditor {

	/**
	 * 
	 */
	public LinkSizeHorizontalPropertyEditor() {
		super();
		rootElement = "LinkSizeHorizontal";
		title = I18n.text("Link size for horizontal axis:") + " <" + rootElement + "></" + rootElement + ">";
		helpText += "<!-- " + I18n.text("Don't use this top element (this is informative)") + " -->\n" +
					"<!ELEMENT LinkSizeHorizontal (LinkSizeGroup)*>\n\n" +
					"<!ELEMENT LinkSizeGroup ((Component), (Component)+)>\n" +
					"<!ELEMENT Component ANY>\n" +
					"<!ATTLIST Component\n" +
					"	id CDATA #REQUIRED\n" +
					">";
	}

}
