/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package traces.dmdtrace;

import sim.run.SimulationBaseRunner;
import sim.content.AbstractContent;
import sim.content.ContentDocument;

/**
 * Used for the records loaded by the workload of requests file of the loaded
 * trace.
 */
public class TraceWorkloadRecord extends AbstractContent {

    private double _time;

    public TraceWorkloadRecord(SimulationBaseRunner sim, long size, long id, double time) {
        super(id, sim, size);
        this._time = time;
    }

    /**
     * Takes into account issue time to avoid conflicts with same ID between
     * different records referring to the same item.
     *
     * @return
     */
    @Override
    public ContentDocument referredContentDocument() {
        return getSim().getTrcDocs().get(getID());
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 29 * hash + (int) (Double.doubleToLongBits(this._time) ^ (Double.doubleToLongBits(this._time) >>> 32));
        return hash;
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        // super checks for getClass() != obj.getClass(), so it can be casted
        final TraceWorkloadRecord other = (TraceWorkloadRecord) obj;
        return Double.doubleToLongBits(this._time) == Double.doubleToLongBits(other._time);
    }

    /**
     * @return the _time of the workload request record.
     */
    public double getTime() {
        return _time;
    }

    /**
     * @param _time the _time to set
     */
    public void setTime(double _time) {
        this._time = _time;
    }

}
