/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by rasm
 * Apr 12, 2011
 */
package pt.up.fe.dceg.neptus.plugins.gps.device;

/**
 * Invalid checksum exception.
 * 
 * @author Ricardo Martins
 */
@SuppressWarnings("serial")
public class InvalidChecksumException extends Exception {
    /**
     * Default constructor.
     * 
     * @param computed
     *            computed checksum.
     * @param received
     *            received checksum.
     */
    public InvalidChecksumException(byte computed, byte received) {
        super("computed " + computed + " but received " + received);
    }
}
