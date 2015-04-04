package np2amr.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import np2amr.State;
import np2amr.Token;
import np2amr.Util;
import np2amr.action.Action;

public abstract class FeatureTemplate {
    /**
     * Extract features for each action.
     * @param state
     * @param actions list of actions to extract features
     * @return list of feature sets for each action (in the same order as actions)
     */
    public abstract List<List<Integer>> extractFeatures(State state, List<Action> actions);

    public List<Integer> extractFeatures(State state, Action action) {
        return extractFeatures(state, Arrays.asList(action)).get(0);
    }

    public abstract String getName();

    /**
     * Returns all combinations of action features and given features
     * @param actions list of actions to combine
     * @param fs features to combine with action features
     * @return list of combined features
     */
    public static List<List<Integer>> combineActionFeatures(List<Action> actions, List<Integer> fs) {
        List<List<Integer>> res = new ArrayList<>();
        for (Action action: actions) {
            List<Integer> cfs = new ArrayList<>();
            for (int af: action.actionFeatures()) {
                for (int f: fs) {
                    cfs.add(Util.hash(f, af));
                }
            }
            res.add(cfs);
        }
        return res;
    }

    /**
     * Returns a feature (template name, name, val1, val2, ...)
     * @param name
     * @param vals
     * @return hashed feature
     */
    public int f(String name, int... vals) {
        List<Integer> res = new ArrayList<>();
        res.add(Util.i(getName()));
        res.add(Util.i(name));
        for (int val: vals) {
            res.add(val);
        }
        return Util.hash(res);
    }

    public static final int EMPTY_FEAT = "<EMPTY>".hashCode();

    public static Token tok(List<Token> toks, int i) {
        if (0 <= i && i < toks.size()) {
            return toks.get(i);
        } else {
            return null;
        }
    }

    public static Token getS0(State state) {
        return tok(state.toks, state.top);
    }

    public static Token getS1(State state) {
        return state.left == null ? null : tok(state.toks, state.left.top);
    }

    public static Token getB0(State state) {
        return tok(state.toks, state.right);
    }
}
