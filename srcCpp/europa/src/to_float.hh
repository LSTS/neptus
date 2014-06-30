#ifndef H_to_float
# define H_to_float

# include <PLASMA/Constraint.hh>
# include <PLASMA/ConstraintEngine.hh>
# include <PLASMA/Domain.hh>

class float_to_int :public EUROPA::Constraint {
public:
  float_to_int(EUROPA::LabelStr const &name,
	       EUROPA::LabelStr const &propagator,
	       EUROPA::ConstraintEngineId const &cstrEngine,
	       std::vector<EUROPA::ConstrainedVariableId> const &args);
  void handleExecute();
  
private:
  EUROPA::Domain &m_float;
  EUROPA::Domain &m_int;
};



#endif
