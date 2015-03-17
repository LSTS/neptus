/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 5 de Out de 2010
 */
package pt.lsts.neptus.console.plugins.containers.propeditor;

import pt.lsts.neptus.i18n.I18n;

/**
 * @author pdias
 *
 */
public class VerticalGroupPropertyEditor extends HorizontalGroupPropertyEditor { // Extends HorizontalGroupPropertyEditor in order not to repeat code (getSchema())

	/**
	 * 
	 */
	public VerticalGroupPropertyEditor() {
		super();
		rootElement = "VerticalGroup";
		title = I18n.text("Layout for vertical axis:") + " <" + rootElement + "></" + rootElement + ">";
		helpText += "<!-- " + I18n.text("Don't use this top element (this is informative)") + " -->\n" +
					"<!ELEMENT VerticalGroup (Sequence | Parallel)?>\n\n" +
					"<!ELEMENT Sequence (Component | (Gap | GapComponents | PreferredGap) | Sequence | Parallel)+>\n" +
					"<!ELEMENT Parallel (Component | Gap | Sequence | Parallel)+>\n" +
					"<!ATTLIST Parallel\n" +
					"	alignment (LEADING | TRAILING | CENTER | BASELINE) \"LEADING\"\n" +
					"	resizable NMTOKEN \"true\"\n" +
					">\n"+
					"<!ELEMENT Component ANY>\n" +
					"<!ATTLIST Component\n" +
					"	id CDATA #REQUIRED\n" +
					"	alignment (LEADING | TRAILING | CENTER | BASELINE) \"LEADING\"\n" +
					"	min NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
					"	pref NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
					"	max NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
					">\n" +
					"<!ELEMENT Gap EMPTY>\n" +
					"<!ATTLIST Gap\n" +
					"	min NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
					"	pref NMTOKEN #REQUIRED\n" +
					"	max NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
					">\n" +
					"<!ELEMENT GapComponents EMPTY>\n" +
					"<!ATTLIST GapComponents\n" +
					"	type (RELATED | UNRELATED) \"RELATED\"\n" +
					"	pref NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
					"	max NMTOKEN \"-2\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
					"	firstComponent CDATA #REQUIRED\n" +
					"	secondComponent CDATA #REQUIRED\n" +
					">\n" +
					"<!ELEMENT PreferredGap EMPTY>\n" +
					"<!ATTLIST PreferredGap\n" +
					"	type (RELATED | UNRELATED) \"RELATED\"\n" +
					"	pref NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
					"	max NMTOKEN \"-2\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
					">";
	}

}
