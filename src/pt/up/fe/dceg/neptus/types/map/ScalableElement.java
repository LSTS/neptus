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
 * 28/Jun/2005
 * $Id:: ScalableElement.java 9616 2012-12-30 23:23:22Z pdias             $:
 */
package pt.up.fe.dceg.neptus.types.map;

/**
 * @author ZP
 */
public interface ScalableElement {
	public abstract void grow(double ammount);
	public abstract void shrink(double ammount);
	public abstract double[] getDimension();
	public abstract void setDimension(double[] newDimension);
}
