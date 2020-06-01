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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jan 21, 2014
 */
package pt.lsts.neptus.console;

import org.dom4j.Element;

import pt.lsts.neptus.renderer2d.StateRendererInteraction;

/**
 * @author zp
 *
 */
public interface IConsoleInteraction extends StateRendererInteraction {

    /**
         * Initialize this layer by passing a Console instance 
         * @param console The Console where this IMapLayer has been added to
         */
        public void init(ConsoleLayout console);
        
        /**
         * This layer has been removed or the MapPanel was closed.
         */
        public void clean();
                
        /**
         * Save the configurations of this layer as XML
         * @param rootElement The name of the root XML tag to use when saving
         * @return The XML configuration of this tag
         */
        public Element asElement(String rootElement);
        
        /**
         * Load configuration from a partial XML tree
         * @param xml An XML string having as top-level element the tag relative to this IMapLayer
         */
        public void parseXmlElement(Element elem);
}
