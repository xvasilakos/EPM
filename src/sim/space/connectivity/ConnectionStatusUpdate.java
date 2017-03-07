/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.space.connectivity;

/**
 *
 * @author xvas
 */
public enum ConnectionStatusUpdate {
    REMAINS_DISCONNECTED_WAS_NEVER_CONNECTED,
    REMAINS_DISCONNECTED_WAS_AT_SOME_POINT_CONNECTED,
    GOT_DISCONNECTED {
        @Override
        public boolean hasNowExistedPreviousSC() {
            return true;
        }
    },
    ////////////////////////////////////////////////////////
    ///// the states that the mobile is now connected: /////
    ////////////////////////////////////////////////////////
    REMAINS_CONNECTED_TO_SAME_SC {
        @Override
        public boolean isConnected() {
            return true;
        }
    },
    GOT_RECONNECTED_TO_SC_AFTER_TMP_DISCON // was temporarily only disconnected
    {
        @Override
        public boolean isConnected() {
            return true;
        }
    },
    CONNECTED_FIRST_TIME_TO_SC {
        @Override
        public boolean isConnected() {
            return true;
        }

    },
    HANDOVER_DIRECTLY {
        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public boolean isHandedOver() {
            return true;
        }

        @Override
        public boolean hasNowExistedPreviousSC() {
            return true;
        }
    },
    HANDOVER_AFTER_DISCONNECTION_PERIOD {
        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public boolean isHandedOver() {
            return true;
        }
    };

    public boolean isConnected() {
        return false;
    }

    public boolean isHandedOver() {
        return false;
    }

    public boolean hasNowExistedPreviousSC() {
        return false;
    }
}
