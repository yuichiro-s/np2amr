package np2amr.amr;

import edu.stanford.nlp.util.IdentityHashSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import np2amr.ArrayWeights;
import np2amr.Config;
import np2amr.CoreNlpWrapper;
import np2amr.Main;
import np2amr.StringIdMap;
import np2amr.Token;
import np2amr.Util;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class Io {

    public static List<String> splitLine(String line) {
        String elt = null;
        boolean inQuote = false;
        List<String> res = new ArrayList<>();
        for (String e: line.split("\\s+")) {
            if (e.startsWith("\"")) {
                inQuote = true;
            }

            if (elt == null) {
                elt = e;
            } else {
                elt = elt + " " + e;
            }

            if (e.endsWith("\"")) {
                inQuote = false;
            }

            if (!inQuote) {
                res.add(elt);
                elt = null;
            }
        }
        assert elt == null;
        return res;
    }

    public static List<List<String>> loadTokens(Path path) throws IOException {
        List<List<String>> tokss = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path, Charset.defaultCharset())) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("# ::snt")) {
                    String[] es = line.split(" ");
                    List<String> toks = new ArrayList<>();
                    for (int i = 2; i < es.length; i++) {
                        toks.add(es[i]);
                    }
                    tokss.add(toks);
                }
            }
        }

        return tokss;
    }

    public static Triple<List<List<Token>>, Map<Integer, Set<Concept>>, Set<Integer>> loadAlignment(Path path) throws IOException {
        List<List<Token>> res = new ArrayList<>();

        List<String> tokStrs = null;
        Map<String, Concept> nodeStr2Concept = new HashMap<>();
        Map<String, IdentityHashSet<Concept>> span2Concepts = new HashMap<>();
        Concept root = null;
        // Note: use IdentityHashMap, so that identity of concept holds before and after attachment to another concept
        IdentityHashMap<Concept, Pair<Integer, Concept>> edges = new IdentityHashMap<>();   // concept -> (label id, head concept)
        IdentityHashMap<Concept, Integer> concept2Idx = new IdentityHashMap<>();

        Map<Integer, Set<Concept>> conceptTable = new HashMap<>();
        Set<Integer> labelIds = new HashSet<>();

        // add root label id
        int rootLabelId = Config.stringIdMap.getId("root");
        labelIds.add(rootLabelId);

        try (BufferedReader br = Files.newBufferedReader(path, Charset.defaultCharset())) {
            String line;
            boolean ignore = false;
            while ((line = br.readLine()) != null) {
                try {
                    if (line.startsWith("# THERE WAS AN EXCEPTION IN THE PARSER.")) {
                        // exception in JAMR parser
                        ignore = true;
                        continue;
                    }

                    if (line.startsWith("#")) {
                        // only process lines starting with "#"
                        if (!ignore) {
                            List<String> es = splitLine(line);
                            if (es.size() < 2) { throw new RuntimeException(); }
                            switch (es.get(1)) {
                                case "::tok": {
                                    tokStrs = es.subList(2, es.size());
                                    break;
                                }
                                case "::alignments": {
                                    break;
                                }
                                case "::node": {
                                    if (es.size() != 5) {
                                        throw new RuntimeException("Number of columns is not 3: " + line);
                                    }
                                    String nodeStr = es.get(2);
                                    String name = es.get(3);
                                    Concept c = new Concept(Config.stringIdMap.getId(name));
                                    nodeStr2Concept.put(nodeStr, c);

                                    String spanStr = es.get(4);
                                    int spanFrom = Integer.valueOf(spanStr.split("-")[0]);
                                    concept2Idx.put(c, spanFrom);
                                    IdentityHashSet<Concept> concepts = span2Concepts.get(spanStr);
                                    if (concepts == null) {
                                        concepts = new IdentityHashSet<>();
                                        span2Concepts.put(spanStr, concepts);
                                    }
                                    concepts.add(c);
                                    break;
                                }
                                case "::root": {
                                    String nodeStr = es.get(2);
                                    root = nodeStr2Concept.get(nodeStr);
                                    break;
                                }
                                case "::edge": {
                                    String nodeStr1 = es.get(5);    // parent
                                    String nodeStr2 = es.get(6);    // child
                                    String labelStr = es.get(3);
                                    int labelId  = Config.stringIdMap.getId(labelStr);
                                    labelIds.add(labelId);

                                    Concept c1 = nodeStr2Concept.get(nodeStr1); // head
                                    Concept c2 = nodeStr2Concept.get(nodeStr2);

                                    assert c1.conceptId == Config.stringIdMap.getId(es.get(2));
                                    assert c2.conceptId == Config.stringIdMap.getId(es.get(4));

                                    edges.put(c2, new ImmutablePair<>(labelId, c1));
                                    break;
                                }
                                default: {
                                    break;
                                }
                            }
                        }
                    } else if (tokStrs != null) {
                        // at the end of block
                        // create an AMR
                        if (!ignore)  {
                            Map<Integer, Integer> goldHeads = new HashMap<>();
                            Map<Integer, Integer> goldLabelIds = new HashMap<>();
                            Map<Integer, Integer> goldPositions = new HashMap<>();

                            // connect edges inside fragments
                            for (Map.Entry<String, IdentityHashSet<Concept>> e: span2Concepts.entrySet()) {
                                Set<Concept> cs = e.getValue();
                                for (Map.Entry<Concept, Pair<Integer, Concept>> e2: edges.entrySet()) {
                                    Concept c = e2.getKey();
                                    Pair<Integer, Concept> p = e2.getValue();
                                    int labelId = p.getLeft();
                                    Concept cHead = p.getRight();
                                    if (cs.contains(cHead) && cs.contains(c)) {
                                        cHead.addChild(c, labelId);
                                    }
                                }
                            }

                            // connect edges between two fragments
                            for (Map.Entry<Concept, Pair<Integer, Concept>> e2: edges.entrySet()) {
                                Concept c = e2.getKey();
                                Pair<Integer, Concept> p = e2.getValue();
                                int labelId = p.getLeft();
                                Concept cHead = p.getRight();
                                boolean ok = true;  // false if cHead c are from the same fragment
                                for (Map.Entry<String, IdentityHashSet<Concept>> e: span2Concepts.entrySet()) {
                                    IdentityHashSet<Concept> cs = e.getValue();
                                    if (cs.contains(cHead) && cs.contains(c)) {
                                        ok = false;
                                        break;
                                    }
                                }
                                if (ok) {
                                    Concept cRoot = cHead;
                                    while (cRoot.parent != null) {
                                        cRoot = cRoot.parent;
                                    }
                                    int position = cRoot.getPosition(cHead);
                                    assert position != -1;

                                    int head = concept2Idx.get(cHead);
                                    int tail = concept2Idx.get(c);
                                    goldHeads.put(tail, head);
                                    goldLabelIds.put(tail, labelId);
                                    goldPositions.put(tail, position);
                                }
                            }

                            List<Token> toks = new ArrayList<>();
                            List<Integer> tokIds = new ArrayList<>();
                            for (String tokStr: tokStrs) {
                                tokIds.add(Config.stringIdMap.getId(tokStr));
                            }
                            List<Integer> lemmaIds = CoreNlpWrapper.lemma(tokIds);
                            List<Integer> posIds = CoreNlpWrapper.pos(tokIds);
                            Pair<Map<Integer, Integer>, Map<Integer, Integer>> p = CoreNlpWrapper.dep(tokIds);
                            Map<Integer, Integer> depIdxs = p.getLeft();
                            Map<Integer, Integer> relIds = p.getRight();

                            Map<Integer, Concept> goldConcepts = new HashMap<>();
                            for (Map.Entry<String, IdentityHashSet<Concept>> e: span2Concepts.entrySet()) {
                                String spanStr = e.getKey();
                                String[] es3 = spanStr.split("-");

                                int spanFrom = Integer.valueOf(es3[0]);
                                Set<Concept> cs = e.getValue();
                                Concept rootConcept = null;
                                for (Concept c: cs) {
                                    if (c.parent == null) {
                                        if (rootConcept != null) {
                                            throw new RuntimeException(String.format("Multiple roots: %s & %s",
                                                    Config.stringIdMap.getString(rootConcept.conceptId),
                                                    Config.stringIdMap.getString(c.conceptId)));
                                        }
                                        rootConcept = c;
                                    }
                                }
                                assert rootConcept != null;
                                goldConcepts.put(spanFrom, rootConcept);
                            }

                            // register concept table
                            for (Map.Entry<String, IdentityHashSet<Concept>> e: span2Concepts.entrySet()) {
                                for (Concept c: e.getValue()) {
                                    if (c.parent == null) {
                                        // root
                                        int idx = concept2Idx.get(c);
                                        int lemmaId = lemmaIds.get(idx);
                                        Set<Concept> cs = conceptTable.get(lemmaId);
                                        if (cs == null) {
                                            cs = new HashSet<>();
                                            conceptTable.put(lemmaId, cs);
                                        }
                                        cs.add(c);
                                    }
                                }
                            }

                            toks.add(new Token());  // dummy rootConcept
                            for (int i = 0; i < tokIds.size(); i++) {
                                int surfId = tokIds.get(i);
                                int lemmaId = lemmaIds.get(i);
                                int posId = posIds.get(i);
                                int depHead = depIdxs.containsKey(i) ? depIdxs.get(i) + 1 : -1;
                                int depRelId = relIds.containsKey(i) ? relIds.get(i) : -1;
                                Concept goldConcept = goldConcepts.get(i);
                                Token tok;
                                if (goldConcept == null) {
                                    tok = new Token(surfId, lemmaId, posId, depHead, depRelId);
                                } else {
                                    // default values for root concept
                                    int goldHead = 0;
                                    int goldLabelId = rootLabelId;
                                    int goldPosition = 0;
                                    if (goldHeads.containsKey(i)) {
                                        goldHead = goldHeads.get(i) + 1;
                                        goldLabelId = goldLabelIds.get(i);
                                        goldPosition = goldPositions.get(i);
                                    }
                                    tok = new Token(surfId, lemmaId, posId, depHead, depRelId, goldConcept, goldHead, goldLabelId, goldPosition);
                                }
                                toks.add(tok);
                            }

                            res.add(toks);
                        }
                        
                        // init
                        ignore = false;
                        tokStrs = null;
                        nodeStr2Concept = new HashMap<>();
                        span2Concepts = new HashMap<>();
                        root = null;
                        edges = new IdentityHashMap<>();
                        concept2Idx = new IdentityHashMap<>();
                    }
                } catch (RuntimeException ex) {
                    System.err.println(ex);
                    System.err.println("Failed to read: " + (tokStrs != null ? tokStrs.toString() : ""));
                    ignore = true;
                }
            }
        }

        return new ImmutableTriple<>(res, conceptTable, labelIds);
    }

    public static List<String> loadLines(Path path) throws IOException {
        List<String> res = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path, Charset.defaultCharset())) {
            String line;
            while ((line = br.readLine()) != null) {
                res.add(line);
            }
        }
        return res;
    }

    public static <T> void writeLines(Path path, Iterable<T> list) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.defaultCharset())) {
            for (T obj: list) {
                bw.write(obj.toString());
                bw.write("\n");
            }
        }
    }

    /**
     * Load a file that describes mapping of words to other words.
     * Each line must be tab-separated and LHS of the line is original word and RHS target word.
     * @param path path to the file
     * @return mapping from original word indices to target word indices
     * @throws IOException 
     */
    public static Map<Integer, List<Integer>> loadWordMapping(Path path) throws IOException {
        Map<Integer, List<Integer>> res = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(path, Charset.defaultCharset())) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] es = line.split("\t");
                assert es.length == 2;
                String orig = es[0];
                String trg = es[1];
                int origId = Config.stringIdMap.getId(orig);
                int trgId = Config.stringIdMap.getId(trg);
                if (res.containsKey(origId)) {
                    res.get(origId).add(trgId);
                } else {
                    List<Integer> trgIds = new ArrayList<>();
                    trgIds.add(trgId);
                    res.put(origId, trgIds);
                }
            }
        }

        return res;
    }

    /**
     * Load a file that describes mapping from verbs to Ontonotes predicates and their possible arguments.
     * Each line must be tab-separated, first column is verb, second column is predicate, last column is comma-separated argument numbers.
     * @param path path to the file
     * @return (mapping from verbs to Ontonotes Predicates, mapping from predicate ids to OntonotePredicates)
     * @throws IOException 
     */
    public static Pair<Map<Integer, List<OntoPredicate>>, Map<Integer, OntoPredicate>> loadOntoPredicates(Path path) throws IOException {
        Map<Integer, List<OntoPredicate>> res = new HashMap<>();
        Map<Integer, OntoPredicate> predMap = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(path, Charset.defaultCharset())) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] es = line.split("\t");
                if (es.length != 3) {
                    continue;
                }
                String verb = es[0];
                String name = es[1];
                List<Integer> args = new ArrayList<>();
                for (String n: es[2].split(",")) {
                    args.add(Integer.valueOf(n));
                }
                int predId = Config.stringIdMap.getId(name);
                OntoPredicate pred = new OntoPredicate(predId, args);
                predMap.put(predId, pred);

                int verbId = Config.stringIdMap.getId(verb);
                if (res.containsKey(verbId)) {
                    res.get(verbId).add(pred);
                } else {
                    List<OntoPredicate> preds = new ArrayList<>();
                    preds.add(pred);
                    res.put(verbId, preds);
                }
            }
        }

        return new ImmutablePair<>(res, predMap);
    }

    public static void addReverseRelations() {
        Config.reverseLabelMap = new HashMap<>();
        for (int labelId: new ArrayList<>(Config.labelIds)) {
            String labelStr = Util.s(labelId);
            if (labelStr.endsWith("-of")) {
                String labelWithOutOf = labelStr.substring(0, labelStr.length()-3);
                int labelWithOutOfId = Util.i(labelWithOutOf);
                Config.labelIds.add(labelWithOutOfId);

                Config.reverseLabelMap.put(labelId, labelWithOutOfId);
            }
        }
    }

    public static void loadConfig(Path path) throws IOException {
        Path stringIdMapPath = path.resolve(Main.STRING2ID_PATH);
        Path labelsPath = path.resolve(Main.LABELS_PATH);
        Path conceptTablePath = path.resolve(Main.CONCEPT_TABLE_PATH);

        // create stringIdMap
        Map<String, Integer> str2id = new HashMap<>();
        List<String> id2str = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(stringIdMapPath, Charset.defaultCharset())) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] es = line.split("\t");
                int id = Integer.valueOf(es[0]);
                String str = es[1];
                str2id.put(str, id);
                id2str.add(str);
            }
        }
        Config.stringIdMap = new StringIdMap(str2id, id2str);

        // load labels
        Config.labelIds = new HashSet<>();
        try (BufferedReader br = Files.newBufferedReader(labelsPath, Charset.defaultCharset())) {
            String line;
            while ((line = br.readLine()) != null) {
                Config.labelIds.add(Config.stringIdMap.getId(line));
            }
        }
        addReverseRelations();

        // load concept mapping
        Map<Integer, Set<Concept>> conceptTable = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(conceptTablePath, Charset.defaultCharset())) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] es = line.split("\t", 2);
                int id = Config.stringIdMap.getId(es[0]);
                Set<Concept> cs;
                if (conceptTable.containsKey(id)) {
                    cs = conceptTable.get(id);
                } else {
                    cs = new HashSet<>();
                    conceptTable.put(id, cs);
                }
                Concept c = Concept.fromSexp(es[1]);
                cs.add(c);
            }
        }
        Config.conceptTable = conceptTable;
    }

    public static void saveConfig(Path path) throws IOException {
        Path stringIdMapPath = path.resolve(Main.STRING2ID_PATH);
        Path labelsPath = path.resolve(Main.LABELS_PATH);
        Path conceptTablePath = path.resolve(Main.CONCEPT_TABLE_PATH);

        // save stringIdMap
        List<String> id2str = Config.stringIdMap.id2str;
        try (BufferedWriter bw = Files.newBufferedWriter(stringIdMapPath, Charset.defaultCharset())) {
            int id = 0;
            for (String str: id2str) {
                bw.write(Integer.toString(id));
                bw.write("\t");
                bw.write(str);
                bw.write("\n");
                id++;
            }
        }

        // write labels
        try (BufferedWriter bw = Files.newBufferedWriter(labelsPath, Charset.defaultCharset())) {
            for (int labelId: Config.labelIds) {
                bw.write(Config.stringIdMap.getString(labelId));
                bw.write("\n");
            }
        }

        // write concept mapping
        try (BufferedWriter bw = Files.newBufferedWriter(conceptTablePath, Charset.defaultCharset())) {
            for (Map.Entry<Integer, Set<Concept>> e: Config.conceptTable.entrySet()) {
                int id = e.getKey();
                Set<Concept> cs = e.getValue();
                for (Concept c: cs) {
                    bw.write(Config.stringIdMap.getString(id));
                    bw.write("\t");
                    bw.write(c.toString());
                    bw.write("\n");
                }
            }
        }
    }
    public static ArrayWeights loadWeights(Path path) throws IOException {
        try (FileChannel fc = new FileInputStream(path.toString()).getChannel()) {
            ByteBuffer buf = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            int size = buf.getInt();
            ArrayWeights ws = new ArrayWeights(size);
            while (buf.hasRemaining()) {
                int i = buf.getInt();
                float v = buf.getFloat();
                ws.add(i, v);
            }
            return ws;
        }
    }

    public static void writeWeights(Path modelPath, ArrayWeights ws, ArrayWeights wsAvg, int t) throws IOException {
        int size = wsAvg.size;
        int used = 0;

        try (FileChannel fc = new FileOutputStream(modelPath.toString()).getChannel()) {
            ByteBuffer buf = ByteBuffer.allocate(1 << 28);
            buf.clear();
            buf.putInt(size);
            for (int i = 0; i < size; i++) {
                float w = ws.weights[i];
                float wAvg = wsAvg.weights[i];
                float value = w - wAvg / t;
                if (value != 0f) {
                    buf.putInt(i);  // write index
                    buf.putFloat(value);  // write value
                    used++;
                }
                if (buf.remaining() < 2) {
                    // write buffer
                    buf.flip();
                    fc.write(buf);
                    buf.clear();
                }
            }
            buf.flip();
            fc.write(buf);
        }

        // report load factor
        Path loadFactorPath = modelPath.resolveSibling(modelPath.getFileName() + ".load_factor");
        try (BufferedWriter bw = Files.newBufferedWriter(loadFactorPath, Charset.defaultCharset())) {
            bw.write(String.format("%.3f%% [%d/%d]\n", ((double)used*100)/size, used, size));
        }
    }
}
