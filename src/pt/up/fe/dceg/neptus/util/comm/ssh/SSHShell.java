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
 * 9/Abr/2006
 * $Id:: SSHShell.java 9616 2012-12-30 23:23:22Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.util.comm.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * @author Paulo Dias
 *
 */
public class SSHShell extends SSHCommon
{
    protected Session session = null;
    protected Channel channel;
    
    protected InputStream in;
    protected OutputStream out;
    protected InputStream err; 

    public SSHShell(String vehicleId)
    {
        super(vehicleId);
    }

    
    public SSHShell()
    {
        super();
    }

    public SSHShell(String host, String username, String password, int port)
    {
        super(host, username, password, port);
    }

    public Channel getChannel()
    {
        return channel;
    }


    public Session getSessionVar()
    {
        return session;
    }


    public boolean prepareChannel()
    {
        session = null;
        try
        {
            session = getSession();
        }
        catch (Exception e)
        {
            NeptusLog.pub().error(this + " :: Could not open session."
                    + "\n" + e.getMessage(), e);
            return false;
        }
        
        try
        {
            channel = session.openChannel("shell");
        }
        catch (JSchException e)
        {
            NeptusLog.pub().error(this + " :: Could not open channel."
                    + "\n" + e.getMessage(), e);
            return false;
        }
        return true;
    }
    
    public boolean getStreams()
    {
        try
        {
            in = channel.getInputStream();
        }
        catch (IOException e)
        {
            NeptusLog.pub().error(this + " :: Could not getInputStream.", e);
            return false;
        }
        try
        {
            out = channel.getOutputStream();
        }
        catch (IOException e)
        {
            NeptusLog.pub().error(this + " :: Could not getOutputStream.", e);
            return false;
        }
        //((ChannelExec) channel).setErrStream(System.err);
        return true;
    }

    
    public boolean connectChannel() {
        try {
            int timeout;
            timeout = GeneralPreferences.sshConnectionTimeoutMillis;
            channel.connect(timeout);            
        }
        catch (JSchException e) {
            session.disconnect();
            NeptusLog.pub().error(this + " :: Could not connect a channel.", e);
            return false;
        }
        return true;
    }

    public void sessionCleanup()
    {
        if (session != null)
            session.disconnect();
    }

    public void channelCleanup()
    {
        if (channel != null)
            channel.disconnect();
    }

}
