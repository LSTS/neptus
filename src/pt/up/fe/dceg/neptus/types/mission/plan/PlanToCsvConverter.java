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
 * 20??/??/??
 * $Id:: PlanToCsvConverter.java 9616 2012-12-30 23:23:22Z pdias          $:
 */
package pt.up.fe.dceg.neptus.types.mission.plan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import pt.up.fe.dceg.neptus.gui.MissionFileChooser;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

public class PlanToCsvConverter {

	public static void main(String[] args) {
		File mfile = MissionFileChooser.showOpenMissionDialog(new String[] {"nmis", "nmisz"});
		if (mfile == null)
			System.exit(0);
		
		try {
			final MissionType mt = new MissionType(mfile.getAbsolutePath());
			Vector<String> plans = new Vector<String>();
			for (PlanType plan : mt.getIndividualPlansList().values()) {
				plans.add(plan.getId());
			}
			Object res = JOptionPane.showInputDialog(null, "please choose plan to export", "Select plan", JOptionPane.QUESTION_MESSAGE, null, plans.toArray(new String[0]), plans.firstElement());
			
			JFileChooser chooser = new JFileChooser();			
			if (chooser.showSaveDialog(null) != JFileChooser.CANCEL_OPTION) {
				System.out.println("Save "+res+" to "+chooser.getSelectedFile().getAbsolutePath());
				saveAsCsv(mt.getIndividualPlansList().get(res.toString()), chooser.getSelectedFile());
			}
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void saveAsCsv(PlanType plan, File out) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));

		bw.write("Waypoint Id\tLatitude\tLongitude\tLatDegrees\tLonDegrees\n");

		LocationType lastLocation = null;

		for (Maneuver m : plan.getGraph().getManeuversSequence()) {
			if (m instanceof LocatedManeuver) {
				lastLocation = new LocationType(((LocatedManeuver)m).getManeuverLocation());			
			}
			if (lastLocation != null) {
				bw.write(m.getId()+"\t"+lastLocation.getLatitudeAsPrettyString()+"\t"+lastLocation.getLongitudeAsPrettyString()+"\t"+GuiUtils.getNeptusDecimalFormat(7).format(lastLocation.getAbsoluteLatLonDepth()[0])+"\t"+GuiUtils.getNeptusDecimalFormat(7).format(lastLocation.getAbsoluteLatLonDepth()[1])+"\n");
			}
		}
		bw.close();
	}
}
