/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias
 * 22/Jun/2005
 * $Id:: ApacheAdjustDate.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.util.comm;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.comm.CommMean;
import pt.up.fe.dceg.neptus.types.comm.protocol.AdjustTimeShellArgs;
import pt.up.fe.dceg.neptus.types.comm.protocol.ProtocolArgs;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 */
public class ApacheAdjustDate
implements Runnable, TelnetNotificationHandler
{
    private TelnetClient tc = null;
    
    /**
     * @param vehicleId
     * @return
     */
    public boolean adjustDateTime(String vehicleId)
    {
        //String dateTime = "";
        //Date trialTime = new Date();
        
        //MMddHHmm[[[CC]yy][.ss]]
        //SimpleDateFormat dateFormater = new SimpleDateFormat("MMddHHmm.ss");
        
        //dateTime = dateFormater.format(trialTime);
        //NeptusLog.pub().info("Date to set in vehicle: " + dateTime);

        VehicleType vehicle = VehiclesHolder.getVehicleById(vehicleId);
        if (vehicle == null)
        {
            NeptusLog.pub().error("ApacheAdjustDate :: No vehicle found for id: "
                    + vehicleId);
            return false;            
        }
        else
        {
            /*
            CommMean cm = CommUtil.getActiveCommMean(vehicle);
            if (cm == null)
            {
                NeptusLog.pub().error("ApacheAdjustDate :: No active CommMean for "
                        + "vehicle with id: " + vehicleId);
                return false;                            
            }
            if (!CommUtil.testCommMeanForProtocol(cm, "telnet"))
            {
                NeptusLog.pub()
                        .error("ApacheAdjustDate :: No telnet protocol for CommMean " + "["
                                + cm.getName() + "] for " + "vehicle with id: "
                                + vehicleId);
                return false;                                            
            }
            */
            CommMean cm = CommUtil.getActiveCommMeanForProtocol(vehicle, "telnet");
            if (cm == null)
            {
                NeptusLog.pub().error("ApacheAdjustDate :: No active CommMean for " +
                		"telnet protocol for vehicle with id: " + vehicleId);
                return false;                            
            }
            else
            {
                String host     = cm.getHostAddress();
                int port        = 23;
                String user     = cm.getUserName();
                String password = cm.getPassword();
                
                tc = new TelnetClient();

                TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler(
                        "VT100", false, false, true, false);
                EchoOptionHandler echoopt = new EchoOptionHandler(true, false,
                        true, false);
                SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(
                        true, true, true, true);

                try
                {
                    tc.addOptionHandler(ttopt);
                    tc.addOptionHandler(echoopt);
                    tc.addOptionHandler(gaopt);
                }
                catch (InvalidTelnetOptionException e)
                {
                    NeptusLog.pub().error("ApacheAdjustDate :: Error registering option " +
                    		"handlers: " + e.getMessage());
                }
                
                try
                {
                    tc.connect(host, port);
                    Thread reader = new Thread (this);
                    OutputStream outstr = tc.getOutputStream();
                    DataOutputStream dostream = new DataOutputStream(outstr);
                    reader.start();
                    try
                    {
                        //outstr.write(buff, 0 , ret_read);
                        //outstr.flush();
                        dostream.writeBytes(user + "\n");
                        dostream.flush();
                        dostream.writeBytes(password + "\n");
                        dostream.flush();
                        
                        //trialTime = new Date();
                        //dateTime = dateFormater.format(trialTime);
                        ////dostream.writeBytes("date " + dateTime + "\n");
                        //dostream.writeBytes("date " + dateTime
						//		+ " && hwclock --systohc" + "\n");
                        
                        String command = getAdjustDateCommand(vehicle);
                        dostream.writeBytes(command + "\n");
                        
                        dostream.flush();
                        //NeptusLog.pub().info("Date to set in vehicle: " + dateTime);
                		NeptusLog.pub().info("Date to set in vehicle '" + vehicleId + "': " + command);

                        //dostream.writeBytes("exit \n");
                        //dostream.flush();
                        Thread.sleep(5000);
                        //reader.stop();
                        tc.disconnect();
                    }
                    catch (Exception e)
                    {
                        NeptusLog.pub().error("ApacheAdjustDate :: : " + e.getMessage());
                    }

                    
                } catch (SocketException e1)
                {
                    NeptusLog.pub().error("ApacheAdjustDate :: : " + e1.getMessage());
                    return false;
                } catch (IOException e1)
                {
                    NeptusLog.pub().error("ApacheAdjustDate :: : " + e1.getMessage());
                    return false;
                }


            }
        }
        return true;

    }

	private String getAdjustDateCommand(VehicleType vehicle)
	{
		ProtocolArgs adjParamBase = vehicle.getProtocolsArgs().get(AdjustTimeShellArgs.DEFAULT_ROOT_ELEMENT);
		AdjustTimeShellArgs adjParam = null;
		if (adjParamBase != null)
			adjParam = (AdjustTimeShellArgs) adjParamBase;
		else
			adjParam = new AdjustTimeShellArgs();
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
     * Callback method called when TelnetClient receives an option
     * negotiation command.
     * <p>
     * @param negotiation_code - type of negotiation command received
     * (RECEIVED_DO, RECEIVED_DONT, RECEIVED_WILL, RECEIVED_WONT)
     * <p>
     * @param option_code - code of the option negotiated
     * <p>
     */
    public void receivedNegotiation(int negotiation_code, int option_code)
    {
        String command = null;
        if(negotiation_code == TelnetNotificationHandler.RECEIVED_DO)
        {
            command = "DO";
        }
        else if(negotiation_code == TelnetNotificationHandler.RECEIVED_DONT)
        {
            command = "DONT";
        }
        else if(negotiation_code == TelnetNotificationHandler.RECEIVED_WILL)
        {
            command = "WILL";
        }
        else if(negotiation_code == TelnetNotificationHandler.RECEIVED_WONT)
        {
            command = "WONT";
        }
        System.out.println("Received " + command + " for option code " + option_code);
   }

    
    /***
     * Reader thread.
     * Reads lines from the TelnetClient and echoes them
     * on the screen.
     ***/
    public void run()
    {
        InputStream instr = tc.getInputStream();

        try
        {
            byte[] buff = new byte[1024];
            int ret_read = 0;

            do
            {
                ret_read = instr.read(buff);
                if(ret_read > 0)
                {
                    String ret = new String(buff, 0, ret_read);
                    //System.out.print(ret);
                    NeptusLog.pub().info(this
                            + " :: ApacheAdjustDate:: Echo >" + ret);
                }
                //System.out.print("ddd");
            }
            while (ret_read >= 0);
        }
        catch (Exception e)
        {
            //System.err.println("Exception while reading socket:" + e.getMessage());
            NeptusLog.pub().error("Exception while reading socket:"
                    + e.getMessage());
        }

        try
        {
            tc.disconnect();
        }
        catch (Exception e)
        {
            //System.err.println("Exception while closing telnet:" + e.getMessage());
            NeptusLog.pub().error("Exception while closing telnet:"
                    + e.getMessage());
        }
    }

    public static void main(String[] args)
    {
        ConfigFetch.initialize();
        new ApacheAdjustDate().adjustDateTime("rov-ies");
    }
}
