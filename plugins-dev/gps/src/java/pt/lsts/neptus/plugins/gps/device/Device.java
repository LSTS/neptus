/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: rasm
 * Apr 12, 2011
 */
package pt.lsts.neptus.plugins.gps.device;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import pt.lsts.neptus.NeptusLog;

public class Device implements SerialPortEventListener {
    /** Frame type: 8 data bits, 1 stop bit, no parity. */
    public static final String FRAME_8N1 = "8n1";
    /** Frame type: 8 data bits, 1 stop bit, even parity. */
    public static final String FRAME_8E1 = "8e1";
    /** Frame type: 8 data bits, 1 stop bit, odd parity. */
    public static final String FRAME_8O1 = "8o1";
    /** Frame type: 8 data bits, 2 stop bit, no parity. */
    public static final String FRAME_8N2 = "8n2";
    /** Frame type: 7 data bits, 1 stop bit, even parity. */
    public static final String FRAME_7E1 = "7e1";
    /** Frame type: 7 data bits, 1 stop bit, odd parity. */
    public static final String FRAME_7O1 = "7o1";
    /** Frame type: 7 data bits, 2 stop bit, no parity. */
    public static final String FRAME_7N2 = "7n2";
    /** Frame type: 7 data bits, 2 stop bit, even parity. */
    public static final String FRAME_7E2 = "7e2";
    /** Frame type: 7 data bits, 2 stop bit, odd parity. */
    public static final String FRAME_7O2 = "7o2";
    /** Array of valid frame types. */
    final public static String[] FRAME_TYPES = { FRAME_8N1, FRAME_8E1, FRAME_8O1, FRAME_8N2,
            FRAME_7E1, FRAME_7O1, FRAME_7N2, FRAME_7E2, FRAME_7O2 };
    /** Array of valid baud rates. */
    final public static String[] BAUD_RATES = { "2400", "4800", "9600", "19200", "38400", "57600",
            "115200" };

    public enum Parameter {
        /** Device name. */
        DEV,
        /** Baud rate. */
        BAUD,
        /** Frame type. */
        FRAME,
    }

    /** Serial port handle. */
    private SerialPort handle = null;
    /** Input stream associated with the serial port handle. */
    private InputStream inputStream = null;
    /** Scratch buffer. */
    private final byte[] buffer = new byte[2048];
    /** NMEA parser. */
    private NMEA parser = null;
    /** True if device is connected. */
    private boolean connected = false;

    /**
     * Default constructor.
     * 
     * @param listener
     *            object that will listen to incoming GPS fixes.
     */
    public Device(FixListener listener) {
        parser = new NMEA(listener);
    }

    /**
     * Connect to the serial port of the GPS.
     * 
     * @param params
     *            an hash table with serial port parameters.
     * @throws Exception
     *             if serial port cannot be opened.
     */
    public void connect(HashMap<Parameter, String> params) throws Exception {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(params
                .get(Parameter.DEV));

        if (portIdentifier.isCurrentlyOwned())
            throw new Exception("port '" + portIdentifier.getName() + "' is currently in use");

        CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

        if (!(commPort instanceof SerialPort))
            throw new Exception("device '" + portIdentifier.getName() + "' is not a serial port");

        handle = (SerialPort) commPort;
        int[] args = translateFrameType(params.get(Parameter.FRAME));

        handle.setSerialPortParams(Integer.parseInt(params.get(Parameter.BAUD)), args[0], args[1],
                args[2]);

        inputStream = handle.getInputStream();
        handle.addEventListener(this);
        handle.notifyOnDataAvailable(true);
        connected = true;
    }

    /**
     * Close the serial port connection to the GPS.
     */
    public void disconnect() {
        handle.removeEventListener();
        handle.close();
        connected = false;
    }

    /**
     * Test the connection state.
     * @return true if device is connected, false otherwise.
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * Read and parse serial port data.
     * 
     * @param evt
     *            serial port event.
     */
    @Override
    synchronized public void serialEvent(SerialPortEvent evt) {
        if (evt.getEventType() != SerialPortEvent.DATA_AVAILABLE)
            return;

        try {
            int amount = Math.min(buffer.length, inputStream.available());
            int len = inputStream.read(buffer, 0, amount);
            for (int i = 0; i < len; ++i) {
                try {
                    parser.parse(buffer[i]);
                }
                catch (Exception e) {
                    NeptusLog.pub().info("<###> "+e);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Translate a frame type in the 8n1, 8n2, etc format to broken down
     * parameters understood by RXTX.
     * 
     * @param type
     *            string with abbreviated frame type.
     * @return broken down RXTX parameters.
     * @throws IllegalArgumentException
     *             if the argument type is not valid.
     */
    public static int[] translateFrameType(String type) throws IllegalArgumentException {
        int[] args = new int[3];

        if (type.charAt(0) == '8')
            args[0] = SerialPort.DATABITS_8;
        else if (type.charAt(0) == '7')
            args[0] = SerialPort.DATABITS_7;
        else
            throw new IllegalArgumentException("invalid frame type '" + type + "'");

        if (type.charAt(2) == '1')
            args[1] = SerialPort.STOPBITS_1;
        else if (type.charAt(2) == '2')
            args[1] = SerialPort.STOPBITS_2;
        else
            throw new IllegalArgumentException("invalid frame type '" + type + "'");

        if (type.charAt(1) == 'o')
            args[2] = SerialPort.PARITY_ODD;
        else if (type.charAt(1) == 'e')
            args[2] = SerialPort.PARITY_EVEN;
        else if (type.charAt(1) == 'n')
            args[2] = SerialPort.PARITY_NONE;
        else
            throw new IllegalArgumentException("invalid frame type '" + type + "'");

        return args;
    }

    /**
     * Enumerate available serial ports.
     * 
     * @return available serial ports.
     */
    public static Vector<String> enumerate() {
        Vector<String> devs = new Vector<String>();
        Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers();
        while (ports.hasMoreElements()) {
            CommPortIdentifier dev = (CommPortIdentifier) ports.nextElement();
            if (dev.getPortType() != CommPortIdentifier.PORT_SERIAL)
                continue;

            try {
                CommPort port = dev.open("GPS Enumerator", 50);
                port.close();
                devs.add(dev.getName());
            }
            catch (PortInUseException e) {
                NeptusLog.pub().info("ERROR: serial port '" + dev.getName() + "' is in use");
            }
            catch (Exception e) {
                System.err.println("ERROR: failed to open serial port '" + dev.getName() + "'");
                e.printStackTrace();
            }
        }

        return devs;
    }
}
