package pt.lsts.neptus.plugins.formationcontrol;//package elias.kth.MyFirstPlugin;

import pt.lsts.neptus.plugins.formationcontrol.MyFirstPlugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Normalizer;
import java.util.ArrayList;

/**
 * Created by niklas on 8/23/16.
 */
public class Settingswindow extends JFrame {


    MyFirstPlugin myPlugin = null;

    JPanel mainPanel = null;
    ArrayList<VehicleTile> tiles = new ArrayList<VehicleTile>();

    public Settingswindow(MyFirstPlugin parent)
    {
        super("Settings");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        myPlugin = parent;
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        //mainPanel.add(new JTextArea("hej"));
        super.setSize(250,250);
        add(mainPanel);
        super.setVisible(false);
    }

    public void update()
    {
        mainPanel.removeAll();
        tiles.clear();

        mainPanel.add(new FormationTile());

        for(Vehicle v: myPlugin.getConnectedVehicles())
            tiles.add(new VehicleTile(v));
        for(VehicleTile v: tiles)
            mainPanel.add(v);

        System.out.println("Tiles: " + tiles.size());

        this.setVisible(true);

        this.invalidate();
    }

    class FormationTile extends JPanel implements ActionListener {
        JTextField name = new JTextField("TEXT");

        JPanel container = null;

        JPanel R_container = null;
        JTextArea R_text = null;
        JTextField R_editableText = null;

        JPanel gammaT_container = null;
        JTextArea gammaT_text = null;
        JTextField gammaT_editableText = null;

        JPanel gammaij_container = null;
        JTextArea gammaij_text = null;
        JTextField gammaij_editableText = null;

        JPanel controller_container = null;
        JTextArea controller_text = null;
        JTextField controller_editableText = null;

        public FormationTile()
        {
            super();
            setLayout(new BorderLayout());

            name.setText("Parameters");
            name.setEditable(false);
            add(name, BorderLayout.NORTH);

            //settings for R
            R_container = new JPanel(new BorderLayout());
            R_text = new JTextArea("R       ");
            R_text.setEditable(false);
            R_editableText = new JTextField(String.format("%.10f", myPlugin.R_triple));
            R_editableText.addActionListener(this);
            R_container.add(R_text, BorderLayout.WEST);
            R_container.add(R_editableText, BorderLayout.CENTER);

            //settings for gammaT
            gammaT_container = new JPanel(new BorderLayout());
            gammaT_text = new JTextArea("gammaT       ");
            gammaT_text.setEditable(false);
            gammaT_editableText = new JTextField(String.format("%.10f", myPlugin.gamma_T));
            gammaT_editableText.addActionListener(this);
            gammaT_container.add(gammaT_text, BorderLayout.WEST);
            gammaT_container.add(gammaT_editableText, BorderLayout.CENTER);

            //settings for gammaij
            gammaij_container = new JPanel(new BorderLayout());
            gammaij_text = new JTextArea("gammaij       ");
            gammaij_text.setEditable(false);
            gammaij_editableText = new JTextField(String.format("%.10f", myPlugin.gamma_ij));
            gammaij_editableText.addActionListener(this);
            gammaij_container.add(gammaij_text, BorderLayout.WEST);
            gammaij_container.add(gammaij_editableText, BorderLayout.CENTER);

            //settings for gammaij
            controller_container = new JPanel(new BorderLayout());
            controller_text = new JTextArea("controller       ");
            controller_text.setEditable(false);
            controller_editableText = new JTextField(String.format("%d", myPlugin.controller));
            controller_editableText.addActionListener(this);
            controller_container.add(controller_text, BorderLayout.WEST);
            controller_container.add(controller_editableText, BorderLayout.CENTER);

            //contains all the other JPanels
            container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
            container.add(R_container);
            container.add(gammaT_container);
            container.add(gammaij_container);
            container.add(controller_container);

            add(container, BorderLayout.CENTER);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Enter pressed!");
            //.replaceAll("\\D++", "") tar bort allt förutom nummer (även .)
            if (e.getSource() == R_editableText) {
                System.out.println("Det var: R");
                double newR = Double.parseDouble(R_editableText.getText());
                System.out.println("vehicle params R set to: " + newR);
                myPlugin.R_triple = newR;
            }
            if (e.getSource() == gammaT_editableText) {
                System.out.println("Det var: gamma_T");
                double newgammaT = Double.parseDouble(gammaT_editableText.getText());
                System.out.println("vehicle params gamma_T set to: " + newgammaT);
                myPlugin.gamma_T = newgammaT;
            }
            if (e.getSource() == gammaij_editableText) {
                System.out.println("Det var: gamma_ij");
                double newgammaij = Double.parseDouble(gammaij_editableText.getText());
                System.out.println("vehicle params gamma_ij set to: " + newgammaij);
                myPlugin.gamma_ij = newgammaij;
            }
            if (e.getSource() == controller_editableText) {
                System.out.println("Det var: controller");
                int newcontroller = Integer.parseInt(controller_editableText.getText());
                System.out.println("vehicle params controller set to: " + newcontroller);
                myPlugin.controller = newcontroller;
            }
        }
    }

