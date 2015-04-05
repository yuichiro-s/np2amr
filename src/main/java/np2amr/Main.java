package np2amr;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import np2amr.amr.Concept;
import np2amr.amr.Io;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class Main {

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

    private static void train(Path trainPath, Path destPath, Path dataPath, String wnPath, List<String> featureStrs, int iterNum, int beamWidth, int featSize) throws IOException {
        // stringIdMap must has been initialized, because Io.loadAlignment registers new word IDs on its own.
        Triple<List<List<Token>>, Map<Integer, Set<Concept>>, Set<Integer>> t = Io.loadAlignment(trainPath);

        List<List<Token>> amrs = t.getLeft();
        Map<Integer, Set<Concept>> conceptTable = t.getMiddle();
        Set<Integer> labelIds = t.getRight();

        // set config
        Config.setConfig(dataPath, conceptTable, labelIds, wnPath, featureStrs);

        // train by perceptron
        BeamDecoder decoder = new BeamDecoder(beamWidth);
        Perceptron perceptron = new Perceptron(Config.fts, decoder, featSize);
        perceptron.train(amrs, iterNum, destPath);

        Io.saveConfig(dataPath, destPath);
    }

    private static String parse(List<String> tokStrs, BeamDecoder decoder, LinearScorer scorer) {
        // create tokens
        List<Token> toks = new ArrayList<>();
        List<Integer> tokIds = new ArrayList<>();
        for (String tokStr: tokStrs) {
            tokIds.add(Util.i(tokStr));
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
        ArrayWeights weights = Io.loadWeights(modelPath.resolve(weightsName));

        BeamDecoder decoder = new BeamDecoder(beamWidth);
        LinearScorer scorer = new LinearScorer(Config.fts, weights);
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
