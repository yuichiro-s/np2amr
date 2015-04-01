package np2amr;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CoreNlpWrapper {

    private static final StanfordCoreNLP parser;
    private static final Map<List, Annotation> mem;

    private static final StanfordCoreNLP posTagger;
    private static final Map<List, List> posMem;
    private static final Map<List, List> lemMem;

    static {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,parse");
        parser = new StanfordCoreNLP(props);
        mem = new HashMap<>();

        Properties posProps = new Properties();
        posProps.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        posTagger = new StanfordCoreNLP(posProps);
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


    public static Annotation parse(List<Integer> toks) {
        if (mem.containsKey(toks)) {
            return mem.get(toks);
        } else {
            Annotation a = parser.process(joinTokens(toks));
            mem.put(toks, a);
            return a;
        }
    }

    public static List<String> pos(List<Integer> toks) {
        if (posMem.containsKey(toks)) {
            return posMem.get(toks);
        } else {
            Annotation a = posTagger.process(joinTokens(toks));
            CoreMap sen = a.get(CoreAnnotations.SentencesAnnotation.class).get(0);
            List<String> poss = new ArrayList<>();
            List<String> lems = new ArrayList<>();
            for (CoreLabel tok: sen.get(TokensAnnotation.class)) {
                String pos = tok.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String lem = tok.get(CoreAnnotations.LemmaAnnotation.class);
                String coarsePos = pos.substring(0, Math.min(pos.length(), 2));
                poss.add(coarsePos);
                lems.add(lem);
            }
            posMem.put(toks, poss);
            lemMem.put(toks, lems);
            return poss;
        }
    }

    public static List<String> lemma(List<Integer> toks) {
        if (lemMem.containsKey(toks)) {
            return lemMem.get(toks);
        } else {
            pos(toks);  // pos() memoizes lemmas
            assert lemMem.containsKey(toks);
            return lemMem.get(toks);
        }
    }
    
}
