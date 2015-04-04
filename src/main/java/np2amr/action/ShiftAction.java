package np2amr.action;

import java.util.ArrayList;
import java.util.List;
import np2amr.Util;
import np2amr.amr.Concept;

public class ShiftAction extends Action {

    // rules for invoking concept fragment
    public static enum Type {
        TO_PRED(0),   // look up mapping to predicates
        TO_NOUN(1),   // look up mapping to nouns

        // general rules
        KNOWN_MAP(2),      // mappings that are extracted from training data and cannot be explained by above rules
        LEAVE_AS_IS(3),    // map to singleton fragment with same name
        ;

        public final int value;
        private Type(int value) { this.value = value; }
    }

    public final Type type;
    public final Concept concept;
    private final List<Integer> fs;

    public ShiftAction(Type type, Concept concept) {
        this.type = type;
        this.concept = concept;
        fs = new ArrayList<>();
        fs.add(Util.hash("SHIFT".hashCode()));
        fs.add(Util.hash("SHIFT".hashCode(), type.value));
        fs.add(Util.hash("SHIFT".hashCode(), type.value, concept.hashCode()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SHIFT_");
        sb.append(type.toString());
        sb.append("_");
        sb.append(concept.toString());
        return sb.toString();
    }

    @Override
    public List<Integer> actionFeatures() {
        return fs;
    }

}