    class VehicleTile extends JPanel implements ActionListener
    {
        Vehicle vehicle = null;
        JTextField vehicle_name = new JTextField("TEXT");

        JPanel container = null;

        JPanel d_container = null;
        JTextArea d_text = null;
        JTextField d_editableText = null;

        JPanel delta_container = null;
        JTextArea delta_text = null;
        JTextField delta_editableText = null;

        JPanel alpha_container = null;
        JTextArea alpha_text = null;
        JTextField alpha_editableText = null;

        public VehicleTile(Vehicle v)
        {
            super();
            setLayout(new BorderLayout());

            vehicle = v;
            vehicle_name.setText(vehicle.getSystemName());
            vehicle_name.setEditable(false);
            add(vehicle_name,BorderLayout.NORTH);

            //settings for d
            d_container = new JPanel(new BorderLayout());
            d_text = new JTextArea("d        ");
            d_text.setEditable(false);
            d_editableText = new JTextField(String.format("%.20f",vehicle.params.getD()));
            d_editableText.addActionListener(this);
            d_container.add(d_text,BorderLayout.WEST);
            d_container.add(d_editableText,BorderLayout.CENTER);

            //settings for delta
            delta_container = new JPanel(new BorderLayout());
            delta_text = new JTextArea("delta  ");
            delta_text.setEditable(false);
            delta_editableText = new JTextField(String.format("%.20f",vehicle.params.getDelta()));
            delta_editableText.addActionListener(this);
            delta_container.add(delta_text,BorderLayout.WEST);
            delta_container.add(delta_editableText,BorderLayout.CENTER);

            //settings for Alpha
            alpha_container = new JPanel(new BorderLayout());
            alpha_text = new JTextArea("alpha ");
            alpha_text.setEditable(false);
            alpha_editableText = new JTextField(String.format("%.20f",vehicle.params.getAlpha()));
            alpha_editableText.addActionListener(this);
            alpha_container.add(alpha_text,BorderLayout.WEST);
            alpha_container.add(alpha_editableText,BorderLayout.CENTER);

            //contains all the other JPanels
            container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
            container.add(d_container);
            container.add(delta_container);
            container.add(alpha_container);

            add(container, BorderLayout.CENTER);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Enter pressed!");
            //.replaceAll("\\D++", "") tar bort allt förutom nummer (även .)
            if(e.getSource() == d_editableText)
            {
                System.out.println("Det var: d");
                double newd = Double.parseDouble(d_editableText.getText());
                System.out.println("vehicle params d set to: " + newd);
                vehicle.params.d = newd;
            }
            if(e.getSource() == delta_editableText)
            {
                System.out.println("Det var: delta");
                double newdelta = Double.parseDouble(delta_editableText.getText());
                System.out.println("vehicle params delta set to: " + newdelta);
                vehicle.params.setDelta(newdelta);
            }
            if(e.getSource() == alpha_editableText)
            {
                System.out.println("Det var: alpha");
                double newalpha = Double.parseDouble(alpha_editableText.getText());
                System.out.println("vehicle params alpha set to: " + newalpha);
                vehicle.params.setDelta(newalpha);
            }
        }
    }
}
