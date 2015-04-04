package np2amr.action;

import java.util.List;

public abstract class Action {
    @Override
    public abstract String toString();

    public abstract List<Integer> actionFeatures();
}
