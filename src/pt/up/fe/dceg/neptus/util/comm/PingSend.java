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
 * 28/Jun/2005
 */
package pt.up.fe.dceg.neptus.util.comm;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.comm.CommMean;

/**
 * @author Paulo Dias
 */
public class PingSend
{
    public static int TIME_OUT = 500;

    public static int ECHO_PORT     = 7;
    public static int FTP_PORT      = 21;
    public static int SSH_PORT      = 22;
    public static int TELNET_PORT   = 23;
    public static int HTTP_PORT     = 80;
    public static int NUVP1_PORT    = 6002; //FIXME Confirmar
    
    /**
     * @param host
     * @return
     */
    public static boolean ping(String host)
    {
        try
        {
            InetAddress address = InetAddress.getByName(host);
            boolean ret = address.isReachable(TIME_OUT);
            //System.out.println("Ping " + ret);
            NeptusLog.pub().info("Pinging " + host + "... " + Boolean.toString(ret));
            return ret;
        } catch (UnknownHostException e)
        {
            NeptusLog.pub().info("Unknown host " + host + ".");
            return false;
        } catch (IOException e)
        {
            NeptusLog.pub().info("I/O exception connecting to " + host + " (" +
                    e.getMessage() + ").");
            return false;
        }
    }


    /**
     * @param cm
     * @return
     */
    public static boolean activityTest(CommMean cm)
    {
        int[] testPorts = new int[]{21,23,22,80,7};
        return activityTest(cm, testPorts);
    }

    /**
     * @param cm
     * @param port
     * @return
     */
    public static boolean activityTest(CommMean cm, int port)
    {
        int[] testPorts = new int[]{port};
        return activityTest(cm, testPorts);
    }

    /**
     * @param cm
     * @param testPorts
     * @return
     */
    public static boolean activityTest(CommMean cm, int[] testPorts)
    {
        boolean isActive = false;
        Socket echoSocket = null;
        int i;
        

        for (i = 0; i < testPorts.length; i++)
        {
            try {
                echoSocket = new Socket();
                //echoSocket.setSoTimeout(750);
                //SocketAddress saddr = new SocketAddress();
                
                echoSocket.connect(new InetSocketAddress(cm.getHostAddress(), testPorts[i]), TIME_OUT);
                isActive = true;
                echoSocket.close();
                break;
            } catch (UnknownHostException e) {
                NeptusLog.pub().info("Don't know about host: " +
                        cm.getHostAddress() + ".");
                return false;
            } catch (IOException e) {
                NeptusLog.pub().info("Couldn't get I/O for "
                        + "the connection to: " +
                        cm.getHostAddress() + " [" + testPorts[i] + "].");
            }
        }
        if (isActive)
        {
            NeptusLog.pub().info("Able to get I/O for "
                    + "the connection to: " +
                    cm.getHostAddress() + " [" + testPorts[i] + "].");
        }
        
        return isActive;
    }
    
    public static void main(String[] args) throws IOException
    {
        /*
        DatagramSocket socketTx = null;
        try
        {
            //DatagramSocket socketRx = new DatagramSocket(4445);
            socketTx = new DatagramSocket(4446);
        }catch(SocketException se)
        {
            JOptionPane.showMessageDialog(null,se,"Socket", JOptionPane.ERROR_MESSAGE);
            //err.error(se);
        }
        */
        byte[] buf = new byte[4+12*8];
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(buf.length);
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        
        dataOutputStream.writeByte(0xFA);
        /*
        for (int i=0;i<dblData.length;i++){
            dataOutputStream.writeDouble(dblData[i]);
        }
        */
        
        buf = byteArrayOutputStream.toByteArray();
        
        /*for (int i=0;i<buf.length;i++){
            System.out.println("buf["+i+"] " + buf[i]);
        }*/
        
        //InetAddress address = InetAddress.getByAddress(new byte[]{127,0,0,1});
        //InetAddress address = InetAddress.getByAddress(new byte[]{(byte) 192,(byte) 168,(byte) 106,(byte) 15});
        
        //InetAddress address = InetAddress.getByName("192.168.106.15");
        //boolean ret = address.isReachable(1000);
        //System.out.println("Ping " + ret);
        
        //DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 7);
        //socketTx.send(packet);
        //ret = address.isLoopbackAddress();
        //System.out.println("Ping " + ret);
         
        
        
        /*
        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            echoSocket = new Socket("localhost", 7);
            out = new PrintWriter(echoSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                                        echoSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: taranis.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for "
                               + "the connection to: taranis.");
            System.exit(1);
        }

        BufferedReader stdIn = new BufferedReader(
                new InputStreamReader(System.in));
        String userInput;
        
        while ((userInput = stdIn.readLine()) != null) {
            out.println(userInput);
            System.out.println("echo: " + in.readLine());
        }
        
        out.close();
        in.close();
        stdIn.close();
        echoSocket.close();
        */
        
        
        CommMean cm = new CommMean();
        cm.setHostAddress("localhost");
        ping(cm.getHostAddress());
        activityTest(cm);
        cm.setHostAddress("whale.fe.up.pt");
        ping(cm.getHostAddress());
        activityTest(cm);
        cm.setHostAddress("192.168.106.32");
        ping(cm.getHostAddress());
        activityTest(cm);
        
        cm.setHostAddress("dceg.fe.up.pt");
        ping(cm.getHostAddress());
        activityTest(cm);
        cm.setHostAddress("192.168.106.15");
        ping(cm.getHostAddress());
        activityTest(cm);
        
        cm.setHostAddress("192.168.106.183");
        ping(cm.getHostAddress());
        activityTest(cm);

        cm.setHostAddress("192.168.106.33");
        ping(cm.getHostAddress());
        activityTest(cm);

    }
}
