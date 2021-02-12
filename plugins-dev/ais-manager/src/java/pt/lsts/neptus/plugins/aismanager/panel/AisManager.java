/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: tsm
 * Mar 20, 2018
 */
package pt.lsts.neptus.plugins.aismanager.panel;

import pt.lsts.aismanager.ShipAisSnapshot;
import pt.lsts.aismanager.api.AisContactManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.types.coord.LocationType;

import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@PluginDescription(name = "Ais Contact Manager", description = "Table showing known Ais contacts", author = "tsm")
@Popup(name = "Ais Contact Manager", width = 750, height = 550)
public class AisManager extends ConsolePanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Danger Zone (Meters)", description = "49206665656c20746865206e6565642c20746865206e65656420666f72207370656564")
    public double dangerZone = 100;

    public final Color STATUS_SAFE = Color.GREEN.darker();
    public final Color STATUS_WARN = Color.YELLOW.darker();
    public final Color STATUS_DANGER = Color.RED.darker();

    private final AisManagerTable table = new AisManagerTable();
    private final AisContactManager AIS_MANAGER = AisContactManager.getInstance();
    private final ConcurrentHashMap<String, Long> knownSnapshots = new ConcurrentHashMap<>();

    public AisManager(ConsoleLayout console) {
        super(console);
        PeriodicUpdatesService.registerPojo(this);

        this.setLayout(new BorderLayout());
        this.add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {

    }

    @Periodic(millisBetweenUpdates = 2000)
    public void onPeridicUpdate() {
        synchronized (AIS_MANAGER) {
            for (ShipAisSnapshot ais : AIS_MANAGER.getShips()) {
                String aisLabel = ais.getLabel();
                Long aisTimestamp = ais.getTimestampMs();

                if (!knownSnapshots.containsKey(aisLabel) || knownSnapshots.get(aisLabel) != aisTimestamp) {
                    knownSnapshots.put(aisLabel, aisTimestamp);

                    double[] navInfo = {ais.getSog(), ais.getCog(), ais.getHeading(), ais.getLatRads(), ais.getLonRads()};
                    Color status = computeStatus(ais.getLatDegs(), ais.getLonDegs());
                    table.update(aisLabel, ais.getMmsi(), navInfo, ais.getTimestampMs(), status);
                }
            }
        }
    }

    /**
     * Compute if given AIS System is endangering any IMC system
     * */
    private Color computeStatus(double aisLatDegs, double aisLonDegs) {
        LocationType loc = new LocationType(aisLatDegs, aisLonDegs);

        // check if too close
        Optional<ImcSystem> ret = Arrays.stream(ImcSystemsHolder.lookupAllActiveSystems())
                .filter(sys -> loc.getHorizontalDistanceInMeters(sys.getLocation().convertToAbsoluteLatLonDepth()) <= dangerZone)
                .findAny();

        if (ret.isPresent())
            return STATUS_DANGER;

        // check if in warning zone
        ret = Arrays.stream(ImcSystemsHolder.lookupAllActiveSystems())
                .filter(sys -> loc.getDistanceInMeters(sys.getLocation()) <= dangerZone * 1.3)
                .findAny();

        if (ret.isPresent())
            return STATUS_WARN;

        return STATUS_SAFE;
    }
}
