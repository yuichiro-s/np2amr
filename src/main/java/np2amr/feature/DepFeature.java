package np2amr.feature;

import java.util.ArrayList;
import java.util.List;
import np2amr.State;
import np2amr.Token;
import np2amr.action.Action;

public class DepFeature extends FeatureTemplate {

    @Override
    public String getName() {
        return "dep";
    }

    @Override
    public List<List<Integer>> extractFeatures(State state, List<Action> actions) {
        List<Integer> feats = new ArrayList<>();

        Token s0 = getS0(state);
        Token s1 = getS1(state);
        Token b0 = getB0(state);

        // dependency relation
        int s0Rel = s0 != null ? s0.depRelId : EMPTY_FEAT;
        int s1Rel = s1 != null ? s1.depRelId : EMPTY_FEAT;
        int b0Rel = b0 != null ? b0.depRelId : EMPTY_FEAT;
        feats.add(f("s0_rel", s0Rel));
        feats.add(f("s1_rel", s1Rel));
        feats.add(f("b0_rel", b0Rel));

        // offset to dependency head
        int s0Offset = s0 != null ? (s0.depHead < 0 ? -1 : s0.depHead - state.top) : EMPTY_FEAT;
        int s1Offset = s1 != null ? (s1.depHead < 0 ? -1 : s1.depHead - state.left.top) : EMPTY_FEAT;
        int b0Offset = b0 != null ? (b0.depHead < 0 ? -1 : b0.depHead - state.right) : EMPTY_FEAT;
        feats.add(f("s0_off", s0Offset));
        feats.add(f("s1_off", s1Offset));
        feats.add(f("b0_off", b0Offset));

        return combineActionFeatures(actions, feats);
    }
    
}
