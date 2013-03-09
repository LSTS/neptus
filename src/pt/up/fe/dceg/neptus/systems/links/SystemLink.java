/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
 * Oct 31, 2012
 */
package pt.up.fe.dceg.neptus.systems.links;

import java.net.InetAddress;

import pt.up.fe.dceg.neptus.systems.SystemsManager.SystemLinkType;

/**
 * @author Hugo
 * 
 */
public abstract class SystemLink {
    private SystemLinkType type;
    private InetAddress ip;
    private int port;

    public SystemLink() {
    }

    /**
     * @return the type
     */
    public SystemLinkType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(SystemLinkType type) {
        this.type = type;
    }

    /**
     * @return the ip
     */
    public InetAddress getIp() {
        return ip;
    }

    /**
     * @param ip the ip to set
     */
    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    
}
