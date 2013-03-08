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
 * 10/02/2011
 * $Id:: DeliveryListener.java 9615 2012-12-30 23:08:28Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.util.comm.transports;


/**
 * @author pdias
 *
 */
public interface DeliveryListener {
    public enum ResultEnum {UnFinished, Success, TimeOut, Unreacheable, Error}

    public void deliveryResult(ResultEnum result, Exception error);
}
