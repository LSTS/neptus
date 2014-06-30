/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Jun 27, 2014
 */
package pt.lsts.neptus.plugins.europa.gui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JPanel;

import psengine.PSConstraint;
import psengine.PSConstraintEngineListener;
import psengine.PSPlanDatabaseListener;
import psengine.PSToken;
import psengine.PSVarValue;
import psengine.PSVariable;
import psengine.PSVariableList;
import pt.lsts.neptus.plugins.europa.NeptusSolver;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * @author zp
 *
 */
public class TimelineView extends JPanel implements MouseMotionListener, MouseListener {

    private static final long serialVersionUID = 4762326206387225633L;
    private Vector<PlanToken> plan = new Vector<>();
    private BiMap<PlanToken, PSToken> original = HashBiMap.create();
    private long startTime = 0, endTime = startTime + 3600 * 1000;
    private NeptusSolver solver;
    private PlanToken selectedToken = null;
    
    private PlanToken lookFor(int key) {
        for (Entry<PlanToken, PSToken> entry : original.entrySet())
            if (entry.getValue().getEntityKey() == key)
                return entry.getKey();
        return null;
    }
    
    
    public TimelineView(NeptusSolver solver) {
        this.solver = solver;
        solver.getEuropa().addPlanDatabaseListener(new PSPlanDatabaseListener() {

            @Override
            public void notifyDeactivated(PSToken token) {
                super.notifyDeactivated(token);
                System.out.println("DEACTIVATED "+token.getFullTokenType()+" ("+token.getEntityKey()+")");
            }
        });
        solver.getEuropa().addConstraintEngineListener(new PSConstraintEngineListener() {

            
            @Override
            public void notifyChanged(PSVariable variable, PSChangeType changeType) {
                super.notifyChanged(variable, changeType);
                
                if (!variable.getEntityName().equals("start") && !variable.getEntityName().equals("end")) {
                    return;
                }
                
                int key = variable.getParent().getEntityKey();
                PSToken token = TimelineView.this.solver.getEuropa().getTokenByKey(key);
                
                if (token == null) {
                    return;
                }
                
                token = token.getActive() != null ? token.getActive() : token;
                PlanToken pt = lookFor(key);
                
                if (pt != null) {
                    switch (variable.getEntityName()) {
                        case "start":
                            pt.start = startTime + (long)(token.getStart().getLowerBound() * 1000);
                            //System.out.println(token.getFullTokenType()+" ("+token.getEntityKey()+") <"+changeType+"> "+token.getStart().getLowerBound());
                            break;
                        case "end":
                            pt.end = startTime + (long)(token.getEnd().getLowerBound() * 1000);
                            //System.out.println(token.getFullTokenType()+" ("+token.getEntityKey()+") <"+changeType+"> "+token.getEnd().getLowerBound());
                            break;
                            
                        default:
                            break;
                    }

                    repaint();
                }
            }
        });
        
        addMouseMotionListener(this);
        addMouseListener(this);
    }

    private void addToken(PlanToken tok) {
        plan.add(tok);
        repaint();
    }
    
    /**
     * @param plan the plan to set
     */
    public void setPlan(Collection<PSToken> p) {
        plan.clear();
        original.clear();
        
        long lastTime = startTime = 0;
        for (PSToken tok : p) {
            PlanToken t = new PlanToken();
            
            t.start = startTime + (long)((tok.getStart().getLowerBound() * 1000));
            t.end = startTime + (long)((tok.getEnd().getLowerBound() * 1000));
            t.id = tok.getParameter("task").getSingletonValue().asObject().getEntityName();
            String name = solver.resolvePlanName(t.id);
            if (name != null)
                t.id = name;
            t.speed = (float)tok.getParameter("speed").getLowerBound();
            original.put(t, tok);
            addToken(t);
            lastTime = Math.max(lastTime, t.end);
            startTime = Math.min(startTime, t.start);
        }
        endTime = lastTime;
    }
    
