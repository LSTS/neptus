/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 12 Dec, 2021
 */
package pt.lsts.neptus.comm.iridium;

import pt.lsts.imc.IMCInputStream;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.TextMessage;
import pt.lsts.neptus.NeptusLog;

import java.util.Collection;
import java.util.Vector;

/**
 * @author pdias
 *
 */
public class PlainTextMessage extends IridiumMessage {

    String text;

    public PlainTextMessage() {
        super(-1);
    }

    @Override
    public int serializeFields(IMCOutputStream out) throws Exception {
        out.write(text.getBytes("UTF-8"));
        out.close();
        return text.getBytes("UTF-8").length;
    }

    @Override
    public int deserializeFields(IMCInputStream in) throws Exception {
        int bav = in.available();
        bav = bav < 0 ? 0 : bav;
        byte[] data = new byte[bav];
        in.readFully(data);
        text = new String(data, "UTF-8");
        text = text.trim();
        return text.getBytes("UTF-8").length;
    }

    public final String getText() {
        return text;
    }

    public final void setText(String text) {
        this.text = text;
    }

    @Override
    public Collection<IMCMessage> asImc() {
        Vector<IMCMessage> msgs = new Vector<>();
        msgs.add(new TextMessage("iridium", text));
        return msgs;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + "\tText: " + getText() + "\n";
    }

    static IridiumMessage createTextMessageFrom(IMCInputStream in) throws Exception {
        try {
            PlainTextReportMessage plainTextReport = new PlainTextReportMessage();
            plainTextReport.deserializeFields(in);
            return plainTextReport;
        } catch (Exception e) {
            NeptusLog.pub().warn("Not able to parse iridium msg as PlainTextReportMessage, trying another or simple text");
        }

        PlainTextMessage plainTextMessage = new PlainTextMessage();
        plainTextMessage.deserializeFields(in);
        return plainTextMessage;
    }
}
