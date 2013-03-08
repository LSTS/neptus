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
 * Mar 30, 2012
 * $Id:: Html2Latex.java 9615 2012-12-30 23:08:28Z pdias                        $:
 */
package pt.up.fe.dceg.neptus.doc;

import java.io.File;

import pt.up.fe.dceg.neptus.util.FileUtil;

/**
 * @author zp
 *
 */
public class Html2Latex {

    public static void html2Latex(String html) {
                
        html = html.replaceAll("<head>(.*)</head>", "");
        html = html.replaceAll("<ul>", "\\\\begin{itemize}");
        html = html.replaceAll("</ul>", "\\\\end{itemize}");
        html = html.replaceAll("<li>([^<]*)</li>", "\\\\item $1");
        html = html.replaceAll("<p>", "\n");
        html = html.replaceAll("</p>", "\n");
        html = html.replaceAll("<b>([^<]*)</b>", "\\\\emph{$1}");
        html = html.replaceAll("<h1>([^<]*)</h1>", "\\\\chapter{$1}");
        html = html.replaceAll("<h2>([^<]*)</h2>", "\n\n\\\\section{$1}");
        html = html.replaceAll("<h3>([^<]*)</h3>", "\\\\subsection{$1}");
        html = html.replaceAll("<a name=\"(.*)\" .*</a>", "\\\\label{$1}");
        System.out.println(html);
    }
    
    public static void main(String[] args) {
        String html = FileUtil.getFileAsString(new File("doc/seacon/roles.html"));
        html2Latex(html);
    }
    
}
