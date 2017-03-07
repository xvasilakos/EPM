package sim.space.users;

import java.util.HashSet;
import java.util.Set;
import sim.ISimulationMember;
import sim.run.SimulationBaseRunner;
import sim.space.cell.CellRegistry;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class UserGroup implements Comparable<UserGroup>, ISimulationMember {

    protected final SimulationBaseRunner _simulation;
    protected final int id;
    protected final int size;
    protected final String initPos;
    protected final Set<User> users;

    public UserGroup(
            SimulationBaseRunner sim, int idNum, int musNum, String posAtStart
    ) {
        this._simulation = sim;
        this.id = idNum;
        this.size = musNum;
        this.initPos = posAtStart;
        this.users = new HashSet<>();
    }

    @Override
    public int compareTo(UserGroup t) {
        return this.getId() - t.getId();
    }

    public int getSize() {
        return size;
    }

    /**
     * @return the id
     */
    public final int getId() {
        return id;
    }

    /**
     * @return the initPos such as south west, north etc..
     */
    public String getInitPos() {
        return initPos;
    }

    public void add(User u) {
        users.add(u);
    }

    @Override
    public final int simTime() {
        return getSim().simTime();
    }

    @Override
    public String simTimeStr() {
        return "[" + simTime() + "]";
    }

    @Override
    public final int simID() {
        return getSim().getID();
    }

    @Override
    public final SimulationBaseRunner getSim() {
        return _simulation;
    }

    @Override
    public final CellRegistry simCellRegistry() {
        return getSim().getCellRegistry();
    }

}
