/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Mar 30, 2012
 */
package pt.lsts.neptus.doc;

import java.io.File;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.FileUtil;

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
        NeptusLog.pub().info("<###> "+html);
    }
    
    public static void main(String[] args) {
        String html = FileUtil.getFileAsString(new File("doc/seacon/roles.html"));
        html2Latex(html);
    }
    
}
