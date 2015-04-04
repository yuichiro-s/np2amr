package np2amr.feature;

import java.util.ArrayList;
import java.util.List;
import np2amr.State;
import np2amr.Token;
import np2amr.action.Action;

public class BetweenTokensFeature extends FeatureTemplate {

    @Override
    public String getName() {
        return "between";
    }

    public static List<Token> getTokensBetween(List<Token> toks, int i1, int i2) {
        int len = toks.size();
        List<Token> res = new ArrayList<>();
        if (i1 < 0 || len <= i1 || i2 < 0 || len <= i2) {
            return res;
        } else {
            for (int i = i1+1; i < i2; i++) {
                res.add(tok(toks, i));
            }
            return res;
        }
    }

    @Override
    public List<List<Integer>> extractFeatures(State state, List<Action> actions) {
        List<Integer> feats = new ArrayList<>();

        int i1 = state.left == null ? -1 : state.left.top;  // s1
        int i2 = state.top;                                 // s2

        for (Token tok: getTokensBetween(state.toks, i1, i2)) {
            feats.add(f("s1_s0_w", tok.lemmaId));
            feats.add(f("s1_s0_t", tok.posId));
        }

        return combineActionFeatures(actions, feats);
    }

}
