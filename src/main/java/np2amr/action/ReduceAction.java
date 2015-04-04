package np2amr.action;

import java.util.ArrayList;
import java.util.List;
import np2amr.Config;
import np2amr.Util;

public class ReduceAction extends Action {

    public enum Dir {
        LEFT(0), RIGHT(1);
        public final int value;
        private Dir(int value) { this.value = value; }
    }

    public final Dir dir;
    public final int labelId;
    public final int position;  // position to attatch (pre-order traversal of tree)

    private final List<Integer> fs;

    public ReduceAction(Dir dir, int labelId, int position) {
        this.dir = dir;
        this.labelId = labelId;
        this.position = position;

        fs = new ArrayList<>();
        fs.add(Util.hash("REDUCE".hashCode(), dir.value));
        fs.add(Util.hash("REDUCE".hashCode(), dir.value, labelId));
        fs.add(Util.hash("REDUCE".hashCode(), dir.value, labelId, position));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("REDUCE_");
        sb.append(dir);
        sb.append("_");
        sb.append(Config.stringIdMap.getString(labelId));
        sb.append("_");
        sb.append(position);
        return sb.toString();
    }

    @Override
    public List<Integer> actionFeatures() {
        return fs;
    }


}
