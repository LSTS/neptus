/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by canasta
 * 27 de Fev de 2012
 */
package pt.up.fe.dceg.neptus.plugins.uavs.interfaces;

import java.awt.Graphics2D;

/**
 * @author canasta
 *
 */
public interface IUavPainter {

    public void paint(Graphics2D g, int width, int height, Object args);  
}
