/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * 9/Abr/2006
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
