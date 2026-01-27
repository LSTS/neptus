/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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

import java.util.ArrayList;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;

public class Device {
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

    /** NMEA parser. */
    private NMEA parser = null;
    /** True if device is connected. */
    private boolean connected = false;

    private SerialPort serialPort;

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
     * Connect to the serial port of the GPS and read and parse serial port data.
     *
     * @param console original plugin console panel
     * @param device serial port to which the device is connected to
     * @param baudRate serial port baud rate
     * @param dataBits number of data bits to use
     * @param stopBits number of stop bits to use
     * @param parityBits number of parity bits to use
     * @throws Exception if serial port cannot be opened.
     */
    public void connect(ConsolePanel console, String device, int baudRate, int dataBits, int stopBits, int parityBits) throws Exception {
        serialPort = new jssc.SerialPort(device);

        boolean opened = serialPort.openPort();
        if (!opened) {
            serialPort = null;
            Exception e = new Exception("Unable to open port " + device);
            NeptusLog.pub().error(e);
            console.getConsole().post(Notification.error("GPS Device Panel",
                    "Error connecting via serial to  \"" + serialPort + "\".").requireHumanAction(false));

            throw e;
        }
        serialPort.setParams(baudRate, dataBits, stopBits, parityBits);
        serialPort.addEventListener(new jssc.SerialPortEventListener() {
            @Override
            public void serialEvent(jssc.SerialPortEvent serEvt) {
                try {
                    byte[] receivedData = serialPort.readBytes();
                    if (receivedData == null|| receivedData.length == 0)
                        return;

                    for (byte receivedDatum : receivedData) {
                        try {
                            parser.parse(receivedDatum);
                        }
                        catch (Exception e) {
                            NeptusLog.pub().info("<###> " + e);
                        }
                    }
                }
                catch (SerialPortException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        NeptusLog.pub().info("Listening to GPS messages over serial \"" + serialPort + "\".");
        console.getConsole().post(Notification.success("GPS Device Panel", "Connected via serial to \"" + device + "\"."));
    }

    /**
     * Close the serial port connection to the GPS.
     */
    public void disconnect() throws SerialPortException {
        serialPort.closePort();
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
     * Enumerate available serial ports.
     *
     * @return available serial ports.
     */
    public static ArrayList<String> enumerate() {
        ArrayList<String> devs = new ArrayList<>();
        String[] portNames = SerialPortList.getPortNames();
        for (String port : portNames) {
            try {
                SerialPort tempSerialPort = new SerialPort(port);
                tempSerialPort.openPort();
                if (tempSerialPort.isOpened()) {
                    tempSerialPort.closePort();
                    devs.add(port);
                } else {
                    NeptusLog.pub().info("ERROR: failed to open serial port '" + port + "'");
                }
            }
            catch (SerialPortException e) {
                throw new RuntimeException(e);
            }
        }
        return devs;
    }


}
