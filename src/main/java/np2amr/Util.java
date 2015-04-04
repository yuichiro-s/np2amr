package np2amr;

import java.util.Arrays;
import java.util.List;

public class Util {

    /**
     * Concatenates list of atomic features and returns an integer representation.
     * Jenkins one-at-a-time hash.
     * @param arr array of features to concatenate
     * @return integer representation of feature
     */
    public static int hash(List<Integer> arr) {
        int res = 0;
        for (int n: arr) {
            res += n;
            res += (res << 10);
            res ^= (res >> 6);
        }
        res += (res << 3);
        res ^= (res >> 11);
        res += (res << 15);
        
        return res;
    }

    public static int hash(Integer... ns) {
        return hash(Arrays.asList(ns));
    }

    public static String s(int id) {
        return Config.stringIdMap.getString(id);
    }

    public static int i(String str) {
        return Config.stringIdMap.getId(str);
    }
    
}
