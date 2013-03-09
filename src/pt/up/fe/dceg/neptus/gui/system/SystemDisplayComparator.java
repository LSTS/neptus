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
 * 2010/05/30
 */
package pt.up.fe.dceg.neptus.gui.system;

import java.util.Comparator;

import pt.up.fe.dceg.neptus.console.plugins.SystemsList;

/**
 * Comparator to be used to order the {@link SystemDisplay}s in the {@link SystemsList}.
 * @author Paulo Dias
 */
public class SystemDisplayComparator implements Comparator<SystemDisplay> {

    /**
     * The ordering option. ID for ordering by name; ID_AUTHORITY to order by
     * name but also by authority level; and ID_AUTHORITY_MAIN the same as previous
     * but the main system will always be at the top.
     */
    public enum OrderOptionEnum {
        ID, ID_AUTHORITY, ID_AUTHORITY_MAIN
    };
    
    private OrderOptionEnum orderOption = OrderOptionEnum.ID_AUTHORITY_MAIN;
    
    /**
     * Creates one {@link SystemDisplayComparator} with 
     * {@link #orderOption} = {@link OrderOptionEnum}.ID_AUTHORITY_MAIN
     */
    public SystemDisplayComparator() {
    }

    /**
     * Creates one {@link SystemDisplayComparator} with selected {@link #orderOption}.
     * @param orderOption (see {@link OrderOptionEnum})
     */
    public SystemDisplayComparator(OrderOptionEnum orderOption) {
        setOrderOption(orderOption);
    }

    /**
     * @return the orderOption (see {@link OrderOptionEnum})
     */
    public OrderOptionEnum getOrderOption() {
        return orderOption;
    }
    
    /**
     * @param orderOption the orderOption to set (see {@link OrderOptionEnum})
     */
    public void setOrderOption(OrderOptionEnum orderOption) {
        this.orderOption = orderOption;
    }
    
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(SystemDisplay o1, SystemDisplay o2) {
	    // Comparison if main should be first
        if ((o1.isMainVehicle() ^ o2.isMainVehicle()) && orderOption == OrderOptionEnum.ID_AUTHORITY_MAIN)
            return o1.isMainVehicle() ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        // Comparison if authority option and only one has it
        if ((o1.isWithAuthority() ^ o2.isWithAuthority()) && orderOption != OrderOptionEnum.ID)
            return o1.isWithAuthority() ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        // Comparison if authority option and the levels are different
        if ((o1.getWithAuthority() != o2.getWithAuthority()) && orderOption != OrderOptionEnum.ID)
            return o2.getWithAuthority().ordinal() - o1.getWithAuthority().ordinal();

        return o1.compareTo(o2);
	}
}
