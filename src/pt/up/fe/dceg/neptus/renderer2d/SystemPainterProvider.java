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
 * May 6, 2012
 */
package pt.up.fe.dceg.neptus.renderer2d;

/**
 * @author pdias
 *
 */
public interface SystemPainterProvider extends Renderer2DPainter {
    public boolean isSystemPainterEnabled();
}
