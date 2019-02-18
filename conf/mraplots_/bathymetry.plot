Bathymetry

Depth: ${EstimatedState.depth}
Altitude: ${EstimatedState.alt}
Bathymetry: ${EstimatedState.alt} + ${EstimatedState.depth}

init:$deepest = -100; $dtime = 0;
: bathym = ${EstimatedState.depth} + ${EstimatedState.alt}; if (bathym > $deepest) {$deepest = bathym; $dtime = $time;}
end: mark($dtime,"Deepest point");