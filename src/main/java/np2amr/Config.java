package np2amr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import np2amr.action.ShiftAction;
import np2amr.amr.Concept;
import np2amr.amr.OntoPredicate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Singleton class that holds mapping between words and IDs, and mappings between words.
 */
public class Config {

    public static StringIdMap stringIdMap = new StringIdMap();

    public static Map<Integer, List<OntoPredicate>> noun2pred = null;
    public static Map<Integer, List<Integer>> adj2noun = null;
    public static Map<Integer, List<OntoPredicate>> adj2pred = null;
    public static Map<Integer, List<OntoPredicate>> verb2pred = null;
    public static Map<Integer, Set<Concept>> conceptTable = null;

    public static Map<Integer, OntoPredicate> preds = null;

    private Config() {}

    public static Set<Integer> labelIds;
    public static Map<Integer, Integer> reverseLabelMap;    // entries are for example "ARG0-of".id -> "ARG0".id

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
    
}
