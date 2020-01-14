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
 * Jun 27, 2014
 */
package pt.lsts.neptus.plugins.europa.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JPanel;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import psengine.PSConstraint;
import psengine.PSToken;
import psengine.PSVarValue;
import psengine.PSVariable;
import psengine.PSVariableList;
import pt.lsts.neptus.plugins.europa.NeptusSolver;
import pt.lsts.neptus.util.DateTimeUtil;

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
    private PlanToken ghostToken = null;
    private long ghostStartOffset = 0, ghostDurationOffset;
    private static int count = 0;

    private Color c1 = new Color(255, 255, 255);
    private Color c2 = new Color(192, 192, 192);
    private HashSet<TimelineViewListener> listeners = new HashSet<>();

    
    private static final String TRANSIT_ID = "__TRANSIT__";
    
    public void addListener(TimelineViewListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TimelineViewListener listener) {
        listeners.remove(listener);
    }

    public long computeEndTime() {
        if (plan.isEmpty())
            return 0;
        return plan.lastElement().end;
    }

    public TimelineView(NeptusSolver solver) {
        this.solver = solver;
        setMinimumSize(new Dimension(50, 50));
        setPreferredSize(new Dimension(600, 50));

        addMouseMotionListener(this);
        addMouseListener(this);
    }

    private void addToken(PlanToken tok) {
        plan.add(tok);
        repaint();
    }
    
    public void reloadPlan() {
        long end = endTime;
        for (PlanToken p : plan) {
            synchronized (solver.getEuropa()) {
                p.start = (long)(1000*p.original.getStart().getLowerBound());
                p.end = (long)(1000*p.original.getEnd().getLowerBound());
            }
        }
        if (computeEndTime() > end)
            for (TimelineViewListener l : listeners)
                l.endTimeChanged(this, computeEndTime());
    }

    /**
     * @param plan the plan to set
     */
    public void setPlan(Collection<PSToken> p) {
        plan.clear();
        original.clear();

        synchronized (solver.getEuropa()) {
            long startTime = 0;
            for (PSToken tok : p) {
                
                PlanToken t = new PlanToken();

                t.start = startTime + (long) ((tok.getStart().getLowerBound() * 1000));
                t.end = startTime + (long) ((tok.getEnd().getLowerBound() * 1000));
                t.original = tok;
                if (tok.getParameter("task") != null) {
                    t.id = tok.getParameter("task").getSingletonValue().asObject().getEntityName();
                    String name = solver.resolvePlanName(t.id);
                    if (name != null)
                        t.id = name;
                    t.speed = (float) tok.getParameter("speed").getLowerBound();
                    original.put(t, tok);
                }
                else {
                    t.id = TRANSIT_ID;
                    t.speed = (float) tok.getParameter("speed").getLowerBound();                    
                }
                
                addToken(t);
                long newStart = Math.min(startTime, t.start);
                long newEnd = Math.max(endTime, t.end);

                if (newEnd != endTime) {
                    endTime = newEnd;
                    for (TimelineViewListener l : listeners)
                        l.endTimeChanged(this, endTime);
                }

                if (newStart != startTime) {
                    startTime = newStart;
                    for (TimelineViewListener l : listeners)
                        l.startTimeChanged(this, startTime);
                }
            }
        }
    }

    private long screenToTime(Point2D pointOnScreen) {
        double scale = (double) (endTime - startTime) / getWidth();
        return startTime + (long) (scale * pointOnScreen.getX());
    }

    private double timeOnScreen(long timeMillis) {
        return (double) (timeMillis - startTime) / (endTime - startTime) * getWidth();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.gray.brighter());
        g2d.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

        for (long h = 0; h < endTime; h += 3600 * 2000) {
            g2d.setColor(c1);
            double x1 = timeOnScreen(h);
            double x2 = timeOnScreen(h + 3600 * 1000);
            double x3 = timeOnScreen(h + 3600 * 2000);
            g2d.fill(new Rectangle2D.Double(x1, 0, x2, getHeight()));
            g2d.setColor(c2);
            g2d.fill(new Rectangle2D.Double(x2, 0, x3, getHeight()));
        }

        synchronized (plan) {
            for (PlanToken tok : plan) {
                if (selectedToken == tok)
                    paintToken(tok, 0, 0, g2d, new Color(128, 255, 128, 224), new Color(255, 255, 255, 224));
                else
                    paintToken(tok, 0, 0, g2d, new Color(255, 255, 128, 224), new Color(192, 192, 64, 224));
            }
        }

        if (ghostToken != null) {
            long durationInc = ghostDurationOffset;
            paintToken(ghostToken, ghostStartOffset-durationInc/2, ghostStartOffset+durationInc/2, g2d, new Color(192, 128, 128, 64), new Color(192,
                    192, 64, 64));
        }
    }

    private void paintToken(PlanToken tok, long startOffset, long endOffset, Graphics2D g2d, Color c1, Color c2) {
        double start = timeOnScreen(tok.start + startOffset);
        double end = timeOnScreen(tok.end + endOffset);
        
        if (!tok.id.equals(TRANSIT_ID)) {
            g2d.setPaint(new GradientPaint(new Point2D.Double(start, 0), c1, new Point2D.Double(end, getHeight()), c2));
            g2d.fill(new RoundRectangle2D.Double(start, 2, end - start, getHeight() - 4, 12, 12));
            g2d.setColor(Color.black);
            g2d.draw(new RoundRectangle2D.Double(start, 2, end - start, getHeight() - 4, 12, 12));
            g2d.drawString(tok.id, (int) start + 5, 14);
            g2d.drawString(String.format(Locale.US, "%.1f m/s", tok.speed), (int) start + 5, 30);
        }
        else {
            c1 = c1.darker();
            c1 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha()/2);
            c2 = c2.darker();
            c2 = new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha()/2);
            g2d.setPaint(new GradientPaint(new Point2D.Double(start, 0), c1, new Point2D.Double(end, getHeight()), c2));
            g2d.fill(new RoundRectangle2D.Double(start, 2, end - start, getHeight() - 4, 12, 12));
            g2d.setColor(Color.gray.darker());
            g2d.drawString(String.format(Locale.US, "%.1f m/s", tok.speed), (int) start + 5, 30);
        }
    }

    static class PlanToken {
        long start, end;
        float speed;
        String id;
        PSToken original = null;
        PSConstraint startConstraint = null;
        PSConstraint endConstraint = null;
    }

    Point2D lastDragPoint = null;

    @Override
    public void mouseDragged(MouseEvent e) {

        if (ghostToken != null) {
            double xDragAmount = e.getX() - lastDragPoint.getX();
            double scale = (double) (endTime - startTime) / getWidth();
            long timeIncrease = (long) (xDragAmount * scale);

            if (!e.isControlDown()) {
                ghostStartOffset += timeIncrease;
                repaint();
            }
            else {
                ghostDurationOffset += timeIncrease;
                repaint();
            }
            lastDragPoint = e.getPoint();
        }

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
            selected = intercepted.id + " ";

        setToolTipText(selected + DateTimeUtil.milliSecondsToFormatedString(screenToTime(e.getPoint())));
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        PlanToken before = getSelectedToken();
        setSelectedToken(intercepted(e));
        if (getSelectedToken() != before)
            for (TimelineViewListener l : listeners)
                l.tokenSelected(this, getSelectedToken());
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        PlanToken intercepted = intercepted(e);
        if (intercepted != null) {
            ghostToken = intercepted;// .clone();
            ghostStartOffset = ghostDurationOffset = 0;
            lastDragPoint = e.getPoint();
        }
    }
    
    private PSConstraint addConstraint(PSVariable var, int value, PSConstraint existing) {
        
        try {
            String prefix = "";
            
            if (var.getParent() != null) {
                PSToken token = solver.getEuropa().getTokenByKey(var.getParent().getEntityKey());
                if (token != null) {
                    prefix = token.getFullTokenType()+" ("+token.getEntityKey()+").";
                }
            }
            
            solver.log(" --"+prefix+var.toLongString() +" constrained to ["+value+", +inf]");            
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        
        PSConstraint created = null;
        
        synchronized (solver.getEuropa()) {
            solver.getEuropa().setAutoPropagation(false);
            PSVariableList list = new PSVariableList();
            PSVariable varNew = solver.getEuropa().getPlanDatabaseClient()
                    .createVariable("int", "var_"+(++count), true);
            varNew.specifyValue(PSVarValue.getInstance(value));
            list.push_back(varNew);
            list.push_back(var);
            
            created = solver.getEuropa().getPlanDatabaseClient().createConstraint("leq", list);
            boolean propResult = solver.getEuropa().propagate();
            solver.getEuropa().setAutoPropagation(true);
        
            if (!propResult) {
                try {
                    solver.log("Adding constraint failed.");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                solver.getEuropa().setAutoPropagation(false);
                solver.getEuropa().getPlanDatabaseClient().deleteConstraint(created);
                solver.getEuropa().propagate();
                solver.getEuropa().setAutoPropagation(true);
                created = null;
                return existing;
            }
            else {
                if (existing != null) {
                    solver.getEuropa().setAutoPropagation(false);
                    solver.getEuropa().getPlanDatabaseClient().deleteConstraint(existing);
                    solver.getEuropa().propagate();
                    solver.getEuropa().setAutoPropagation(true);                     
                }
                try {
                    solver.log("\n\n--PLAN --\n");
                    solver.log(solver.getEuropa().planDatabaseToString());
                    solver.log("\n--PLAN END --\n");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                //System.out.println(var.toLongString());                
            }
        }
        
        for (TimelineViewListener l : listeners)
            l.planChanged();
        return created;        
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        if (ghostToken != null) {
            PSToken tok = ghostToken.original;
            
            if (ghostStartOffset != 0) {
                int newTime = (int) ((ghostToken.start + ghostStartOffset) - startTime) / 1000;
                ghostToken.startConstraint = addConstraint(tok.getStart(), newTime, ghostToken.startConstraint);
            }

//            if (ghostEndOffset != 0) {
//                int newTime = (int) ((ghostToken.end + ghostEndOffset) - startTime) / 1000;
//                ghostToken.endConstraint = addConstraint(tok.getEnd(), newTime, ghostToken.endConstraint);
//            }
            
            if (ghostDurationOffset != 0) {
                int newTime = (int) Math.min(tok.getDuration().getUpperBound(), ((ghostToken.end-ghostToken.start) + ghostDurationOffset) / 1000);
                ghostToken.endConstraint = addConstraint(tok.getDuration(), newTime, ghostToken.endConstraint);
            }             

        }

        ghostToken = null;
        ghostStartOffset = ghostDurationOffset = 0;
        lastDragPoint = null;
        
        repaint();
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
        repaint();
    }

    /**
     * @return the endTime
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
        repaint();
    }

    /**
     * @return the selectedToken
     */
    public PlanToken getSelectedToken() {
        return selectedToken;
    }

    /**
     * @param selectedToken the selectedToken to set
     */
    public void setSelectedToken(PlanToken selectedToken) {
        this.selectedToken = selectedToken;
        repaint();
    }
}
