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
 * 26/Out/2005
 */
package pt.up.fe.dceg.neptus.util.comm.ssh;

import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;


/**
 * @author Paulo Dias
 *
 */
public class SSHAdjustDate extends SSHExec
{

//	private Window parentWindow = null;
//	private PanelResult resInterface = null;
	
	public SSHAdjustDate(String vehicleId)
	{
		super(vehicleId);
		initialize();
	}

//	public SSHAdjustDate(String vehicleId, Window parentWindow)
//	{
//		super(vehicleId);
//		setParentWindow(parentWindow);
//		initialize();
//	}
	
	
	private void initialize() {
		//resInterface  = new PanelResult(parentWindow);
	}

//	/**
//	 * @return the parentWindow
//	 */
//	public Window getParentWindow() {
//		return parentWindow;
//	}
//
//	/**
//	 * @param parentWindow the parentWindow to set
//	 */
//	public void setParentWindow(Window parentWindow) {
//		this.parentWindow = parentWindow;
//	}



	/**
	 * Don't use this use the one without arguments, this only will work
	 * with {@link SSHExec.ADJUST_DATE}, other wise return false;
	 * @see pt.up.fe.dceg.neptus.util.comm.ssh.SSHExec#exec(java.lang.String)
	 */
	@Override
	public boolean exec(String command) {
		if (SSHExec.ADJUST_DATE.equalsIgnoreCase(command))
			return false;
		return exec();
	}

	public boolean exec() {
		boolean ret = super.exec(SSHExec.ADJUST_DATE);
		//resInterface.tMsg.writeMessageTextln(getExecResponse(), (ret) ? MessagePanel.INFO : MessagePanel.ERROR);
		return ret;
	}

	
	public void showInterface() {
		//resInterface.setModal(true);
		//resInterface.setVisible(true);
	}

//	@Override
//	protected void finalize() throws Throwable {
//		super.finalize();
//		resInterface.setVisible(false);
//		resInterface.dispose();
//	}

//	public static boolean adjust(String vehicleId, Window owner)
//	{
//		SSHAdjustDate sshAdj = new SSHAdjustDate(vehicleId, owner);
//		sshAdj.showInterface();
//		boolean ret = sshAdj.exec();
//		return ret;
//	}

    

	public static boolean adjust (String vehicleId)
	{
		return SSHExec.exec(vehicleId, SSHExec.ADJUST_DATE);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ConfigFetch.initialize();
        //boolean rt = SSHExec.exec("lauv", SSHExec.ADJUST_DATE);
        //System.out.println(rt);
		
//		boolean rt = adjust("lauv");
//        System.out.println(rt);
        
//        PanelResult pRes = new PanelResult((Window)null);
//        pRes.setVisible(true);
		
		//SSHAdjustDate sshAdj = new SSHAdjustDate("lauv-blue");
		//sshAdj.showInterface();
		//sshAdj.exec();
		
		adjust("lauv-blue");
		
	}

}

//@SuppressWarnings("serial")
//class PanelResult extends JDialog {
//	MessagePanel tMsg;
//	JButton exitBtn;
//
//	public PanelResult(Window owner) {
//		super(owner);
//		initialize();
//	}
//
//	private void initialize() {
//        this.setVisible(false);
//		this.setSize(300, 200);
//		tMsg = new MessagePanel();
//		exitBtn = new JButton(new AbstractAction("Exit") {
//			private static final long serialVersionUID = -7453069027233059180L;
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				PanelResult.this.dispose();
//			}
//		});
//		
//		GroupLayout layout = new GroupLayout(this.getContentPane());
//		getContentPane().setLayout(layout);
//		layout.setAutoCreateGaps(true);
//        //layout.setAutoCreateContainerGaps(true);
//
//        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
//        		.addComponent(tMsg)
//        		.addGroup(layout.createSequentialGroup()
//        				.addComponent(exitBtn)));
//
//        layout.setVerticalGroup(layout.createSequentialGroup()
//        		.addComponent(tMsg)
//        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
//        				.addComponent(exitBtn)));
//        
//        //layout.linkSize(SwingConstants.HORIZONTAL, getControlMode, setControlMode);
//        //layout.linkSize(SwingConstants.VERTICAL, getControlMode, setControlMode);
//
//        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
//        this.setVisible(false);
//	}
//}
