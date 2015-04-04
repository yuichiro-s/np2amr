package np2amr.amr;

import java.util.List;

/**
 * Ontonotes predicate with possible argument numbers.
 */
public class OntoPredicate {

    public final int predId;
    public final List<Integer> args;

    public OntoPredicate(int predId, List<Integer> args) {
        this.predId = predId;
        this.args = args;
    }
    
}
