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
 * Author: zp
 * Nov 26, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Scanner;

import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MarkElement;

/**
 * @author zp
 *
 */
public class SamplePointTask extends MVPlannerTask {

    private MarkElement elem;
    
    public SamplePointTask(LocationType loc) {
        elem = new MarkElement();
        elem.setId(getName());
        elem.setCenterLocation(loc);
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        String payloads = getPayloadsAbbreviated();
        
        Point2D pt = renderer.getScreenPosition(elem.getCenterLocation());
        
        loadImages();
        if (getAssociatedAllocation() == null)
            g.drawImage(orangeLed, (int)pt.getX()-8, (int)pt.getY()-8, null);
        else
            g.drawImage(greenLed, (int)pt.getX()-8, (int)pt.getY()-8, null);
            
        g.setColor(Color.black);
        g.drawString(getName()+" ("+payloads+")", (int)pt.getX()+8, (int)pt.getY()+8);
        if (getAssociatedAllocation() != null) {
            g.setColor(Color.green.brighter().brighter());
            g.drawString(getName()+" ("+payloads+")", (int)pt.getX()+7, (int)pt.getY()+7);
        }
        else {
            g.setColor(Color.orange);
            g.drawString(getName()+" ("+payloads+")", (int)pt.getX()+7, (int)pt.getY()+7);
        }        
    }

    @Override
    public boolean containsPoint(LocationType lt, StateRenderer2D renderer) {
        return elem.containsPoint(lt, renderer);
    }

    @Override
    public void translate(double offsetNorth, double offsetEast) {
        elem.translate(offsetNorth, offsetEast, 0);
    }
    
    public LocationType getLocation() {
        return elem.getCenterLocation().convertToAbsoluteLatLonDepth();
    }
    
    @Override
    public void mouseDragged(MouseEvent e, StateRenderer2D renderer) {
        elem.setCenterLocation(renderer.getRealWorldLocation(e.getPoint()));
    }


    @Override
    public void rotate(double amountRads) {
        // nothing
    }
    
    @Override
    public void setYaw(double yawRads) {
        // nothing
    }

    @Override
    public void growWidth(double amount) {
        // nothing
    }

    @Override
    public void growLength(double amount) {
        // nothing
    }
    
    @Override
    public LocationType getCenterLocation() {
        return getLocation();
    }

    @Override
    public String marshall() throws IOException {
        LocationType loc = new LocationType(getCenterLocation());
        loc.convertToAbsoluteLatLonDepth();
        return String.format("sample %s %s %f %f %s", getName(), isFirstPriority(), loc.getLatitudeDegs(), loc.getLongitudeDegs(), getRequiredPayloads());
    }

    @Override
    public void unmarshall(String data) throws IOException {
        Scanner input = new Scanner(data);
        input.next("[\\w]+");
        this.name = input.next("[\\w]+");
        this.firstPriority = input.nextBoolean();
        double latDegs = input.nextDouble();
        double lonDegs = input.nextDouble();
        elem.setCenterLocation(new LocationType(latDegs, lonDegs));
        String[] payloads = input.nextLine().replaceAll("[\\[\\]]", "").trim().split("[, ]+");
        getRequiredPayloads().clear();
        for (String p : payloads)
            getRequiredPayloads().add(PayloadRequirement.valueOf(p));
        input.close();        
    }
    
    public static void main(String[] args) throws Exception {
        SamplePointTask pt = new SamplePointTask(new LocationType(41.4534, -8.23434));
        pt.name = "t01";
        pt.firstPriority = true;
        pt.requiredPayloads.add(PayloadRequirement.ctd);
        pt.requiredPayloads.add(PayloadRequirement.camera);

        
        System.out.println(pt.marshall());
        
        SamplePointTask pt2 = new SamplePointTask(new LocationType());
        pt2.unmarshall(pt.marshall());
        System.out.println(pt2.marshall());
    }
}
