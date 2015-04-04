package np2amr.feature;

import java.util.ArrayList;
import java.util.List;
import np2amr.State;
import np2amr.action.Action;

public class ConceptFeature extends FeatureTemplate {

    @Override
    public String getName() {
        return "concept";
    }

    @Override
    public List<List<Integer>> extractFeatures(State state, List<Action> actions) {
        List<Integer> feats = new ArrayList<>();

        // concept id of root
        int s0Id = state.concept != null ? state.concept.conceptId : EMPTY_FEAT;
        int s1Id = state.left != null && state.left.concept != null? state.left.concept.conceptId : EMPTY_FEAT;
        feats.add(f("s0", s0Id));
        feats.add(f("s1", s1Id));
        feats.add(f("s0_s1", s0Id, s1Id));

        return combineActionFeatures(actions, feats);
    }
    
}
