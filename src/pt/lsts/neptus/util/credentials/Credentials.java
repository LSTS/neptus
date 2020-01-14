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
 * Dec 21, 2015
 */
package pt.lsts.neptus.util.credentials;

import java.awt.Component;
import java.beans.PropertyEditor;
import java.io.File;
import java.nio.charset.Charset;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.StringUtils;

import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PropertyType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * This class is used to manage login credentials.
 * Credentials are stored encrypted to disk. 
 * @author zp
 */
public class Credentials implements PropertyType {
    
    @NeptusProperty
    private String username = null;
    @NeptusProperty
    private String password = null;
    private File storage;
    
    /**
     * Class constructor
     * @param storage The File where credentials will be stored
     */
    public Credentials(File storage) {
        this.storage = storage;
        if (storage.exists())
        {
            try {
                PluginUtils.loadProperties(storage.getAbsolutePath(), this);
            }
            catch (Exception e) {
            }
        }
    }
    
    /**
     * This method shows a dialog where the user can edit the current credentials
     * @param title The title to show in the dialog
     * @return <code>false</code> if cancelled or <code>true</code> otherwise. 
     */
    public boolean showDialog(Component parent, String title) {
        Pair<String, String> credentials = GuiUtils.askCredentials(parent,
                title, getUsername(), getPassword());
        if (credentials != null) {
            setUsername(credentials.first());
            setPassword(credentials.second());
            try {
                PluginUtils.saveProperties(storage.getAbsolutePath(), this);
            }
            catch (Exception e) {
            }
            return true;
        }
        return false;
    }
    
    /**
     * @return the stored login user name
     */
    public String getUsername() {
        if (username == null)
            return "";
        return username;
    }

    /**
     * @return the (plain) selected login password 
     */
    public String getPassword() {
        if (password == null)
            return "";
        return StringUtils.newStringUtf8(DatatypeConverter.parseBase64Binary(password));
    }

    /**
     * Change password
     * @param password The (plain) password for this login 
     */
    public void setPassword(String password) {
        if (password == null)
            this.password = null;

        this.password = DatatypeConverter.printBase64Binary(password.getBytes(Charset.forName("UTF8")));
    }

    /**
     * Change the login user name
     * @param username The user name for this login
     */
    public void setUsername(String username) {
        if (username == null)
            this.username = null;
        this.username = username;
    }
    
    @Override
    public String toString() {
        return getUsername() + " / " + getPassword().replaceAll(".", "*");
    }
    
    // example usage
    public static void main(String[] args) {
        Credentials c = new Credentials(new File("/tmp/credentials.txt"));
        System.out.println(c.getUsername()+" / "+c.getPassword());
        if (c.showDialog(null, "Please enter credentials"))
            System.out.println(c.getUsername()+" / "+c.getPassword());
        else
            System.out.println("The user has cancelled.");
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.PropertyType#fromString(java.lang.String)
     */
    @Override
    public void fromString(String value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Class<? extends PropertyEditor> getPropertyEditor() {
        return CredentialsEditor.class;
    }
}
