package pt.lsts.neptus.plugins.nvl.dsl
import pt.lsts.imc.IMCMessage
import pt.lsts.nvl.runtime.NodeSet
import pt.lsts.nvl.runtime.Node
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager
import pt.lsts.neptus.plugins.nvl.*

class Instructions {

  static IMCPlanTask imcPlan(String id) {
    NeptusPlatform.INSTANCE.getPlatformTask(id)
  }
  
  static IMCPlanTask imcPlan(Closure cl) {
     // TODO use IMC DSL defined by closure
     // TODO MUST set closure delegate to appropriate object
  }
  
  static void sendMessage(NodeSet nodes, IMCMessage message) {
     for (Node n : nodes) {
       NeptusPlatform.INSTANCE.displayMessage 'Sending \'%s\' to \'%s\'', 
                                               message.getAbbrev(), 
                                               n.getId()
       ImcMsgManager.getManager().sendMessageToSystem message,
                                                      n.getId()         
     }
  }
  
  static main(args) {
    NeptusPlatform.INSTANCE.displayMessage 'Neptus language extensions loaded!'
  }
}

