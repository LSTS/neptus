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
 * 31/10/2010
 */
package pt.up.fe.dceg.neptus.plugins.containers.propeditor;

/**
 * @author pdias
 *
 */
public class ProfilesPropertyEditor extends HorizontalGroupPropertyEditor {

	/**
	 * 
	 */
	public ProfilesPropertyEditor() {
		super();
		rootElement = "Profiles";
		title = "Profiles: <" + rootElement + "></" + rootElement + ">";
		helpText += "<!-- Don't use this top element (this is informative) -->\n" +
					"<!ELEMENT Profiles (Default?, (Profile)*)?>\n\n" +
					"<!ELEMENT Default EMPTY>\n" +
                    "<!ATTLIST Default\n" +
                    "    profile CDATA #REQUIRED\n" +
                    ">\n\n" +
					"<!ELEMENT Profile ((Component)*, Layout?)>\n" +
					"<!ATTLIST Profile\n" +
					"	name CDATA #REQUIRED\n" +
					"	exclude NMTOKEN \"false\"\n" +
					">\n" +
					"<!ELEMENT Component ANY>\n" +
					"<!ATTLIST Component\n" +
					"	id CDATA #REQUIRED\n" +
					">\n\n" +
					"<!ELEMENT Layout ((HorizontalGroup, VerticalGroup, LinkSizeHorizontal?, LinkSizeVertical?))>\n\n" +
					"<!ELEMENT HorizontalGroup (Sequence | Parallel)?>\n\n" +
                    "<!ELEMENT VerticalGroup (Sequence | Parallel)?>\n\n" +
                    "<!ELEMENT Sequence (Component | (Gap | GapComponents | PreferredGap) | Sequence | Parallel)+>\n" +
                    "<!ELEMENT Parallel (Component | Gap | Sequence | Parallel)+>\n" +
                    "<!ATTLIST Parallel\n" +
                    "   alignment (LEADING | TRAILING | CENTER | BASELINE) \"LEADING\"\n" +
                    "   resizable NMTOKEN \"true\"\n" +
                    ">\n"+
                    "<!ELEMENT Component ANY>\n" +
                    "<!ATTLIST Component\n" +
                    "   id CDATA #REQUIRED\n" +
                    "   alignment (LEADING | TRAILING | CENTER | BASELINE) \"LEADING\"\n" +
                    "   min NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
                    "   pref NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
                    "   max NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
                    ">\n" +
                    "<!ELEMENT Gap EMPTY>\n" +
                    "<!ATTLIST Gap\n" +
                    "   min NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
                    "   pref NMTOKEN #REQUIRED\n" +
                    "   max NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
                    ">\n" +
                    "<!ELEMENT GapComponents EMPTY>\n" +
                    "<!ATTLIST GapComponents\n" +
                    "   type (RELATED | UNRELATED) \"RELATED\"\n" +
                    "   pref NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
                    "   max NMTOKEN \"-2\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
                    "   firstComponent CDATA #REQUIRED\n" +
                    "   secondComponent CDATA #REQUIRED\n" +
                    ">\n" +
                    "<!ELEMENT PreferredGap EMPTY>\n" +
                    "<!ATTLIST PreferredGap\n" +
                    "   type (RELATED | UNRELATED) \"RELATED\"\n" +
                    "   pref NMTOKEN \"-1\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
                    "   max NMTOKEN \"-2\" <!-- DEFAULT_SIZE=-1;PREFERRED_SIZE=-2 -->\n" +
                    ">\n\n" +
                    "<!ELEMENT LinkSizeHorizontal (LinkSizeGroup)*>\n\n" +
                    "<!ELEMENT LinkSizeVertical (LinkSizeGroup)*>\n\n" +
                    "<!ELEMENT LinkSizeGroup ((Component), (Component)+)>\n" +
                    "<!ELEMENT Component ANY>\n" +
                    "<!ATTLIST Component\n" +
                    "   id CDATA #REQUIRED\n" +
                    ">";
	}

}
