#ifndef H_LatLonDist 
# define H_LatLonDist 

# include <PLASMA/Constraint.hh>
# include <PLASMA/ConstraintEngine.hh>
# include <PLASMA/Domain.hh>

class LatLonDist :public EUROPA::Constraint {
public:
  LatLonDist(EUROPA::LabelStr const &name,
             EUROPA::LabelStr const &propagator,
             EUROPA::ConstraintEngineId const &cstrEngine,
             std::vector<EUROPA::ConstrainedVariableId> const &args);
  void handleExecute();
  
private:
  EUROPA::Domain &m_dist;
  EUROPA::Domain &m_lat_from;
  EUROPA::Domain &m_lon_from;
  EUROPA::Domain &m_lat_to;
  EUROPA::Domain &m_lon_to;
};


#endif // H_LatLonDist