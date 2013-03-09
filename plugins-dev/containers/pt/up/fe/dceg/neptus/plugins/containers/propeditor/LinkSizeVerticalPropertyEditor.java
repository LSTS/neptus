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
 */
package pt.up.fe.dceg.neptus.plugins.containers.propeditor;

import pt.up.fe.dceg.neptus.i18n.I18n;

/**
 * @author pdias
 *
 */
public class LinkSizeVerticalPropertyEditor extends HorizontalGroupPropertyEditor {

	/**
	 * 
	 */
	public LinkSizeVerticalPropertyEditor() {
		super();
		rootElement = "LinkSizeVertical";
		title = I18n.text("Link size for vertical axis:") + " <" + rootElement + "></" + rootElement + ">";
		helpText += "<!-- " + I18n.text("Don't use this top element (this is informative)") + " -->\n" +
					"<!ELEMENT LinkSizeVertical (LinkSizeGroup)*>\n\n" +
					"<!ELEMENT LinkSizeGroup ((Component), (Component)+)>\n" +
					"<!ELEMENT Component ANY>\n" +
					"<!ATTLIST Component\n" +
					"	id CDATA #REQUIRED\n" +
					">";
	}

}
