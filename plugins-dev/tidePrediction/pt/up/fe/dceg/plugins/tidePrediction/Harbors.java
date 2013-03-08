/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by mfaria
 * ??/??/???
 * $Id:: Harbors.java 9929 2013-02-14 14:28:47Z pdias                          $:
 */
package pt.up.fe.dceg.plugins.tidePrediction;

public enum Harbors {
    LEIXOES("362,64,440,90", "Leixoes"),
    VIANA_DO_CASTELO("333,34,433,52", "Viana do Castelo"),
    SESIMBRA("360,277,414,300", "Sesimbra");

    private final String coordinates;
    private final String name;

    private Harbors(String coordinates, String name) {
        this.coordinates = coordinates;
        this.name = name;
    }

    public String getCoordinates(){
        return coordinates;
    }

    @Override
    public String toString() {
        return name;
    }
}
