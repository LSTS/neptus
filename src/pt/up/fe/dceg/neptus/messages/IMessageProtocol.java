/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: IMessageProtocol.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.messages;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

public interface IMessageProtocol<T extends IMessage> {
        String name();
        String version();
        public int serializationSize(T msg);
        public void serialize(T m, OutputStream os) throws Exception;
        public T unserialize(InputStream is) throws Exception;
        public T newMessage(int id) throws Exception;
        public T newMessage(String name) throws Exception;
        public Collection<String> getMessageNames();
        public int getMessageCount();
        public String getMessageName(int id) throws Exception;
        public int getMessageId(String name) throws Exception;
}
