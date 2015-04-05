package np2amr.feature;

import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import np2amr.Config;
import np2amr.State;
import np2amr.Token;
import np2amr.Util;
import np2amr.action.Action;
import np2amr.amr.Concept;

public class WordnetFeature extends FeatureTemplate {

    @Override
    public String getName() {
        return "wn";
    }

    private static Set<ISynset> allHypernyms(ISynset synset) {
        HashSet<ISynset> acc = new HashSet<>();
        allHypernymsRec(synset, acc);
        return acc;
    }

    private static void allHypernymsRec(ISynset synset, HashSet<ISynset> acc) {
        acc.add(synset);
        for (ISynsetID hypernymID: synset.getRelatedSynsets(Pointer.HYPERNYM)) {
            ISynset hypernym = Config.wndict.getSynset(hypernymID);
            if (!acc.contains(hypernym)) {
                // avoid inifinite-loop
                allHypernymsRec(hypernym, acc);
            }
        }
    }

    private static ISynset getSynset(String word, POS pos) {
        IIndexWord idxWord = Config.wndict.getIndexWord(word, pos);
        if (idxWord != null) {
            List<IWordID> wordIDs = idxWord.getWordIDs();
            if (wordIDs.size() > 0) {
                return Config.wndict.getWord(wordIDs.get(0)).getSynset();
            }
        }
        return null;
    }

    private static final Map<Integer, List<Integer>> allHypernymIdsNounMem = new HashMap<>();
    private static final Map<Integer, List<Integer>> allHypernymIdsVerbMem = new HashMap<>();

    public List<Integer> getAllHypernymIdsNoun(int id) {
        return getAllHypernymIds(id, POS.NOUN, allHypernymIdsNounMem);
    }

    public List<Integer> getAllHypernymIdsVerb(int id) {
        return getAllHypernymIds(id, POS.VERB, allHypernymIdsVerbMem);
    }

    public List<Integer> getAllHypernymIds(int id, POS pos, Map<Integer, List<Integer>> mem) {
        if (mem.containsKey(id)) {
            return mem.get(id);
        }
        List<Integer> res = new ArrayList<>();

        String str = Util.s(id);
        ISynset synset = getSynset(str, pos);
        if (synset != null) {
            for (ISynset h: allHypernyms(synset)) {
                String lemma = h.getWords().get(0).getLemma();
                res.add(Util.i(lemma));
            }
        }

        mem.put(id, res);
        return res;
    }

    public void addHypernymFeatures(List<Integer> feats, Token tok, String name) {
        if (tok == null) {
            feats.add(f(name, EMPTY_FEAT)); 
        } else {
            String lemmaStr = Util.s(tok.lemmaId);
            ISynset synset;
            if (tok.isNoun) {
                synset = getSynset(lemmaStr, POS.NOUN);
            } else {
                synset = getSynset(lemmaStr, POS.VERB);
            }
            if (synset != null) {
                for (ISynset h: allHypernyms(synset)) {
                    String str = h.getWords().get(0).getLemma();
                    feats.add(f(name, Util.i(str)));
                }
            }
        }
    }

    private void addHypernymFeatures(List<Integer> feats, int lemmaId, boolean isVerb) {
        if (isVerb) {
            feats.addAll(getAllHypernymIdsVerb(lemmaId));
        } else {
            feats.addAll(getAllHypernymIdsNoun(lemmaId));
        }
    }


    public static final Map<Integer, Boolean> isPredMem = new HashMap<>();
    /**
     * Returns whether given concept is verb or not.
     * Determined by whether suffix is "-xx".
     * @param c
     * @return 
     */
    private static boolean isPred(Concept c) {
        int id = c.conceptId;
        if (isPredMem.containsKey(id)) {
            return isPredMem.get(id);
        }
        String str = Util.s(id);
        boolean res = str.contains("-") && str.length() > 3;
        isPredMem.put(id, res);
        return res;
    }

    public static final Map<Integer, Integer> verbIdMem = new HashMap<>();
    /**
     * Removes concept number at the end.
     * e.g. id("walk-01") -> id("walk")
     * @param c
     * @return
     */
    private static int getVerbId(int predId) {
        if (verbIdMem.containsKey(predId)) {
            return verbIdMem.get(predId);
        }
        String str = Util.s(predId);
        String newStr = str.substring(0, str.length()-3);
        int res = Util.i(newStr);
        verbIdMem.put(predId, res);
        return res;
    }

    @Override
    public List<List<Integer>> extractFeatures(State state, List<Action> actions) {
        List<Integer> feats = new ArrayList<>();

        // identified root concept at cs0, cs1
        Concept cs0 = state.concept;
        Concept cs1 = state.left == null ? null : state.left.concept;

        if (cs0 == null) {
            feats.add(f("cs0", EMPTY_FEAT));
        } else {
            if (isPred(cs0)) {
                addHypernymFeatures(feats, getVerbId(cs0.conceptId), true);
            } else {
                addHypernymFeatures(feats, cs0.conceptId, false);
            }
        }

        if (cs1 == null) {
            feats.add(f("cs1", EMPTY_FEAT));
        } else {
            if (isPred(cs1)) {
                addHypernymFeatures(feats, getVerbId(cs1.conceptId), true);
            } else {
                addHypernymFeatures(feats, cs1.conceptId, false);
            }
        }

        // token at b0
        /*
        Token tb0 = tok(state.toks, state.right);

        if (tb0 == null) {
            feats.add(f("tb0", EMPTY_FEAT));
        } else {
            addHypernymFeatures(feats, tb0.lemmaId, tb0.isVerb || tb0.isAdj);
        }
                */

        return combineActionFeatures(actions, feats);
    }
}
