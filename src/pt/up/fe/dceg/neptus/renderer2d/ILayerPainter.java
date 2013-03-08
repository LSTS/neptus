/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/09/22
 * $Id:: ILayerPainter.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.renderer2d;

import java.util.Collection;

/**
 * @author zp
 *
 */
public interface ILayerPainter {

	public boolean addPostRenderPainter(Renderer2DPainter painter, String name);
	public boolean removePostRenderPainter(Renderer2DPainter painter);
	public Collection<Renderer2DPainter> getPostPainters();

	public void addPreRenderPainter(Renderer2DPainter painter);
	public void removePreRenderPainter(Renderer2DPainter painter);

}
