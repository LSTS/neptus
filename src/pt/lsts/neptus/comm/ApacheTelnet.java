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
 * Author: Paulo Dias
 * 22/Jun/2005
 */
package pt.lsts.neptus.comm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.SimpleOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;

import pt.lsts.neptus.NeptusLog;

/**
 * @author Paulo Dias
 */
public class ApacheTelnet
implements Runnable, TelnetNotificationHandler
{
    static TelnetClient tc = null;

    /***
     * Main for the TelnetClientExample.
     ***/
    public static void main(String[] args) throws IOException
    {
        FileOutputStream fout = null;

        if(args.length < 1)
        {
            System.err.println("Usage: TelnetClientExample1 <remote-ip> [<remote-port>]");
            System.exit(1);
        }

        String remoteip = args[0];

        int remoteport;

        if (args.length > 1)
        {
            remoteport = Integer.parseInt(args[1]);
        }
        else
        {
            remoteport = 23;
        }

        try
        {
            fout = new FileOutputStream ("spy.log", true);
        }
        catch (Exception e)
        {
            System.err.println(
                "Exception while opening the spy file: "
                + e.getMessage());
        }

        tc = new TelnetClient();

        TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("VT100", false, false, true, false);
        EchoOptionHandler echoopt = new EchoOptionHandler(true, false, true, false);
        SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true, true, true, true);

        try
        {
            tc.addOptionHandler(ttopt);
            tc.addOptionHandler(echoopt);
            tc.addOptionHandler(gaopt);
        }
        catch (InvalidTelnetOptionException e)
        {
            System.err.println("Error registering option handlers: " + e.getMessage());
        }

        while (true)
        {
            boolean end_loop = false;
            try
            {
                tc.connect(remoteip, remoteport);


                Thread reader = new Thread (new ApacheTelnet());
                tc.registerNotifHandler(new ApacheTelnet());
                NeptusLog.pub().info("<###>TelnetClientExample");
                NeptusLog.pub().info("<###>Type AYT to send an AYT telnet command");
                NeptusLog.pub().info("<###>Type OPT to print a report of status of options (0-24)");
                NeptusLog.pub().info("<###>Type REGISTER to register a new SimpleOptionHandler");
                NeptusLog.pub().info("<###>Type UNREGISTER to unregister an OptionHandler");
                NeptusLog.pub().info("<###>Type SPY to register the spy (connect to port 3333 to spy)");
                NeptusLog.pub().info("<###>Type UNSPY to stop spying the connection");

                reader.start();
                OutputStream outstr = tc.getOutputStream();

                byte[] buff = new byte[1024];
                int ret_read = 0;

                do
                {
                    try
                    {
                        ret_read = System.in.read(buff);
                        if(ret_read > 0)
                        {
                            if((new String(buff, 0, ret_read)).startsWith("AYT"))
                            {
                                try
                                {
                                    NeptusLog.pub().info("<###>Sending AYT");

                                    NeptusLog.pub().info("<###>AYT response:" + tc.sendAYT(5000));
                                }
                                catch (Exception e)
                                {
                                    System.err.println("Exception waiting AYT response: " + e.getMessage());
                                }
                            }
                            else if((new String(buff, 0, ret_read)).startsWith("OPT"))
                            {
                                 NeptusLog.pub().info("<###>Status of options:");
                                 for(int ii=0; ii<25; ii++)
                                    NeptusLog.pub().info("<###>Local Option " + ii + ":" + tc.getLocalOptionState(ii) + " Remote Option " + ii + ":" + tc.getRemoteOptionState(ii));
                            }
                            else if((new String(buff, 0, ret_read)).startsWith("REGISTER"))
                            {
                                StringTokenizer st = new StringTokenizer(new String(buff));
                                try
                                {
                                    st.nextToken();
                                    int opcode = (new Integer(st.nextToken())).intValue();
                                    boolean initlocal = (new Boolean(st.nextToken())).booleanValue();
                                    boolean initremote = (new Boolean(st.nextToken())).booleanValue();
                                    boolean acceptlocal = (new Boolean(st.nextToken())).booleanValue();
                                    boolean acceptremote = (new Boolean(st.nextToken())).booleanValue();
                                    SimpleOptionHandler opthand = new SimpleOptionHandler(opcode, initlocal, initremote,
                                                                    acceptlocal, acceptremote);
                                    tc.addOptionHandler(opthand);
                                }
                                catch (Exception e)
                                {
                                    if(e instanceof InvalidTelnetOptionException)
                                    {
                                        System.err.println("Error registering option: " + e.getMessage());
                                    }
                                    else
                                    {
                                        System.err.println("Invalid REGISTER command.");
                                        System.err.println("Use REGISTER optcode initlocal initremote acceptlocal acceptremote");
                                        System.err.println("(optcode is an integer.)");
                                        System.err.println("(initlocal, initremote, acceptlocal, acceptremote are boolean)");
                                    }
                                }
                            }
                            else if((new String(buff, 0, ret_read)).startsWith("UNREGISTER"))
                            {
                                StringTokenizer st = new StringTokenizer(new String(buff));
                                try
                                {
                                    st.nextToken();
                                    int opcode = (new Integer(st.nextToken())).intValue();
                                    tc.deleteOptionHandler(opcode);
                                }
                                catch (Exception e)
                                {
                                    if(e instanceof InvalidTelnetOptionException)
                                    {
                                        System.err.println("Error unregistering option: " + e.getMessage());
                                    }
                                    else
                                    {
                                        System.err.println("Invalid UNREGISTER command.");
                                        System.err.println("Use UNREGISTER optcode");
                                        System.err.println("(optcode is an integer)");
                                    }
                                }
                            }
                            else if((new String(buff, 0, ret_read)).startsWith("SPY"))
                            {
                                try
                                {
                                    tc.registerSpyStream(fout);
                                }
                                catch (Exception e)
                                {
                                    NeptusLog.pub().error(e.getMessage());
                                }
                            }
                            else if((new String(buff, 0, ret_read)).startsWith("UNSPY"))
                            {
                                tc.stopSpyStream();
                            }
                            else
                            {
                                try
                                {
                                        outstr.write(buff, 0 , ret_read);
                                        outstr.flush();
                                }
                                catch (Exception e)
                                {
                                    NeptusLog.pub().error(e.getMessage());
                                    end_loop = true;
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        NeptusLog.pub().error(e.getMessage());
                        end_loop = true;
                    }
                }
                while((ret_read > 0) && (!(end_loop)));

                try
                {
                    tc.disconnect();
                }
                catch (Exception e)
                {
                          System.err.println("Exception while connecting:" + e.getMessage());
                }
            }
            catch (Exception e)
            {
                    System.err.println("Exception while connecting:" + e.getMessage());
                    System.exit(1);
            }
        }
    }


    /***
     * Callback method called when TelnetClient receives an option
     * negotiation command.
     * <p>
     * @param negotiation_code - type of negotiation command received
     * (RECEIVED_DO, RECEIVED_DONT, RECEIVED_WILL, RECEIVED_WONT)
     * <p>
     * @param option_code - code of the option negotiated
     * <p>
     ***/
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
        NeptusLog.pub().info("<###>Received " + command + " for option code " + option_code);
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
                    System.out.print(new String(buff, 0, ret_read));
                }
            }
            while (ret_read >= 0);
        }
        catch (Exception e)
        {
            System.err.println("Exception while reading socket:" + e.getMessage());
        }

        try
        {
            tc.disconnect();
        }
        catch (Exception e)
        {
            System.err.println("Exception while closing telnet:" + e.getMessage());
        }
    }
}
