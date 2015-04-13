package np2amr;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import np2amr.amr.Concept;
import np2amr.amr.Io;
import np2amr.amr.OntoPredicate;
import np2amr.feature.BetweenTokensFeature;
import np2amr.feature.ConceptFeature;
import np2amr.feature.DepFeature;
import np2amr.feature.FeatureTemplate;
import np2amr.feature.LemmaFeature;
import np2amr.feature.PosFeature;
import np2amr.feature.Suffix2Feature;
import np2amr.feature.Suffix3Feature;
import np2amr.feature.WordnetFeature;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Singleton class that holds mapping between words and IDs, and mappings between words.
 */
public class Config {

    public static final String N2V_NAME = "noun2verb";
    public static final String A2V_NAME = "adj2verb";
    public static final String A2N_NAME = "adj2noun";
    public static final String ONTO_NAME = "ontonotes_predicates";
    public static final String FTS_NAME = "fts";
    public static final String WN_NAME = "wordnet";
    public static final String WEIGHTS_TYPE_NAME = "weights_type";
    public static final String STRING2ID_NAME = "string_id_map";
    public static final String LABELS_NAME = "labels";
    public static final String CONCEPT_TABLE_NAME = "concept_table";

    public static final String ROOT_LABEL = "root";

    private Config() {}

    static final List<FeatureTemplate> allFts;
    static final Map<String, FeatureTemplate> name2Ft;
    static {
        allFts = new ArrayList<>();
        allFts.add(new LemmaFeature());
        allFts.add(new PosFeature());
        allFts.add(new DepFeature());
        allFts.add(new BetweenTokensFeature());
        allFts.add(new ConceptFeature());
        allFts.add(new Suffix2Feature());
        allFts.add(new Suffix3Feature());
        allFts.add(new WordnetFeature());

        name2Ft = new HashMap<>();
        for (FeatureTemplate ft: allFts) {
            name2Ft.put(ft.getName(), ft);
        }
    }

    /**
     * Mapping between String and their ids.
     */
    public static StringIdMap stringIdMap = new StringIdMap();

    /**
     * Mapping from adjectives to their etymological nouns.
     */
    public static Map<Integer, List<Integer>> adj2noun = null;

    /**
     * Mapping from words to related Ontonotes predicates.
     */
    public static Map<Integer, List<OntoPredicate>> noun2pred = null;
    public static Map<Integer, List<OntoPredicate>> adj2pred = null;
    public static Map<Integer, List<OntoPredicate>> verb2pred = null;

    /**
     * Mapping from predicate names such as "walk-01" to Ontonotes predicates.
     */
    public static Map<Integer, OntoPredicate> preds = null;

    /**
     * All concept fragements that appeared in the training data.
     */
    public static Map<Integer, Set<Concept>> conceptTable = null;

    /**
     * All labels that appeared in the training data.
     */
    public static Set<Integer> labelIds;

    /**
     * Wordnet data.
     */
    public static IDictionary wndict;
    public static String wndictPath;

    public static List<FeatureTemplate> fts;

    public enum WeightsType {
        ARRAY, MAP;
    }
    public static WeightsType weightsType = null;

    public static void setConfig(Path dataPath, Map<Integer, Set<Concept>> conceptTable, Set<Integer> labelIds, String wndictPath, List<String> featureStrs, WeightsType weightsType) throws IOException {

        Path noun2VerbPath = dataPath.resolve(N2V_NAME);
        Path adj2VerbPath = dataPath.resolve(A2V_NAME);
        Path adj2NounPath = dataPath.resolve(A2N_NAME);
        Path verb2PredPath = dataPath.resolve(ONTO_NAME);

        // load mappings
        Config.adj2noun = Io.loadWordMapping(adj2NounPath);
        Pair<Map<Integer, List<OntoPredicate>>, Map<Integer, OntoPredicate>> p = Io.loadOntoPredicates(verb2PredPath);
        Config.verb2pred = p.getLeft();
        Config.preds = p.getRight();
        
        // construct noun-to-predicate mapping
        Map<Integer, List<Integer>> noun2verb = Io.loadWordMapping(noun2VerbPath);
        Config.noun2pred = new HashMap<>();
        noun2verb.entrySet().stream().forEach((e) -> {
            int nounId = e.getKey();
            List<Integer> verbIds = e.getValue();
            List<OntoPredicate> preds = new ArrayList<>();
            verbIds.stream().filter((verbId) -> (Config.verb2pred.containsKey(verbId))).forEach((verbId) -> {
                preds.addAll(Config.verb2pred.get(verbId));
            });
            Config.noun2pred.put(nounId, preds);
        });
        
        // construct adjective-to-predicate mapping
        Map<Integer, List<Integer>> adj2verb = Io.loadWordMapping(adj2VerbPath);
        Config.adj2pred = new HashMap<>();
        adj2verb.entrySet().stream().forEach((e) -> {
            int adjId = e.getKey();
            List<Integer> verbIds = e.getValue();
            List<OntoPredicate> ps = new ArrayList<>();
            verbIds.stream().filter((verbId) -> (Config.verb2pred.containsKey(verbId))).forEach((verbId) -> {
                ps.addAll(Config.verb2pred.get(verbId));
            });
            Config.adj2pred.put(adjId, ps);
        });

        Config.conceptTable = conceptTable;
        Config.labelIds = labelIds;

        // set Wordnet
        Config.wndict = new Dictionary(new File(wndictPath));
        Config.wndict.open();
        Config.wndictPath = wndictPath;

        // set features
        Config.fts = new ArrayList<>();
        for (String f: featureStrs) {
            FeatureTemplate ft = name2Ft.get(f);
            if (ft == null) {
                throw new RuntimeException("Unknown feature: " + f);
            }
            Config.fts.add(ft);
        }

        Config.weightsType = weightsType;
    }
}
