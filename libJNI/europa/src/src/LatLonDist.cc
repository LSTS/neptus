#include "LatLonDist.hh"

#include <DUNE/Math/Angles.hpp>
#include <DUNE/Coordinates/WGS84.hpp>

using EUROPA::LabelStr;
using EUROPA::Constraint;

using DUNE::Math::Angles;
using DUNE::Coordinates::WGS84;


LatLonDist::LatLonDist(LabelStr const &name,
                       LabelStr const &propagator,
                       EUROPA::ConstraintEngineId const &cstrEngine,
                       std::vector<EUROPA::ConstrainedVariableId> const &args)
:Constraint(name, propagator,cstrEngine, args),
 m_dist(getCurrentDomain(m_variables[0])),
 m_lat_from(getCurrentDomain(m_variables[1])),
 m_lon_from(getCurrentDomain(m_variables[2])),
 m_lat_to(getCurrentDomain(m_variables[3])),
 m_lon_to(getCurrentDomain(m_variables[4])) {}

void LatLonDist::handleExecute() {
  // A distance is always positive :
  m_dist.intersect(0.0, std::numeric_limits<EUROPA::edouble>::infinity());
  if( m_dist.isEmpty() )
    return;
  
  // Compute distance only for fully set positions for now
  if( m_lat_from.isSingleton() && m_lon_from.isSingleton() &&
     m_lat_to.isSingleton() && m_lon_to.isSingleton() ) {
    double lat1, lon1, lat2, lon2, dist;
    
    lat1 = Angles::radians(cast_basis(m_lat_from.getSingletonValue()));
    lon1 = Angles::radians(cast_basis(m_lon_from.getSingletonValue()));
    lat2 = Angles::radians(cast_basis(m_lat_to.getSingletonValue()));
    lon2 = Angles::radians(cast_basis(m_lon_to.getSingletonValue()));
    dist = WGS84::distance(lat1, lon1, 0, lat2, lon2, 0);
    m_dist.intersect(dist, dist);
  }
}
