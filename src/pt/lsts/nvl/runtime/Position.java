package pt.lsts.nvl.runtime;

public interface Position {
  double latitude();
  double longitude();
  double height();
  
  boolean near(Position areaCenter, double areaRadius);
}
