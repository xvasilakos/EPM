/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package traces.dmdtrace;

import java.util.Comparator;
import java.util.List;

/**
 *
 * @author xvas
 */
class DocTuple {

    long _id;
    long _pop;
    long _size;
    long _app;

    public String toString() {
        return "<_id=" + _id
                + ", _pop=" + _pop
                + ", _size=" + _size
                + ", _app=" + _app
                + ">";
    }

    long get(int i) {
        switch (i) {
            case 0:
                return _id;
            case 1:
                return _pop;
            case 2:
                return _size;
            case 3:
                return _app;
            default:
                throw new RuntimeException("Mistake!");
        }
    }

    static final class POP_COMPARATOR implements Comparator<DocTuple> {

        public POP_COMPARATOR() {
        }

        @Override
        /**
         * Make comparison such that the order becomes descending wrt the
         * popularity of requests and - in case of same popularity - in
         * ascending order of ID
         */
        public int compare(DocTuple t1, DocTuple t2) {
            // descending order
            int compare = (int) (t2._pop - t1._pop);
            return compare != 0 ? compare : (int) (t1._id - t2._id);
        }

    }
}
