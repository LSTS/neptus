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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * Classe que permite consultar as <i>properties </i>. <br>
 * <br>
 * E.g.: <br>
 * &nbsp;&nbsp;&nbsp;String getProperty(String key) <br>
 * &nbsp;&nbsp;&nbsp;String getProperty(String key, String defaultValue) <br>
 * &nbsp;&nbsp;&nbsp;boolean containsKey(Object key) <br>
 * &nbsp;&nbsp;&nbsp;Object get(Object key) <br>
 * 
 * @author Paulo Dias
 * @author Sergio Fraga
 * @version 1.0 21/07/2000
 * @version 1.1 03/04/2003
 * @version 1.2 17/05/2005
 */
public class PropertiesLoader extends Properties {
    private static final long serialVersionUID = 1L;

    private String workingFile = "";

    public final static short PROPERTIES = 0;
    public final static short XML_PROPERTIES = 1;

    public PropertiesLoader() {
        super();
        try {
            setFile("Sistema.properties");
            this.workingFile = "Sistema.properties";
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error("File not found. : " + e.getMessage());
        }
        catch (IOException e) {
            NeptusLog.pub().error("Can't read file. : " + e.getMessage());
        }
    }

    public PropertiesLoader(String txfile) {
        this(txfile, PROPERTIES); 
    }

    /**
     * @param txfile
     * @param type
     */
    public PropertiesLoader(String txfile, short type) {
        super();
        try {
            this.workingFile = txfile;
            if (type == XML_PROPERTIES)
                setXmlFile(txfile);
            else
                setFile(txfile);
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error(e.getMessage());
            try {
                setFile("pt.lsts.neptus.properties");
                this.workingFile = "pt.lsts.neptus.properties";
            }
            catch (FileNotFoundException e1) {
                NeptusLog.pub().error("File not found. " + e1.getMessage());
            }
            catch (IOException e1) {
                NeptusLog.pub().error("Can't read file. " + e1.getMessage());
            }
        }
        catch (IOException e) {
            NeptusLog.pub().error(e.getMessage());
            try {
                setFile("pt.lsts.neptus.properties");
                this.workingFile = "pt.lsts.neptus.properties";
            }
            catch (FileNotFoundException e1) {
                NeptusLog.pub().error("File not found. " + e1.getMessage());
            }
            catch (IOException e1) {
                NeptusLog.pub().error("Can't read file. " + e1.getMessage());
            }
        }
    }

    /**
     * Carrega as <i>properties </i> usando o <i>FileInputStream </i> indicado.
     * 
     * @param in <i>FileInputStream </i> dos <i>properties </i>.
     */
    public void setFile(InputStream in) throws IOException {
        clear();
        load(in);
        in.close();
    }

    /**
     * Carrega as <i>properties </i> usando o ficheiro indicado.
     * 
     * @param txfile Nome do ficheiro dos <i>properties </i>.
     */
    public void setFile(String txfile) throws FileNotFoundException, IOException {
        InputStream in = new FileInputStream(txfile);
        setFile(in);
    }

    /**
     * @param in
     * @throws IOException
     */
    public void setXmlFile(InputStream in) throws IOException {
        clear();
        loadFromXML(in);
        in.close();
    }

    /**
     * @param txfile
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void setXmlFile(String txfile) throws FileNotFoundException, IOException {
        InputStream in = new FileInputStream(txfile);
        setXmlFile(in);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Properties#store(java.io.OutputStream, java.lang.String)
     */
    public void store(OutputStream out, String header) throws IOException {
        try {
            super.store(out, header);
        }
        catch (IOException e) {
            NeptusLog.pub().error("Cannot store properties. : " + e.getMessage());
        }
    }

    /**
     * @param header
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void store(String header) throws FileNotFoundException, IOException {
        try {
            if (workingFile.compareTo("") == 0) {
                NeptusLog.pub().error(this + ": Error. Cannot find file to writo to.");
                throw new IOException("Cannot find file to write to");
            }
            else {
                OutputStream out = new FileOutputStream(workingFile);
                store(out, header);
            }

        }
        catch (IOException e) {
            NeptusLog.pub().error("Cannot store properties. : " + e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Properties#storeToXML(java.io.OutputStream, java.lang.String)
     */
    public void storeToXML(OutputStream out, String header) throws IOException {
        try {
            super.storeToXML(out, header);
        }
        catch (IOException e) {
            NeptusLog.pub().error("Cannot store properties. : " + e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Properties#storeToXML(java.io.OutputStream, java.lang.String, java.lang.String)
     */
    public void storeToXML(OutputStream out, String header, String encoding) throws IOException {
        try {
            super.storeToXML(out, header, encoding);
        }
        catch (IOException e) {
            NeptusLog.pub().error("Cannot store properties. : " + e.getMessage());
        }
    }

    /**
     * @param header
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void storeToXML(String header) throws FileNotFoundException, IOException {
        OutputStream out = null;
        try {
            if (workingFile.compareTo("") == 0) {
                NeptusLog.pub().error(this + ": Error. Cannot find file to write to.");
                throw new IOException("Cannot find file to write to");
            }
            else {
                out = new FileOutputStream(workingFile);
                storeToXML(out, header);
                out.flush();
                out.close();
            }

        }
        catch (IOException e) {
            NeptusLog.pub().error(this + ": Cannot store properties.");
            e.printStackTrace();
        }
        finally {
            try {
                out.close();
            }
            catch (Exception e) {
                NeptusLog.pub().error(e.getMessage());
            }
        }
    }

    /**
     * @param header
     * @param encoding
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void storeToXML(String header, String encoding) throws FileNotFoundException, IOException {
        try {
            if (workingFile.compareTo("") == 0) {
                NeptusLog.pub().error(this + ": Error. Cannot find file to writo to.");
                throw new IOException("Cannot find file to write to");
            }
            else {
                OutputStream out = new FileOutputStream(workingFile);
                storeToXML(out, header, encoding);
            }

        }
        catch (IOException e) {
            NeptusLog.pub().error("Cannot store properties. : " + e.getMessage());
        }
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
    
    public synchronized Object setProperty(String key, String value) {
        Object obj = super.setProperty(key, value);
        GeneralPreferences.warnPreferencesListeners();
        return obj;
    }
}
