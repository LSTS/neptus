/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Jul 3, 2013
 */
package pt.lsts.neptus.comm.iridium;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Vector;

import com.google.common.eventbus.Subscribe;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import pt.lsts.imc.IridiumMsgRx;
import pt.lsts.imc.IridiumMsgTx;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 *
 */
@IridiumProvider(id = "sim", name = "Simulated Messenger", description = "This messenger posts the Iridium message "
        + "directly in the bus of the destination via IMC. Used only for debug / simulation purposes")
public class SimulatedMessenger implements IridiumMessenger {

    protected Vector<IridiumMessage> messagesReceived = new Vector<>();

    protected HashSet<IridiumMessageListener> listeners = new HashSet<>();

    protected String serverUrl = GeneralPreferences.ripplesUrl + "/api/v1/";
    protected String messagesUrl = serverUrl+"irsim";
    protected int timeoutMillis = 10000;

    public SimulatedMessenger() {
        ImcMsgManager.getManager().registerBusListener(this);
    }

    @Override
    public void addListener(IridiumMessageListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IridiumMessageListener listener) {
        listeners.remove(listener);
    }

    @Subscribe
    public void on(IridiumMsgTx tx) throws Exception {

        if (IridiumManager.getManager().getCurrentMessenger() != this)
            return;

        IridiumMessage m = IridiumMessage.deserialize(tx.getData());

        byte[] data = m.serialize();
        data = new String(Hex.encodeHex(data)).getBytes();

        URL u = new URL(messagesUrl);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/hub");
        conn.setRequestProperty("Content-Length", String.valueOf(data.length * 2));
        conn.setConnectTimeout(timeoutMillis);

        OutputStream os = conn.getOutputStream();
        os.write(data);
        os.close();

        NeptusLog.pub().info(messagesUrl + " : " + conn.getResponseCode() + " " + conn.getResponseMessage());

        InputStream is = conn.getInputStream();
        ByteArrayOutputStream incoming = new ByteArrayOutputStream();
        IOUtils.copy(is, incoming);
        is.close();

        NeptusLog.pub().info("Sent " + m.getClass().getSimpleName() + " through HTTP: " + conn.getResponseCode() + " " + conn.getResponseMessage());

        if (conn.getResponseCode() != 200) {
            throw new Exception("Server returned " + conn.getResponseCode() + ": " + conn.getResponseMessage());
        }
    }

    @Override
    public void sendMessage(IridiumMessage msg) throws Exception {
        IridiumMsgRx rx = new IridiumMsgRx();
        rx.setOrigin("Iridium simulated messenger");
        rx.setDst(msg.getDestination());
        rx.setSrc(msg.getSource());
        rx.setData(msg.serialize());
        rx.setHtime(msg.timestampMillis / 1000.0);
        ImcMsgManager.getManager().sendMessage(rx);
    }

    @Override
    public Collection<IridiumMessage> pollMessages(Date timeSince) throws Exception {
        return new Vector<>();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getName() {
        return "Simulated messenger";
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void cleanup() {
        listeners.clear();
        messagesReceived.clear();
    }
}
