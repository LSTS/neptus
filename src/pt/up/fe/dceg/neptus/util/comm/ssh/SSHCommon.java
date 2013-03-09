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
 * 26/Out/2005
 */
package pt.up.fe.dceg.neptus.util.comm.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.comm.CommMean;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.comm.CommUtil;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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
