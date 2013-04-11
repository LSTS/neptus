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
 * 24/Out/2005
 */
package pt.up.fe.dceg.neptus.util.comm.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.comm.protocol.AdjustTimeShellArgs;
import pt.up.fe.dceg.neptus.types.comm.protocol.ProtocolArgs;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * @author Paulo Dias
 * 
 */
public class SSHExec
extends SSHCommon
{

	public static final String ADJUST_DATE = "ADJUST-TIME";
	
	protected Session session = null;
	protected Channel channel;
	
	protected InputStream in;
	protected OutputStream out;
	protected InputStream err; 

	protected int exitStatus = 0;
	protected String execResponse = "";
	//protected String execErrResponse = "";
	
    public SSHExec()
    {
        super();
    }

    public SSHExec(String vehicleId)
	{
		super(vehicleId);
	}


    public int getExitStatus()
	{
		return exitStatus;
	}
    

	/**
	 * @return the execResponse
	 */
	public String getExecResponse() {
		return execResponse;
	}

	public boolean prepareChannel()
	{
    	session = null;
    	try
		{
			session = getSession();
		}
		catch (JSchException e)
		{
			NeptusLog.pub().error(this + " :: Could not open session.", e);
			execResponse += "\n :: Could not open session. " + e.getMessage();
			return false;
		}
		
		try
		{
			channel = session.openChannel("exec");
		}
		catch (JSchException e)
		{
			NeptusLog.pub().error(this + " :: Could not open channel.", e);
			execResponse += "\n :: Could not open channel. " + e.getMessage();
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
			execResponse += "\n :: Could not getInputStream. " + e.getMessage();
			return false;
		}
        try
		{
			out = channel.getOutputStream();
		}
		catch (IOException e)
		{
			NeptusLog.pub().error(this + " :: Could not getOutputStream.", e);
			execResponse += "\n :: Could not getOutputStream. " + e.getMessage();
			return false;
		}
		((ChannelExec) channel).setErrStream(System.err);
		return true;
	}
	
    public void setCommand (String command)
	{
		((ChannelExec) channel).setCommand(command);
	}
	
    public boolean connectChannel()
	{
		try
		{
			channel.connect();
		}
		catch (JSchException e)
		{
			session.disconnect();
			NeptusLog.pub().error(this + " :: Could not connect a channel.", e);
			execResponse += "\n :: Could not connect a channel. " + e.getMessage();
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

	public boolean exec (String command)
    {
		execResponse = "";
		if (!prepareChannel())
		{
			sessionCleanup();
			return false;
		}

        //channel.setXForwarding(true);

        //channel.setInputStream(System.in);
        //channel.setOutputStream(System.out);

        // FileOutputStream fos=new FileOutputStream("/tmp/stderr");
        // ((ChannelExec)channel).setErrStream(fos);
        //((ChannelExec) channel).setErrStream(System.err);

		if (!getStreams())
		{
			sessionCleanup();
			return false;
		}

        if (command.equalsIgnoreCase(ADJUST_DATE) )
        {
        	command = getAdjustDateCommand();
        }
		setCommand(command);
		
		if (!connectChannel())
		{
			channelCleanup();
			sessionCleanup();
			return false;			
		}
		NeptusLog.pub().info("Date to set in vehicle '" + vehicleId + "': " + command);

		try
		{
			byte[] tmp = new byte[1024];
			while (true)
			{
			    while (in.available() > 0)
			    {
			        int i = in.read(tmp, 0, 1024);
			        if (i < 0)
			            break;
			        String tmpStr = new String(tmp, 0, i);
			        execResponse+="\n"+tmpStr;
			        System.out.print(tmpStr);
			    }
			    if (channel.isClosed())
			    {
			        NeptusLog.pub().info("<###>exit-status: "
			                + channel.getExitStatus());
			        exitStatus = channel.getExitStatus();
			        break;
			    }
			    try
			    {
			        Thread.sleep(1000);
			    }
			    catch (Exception ee)
			    {
			    }
			}
		}
		catch (IOException e)
		{
			NeptusLog.pub().error(this + " :: Error reading from InputStream.", e);
			execResponse += "\n :: Error reading from InputStream. " + e.getMessage();
			channel.disconnect();
	        session.disconnect();
	        return false;
		}
		exitStatus = channel.getExitStatus();
        channel.disconnect();
        session.disconnect();
		return (exitStatus == 0)?true:false;
    }
    
    
	private String getAdjustDateCommand()
	{
		AdjustTimeShellArgs adjParam;
		try {
			ProtocolArgs adjParamBase = vehicle.getProtocolsArgs().get(AdjustTimeShellArgs.DEFAULT_ROOT_ELEMENT);
			adjParam = null;
			if (adjParamBase != null)
				adjParam = (AdjustTimeShellArgs) adjParamBase;
			else
				adjParam = new AdjustTimeShellArgs();
		} catch (Exception e) {
			NeptusLog.pub().warn("AdjustTimeShellArgs not loaded, using default!", e);
			adjParam = new AdjustTimeShellArgs();
		}
		String dateTime = "date ";
        //MMddHHmm[[[CC]yy][.ss]]
		String yearFormat = (!adjParam.isSetYear())?"":((adjParam.isUse2DigitYear())?"yy":"yyyy");
		String secondsFormat = (!adjParam.isSetSeconds())?"":".ss";
        //SimpleDateFormat dateFormater = new SimpleDateFormat("MMddHHmmyyyy.ss");
        SimpleDateFormat dateFormater = new SimpleDateFormat("MMddHHmm"+yearFormat+secondsFormat);
        Date trialTime = new Date();
        dateTime += dateFormater.format(trialTime);
        if (adjParam.isUseHwClock())
        	dateTime += " && hwclock --systohc";
        return dateTime;
	}


	/**
     * @param args
     */
    public static boolean exec (String vehicleId, String command)
    {
        SSHExec ssexec = new SSHExec(vehicleId);
        return ssexec.exec(command);
    }
    
    
    public static void main(String[] args)
    {
        ConfigFetch.initialize();
        boolean rt = SSHExec.exec("rov-sim", "set|grep SSH");
        NeptusLog.pub().info("<###> "+rt);
        rt = SSHExec.exec("rov-sim", "date");
        NeptusLog.pub().info("<###> "+rt);
    }
    
}
