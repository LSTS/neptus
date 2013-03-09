/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;

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
            NeptusLog.pub().error(this + ": File not found.");
        }
        catch (IOException e) {
            NeptusLog.pub().error(this + ": Can't read file.");
        }
    }

    public PropertiesLoader(String txfile) {
        super();
        try {
            setFile(txfile);
            this.workingFile = txfile;
        }
        catch (FileNotFoundException e) {
            try {
                setFile("pt.up.fe.dceg.neptus.properties");
                this.workingFile = "pt.up.fe.dceg.neptus.properties";
            }
            catch (FileNotFoundException e1) {
                NeptusLog.pub().error(this + ": File not found.");
            }
            catch (IOException e1) {
                NeptusLog.pub().error(this + ": Can't read file.");
            }
        }
        catch (IOException e) {
            try {
                setFile("pt.up.fe.dceg.neptus.properties");
                this.workingFile = "pt.up.fe.dceg.neptus.properties";
            }
            catch (FileNotFoundException e1) {
                NeptusLog.pub().error(this + ": File not found.");
            }
            catch (IOException e1) {
                NeptusLog.pub().error(this + ": Can't read file.");
            }
        }
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
            try {
                setFile("pt.up.fe.dceg.neptus.properties");
                this.workingFile = "pt.up.fe.dceg.neptus.properties";
            }
            catch (FileNotFoundException e1) {
                NeptusLog.pub().error(this + ": File not found.");
            }
            catch (IOException e1) {
                NeptusLog.pub().error(this + ": Can't read file.");
            }
        }
        catch (IOException e) {
            try {
                setFile("pt.up.fe.dceg.neptus.properties");
                this.workingFile = "pt.up.fe.dceg.neptus.properties";
            }
            catch (FileNotFoundException e1) {
                NeptusLog.pub().error(this + ": File not found.");
            }
            catch (IOException e1) {
                NeptusLog.pub().error(this + ": Can't read file.");
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
            NeptusLog.pub().error(this + ": Cannot store properties.");
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
            NeptusLog.pub().error(this + ": Cannot store properties.");
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
            NeptusLog.pub().error(this + ": Cannot store properties.");
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
            NeptusLog.pub().error(this + ": Cannot store properties.");
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
            NeptusLog.pub().error(this + ": Cannot store properties.");
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
        GeneralPreferences.warnPreferencesListeneres();
        return obj;
    }
}
