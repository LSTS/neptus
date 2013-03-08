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
 * 2006/11/11
 * $Id:: XmlInputMethodsFromFile.java 9616 2012-12-30 23:23:22Z pdias     $:
 */
package pt.up.fe.dceg.neptus.types;

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
 *  (fetch from the {@link pt.up.fe.dceg.neptus.util.conf.ConfigFetch}).
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
     * {@link pt.up.fe.dceg.neptus.util.XMLValidator}). 
     * @param url
     * @return
     */
    public abstract boolean loadFile (String url);

    /**
     * Should set {@link #isLoadOk()} return value.
     * You should also call the XSD validation(you can use 
     * {@link pt.up.fe.dceg.neptus.util.XMLValidator}). 
     * @param file
     * @return
     */
    public abstract boolean loadFile (File file);

    
    //public abstract boolean validate(String xml);
    //public abstract boolean validate(File file);

}
