#ifndef H_sqrt
# define H_sqrt

# include <PLASMA/Constraint.hh>
# include <PLASMA/ConstraintEngine.hh>
# include <PLASMA/Domain.hh>

class sqrt_cstr :public EUROPA::Constraint {
public:
  sqrt_cstr(EUROPA::LabelStr const &name,
	    EUROPA::LabelStr const &propagator,
	    EUROPA::ConstraintEngineId const &cstrEngine,
	    std::vector<EUROPA::ConstrainedVariableId> const &args);
  void handleExecute();
  
private:
  EUROPA::Domain &m_root;
  EUROPA::Domain &m_square;
};



#endif
