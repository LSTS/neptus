/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
 * Oct 22, 2012
 * $Id:: NeptusProperties.java 9615 2012-12-30 23:08:28Z pdias                  $:
 */
package pt.up.fe.dceg.neptus;

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
