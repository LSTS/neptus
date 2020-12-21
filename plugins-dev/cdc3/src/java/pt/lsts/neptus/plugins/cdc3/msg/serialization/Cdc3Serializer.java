/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * Apr 20, 2019
 */
package pt.lsts.neptus.plugins.cdc3.msg.serialization;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import pt.lsts.neptus.plugins.cdc3.msg.Cdc3Message;
import pt.lsts.neptus.plugins.cdc3.msg.EnableMessage;
import pt.lsts.neptus.plugins.cdc3.msg.RetaskToMissionMessage;
import pt.lsts.neptus.plugins.cdc3.msg.RetaskToWaypointMessage;
import pt.lsts.neptus.plugins.cdc3.msg.StatusMessage;

/**
 * @author pdias
 *
 */
public class Cdc3Serializer {

    private static final ByteOrder ENDIANESS = ByteOrder.LITTLE_ENDIAN;
    public static final int SYNC_CODE = 0xCD;
    public static final int POLY_CODE = 0x07;

    public enum CMD_CODES_ENUM {
        ENABLE(1),
        REPORT(3),
        RETASK_MISSION(5),
        RETASK_WP(6),
        EXTENDED_CMD(7);
        
        protected int value;
        
        private CMD_CODES_ENUM(int value) {
            this.value = value;
        }
        
        public static CMD_CODES_ENUM valueOf(long value) throws IllegalArgumentException {
            for (CMD_CODES_ENUM v : CMD_CODES_ENUM.values()) {
                if (v.value == value) {
                    return v;
                }
            }
            throw new IllegalArgumentException("Invalid value for CMD_CODES_ENUM: " + value);
        }

    }

    public static <M extends Cdc3Message> ByteBuffer serialize(M message) {
        byte[] data = new byte[1024];
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ENDIANESS);
        buf.put((byte) SYNC_CODE);
        
        if (message.getClass().isAssignableFrom(EnableMessage.class)) {
            buf.put((byte) CMD_CODES_ENUM.ENABLE.value);

            EnableMessage msg = (EnableMessage) message;
            buf.put((byte) (msg.getMsgOrdinal() & 0xFF));
            buf.put((byte) (msg.getMsgEnableDisable() & 0xFF));
        } 
        else if (message.getClass().isAssignableFrom(StatusMessage.class)) {
            buf.put((byte) CMD_CODES_ENUM.REPORT.value);
            
            StatusMessage msg = (StatusMessage) message;
            buf.putFloat(msg.getLatitudeRads());
            buf.putFloat(msg.getLongitudeRads());
            buf.put((byte) (msg.getDepth() & 0xFF));
            buf.putShort((short) (msg.getYawRad() * 100));
            buf.putShort((short) (msg.getAltitude() * 10));
            buf.put((byte) (msg.getProgress()));
            buf.put((byte) (msg.getFuelLevel() & 0xFF));
            buf.put((byte) (msg.getFuelConfidence() & 0xFF));
        }
        else if (message.getClass().isAssignableFrom(RetaskToMissionMessage.class)) {
            buf.put((byte) CMD_CODES_ENUM.RETASK_MISSION.value);

            RetaskToMissionMessage msg = (RetaskToMissionMessage) message;
            buf.putInt((int) (msg.getTimestampSeconds() & 0xFFFFFFFF));
            buf.putInt((int) (msg.getMissionId() & 0xFFFFFFFF));
        } 
        else if (message.getClass().isAssignableFrom(RetaskToWaypointMessage.class)) {
            buf.put((byte) CMD_CODES_ENUM.RETASK_WP.value);

            RetaskToWaypointMessage msg = (RetaskToWaypointMessage) message;
            buf.putInt((int) (msg.getTimestampSeconds() & 0xFFFFFFFF));
            buf.putFloat(msg.getLatitudeRads());
            buf.putFloat(msg.getLongitudeRads());
            buf.putFloat(msg.getSpeedMps());
        }
        else {
            return null;
        }
        
