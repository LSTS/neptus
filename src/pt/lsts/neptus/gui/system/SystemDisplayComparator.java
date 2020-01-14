/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 2010/05/30
 */
package pt.lsts.neptus.gui.system;

import java.util.Comparator;

import pt.lsts.neptus.console.plugins.SystemsList;

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
