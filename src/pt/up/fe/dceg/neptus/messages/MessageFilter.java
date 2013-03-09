/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 13 de Mar de 2011
 */
package pt.up.fe.dceg.neptus.messages;

import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;

/**
 * @author pdias
 *
 */
public interface MessageFilter<Mi extends MessageInfo, M extends IMessage> {
    public boolean isMessageToListen(Mi info, M msg);
}
