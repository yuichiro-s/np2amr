package np2amr.feature;

import java.util.ArrayList;
import java.util.List;
import np2amr.State;
import np2amr.Token;
import np2amr.action.Action;

public abstract class TokenFeature extends FeatureTemplate {

    public abstract int getTokenFeature(Token tok);

    @Override
    public List<List<Integer>> extractFeatures(State state, List<Action> actions) {
        List<Integer> feats = new ArrayList<>();

        Token s0 = getS0(state);
        Token s1 = getS1(state);
        Token b0 = getB0(state);
        int s0Id = s0 != null ? getTokenFeature(s0) : EMPTY_FEAT;
        int s1Id = s1 != null ? getTokenFeature(s1) : EMPTY_FEAT;
        int b0Id = b0 != null ? getTokenFeature(b0) : EMPTY_FEAT;
        feats.add(f("s0", s0Id));
        feats.add(f("s1", s1Id));
        feats.add(f("b0", b0Id));
        feats.add(f("s0_s1", s0Id, s1Id));
        feats.add(f("s0_b0", s0Id, b0Id));

        return combineActionFeatures(actions, feats);
    }
    
}
