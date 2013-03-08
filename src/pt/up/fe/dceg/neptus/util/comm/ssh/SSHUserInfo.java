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
 * 24/Out/2005
 * $Id:: SSHUserInfo.java 9616 2012-12-30 23:23:22Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.util.comm.ssh;

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
