package pt.lsts.neptus.plugins.nvl.dsl
import pt.lsts.neptus.plugins.nvl.*

class Instructions {

  static IMCPlanTask imcPlan(String id) {
    NeptusPlatform.INSTANCE.getPlatformTask(id)
  }
  
  static IMCPlanTask imcPlan(Closure cl) {
     // TODO use IMC DSL defined by closure
     // TODO MUST set closure delegate to appropriate object
  }
  
  
  static main(args) {
    NeptusPlatform.INSTANCE.displayMessage 'Neptus language extensions loaded!'
  }
}

