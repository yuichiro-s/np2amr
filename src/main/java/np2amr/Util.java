package np2amr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import np2amr.action.ShiftAction;
import np2amr.amr.Concept;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public final class Util {

    private Util() {}

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

    /**
     * Lists possible concept fragments invoked by the word (lemma, POS)
     * @param tok token from which to identy concepts
     * @return list of pairs of rules and concept fragments
     */
    public static List<Pair<ShiftAction.Type, Concept>> identifyConcepts(Token tok) {
        List<Pair<ShiftAction.Type, Concept>> res = new ArrayList<>();
        int id = tok.lemmaId;

        Set<Integer> identifiedConceptIds = new HashSet<>();
        boolean leaveAsIs = true;   // check if LEAVE_AS_IS is redundant
        if (tok.isNoun) {
            if (Config.noun2pred.containsKey(id)) {
                Config.noun2pred.get(id).stream().forEach((pred) -> {
                    res.add(new ImmutablePair<>(ShiftAction.Type.TO_PRED, new Concept(pred.predId)));
                    identifiedConceptIds.add(pred.predId);
                });
            }
        } else if (tok.isAdj) {
            // adjective-to-predicate
            if (Config.adj2pred.containsKey(id)) {
                Config.adj2pred.get(id).stream().forEach((pred) -> {
                    res.add(new ImmutablePair<>(ShiftAction.Type.TO_PRED, new Concept(pred.predId)));
                    identifiedConceptIds.add(pred.predId);
                });
            }
            if (Config.adj2noun.containsKey(id)) {
                for (int nounId: Config.adj2noun.get(id)) {
                    if (id == nounId) {
                        leaveAsIs = false;
                    }
                    res.add(new ImmutablePair<>(ShiftAction.Type.TO_NOUN, new Concept(nounId)));
                    identifiedConceptIds.add(nounId);
                }
            }
        } else if (tok.isVerb) {
            if (Config.verb2pred.containsKey(id)) {
                Config.verb2pred.get(id).stream().forEach((pred) -> {
                    res.add(new ImmutablePair<>(ShiftAction.Type.TO_PRED, new Concept(pred.predId)));
                    identifiedConceptIds.add(pred.predId);
                });
            }
        }

        if (leaveAsIs) {
            res.add(new ImmutablePair<>(ShiftAction.Type.LEAVE_AS_IS, new Concept(id)));
            identifiedConceptIds.add(id);
        }

        // use mapping from training data
        if (Config.conceptTable.containsKey(id)) {
            Config.conceptTable.get(id).stream().forEach((c) -> {
                // make sure not to add the same concept twice as different actions
                if (c.size() >= 2 || !identifiedConceptIds.contains(c.conceptId)) {
                    res.add(new ImmutablePair<>(ShiftAction.Type.KNOWN_MAP, new Concept(c)));
                }
            });
        }

        /*
        System.err.println(tok);
        for (Pair<ShiftAction.Type, Concept> p: res) {
            System.err.format("%s: %s\n", p.getLeft(), p.getRight());
        }
        System.err.println();
                */

        assert !res.isEmpty();

        return res;
    }

    private static final Map<Integer, Boolean> isPredMem = new HashMap<>();
    /**
     * Returns whether given id refferes to Ontonotes predicate or not.
     * Determined by whether suffix is "-xx".
     * @param id
     * @return 
     */
    public static boolean isPred(int id) {
        if (isPredMem.containsKey(id)) {
            return isPredMem.get(id);
        }
        String str = Util.s(id);
        boolean res = str.contains("-") && str.length() > 3;
        isPredMem.put(id, res);
        return res;
    }

    private static final Map<Integer, Boolean> isArgLabelMem = new HashMap<>();
    /**
     * Returns whether this label is ARGn.
     * @param labelId
     * @return 
     */
    public static boolean isArgLabel(int labelId) {
        if (isArgLabelMem.containsKey(labelId)) {
            return isArgLabelMem.get(labelId);
        } else {
            boolean b = Util.s(labelId).startsWith("ARG");
            isArgLabelMem.put(labelId, b);
            return b;
        }
    }

    public static Map<Integer, Boolean> reversedLabelMem = new HashMap<>();
    /**
     * Checks whether it's a reversed label, such as "ARG0-of".
     * @param labelId
     * @return 
     */
    public static boolean isReversedLabel(int labelId) {
        if (reversedLabelMem.containsKey(labelId)) {
            return reversedLabelMem.get(labelId);
        }

        String labelStr = Util.s(labelId);
        boolean res = labelStr.endsWith("-of");
        reversedLabelMem.put(labelId, res);
        return res;
    }

    public static Map<Integer, Integer> normalizedLabelMem = new HashMap<>();
    /**
     * Reverses normalized (without -of) version of label.
     * If normalized version of the label is unknown, returns -1.
     * e.g. id("ARG0-of") -> id("ARG0")
     *      id("ARG0") -> -1
     *      id("mod") -> -1
     * @param labelId
     * @return
     */
    public static int normalizeLabel(int labelId) {
        if (normalizedLabelMem.containsKey(labelId)) {
            return normalizedLabelMem.get(labelId);
        }

        int res = -1;
        String labelStr = Util.s(labelId);
        if (labelStr.endsWith("-of")) {
            String labelWithOutOf = labelStr.substring(0, labelStr.length()-3);
            res = Util.i(labelWithOutOf);
        }

        normalizedLabelMem.put(labelId, res);
        return res;
    }
}
