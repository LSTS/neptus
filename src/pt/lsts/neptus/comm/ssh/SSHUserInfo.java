/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * 24/Out/2005
 */
package pt.lsts.neptus.comm.ssh;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jcraft.jsch.UserInfo;

/**
 * @author Paulo Dias
 *
 */
public class SSHUserInfo
implements UserInfo
{
    private String password = null;
    private JTextField passwordField = (JTextField) new JPasswordField(20);
    private boolean trustAllCertificates = false;
    private boolean firstTry = true;

    public SSHUserInfo()
	{
		super();
		this.trustAllCertificates = false;
	}

	public SSHUserInfo(String password, boolean trustAllCertificates)
	{
		super();
		this.password = password;
		this.trustAllCertificates = trustAllCertificates;
	}

    /**
	 * @param passwd
	 */
    public void setPassword(String passwd)
    {
        this.password = passwd;
    }

    /**
     * Sets the trust.
     * @param trust whether to trust or not.
     */
    public void setTrust(boolean trust)
    {
        this.trustAllCertificates = trust;
    }

    /**
     * @return whether to trust or not.
     */
    public boolean getTrust()
    {
        return this.trustAllCertificates;
    }


	/* (non-Javadoc)
	 * @see com.jcraft.jsch.UserInfo#getPassword()
	 */
	public String getPassword()
    {
        return password;
    }

	/* (non-Javadoc)
	 * @see com.jcraft.jsch.UserInfo#getPassphrase()
	 */
	public String getPassphrase()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see com.jcraft.jsch.UserInfo#promptPassphrase(java.lang.String)
     */
    public boolean promptPassphrase(String message)
    {
        return true;
    }

    /* (non-Javadoc)
     * @see com.jcraft.jsch.UserInfo#promptPassword(java.lang.String)
     */
    public boolean promptPassword(String message)
    {
        if (firstTry)
        {
        	if (password != null)
        	{
        		firstTry = false;
        		return true;
        	}
        }
    	passwordField.setText(password);
        Object[] ob = { passwordField };
        int result = JOptionPane.showConfirmDialog(null, ob, message,
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION)
        {
            password = passwordField.getText();
            return true;
        }
        else
        {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see com.jcraft.jsch.UserInfo#promptYesNo(java.lang.String)
     */
    public boolean promptYesNo(String str)
    {
    	if (trustAllCertificates)
    		return trustAllCertificates;

    	Object[] options = { "yes", "no" };
        int foo = JOptionPane.showOptionDialog(null, str, "Warning",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                options, options[0]);
        return foo == 0;
    }

    /* (non-Javadoc)
     * @see com.jcraft.jsch.UserInfo#showMessage(java.lang.String)
     */
    public void showMessage(String message)
    {
        JOptionPane.showMessageDialog(null, message);
    }
}
