/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.space.cell;

import app.properties.valid.Values;
import caching.base.AbstractCachingPolicy;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.WrongOrImproperArgumentException;
import java.util.List;
import java.util.Set;
import sim.content.Chunk;
import sim.space.Point;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.mobile.MobileUser;
import utils.CommonFunctions;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class CellUtilities {

    public static boolean checkCoverage(AbstractCell cell, Point p) {
        boolean cell_knows = cell.covers(p);
        if (cell instanceof SmallCell) {
            boolean p_knows = p.isCoveredBy(cell);
            if (!p_knows && cell_knows) {
                throw new InconsistencyException("Inconsistent coverage knowledge. "
                        + "Cell " + cell.toSynopsisString() + " thinks it covers point " + p.toSynopsisString()
                        + " which believes it is not in inside  the former cell's coverage area.");
            }
            if (p_knows && !cell_knows) {
                throw new InconsistencyException("Inconsistent coverage knowledge. "
                        + "Cell " + cell.toSynopsisString() + " does not know that  it covers point  " + p.toSynopsisString()
                        + " which belives it is in the former cell's coverage area.");
            }
        } else if (cell instanceof MacroCell) {
            boolean p_knows = false;
            MacroCell coveringMacroCell = p.getCoveringMacroCell();
            p_knows = coveringMacroCell.equals(cell);
            if (p_knows != cell_knows) {
                throw new InconsistencyException("Inconsistent coverage knowledge. "
                        + "Cell believes it covers=" + cell_knows + " while point belives it is in coverage=" + p_knows);
            }
        }
        return cell_knows;
    }

    private CellUtilities() {
    }

    /**
     * Decides if a mobile user was handed over to another small cell.
     *
     * @param srcCell the source small cell of the mobile user before moving
     * @param destCell the destination small cell of the mobile user after
     * moving
     *
     * @return true if a mobile user was handed over to another small cell;
     * otherwise false
     */
    public static boolean handoverCheck(SmallCell srcCell, SmallCell destCell) {
        if (srcCell == destCell) {
            // No connectivity change; covers also the case of both src and dest being null
            return false;
        }

        if (destCell == null) {
            return false; // maybe temporarilly disconnected or first time entered simulation
        } else if (srcCell == null) {
            /*
          * if code reaches here, then destCell!=null, 
          * thus this is the first connection and not a "handoff".
             */
            return false;
        }

        /* 
       * Code reaches here => 
       * 1) both cells are surelly not null and surelly not equal to each other,
       * or 
       * 2) srcCell is null but destCell is not null (hadoffs for first time or 
       * after restting mobile or after begining of simulation)
         */
        return true;
    }

    /**
     * Computes a candidate small cell according the handoff policy list.
     *
     * @param registry
     * @param mu
     * @param connPolicySC
     * @return a candidate SC that the mu can connect to, or null if either the
     * area is uncovered or the policy results in no such a SC out of range of
     * any smallcell.
     * @throws exceptions.WrongOrImproperArgumentException
     * @throws exceptions.InvalidOrUnsupportedException
     * @throws exceptions.CriticalFailureException
     */
    public static SmallCell findHandoffcandidates(
            CellRegistry registry, MobileUser mu, List<String> connPolicySC
    ) throws WrongOrImproperArgumentException, InvalidOrUnsupportedException, CriticalFailureException {

        Point currPosition = mu.getCoordinates();
        if (currPosition == null) {
            throw new InconsistencyException("Cannot find hHandoff candidate SCs for a MU that is currently of area.");
        }

        /*for smaller cells connectivity*/
        Set<SmallCell> coveringSCs = mu.getCoordinates().getCoveringSCs();
        if (coveringSCs.isEmpty()) {
            return null;
        }

        /*
       * connection_policy_sc is a list in which the position of each combined policy denotes a priority, e.g. 
       * chose first from the cell having your item in their cach, then (if multiple choices from previous policy) chose
       * the cell which the MU is closer to.. then randomly etc...
         */
        for (String nxtPolicy : connPolicySC) {

            switch (nxtPolicy) {
                case Values.OUT_OF_RANGE: // break if it gets out range so that the next policy rule is applied
                    SmallCell upToNowConnSC = mu.getCurrentlyConnectedSC();
                    if (upToNowConnSC == null) {
                        break; // MU is anyway out of range
                    }

                    if (currPosition.isCoveredBy(upToNowConnSC)) {
                        return upToNowConnSC; // surelly did not get out of range
                    }

                    break; // MU got now surely out of range

                case Values.MAX_CACHED_EPC_STD:
                    //<editor-fold defaultstate="collapsed" desc="The cell with the most items requested by the MU in its cache">
                    List<AbstractCachingPolicy> cachingPolicies = mu.getSimulation().getCachingStrategies();
                    if (cachingPolicies.size() != 1) {
                        throw new exceptions.InvalidOrUnsupportedException(
                                "There must one and only one caching policy used when simulating a mobile's "
                                + "cell attachment decision with attachement policy " + Values.MAX_CACHED_EPC_STD
                                + "\n Yet the current simulation simulates more caching policies concurrently: "
                                + CommonFunctions.toString(cachingPolicies)
                        );
                    }

                    /*
                     *  Which SCs with active requests are included in the cell 
                     *  covering the current position?
                     */
                    int max_cachedItemsNum = 0;
                    SmallCell max_coveredSC = null;
                    for (SmallCell nxtCoveredCell : coveringSCs) {

                        Set<Chunk> itemsCached
                                = nxtCoveredCell.bufferCached(cachingPolicies.get(0), mu);
                        if (itemsCached.size() > max_cachedItemsNum) {
                            max_cachedItemsNum = itemsCached.size();
                            max_coveredSC = nxtCoveredCell;
                        }

                    }

                    if (max_coveredSC != null) {
                        return max_coveredSC;
                    }
                    // if not returned so far, then no such cell exists
                    break;
//</editor-fold>
                case Values.CLOSEST_IN_RANGE:
                    //<editor-fold defaultstate="collapsed" desc="Choose a cell after its range proximity">
                    SmallCell closestSC
                            = mu.getCoordinates().getClosestCoveringSCs(false, false).peek();//registry.closestCoveringSCs(mu);

                    if (closestSC != null) {
                        return closestSC;
                    }
                    break;
                //</editor-fold>
                case Values.RANDOM_IN_RANGE:
                    //<editor-fold defaultstate="collapsed" desc="random cell choice">
                    SmallCell rndCell = registry.coveringRandomSmallcell(mu);
                    if (rndCell != null) {
                        return rndCell;
                    }
                    break;
//</editor-fold>
                default:
                    throw new UnsupportedOperationException("Unknown policy: " + connPolicySC);
            }//switch
        }//for
        return null;
        /* if not returned so far, then no eligible cell is found
       * for any of the combined rules*/

    }
}
