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
 * 1/09/2011
 */
package pt.up.fe.dceg.neptus.plugins.containers;

import java.awt.Component;

/**
 * @author pdias
 *
 */
public interface LayoutProfileProvider {
    /**
     * If the LayoutProfileProvider child's have a different profile
     * is acceptable to only inform the own profile.
     * @return
     */
    public String getActiveProfile();
    
    /**
     * To activate a profile. Empty restores the full view.
     * (It is necessary to propagate the profile change.)
     * @param name
     * @return
     */
    public boolean setActiveProfile(String name);
    
//    /**
//     * It the same as calling {@link #setActiveProfile(String)} with empty string.
//     * @return
//     */
//    public boolean resetActiveProfile();
//    
//    public String getDefaultProfile();
//    
//    public boolean setDefaultProfile();
    
    public String[] listProfileNames();
    
    public boolean supportsMaximizePanelOnContainer();
    
    public boolean maximizePanelOnContainer(Component comp);

}
