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
 * Author: Hugo Dias
 * Oct 22, 2012
 */
package pt.lsts.neptus;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

/**
 * @author Hugo
 * 
 */

public class NeptusProperties extends Properties {

    private static final long serialVersionUID = 3202355484050073116L;
    private String workingFile = null;
    private NeptusConfig config;

    /**
     * Construct
     * 
     */
    public NeptusProperties(NeptusConfig config) {
        super();
        this.config = config;
    }

    public NeptusProperties load(String file) throws InvalidPropertiesFormatException, IOException {
        this.workingFile = file;
        InputStream in = new FileInputStream(file);
        this.loadFromXML(in);
        return this;
    }

    /**
     * 
     * @param header comment
     * @throws IOException
     */
    public void storeToXML(String header) throws IOException {
        OutputStream out = new FileOutputStream(workingFile);
        storeToXML(out, header);
    }
    
    /**
     * Stores with neptus default comment header for xml files
     * @throws IOException
     */
    public void storeToXML() throws IOException {
        OutputStream out = new FileOutputStream(workingFile);
        storeToXML(out, config.versionForXml());
    }

    /**
     * @return Returns the workingFile.
     */
    public String getWorkingFile() {
        return workingFile;
    }

    /**
     * @param workingFile The workingFile to set.
     */
    public void setWorkingFile(String workingFile) {
        this.workingFile = workingFile;
    }

}
