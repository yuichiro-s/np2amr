package np2amr.action;

import java.util.ArrayList;
import java.util.List;
import np2amr.Util;

public class DummyAction extends Action {

    private final List<Integer> fs;
    public DummyAction() {
        fs = new ArrayList<>();
        fs.add(Util.hash("DUMMY".hashCode()));
    }

    @Override
    public String toString() {
        return "DUMMY";
    }

    @Override
    public List<Integer> actionFeatures() {
        return fs;
    }
}
