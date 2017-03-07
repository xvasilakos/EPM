package sim;

import sim.run.SimulationBaseRunner;
import sim.space.cell.CellRegistry;

/**
 * Must be implemented by all classes whose objects are owned by a particular simulation,
 * and thus all objects of such classes must have a reference to their simulation owner.
 *
 * @author xvas
 */
public interface ISimulationMember {

   public int simTime();
   public String simTimeStr();

   public int simID();

   public SimulationBaseRunner<?> getSim();

   public CellRegistry simCellRegistry();
   

   @Override
   public int hashCode();
}
