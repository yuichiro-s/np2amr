package np2amr.feature;

import java.util.HashMap;
import java.util.Map;
import np2amr.Token;
import np2amr.Util;

public abstract class SuffixFeature extends TokenFeature {

    public abstract int getSuffixLength();

    @Override
    public String getName() {
        return "suffix" + getSuffixLength();
    }

    private static final Map<Integer, Map<Integer, Integer>> suffixMems = new HashMap<>();

    /**
     * Returns id of suffix of given string
     * @param id id of string to get suffix
     * @return id of suffix
     */
    public int suffix(int id) {
        int sufLen = getSuffixLength();
        if (!suffixMems.containsKey(sufLen)) {
            suffixMems.put(sufLen, new HashMap<>());
        }
        Map<Integer, Integer> suffixMem = suffixMems.get(sufLen);

        if (suffixMem.containsKey(id)) {
            return suffixMem.get(id);
        }
        String str = Util.s(id);
        int len = str.length();
        String suffix = str.substring(Math.max(0, len - sufLen), len);
        int suffixId = Util.i(suffix);
        suffixMem.put(len, suffixId);
        return suffixId;
    }

    @Override
    public int getTokenFeature(Token tok) {
        return suffix(tok.lemmaId);
    }
    
}
