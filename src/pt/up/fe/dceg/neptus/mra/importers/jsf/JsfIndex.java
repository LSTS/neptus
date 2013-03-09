/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Feb 8, 2013
 */
package pt.up.fe.dceg.neptus.mra.importers.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * @author jqcorreia
 *
 */
public class JsfIndex implements Serializable {
    private static final long serialVersionUID = 1L;

    long firstTimestamp = -1;
    long lastTimestamp = -1;
    int numberOfPackets = -1;
    
    LinkedHashMap<Long, Integer> pingMap = new LinkedHashMap<Long, Integer>();
    LinkedHashMap<Integer, ArrayList<Integer>> positionMap = new LinkedHashMap<>();
    
    ArrayList<Float> frequenciesList = new ArrayList<Float>();
    ArrayList<Integer> subSystemsList = new ArrayList<Integer>();
    
}

