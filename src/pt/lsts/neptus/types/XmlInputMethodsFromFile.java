/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 2006/11/11
 */
package pt.lsts.neptus.types;

import java.io.File;


/**
 * This interface is to used to every class that loads it self from XML.
 * In adiction to the {@link #isLoadOk()} and if the class loads directly
 *  from a file with a XSD you should also implement the following methods: 
 * <ul>
 *  <li>public static abstract boolean validate(String xml);</li>
 *  <li>public static abstract boolean validate(File file);</li>
 * </ul>
 * This will serve to be called by the <i>load</i> method in order to
 *  validate the XML content again the their schema 
 *  (fetch from the {@link pt.lsts.neptus.util.conf.ConfigFetch}).
 *  
 * @author Paulo Dias
 */
public interface XmlInputMethodsFromFile
{
    /**
     * @return If the load of the XML was successful.
     */
    public abstract boolean isLoadOk();
    
    /**
     * Should set {@link #isLoadOk()} return value.
     * You should also call the XSD validation(you can use 
     * {@link pt.lsts.neptus.util.XMLValidator}). 
     * @param url
     * @return
     */
    public abstract boolean loadFile (String url);

    /**
     * Should set {@link #isLoadOk()} return value.
     * You should also call the XSD validation(you can use 
     * {@link pt.lsts.neptus.util.XMLValidator}). 
     * @param file
     * @return
     */
    public abstract boolean loadFile (File file);

    
    //public abstract boolean validate(String xml);
    //public abstract boolean validate(File file);

}
