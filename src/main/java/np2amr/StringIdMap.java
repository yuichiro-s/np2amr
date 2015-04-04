package np2amr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StringIdMap {

    public final Map<String, Integer> str2id;
    public final List<String> id2str;
    public int entityCount;

    public StringIdMap(Map<String, Integer> str2id, List<String> id2str) {
        assert str2id.size() == id2str.size();
        this.str2id = str2id;
        this.id2str = id2str;
        this.entityCount = str2id.size();
    }

    public StringIdMap() {
        str2id = new HashMap<>();
        id2str = new ArrayList<>();
        this.entityCount = 0;
    }

    /**
     * Adds a string to the mapping.
     * @param str 
     */
    private void add(String str) {
        assert !str2id.containsKey(str);
        str2id.put(str, entityCount);
        id2str.add(str);
        entityCount++;
        assert id2str.size() == entityCount;
    }

    public String getString(int id) {
        return id2str.get(id);
    }

    public int getId(String str) {
        if (!str2id.containsKey(str)) {
            add(str);
        }
        return str2id.get(str);
    }
}
