package pt.lsts.neptus.plugins.nvl_runtime;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.VehicleState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.nvl.runtime.Availability;
import pt.lsts.nvl.runtime.Filter;
import pt.lsts.nvl.runtime.NVLRuntime;
import pt.lsts.nvl.runtime.NVLVehicle;
import pt.lsts.nvl.runtime.NVLVehicleType;
import pt.lsts.nvl.runtime.TaskExecution;
import pt.lsts.nvl.runtime.TaskSpecification;
import pt.lsts.nvl.runtime.VehicleRequirements;

@PluginDescription(name = "NVL Runtime Feature", author = "Keila Lima")
@Popup(pos = POSITION.BOTTOM_RIGHT, width=300, height=300, accelerator='y')
@SuppressWarnings("serial")
public class NeptusRuntime extends InteractionAdapter implements NVLRuntime {
   private  Map<String,NeptusVehicleAdapter> vehicles;
   private  Map<String,NeptusTaskSpecificationAdapter> tasks; //Or List?
   private  List<NeptusTaskExecutionAdapter> runningTasks;
   private JButton testButton;
	/**
     * @param console
     */
    public NeptusRuntime(ConsoleLayout console) {
        super(console);
        
    }

    
    @Override
    public void initSubPanel() {
        runningTasks = Collections.synchronizedList(new ArrayList<>());
        vehicles = Collections.synchronizedMap(new HashMap<>());
        tasks = Collections.synchronizedMap(new HashMap<>());
        //initialize active vehicles
        for(ImcSystem vec: ImcSystemsHolder.lookupActiveSystemVehicles()){
            VehicleState systemState = ImcMsgManager.getManager().getState(vec.getName()).last(VehicleState.class);
            STATE state = STATE.valueOf(systemState.getOpModeStr());
            vehicles.put(vec.getName(),new NeptusVehicleAdapter(vec,state));
            System.out.println("V " + vec.getName()+" "+state.toString());
        }
        //initialize existing plans in the console
        for(PlanType plan: getConsole().getMission().getIndividualPlansList().values()){
            tasks.put(plan.getId(),new NeptusTaskSpecificationAdapter(plan));
            //System.out.println("P " + plan.getId());

        }
        test();
    }
    private void test() {
        testButton = new JButton(
                new AbstractAction(I18n.text("Test!")) {

                    @Override
                    public void actionPerformed(ActionEvent e) {   
                        // Plano IMC
                        NeptusTaskSpecificationAdapter ts = (NeptusTaskSpecificationAdapter) getTasks( x -> x.getId().equals("nvlPlan")).get(0); //Filter<TaskSpecification>
                        System.out.println("Task "+ts.getId());
                        VehicleRequirements reqs = ts.getRequirements() .get(0)
                        .type(NVLVehicleType.AUV)
                        .availability(Availability.AVAILABLE)
                        .name("lauv-noptilus-2");
                        
                        System.out.println("Requirements after change params: "+reqs);
                        
                        ts.setRequirements(reqs);
                        tasks.put(ts.getId(),ts);
                        // Veículos disponíveis
                        List<NVLVehicle> vs = getVehicles(ts.getRequirements().get(0));
                        for(NVLVehicle v: vs){
                            NeptusLog.pub().info(I18n.text("SELECTED VEHICLE "+v.getId()));
                        }
                        launchTask(ts, vs);
                        
                       
                    }
                });
        
        add(testButton);

    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
    
    
    @Override
	public List<NVLVehicle> getVehicles(Filter<NVLVehicle> f) { //VehicleRequirement <- Filter<NVLVehicle>
	        List <NVLVehicle> result = new ArrayList<>();
	        
	        for(NVLVehicle v: vehicles.values()){
	            if(f.apply(v))
	                result.add(v);
	        }
        //vehicles.values().stream().filter(x -> f.apply(x)).forEach(v -> result.add(v));
		return result;
	}

    @Subscribe
    public void onVehicleStateChanged(ConsoleEventVehicleStateChanged e) {
          
        ImcSystem imcsystem = ImcSystemsHolder.getSystemWithName(e.getVehicle());
        vehicles.put(e.getVehicle(), new NeptusVehicleAdapter(imcsystem,e.getState())); //Pode ser disconnected
    }


	@Override
	public TaskExecution launchTask(TaskSpecification task, List<NVLVehicle> vehicles) { 
	    
	    tasks.put(task.getId(),(NeptusTaskSpecificationAdapter) task);
		boolean acoustics=false; 
		List<String> vs = new ArrayList<>();
		vehicles.stream().map(v -> v.getId()).forEach(id -> vs.add(id));
		vehicles.stream().map(v -> v.getId()).forEach(id -> ((NeptusTaskSpecificationAdapter)task).getPlan().setVehicle(id));
		
        //ADD plan to console
	    PlanType plan = ((NeptusTaskSpecificationAdapter) task).getPlan();
        
        //sendMessage(IMCMessage msg, String errorTextForDialog, boolean sendOnlyThroughOneAcoustically,String... ids)
        boolean sent = IMCSendMessageUtils.sendMessage(plan.asIMCPlan(),null,acoustics, vs.toArray(new String[vs.size()]));
        
        NeptusTaskExecutionAdapter exec = new NeptusTaskExecutionAdapter(task.getId());
//        PlanDBControl pdbControl;
//        pdbControl.setRemoteSystemId(vs.get(index));
//        pdbControl.sendPlan(plan1);
//        int reqId = IMCSendMessageUtils.getNextRequestId();
//        PlanControl pc = new PlanControl();
//        pc.setType(PlanControl.TYPE.REQUEST);
//        pc.setRequestId(reqId);
//        String cmdStrMsg = "";
        
        exec.synchronizedWithVehicles(sent); 
        runningTasks.add(exec);  
        
		return exec;
	   	}

    /**
     * @return the runningTasks
     */
    public List<NeptusTaskExecutionAdapter> getRunningTasks() {
        return runningTasks;
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.nvl.runtime.NVLRuntime#getVehicle(java.lang.String)
     */
    @Override
    public NVLVehicle getVehicle(String id) {
        
        return vehicles.get(id);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.nvl.runtime.NVLRuntime#getTasks(pt.lsts.neptus.nvl.runtime.Filter)
     */
    @Override
    public List<TaskSpecification> getTasks(Filter<TaskSpecification> filter) {
        List <TaskSpecification> result = new ArrayList<>();
        for(TaskSpecification task: tasks.values()){
            if(filter.apply(task))
                result.add(task);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.nvl.runtime.NVLRuntime#getTask(java.lang.String)
     */
    @Override
    public TaskSpecification getTask(String id) {
        
        return tasks.get(id);
    }
}