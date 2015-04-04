package np2amr.action;

import java.util.ArrayList;
import java.util.List;
import np2amr.Util;

public class EmptyAction extends Action {

    private final List<Integer> fs;
    public EmptyAction() {
        fs = new ArrayList<>();
        fs.add(Util.hash("EMPTY".hashCode()));
    }

    @Override
    public String toString() {
        return "EMPTY";
    }

    @Override
    public List<Integer> actionFeatures() {
        return fs;
    }
}
