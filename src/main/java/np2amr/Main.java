package np2amr;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class Main {

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

    public static final String N2V_PATH = "noun2verb";
    public static final String A2V_PATH = "adj2verb";
    public static final String A2N_PATH = "adj2noun";
    public static final String ONTO_PATH = "ontonotes_predicates";
    public static final String FTS_PATH = "fts";
    public static final String WN_PATH = "wordnet";
    public static final String STRING2ID_PATH = "string_id_map";
    public static final String LABELS_PATH = "labels";
    public static final String CONCEPT_TABLE_PATH = "concept_table";

    private static void loadMappingFiles(Path dirPath) throws IOException {
        Path noun2VerbPath = dirPath.resolve(N2V_PATH);
        Path adj2VerbPath = dirPath.resolve(A2V_PATH);
        Path adj2NounPath = dirPath.resolve(A2N_PATH);
        Path verb2PredPath = dirPath.resolve(ONTO_PATH);

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
            List<OntoPredicate> preds = new ArrayList<>();
            verbIds.stream().filter((verbId) -> (Config.verb2pred.containsKey(verbId))).forEach((verbId) -> {
                preds.addAll(Config.verb2pred.get(verbId));
            });
            Config.adj2pred.put(adjId, preds);
        });
    }

    public static void main(String[] args) throws IOException {
        Logger.getGlobal().setLevel(Level.ALL);

        Options opts = new Options();

        opts.addOption(null, "train", true, "training mode");
        opts.addOption(null, "data", true, "directory where mapping files reside");
        opts.addOption(null, "model", true, "location of model directory");
        Option featureOpt = new Option(null, "feature", true, "features to use");
        featureOpt.setArgs(Option.UNLIMITED_VALUES);
        opts.addOption(featureOpt);
        opts.addOption(null, "featSize", true, "feature size");
        opts.addOption(null, "iter", true, "number of iterations");
        opts.addOption(null, "wn", true, "path to Wordnet dictionary directory");

        opts.addOption(null, "test", true, "test mode");
        opts.addOption(null, "interactive", false, "interactive test mode");
        opts.addOption(null, "ws", true, "name of weights file");

        opts.addOption(null, "beam", true, "beam width");

        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(opts, args);

            Path modelPath = Paths.get(cmd.getOptionValue("model"));
            int beamWidth = Integer.valueOf(cmd.getOptionValue("beam"));

            if (cmd.hasOption("train")) {
                // training mode
                Path trainPath = Paths.get(cmd.getOptionValue("train"));
                Path dataPath = Paths.get(cmd.getOptionValue("data"));
                String[] featureNames = cmd.getOptionValues("feature");
                int iterNum = Integer.valueOf(cmd.getOptionValue("iter"));
                int featSize = 1 << (Integer.valueOf(cmd.getOptionValue("featSize")));
                String wnPath = cmd.getOptionValue("wn");
                train(trainPath, modelPath, dataPath, wnPath, Arrays.asList(featureNames), iterNum, beamWidth, featSize);
            } else {
                // test mode
                String weightsName = cmd.getOptionValue("ws");
                if (cmd.hasOption("interactive")) {
                    test(null, modelPath, weightsName, beamWidth);
                } else if (cmd.hasOption("test")) {
                    Path testPath = Paths.get(cmd.getOptionValue("test"));
                    test(testPath, modelPath, weightsName, beamWidth);
                }
            }

        } catch (ParseException ex) {
            Logger.getGlobal().log(Level.SEVERE, "Couldn't parse the arguments.", ex);
        }
        
    }

    private static void copyDataFile(Path srcDirPath, Path trgDirPath, String name) throws IOException {
        Files.copy(srcDirPath.resolve(name), trgDirPath.resolve(name), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void train(Path trainPath, Path destPath, Path dataPath, String wnPath, List<String> features, int iterNum, int beamWidth, int featSize) throws IOException {
        loadMappingFiles(dataPath);
        Triple<List<List<Token>>, Map<Integer, Set<Concept>>, Set<Integer>> t = Io.loadAlignment(trainPath);

        List<List<Token>> amrs = t.getLeft();
        Config.conceptTable = t.getMiddle();
        Config.labelIds = t.getRight();
        Io.addReverseRelations();

        Config.loadWnDict(wnPath);

        List<FeatureTemplate> fts = new ArrayList<>();
        for (String f: features) {
            FeatureTemplate ft = name2Ft.get(f);
            if (ft == null) {
                throw new RuntimeException("Unknown feature: " + f);
            }
            fts.add(ft);
        }

        // train by perceptron
        BeamDecoder decoder = new BeamDecoder(beamWidth);
        Perceptron perceptron = new Perceptron(fts, decoder, featSize);
        perceptron.train(amrs, iterNum, destPath);

        Io.saveConfig(destPath);

        // copy mapping files
        copyDataFile(dataPath, destPath, N2V_PATH);
        copyDataFile(dataPath, destPath, A2V_PATH);
        copyDataFile(dataPath, destPath, A2N_PATH);
        copyDataFile(dataPath, destPath, ONTO_PATH);

        // save features
        try (BufferedWriter bw = Files.newBufferedWriter(destPath.resolve(FTS_PATH), Charset.defaultCharset())) {
            for (FeatureTemplate ft: fts) {
                bw.write(ft.getName());   // first line is size of feature vector
                bw.write("\n");
            }
        }

    }

    private static String parse(List<String> tokStrs, BeamDecoder decoder, LinearScorer scorer) {
        // create tokens
        List<Token> toks = new ArrayList<>();
        List<Integer> tokIds = new ArrayList<>();
        for (String tokStr: tokStrs) {
            tokIds.add(Config.stringIdMap.getId(tokStr));
        }

        if (tokIds.isEmpty() || CoreNlpWrapper.joinTokens(tokIds).length() == 0) {
            return "(a / amr-empty)";
        } else {
            List<Integer> lemmaIds = CoreNlpWrapper.lemma(tokIds);
            List<Integer> posIds = CoreNlpWrapper.pos(tokIds);
            Pair<Map<Integer, Integer>, Map<Integer, Integer>> p = CoreNlpWrapper.dep(tokIds);
            Map<Integer, Integer> depIdxs = p.getLeft();
            Map<Integer, Integer> relIds = p.getRight();
            toks.add(new Token());  // dummy rootConcept
            for (int i = 0; i < tokIds.size(); i++) {
                int surfId = tokIds.get(i);
                int lemmaId = lemmaIds.get(i);
                int posId = posIds.get(i);
                int depHead = depIdxs.containsKey(i) ? depIdxs.get(i) + 1 : -1;
                int depRelId = relIds.containsKey(i) ? relIds.get(i) : -1;
                Token tok = new Token(surfId, lemmaId, posId, depHead, depRelId);
                toks.add(tok);
            }
            State finalState = decoder.decode(new State(toks), scorer);
            return finalState.toAmr().toSexp();
        }
    }

    /**
     * Parses input sentences into AMRs and prints them. If testPath is null, it parses user's input interactively.
     * @param testPath test files
     * @param modelPath
     * @param weightsName
     * @param beamWidth
     * @throws IOException 
     */
    private static void test(Path testPath, Path modelPath, String weightsName, int beamWidth) throws IOException {
        // each step depends on the previous step, don't scramble the order of execution
        Io.loadConfig(modelPath);
        loadMappingFiles(modelPath);
        ArrayWeights weights = Io.loadWeights(modelPath.resolve(weightsName));

        // load features
        List<FeatureTemplate> fts = new ArrayList<>();
        for (String name: Io.loadLines(modelPath.resolve(FTS_PATH))) {
            fts.add(name2Ft.get(name));
        }

        BeamDecoder decoder = new BeamDecoder(beamWidth);
        LinearScorer scorer = new LinearScorer(fts, weights);
        if (testPath == null) {
            // interactive mode
            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.print("> ");
                String line = sc.nextLine();
                List<String> toks = Arrays.asList(line.split(" "));
                String amrStr = parse(toks, decoder, scorer);
                System.out.println(amrStr);
            }
        } else {
            for (List<String> tokStrs: Io.loadTokens(testPath)) {
                // print tokens
                StringBuilder sb = new StringBuilder();
                sb.append("# ::snt");
                for (String tok: tokStrs) {
                    sb.append(" ");
                    sb.append(tok);
                }
                System.out.println(sb.toString());

                String amrStr = parse(tokStrs, decoder, scorer);
                System.out.println(amrStr);
                System.out.println();
            }
        }
    }

}