        int crc8 = crc8(buf.array(), buf.arrayOffset(), buf.position());
        buf.put((byte) (crc8 & 0xFF));
        return buf;
    }

    @SuppressWarnings("unchecked")
    public static <M extends Cdc3Message> M unserialize(ByteBuffer buf) {
        if (!buf.hasArray() || !buf.hasRemaining())
            return null;
        
        int syncCode = buf.get() & 0xFF;
        if (syncCode != SYNC_CODE)
            return null;

        int cmdCode = buf.get() & 0xFF;

        if (cmdCode == CMD_CODES_ENUM.ENABLE.value) {
            EnableMessage msg = new EnableMessage();
            msg.setMsgOrdinal(buf.get() & 0xFF);
            msg.setMsgEnableDisable(buf.get() & 0xFF);

            int crc8Calc = crc8(buf.array(), buf.arrayOffset(), buf.position());
            int crc8 = buf.get() & 0xFF;
            if (crc8 != crc8Calc)
                return null;
            return (M) msg;
        }
        else if (cmdCode == CMD_CODES_ENUM.REPORT.value) {
            StatusMessage msg = new StatusMessage();
            msg.setLatitudeRads(buf.getFloat());
            msg.setLongitudeRads(buf.getFloat());
            msg.setDepth(buf.get() & 0xFF);
            msg.setYawRad(buf.getShort() / 100f);
            msg.setAltitude(buf.getShort() / 10f);
            msg.setProgress(buf.get());
            msg.setFuelLevel(buf.get() & 0xFF);
            msg.setFuelConfidence(buf.get() & 0xFF);
            
            int crc8Calc = crc8(buf.array(), buf.arrayOffset(), buf.position());
            int crc8 = buf.get() & 0xFF;
            if (crc8 != crc8Calc)
                return null;
            return (M) msg;
        }
        else if (cmdCode == CMD_CODES_ENUM.RETASK_MISSION.value) {
            RetaskToMissionMessage msg = new RetaskToMissionMessage();
            msg.setTimestampSeconds(buf.getInt() & 0xFFFFFFFF);
            msg.setMissionId(buf.getInt() & 0xFFFFFFFF);

            int crc8Calc = crc8(buf.array(), buf.arrayOffset(), buf.position());
            int crc8 = buf.get() & 0xFF;
            if (crc8 != crc8Calc)
                return null;
            return (M) msg;
        }
        else if (cmdCode == CMD_CODES_ENUM.RETASK_WP.value) {
            RetaskToWaypointMessage msg = new RetaskToWaypointMessage();
            msg.setTimestampSeconds(buf.getInt() & 0xFFFFFFFF);
            msg.setLatitudeRads(buf.getFloat());
            msg.setLongitudeRads(buf.getFloat());
            msg.setSpeedMps(buf.getFloat());

            int crc8Calc = crc8(buf.array(), buf.arrayOffset(), buf.position());
            int crc8 = buf.get() & 0xFF;
            if (crc8 != crc8Calc)
                return null;
            return (M) msg;
        }
        else if (cmdCode == CMD_CODES_ENUM.EXTENDED_CMD.value) {
        }

        return null;
    }

    public static int crc8(byte[] bytes, int offset, int length) {
        int polynomial = 0x07;
        int crc = 0x00;
        for (int i = offset; i < offset + length; i++) {
            crc ^= bytes[i];
            for (int j = 0; j < 8; ++j) {
              if ((crc & 0x80) != 0) {
                crc = ((crc << 1) ^ polynomial);
              }
              else {
                crc <<= 1;
              }
            }
            crc &= 0xFF;
        }
        return crc;
    }
    
    private static void serializeAndPrintDebug(Cdc3Message stMsg) {
        ByteBuffer stSer = serialize(stMsg);
        String outSer = bufferToString(stSer);
        System.out.println(stMsg);
        System.out.println(String.format("%s serialize to %d bytes as %s", stMsg.getClass().getSimpleName(), stSer.position() - stSer.arrayOffset(), outSer));
        ByteBuffer bufUnser = stSer.duplicate();
        bufUnser.order(ENDIANESS);
        bufUnser.position(bufUnser.arrayOffset());
        System.out.println(unserialize(bufUnser));
        System.out.println();
    }

    /**
     * @param stSer
     * @return
     */
    public static String bufferToString(ByteBuffer stSer) {
        String outSer = "";
        for (int i = stSer.arrayOffset(); i < stSer.position(); i++) {
            outSer += String.format(" 0x%02X", stSer.array()[i] & 0xFF);
        }
        return outSer;
    }

    public static void main(String[] args) {
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        data = "test".getBytes();
//        data = "hello world".getBytes();
//        data = "123456789".getBytes();
        System.out.println(String.format("0xB9=0x%02X", crc8(data, 0, data.length)));
        System.out.println(String.format("185=%d", crc8(data, 0, data.length)));
        
        
        StatusMessage stMsg = new StatusMessage();
        serializeAndPrintDebug(stMsg);

        StatusMessage st1Msg = new StatusMessage();
        st1Msg.setLatitudeDegs(41.17785f);
        st1Msg.setLongitudeDegs(-8.59796f);
        st1Msg.setDepth(1);
        st1Msg.setAltitude(2);
        st1Msg.setYawDeg(16);
        st1Msg.setProgress(11);
        st1Msg.setFuelLevel(10);
        st1Msg.setFuelConfidence(10);
        serializeAndPrintDebug(st1Msg);

        StatusMessage st2Msg = new StatusMessage();
        st2Msg.setLatitudeRads(2.0f);
        st2Msg.setLongitudeRads(1.0f);
        st2Msg.setDepth(5);
        st2Msg.setAltitude(7);
        st2Msg.setYawRad(0.02f);
        st2Msg.setProgress(99);
        st2Msg.setFuelLevel(22);
        st2Msg.setFuelConfidence(0);
        serializeAndPrintDebug(st2Msg);

        RetaskToMissionMessage rtmMsg = new RetaskToMissionMessage();
        serializeAndPrintDebug(rtmMsg);

        RetaskToMissionMessage rtm1Msg = new RetaskToMissionMessage();
        rtm1Msg.setMissionId(1);
        serializeAndPrintDebug(rtm1Msg);

        RetaskToWaypointMessage rtwMsg = new RetaskToWaypointMessage();
        serializeAndPrintDebug(rtwMsg);

        RetaskToWaypointMessage rtw1Msg = new RetaskToWaypointMessage();
        rtw1Msg.setLatitudeDegs(41.17785f);
        rtw1Msg.setLongitudeDegs(-8.59796f);
        rtw1Msg.setSpeedMps(2);
        serializeAndPrintDebug(rtw1Msg);

        EnableMessage enblMsg = new EnableMessage();
        serializeAndPrintDebug(enblMsg);

        EnableMessage enbl1Msg = new EnableMessage();
        enbl1Msg.setMsgOrdinal(3);
        enbl1Msg.setMsgEnableDisable(1);
        serializeAndPrintDebug(enbl1Msg);
    }
}