    private long screenToTime(Point2D pointOnScreen) {
        double scale = (double)(endTime - startTime) / getWidth();
        return startTime + (long)(scale * pointOnScreen.getX());
    }
    
    
    private double timeOnScreen(long timeMillis) {
        return (double)(timeMillis - startTime) / (endTime - startTime) * getWidth();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        synchronized (plan) {
            for (PlanToken tok : plan) {
                double start = timeOnScreen(tok.start);
                double end = timeOnScreen(tok.end);
                if (selectedToken == tok)
                    g2d.setPaint(new GradientPaint(new Point2D.Double(start, 0), Color.green.brighter(), new Point2D.Double(end, getHeight()), Color.white));
                else
                    g2d.setPaint(new GradientPaint(new Point2D.Double(start, 0), Color.yellow.brighter(), new Point2D.Double(end, getHeight()), Color.yellow.darker()));
                g2d.fill(new RoundRectangle2D.Double(start, 2, end-start, getHeight()-32, 12, 12));
                g2d.setColor(Color.black);
                g2d.draw(new RoundRectangle2D.Double(start, 2, end-start, getHeight()-32, 12, 12));
                g2d.drawString(tok.id, (int)start+5, 14);
                g2d.drawString(String.format("%.1f m/s", tok.speed), (int)start+5, 30);
            }
        }    
    }
        
    static class PlanToken {
        long start, end;
        float speed;
        String id;
        PSConstraint startConstraint = null;
    }

    Point2D lastDragPoint = null;
    @Override
    public void mouseDragged(MouseEvent e) {    
        if (selectedToken != null && lastDragPoint != null) {
            double xDragAmount = e.getX() - lastDragPoint.getX();
            double scale = (double)(endTime - startTime) / getWidth();
            long timeIncrease = (long)(xDragAmount * scale);
            
            PSToken tok = original.get(selectedToken);
            PSVariableList list = new PSVariableList();
            PSVariable var = solver.getEuropa().getPlanDatabaseClient().createVariable("int", "selected_start_"+selectedToken.hashCode(), true);
            
            int newTime = (int)((selectedToken.start + timeIncrease) - startTime)/1000;
            var.specifyValue(PSVarValue.getInstance(newTime));
            list.push_back(var);
            list.push_back(tok.getStart());
            PSConstraint created = solver.getEuropa().getPlanDatabaseClient().createConstraint("leq", list);
            boolean propResult = solver.getEuropa().propagate();
            
            if (!propResult) {
                solver.getEuropa().getPlanDatabaseClient().deleteConstraint(created);
                solver.getEuropa().propagate();
            }
            else {
                if (selectedToken.startConstraint != null)
                    solver.getEuropa().getPlanDatabaseClient().deleteConstraint(selectedToken.startConstraint);
                selectedToken.startConstraint = created;
            } 
        }
        lastDragPoint = e.getPoint();
    }

    private PlanToken intercepted(MouseEvent e) {
        long time = screenToTime(e.getPoint());
        synchronized (plan) {
            for (PlanToken tok : plan) {
                if (tok.start <= time && tok.end >= time)
                    return tok;
            }
        }
        return null;
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        PlanToken intercepted = intercepted(e);
        String selected = "";
        if (intercepted != null)
            selected = intercepted.id+" ";
        
        setToolTipText(selected+DateTimeUtil.milliSecondsToFormatedString(screenToTime(e.getPoint())));
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        selectedToken = intercepted(e);
        if (e.getButton() == MouseEvent.BUTTON3) {
            if (selectedToken != null) {
                GuiUtils.showInfoPopup("Token",  original.get(selectedToken).toLongString());
            }
        }
        repaint();
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        selectedToken = intercepted(e);
        
        
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        lastDragPoint = null;
        System.out.println(DateTimeUtil.milliSecondsToFormatedString(endTime)+" --> "+DateTimeUtil.milliSecondsToFormatedString(plan.lastElement().end));
        endTime = plan.lastElement().end;
        repaint();
    }    
}
