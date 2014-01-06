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
 * 26/Out/2005
 */
package pt.lsts.neptus.comm.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.CommUtil;
import pt.lsts.neptus.types.comm.CommMean;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.conf.ConfigFetch;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * @author Paulo Dias
 *
 */
public abstract class SSHCommon
{

    protected String host            = "localhost";
    protected String knownHostsFile  = ".ssh/known_hosts";
    protected int port               = 22;
    protected String user            = "";
    protected String password        = "";
    protected SSHUserInfo userinfo   = new SSHUserInfo();
    protected VehicleType vehicle    = null;
    protected String vehicleId       = "";
    protected CommMean cm            = null;
    
    public SSHCommon (String vehicleId)
    {
    	init (vehicleId);
    }

    
    public SSHCommon()
    {
        init(host, user, password, port);
    }

    public SSHCommon(String host, String username, String password, int port)
    {
        init(host, username, password, port);
    }

    public boolean init(String host, String username, String password, int port)
    {
        if (password.length() == 0)
        {
            String[] cData = SSHConnectionDialog.showConnectionDialog(host, user, password, port);
            if (cData.length == 0)
                return false;
            
            this.host     = cData[0];
            this.user     = cData[1];
            this.password = cData[2];
            this.port     = Integer.parseInt(cData[3]);            
        }
        else
        {
            this.host     = host;
            this.user     = username;
            this.password = password;
            this.port     = port;
        }
        
        String knownHostsFilePath = ConfigFetch.resolvePath(knownHostsFile);
        if (knownHostsFilePath == null)
            knownHostsFile = knownHostsFilePath;
        else
            knownHostsFile = knownHostsFilePath;
        return true;
    }


    public boolean init (String vehicleId)
    {
        VehicleType vehicle = VehiclesHolder.getVehicleById(vehicleId);
        if (vehicle == null)
        {
            NeptusLog.pub().error("SSHCommon :: No vehicle found for id: "
                    + vehicleId);
            return false;            
        }
        
        this.vehicleId = vehicleId;
        this.vehicle = vehicle;

        cm = CommUtil.getActiveCommMeanForProtocol(vehicle, "ssh");
        if (cm == null)
        {
            NeptusLog.pub().error("SSHCommon :: No active CommMean for " +
                    "ssh protocol for vehicle with id: " + vehicleId);
            return false;
        }
        host     = cm.getHostAddress();
        user     = cm.getUserName();
        password = cm.getPassword();
        
        String knownHostsFilePath = ConfigFetch.resolvePath(knownHostsFile);
        if (knownHostsFilePath == null)
        	knownHostsFile = knownHostsFilePath;
        else
        	knownHostsFile = knownHostsFilePath;
        return true;
    }
    
    protected Session getSession() 
    throws JSchException
    {
    	JSch jsch = new JSch();
    	Session session = jsch.getSession(user, host, port);

    	if ( (knownHostsFile != null) && !knownHostsFile.equalsIgnoreCase(""))
    		jsch.setKnownHosts(knownHostsFile);
    	
        /*
         * String xhost="127.0.0.1"; int xport=0; String
         * display=JOptionPane.showInputDialog("Enter display name",
         * xhost+":"+xport); xhost=display.substring(0,
         * display.indexOf(':'));
         * xport=Integer.parseInt(display.substring(display.indexOf(':')+1));
         * session.setX11Host(xhost); session.setX11Port(xport+6000);
         */

        // username and password will be given via UserInfo interface.
    	userinfo = new SSHUserInfo(password, true);
    	//userinfo.setPassword(password);
        session.setUserInfo(userinfo);
        //session.setPassword(password);
        session.connect();
        return session;
    }
    
    
    /* ---------------------------------------------------------------
     * Utils
     */
    protected void sendAck(OutputStream out) 
    throws IOException
    {
        byte[] buf = new byte[1];
        buf[0] = 0;
        out.write(buf);
        out.flush();
    }
    
    protected int checkAck(InputStream in)
    throws IOException
    {
        int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1
        if (b == 0)
            return b;
        if (b == -1)
            return b;

        if (b==1 || b==2)
        {
            StringBuffer sb = new StringBuffer();
            int c;
            do
            {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            if (b == 1)
            { // error
                NeptusLog.pub()
                        .error("SSH checkAck - Server indicated an error: "
                                + sb.toString());
            }
            else if (b == 2)
            { // fatal error
                NeptusLog.pub()
                        .error("SSH checkAck - Server indicated an fatal error: "
                                + sb.toString());
            }
            else
            {
                NeptusLog.pub()
                        .error("SSH checkAck - Server indicated an unknown error: "
                                + sb.toString());
            }
        }
        return b;
    }

    protected boolean waitForAck(InputStream in)
    {
    	try
		{
			checkAck(in);
		}
		catch (IOException e)
		{
			NeptusLog.pub().error(this
					+ " - Error while waiting for Ackowlegment.", e);
			return false;
		}
		return true;
    }

    protected int waitForAckInt(InputStream in)
    {
    	try
		{
			return checkAck(in);
		}
		catch (IOException e)
		{
			NeptusLog.pub().error(this
					+ " - Error while waiting for Ackowlegment.", e);
			return -1;
		}
    }


    public String getHost()
    {
        return host;
    }


    public void setHost(String host)
    {
        this.host = host;
    }


    public String getPassword()
    {
        return password;
    }


    public void setPassword(String password)
    {
        this.password = password;
    }


    public int getPort()
    {
        return port;
    }


    public void setPort(int port)
    {
        this.port = port;
    }


    public String getUser()
    {
        return user;
    }


    public void setUser(String user)
    {
        this.user = user;
    }

}
