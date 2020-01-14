/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 5 de Out de 2010
 */
package pt.lsts.neptus.console.plugins.containers.propeditor;

import java.io.InputStream;

import javax.xml.validation.Schema;

import pt.lsts.neptus.console.plugins.containers.GroupLayoutContainer;
import pt.lsts.neptus.gui.editor.XMLPropertyEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
public class HorizontalGroupPropertyEditor extends XMLPropertyEditor {

	/**
	 * 
	 */
	public HorizontalGroupPropertyEditor() {
		super();
		rootElement = "HorizontalGroup";
        xmlSchemaName = "GroupLayoutContainer";
		title = I18n.text("Layout for horizontal axis:") + " <" + rootElement + "></" + rootElement + ">";
		helpText += "<!ELEMENT HorizontalGroup (Sequence | Parallel)?>\n\n" +
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
	
    @Override
	public Schema getSchema() {
        return GroupLayoutContainer.schema;
	}
	
	@Override
	protected InputStream getSchemaInputStream() {
        return GroupLayoutContainer.class.getResourceAsStream(GroupLayoutContainer.GROUP_LAYOUT_SCHEMA);
	}
	
    public static void main(String[] args) {
        HorizontalGroupPropertyEditor xp = new HorizontalGroupPropertyEditor();

        GuiUtils.testFrame(xp.button);
    }
}
