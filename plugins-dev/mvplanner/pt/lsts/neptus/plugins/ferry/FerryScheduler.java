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
 * Author: zp
 * 18/05/2017
 */
package pt.lsts.neptus.plugins.ferry;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.Maneuver;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.ScheduledGoto;
import pt.lsts.imc.StationKeeping;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.util.PlanUtilities;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Ferry Scheduler")
@Popup(name = "Ferry Scheduler", pos = POSITION.CENTER, width = 400, height = 400)
public class FerryScheduler extends ConsolePanel {

    private static final long serialVersionUID = 5443043941058557208L;

    @NeptusProperty
    String schedule = "";

    @NeptusProperty
    String vehicle = "";

    @NeptusProperty
    int scheduleExtentMinutes = 240;

    @NeptusProperty
    double depth = 3;

    private Long startTime = null;
    @SuppressWarnings("unused")
    private int minutesEllapsed = 0;
    private JEditorPane editor = new JEditorPane("text/plain", "");
    private JButton btnStart = new JButton(I18n.text("Start"));
    private JButton btnStop = new JButton(I18n.text("Stop"));

    /**
     * @param console
     */
    public FerryScheduler(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

    @Override
    public void initSubPanel() {
        setLayout(new BorderLayout());
        JPanel bottom = new JPanel();
        bottom.add(new JSeparator(JSeparator.HORIZONTAL));
        bottom.add(btnStart);
        bottom.add(btnStop);
        add(bottom, BorderLayout.SOUTH);
        add(editor, BorderLayout.CENTER);
        editor.setText(schedule);

        btnStart.addActionListener(this::start);
        btnStop.addActionListener(this::stop);
    }

    @Periodic(millisBetweenUpdates = 60000)
    public void everyMinute() {
        if (startTime != null)
            minutesEllapsed++;
    }

    public void start(ActionEvent evt) {
        String[] points = editor.getText().split("\n");
        int line = 1;
        Date startTime = new Date();
        Date endTime = new Date(startTime.getTime() + scheduleExtentMinutes * 60 * 1000);
        ArrayList<Maneuver> maneuvers = new ArrayList<>();
        Date currentTime = startTime;

        while (currentTime.before(endTime)) {
            for (String point : points) {
                Matcher m = Pattern.compile("[ ]*([^ ]+):[ ]*([\\d]+)[ ]*\\-[ ]*([\\d]+)[ ]*").matcher(point);
                if (m.matches()) {
                    AbstractElement elem = MapGroup.getMapGroupInstance(getConsole().getMission())
                            .findObject(m.group(1));
                    if (elem == null) {
                        GuiUtils.errorMessage(this, "Error on line " + line,
                                "No object in the map is named " + m.group(1));
                        return;
                    }
                    int start = Integer.valueOf(m.group(2));
                    int end = Integer.valueOf(m.group(3));
                    if (end <= start) {
                        GuiUtils.errorMessage(this, "Error on line " + line, "End must come after start time");
                        return;
                    }

                    Date startDate = new Date(startTime.getTime() + start * 60 * 1000);
                    currentTime = new Date(startTime.getTime() + start * 60 * 1000);

                    if (currentTime.after(endTime))
                        break;

                    int duration = (end - start) * 60;
                    ScheduledGoto man = new ScheduledGoto();
                    LocationType loc = new LocationType(elem.getCenterLocation());
                    loc.convertToAbsoluteLatLonDepth();
                    man.setLat(loc.getLatitudeRads());
                    man.setLon(loc.getLongitudeRads());
                    man.setZ(depth);
                    man.setZUnits(ZUnits.DEPTH);
                    man.setArrivalTime(startDate.getTime() / 1000.0);
                    maneuvers.add(man);

                    StationKeeping sk = new StationKeeping();
                    sk.setLat(loc.getLatitudeRads());
                    sk.setLon(loc.getLongitudeRads());
                    sk.setZ(0);
                    sk.setZUnits(ZUnits.DEPTH);
                    sk.setDuration(duration);
                    maneuvers.add(sk);

                    line++;
                }
                else {
                    GuiUtils.errorMessage(this, "Error on line " + line,
                            "Lines should be in the form: \"<Name>: <Number>-<Number>\"");
                    return;
                }
            }
            
            startTime = currentTime;
        }
        
        PlanSpecification spec = PlanUtilities.createPlan("ferry", maneuvers.toArray(new Maneuver[]{}));
        PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), spec);
        getConsole().getMission().addPlan(plan);
        getConsole().updateMissionListeners();
        
        PlanControl pc = new PlanControl(TYPE.REQUEST, OP.START, 0, plan.getId(), 0, spec, "");
        send(pc);
        
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);

    }

    public void stop(ActionEvent evt) {
        System.out.println("stop");
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);

    }

    @Override
    public DefaultProperty[] getProperties() {
        schedule = editor.getText();
        return super.getProperties();
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        editor.setText(schedule);
    }

    public static void main(String[] args) {

    }

}
