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
 * Author: Paulo Dias
 * 2022/08/18
 */
package pt.lsts.neptus.comm.transports.tcp;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.transports.IdPair;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class TCPListenerStream  implements TCPMessageListener, Comparable<TCPListenerStream>{
    private IdPair id = IdPair.empty();
    private PipedOutputStream pos;
    private PipedInputStream pis;

    // Needed because the pis.available() not always when return '0' means end of stream
    private boolean isInputClosed = false;

    public TCPListenerStream(IdPair id) {
        this.id = id;

        pos = new PipedOutputStream();
        try {
            pis = new PipedInputStreamAvailableThrowIOException(this, pos);
        }
        catch (IOException e) {
            NeptusLog.pub().fatal(e.getMessage());
        }

    }

    public IdPair getId() {
        return id;
    }

    public InputStream getInputStream() {
        return pis;
    }

    public boolean isEosReached() {
        return isInputClosed;
    }

    @Override
    public void onTCPMessageNotification(TCPNotification req) {
        try {
            if (req.isEosReceived()) {
                pos.flush();
                pos.close();
                pis.close(); //new
                isInputClosed = true;
                NeptusLog.pub().debug(id + " <###>POS Closed");
            }
            else {
                pos.write(req.getBuffer());
                pos.flush();
            }
        } catch (IOException e) {
            NeptusLog.pub().warn(e.getMessage());
        }
    }

    @Override
    public int compareTo(TCPListenerStream o) {
        return id.getId().compareTo(o.id.getId());
    }

    private class PipedInputStreamAvailableThrowIOException extends PipedInputStream {
        private final TCPListenerStream tcpListenerStream;

        public PipedInputStreamAvailableThrowIOException(TCPListenerStream tcpListenerStream,
                                                         PipedOutputStream src) throws IOException {
            super(src);
            this.tcpListenerStream = tcpListenerStream;
        }

        public PipedInputStreamAvailableThrowIOException(TCPListenerStream tcpListenerStream,
                                                         PipedOutputStream src, int pipeSize) throws IOException {
            super(src, pipeSize);
            this.tcpListenerStream = tcpListenerStream;
        }

        public PipedInputStreamAvailableThrowIOException(TCPListenerStream tcpListenerStream) {
            super();
            this.tcpListenerStream = tcpListenerStream;
        }

        public PipedInputStreamAvailableThrowIOException(TCPListenerStream tcpListenerStream, int pipeSize) {
            super(pipeSize);
            this.tcpListenerStream = tcpListenerStream;
        }

        @Override
        public synchronized int available() throws IOException {
            int available = super.available();
            if (available == 0 && tcpListenerStream.isEosReached()) {
                throw new IOException("Stream closed");
            }
            return available;
        }
    }
}
