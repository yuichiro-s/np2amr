package np2amr;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.EnglishGrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class CoreNlpWrapper {

    private static final StanfordCoreNLP parser;
    private static final Map<List, Collection> mem;
    private static final Map<List, List> posMem;
    private static final Map<List, List> lemMem;

    private static final EnglishGrammaticalStructureFactory egsf = new EnglishGrammaticalStructureFactory();

    static {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,parse");
        parser = new StanfordCoreNLP(props);
        mem = new HashMap<>();
        posMem = new HashMap<>();
        lemMem = new HashMap<>();
    }

    public static String joinTokens(List<Integer> toks) {
        // join the tokens so CoreNLP can process
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < toks.size(); i++) {
            if (i != 0) {
                sb.append(" ");
            }
            int tokId = toks.get(i);
            String tokStr = Config.stringIdMap.getString(tokId);
            sb.append(tokStr);
        }
        return sb.toString();
    }

    public static List<Integer> pos(List<Integer> toks) {
        if (posMem.containsKey(toks)) {
            return posMem.get(toks);
        } else {
            parse(toks);  // parse() memoizes poss
            assert posMem.containsKey(toks);
            return posMem.get(toks);
        }
    }

    public static List<Integer> lemma(List<Integer> toks) {
        if (lemMem.containsKey(toks)) {
            return lemMem.get(toks);
        } else {
            parse(toks);  // parse() memoizes lemmas
            assert lemMem.containsKey(toks);
            return lemMem.get(toks);
        }
    }

    public static Collection<TypedDependency> parse(List<Integer> toks) {
        if (mem.containsKey(toks)) {
            return mem.get(toks);
        } else {
            Annotation a = parser.process(joinTokens(toks));

            CoreMap sen = a.get(CoreAnnotations.SentencesAnnotation.class).get(0);
            List<Integer> poss = new ArrayList<>();
            List<Integer> lems = new ArrayList<>();
            for (CoreLabel tok: sen.get(TokensAnnotation.class)) {
                String pos = tok.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String lem = tok.get(CoreAnnotations.LemmaAnnotation.class);
                String coarsePos = pos.substring(0, Math.min(pos.length(), 2));
                int posId = Config.stringIdMap.getId(coarsePos);
                int lemmaId = Config.stringIdMap.getId(lem);
                poss.add(posId);
                lems.add(lemmaId);
            }
            posMem.put(toks, poss);
            lemMem.put(toks, lems);

            Tree tree = sen.get(TreeCoreAnnotations.TreeAnnotation.class);
            Collection<TypedDependency> deps = egsf.newGrammaticalStructure(tree).typedDependencies();

            mem.put(toks, deps);
            return deps;
        }
    }

    public static Pair<Map<Integer, Integer>, Map<Integer, Integer>> dep(List<Integer> tokIds) {
        Collection<TypedDependency> deps = CoreNlpWrapper.parse(tokIds);
        
        Map<Integer, Integer> depIdxs = new HashMap<>();
        Map<Integer, Integer> relIds = new HashMap<>();
        // get dependency
        for (TypedDependency dep: deps) {
            int tail = dep.dep().index() - 1;
            int head = dep.gov().index() - 1;
            String rel = dep.reln().getShortName();
            depIdxs.put(tail, head);
            relIds.put(tail, Config.stringIdMap.getId(rel));
        }

        return new ImmutablePair<>(depIdxs, relIds);
    }
    
}
