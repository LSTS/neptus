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
 * 2008/08/24
 */
package pt.lsts.neptus.util;

import java.util.List;

/**
 * @author pdias
 *
 */
public class SvgUtil {
	public static org.w3c.dom.Document cleanInkscapeSVG(
			org.w3c.dom.Document logoDoc) {
		org.dom4j.Document doc = Dom4JUtil.convertDOMtoDOM4J(logoDoc);
		org.dom4j.Element root = doc.getRootElement(); 
		List<?> list = root.selectNodes("/svg/@*[starts-with(name(.),'sodipodi:')]");
		for (Object obj : list) {
			org.dom4j.Node nd = (org.dom4j.Node) obj;
			//NeptusLog.pub().info("<###> "+nd.asXML());
			root.remove(nd);
		}
		list = root.selectNodes("/svg/*[starts-with(name(.),'sodipodi:')]");
		for (Object obj : list) {
			org.dom4j.Node nd = (org.dom4j.Node) obj;
			//NeptusLog.pub().info("<###> "+nd.asXML());
			root.remove(nd);
		}
		list = root.selectNodes("/svg/@*[starts-with(name(.),'inkscape:')]");
		for (Object obj : list) {
			org.dom4j.Node nd = (org.dom4j.Node) obj;
			//NeptusLog.pub().info("<###> "+nd.asXML());
			root.remove(nd);
		}
		list = root.selectNodes("/svg/@xml:base");
		for (Object obj : list) {
			org.dom4j.Node nd = (org.dom4j.Node) obj;
			//NeptusLog.pub().info("<###> "+nd.asXML());
			root.remove(nd);
		}
		//list = root.selectNodes("/svg/metadata");
		//for (Object obj : list) {
		//	org.dom4j.Node nd = (org.dom4j.Node) obj;
		//	NeptusLog.pub().info("<###> "+nd.asXML());
		//	root.remove(nd);
		//}
		list = root.additionalNamespaces();
		for (Object obj : list) {
			org.dom4j.Namespace nd = (org.dom4j.Namespace) obj;
			//NeptusLog.pub().info("<###> "+nd.getPrefix());
			String pf = nd.getPrefix();
			if ("inkscape".equalsIgnoreCase(pf) || "sodipodi".equalsIgnoreCase(pf)
					|| "cc".equalsIgnoreCase(pf) || "dc".equalsIgnoreCase(pf))
				root.remove(nd);
		}
		//NeptusLog.pub().info("<###> "+doc.asXML());
		return Dom4JUtil.convertDOM4JtoDOM(doc);
	}
}
