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
 * 2008/08/24
 * $Id:: SvgUtil.java 9616 2012-12-30 23:23:22Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.util;

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
			//System.out.println(nd.asXML());
			root.remove(nd);
		}
		list = root.selectNodes("/svg/*[starts-with(name(.),'sodipodi:')]");
		for (Object obj : list) {
			org.dom4j.Node nd = (org.dom4j.Node) obj;
			//System.out.println(nd.asXML());
			root.remove(nd);
		}
		list = root.selectNodes("/svg/@*[starts-with(name(.),'inkscape:')]");
		for (Object obj : list) {
			org.dom4j.Node nd = (org.dom4j.Node) obj;
			//System.out.println(nd.asXML());
			root.remove(nd);
		}
		list = root.selectNodes("/svg/@xml:base");
		for (Object obj : list) {
			org.dom4j.Node nd = (org.dom4j.Node) obj;
			//System.out.println(nd.asXML());
			root.remove(nd);
		}
		//list = root.selectNodes("/svg/metadata");
		//for (Object obj : list) {
		//	org.dom4j.Node nd = (org.dom4j.Node) obj;
		//	System.out.println(nd.asXML());
		//	root.remove(nd);
		//}
		list = root.additionalNamespaces();
		for (Object obj : list) {
			org.dom4j.Namespace nd = (org.dom4j.Namespace) obj;
			//System.out.println(nd.getPrefix());
			String pf = nd.getPrefix();
			if ("inkscape".equalsIgnoreCase(pf) || "sodipodi".equalsIgnoreCase(pf)
					|| "cc".equalsIgnoreCase(pf) || "dc".equalsIgnoreCase(pf))
				root.remove(nd);
		}
		//System.out.println(doc.asXML());
		return Dom4JUtil.convertDOM4JtoDOM(doc);
	}
}
