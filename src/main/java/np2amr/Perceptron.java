package np2amr;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import np2amr.action.Action;
import np2amr.amr.Io;
import np2amr.feature.FeatureTemplate;

public class Perceptron {

    public final List<FeatureTemplate> fts;
    public final ArrayWeights ws;
    public final ArrayWeights wsAvg;
    private int t;

    public final LinearScorer scorer;
    public BeamDecoder decoder;

    public Perceptron(List<FeatureTemplate> fts, BeamDecoder decoder, int featSize) {
        this.fts = fts;
        this.decoder = decoder;
        this.ws = new ArrayWeights(featSize);
        this.wsAvg = new ArrayWeights(featSize);
        this.scorer = new LinearScorer(fts, this.ws);
        this.t = 1;
    }

    public void train(Iterable<List<Token>> amrs, int iterNum, Path dest) throws IOException {
        if (dest != null) {
            Files.createDirectories(dest);
            Path logPath = dest.resolve("log");
            FileHandler fh;
            fh = new FileHandler(logPath.toString());
            fh.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return new Date(record.getMillis()) + ":\t" + record.getMessage() + "\n";
                }
            });
            Logger.getGlobal().addHandler(fh);
        }

        // report features used
        for (FeatureTemplate ft: fts) {
            Logger.getGlobal().log(Level.INFO, "Using feature: " + ft.getName());
        }

        for (int iterCount = 0; iterCount < iterNum; iterCount++) {
            Logger.getGlobal().log(Level.INFO, "Iter #" + (iterCount+1));
            for (List<Token> amr: amrs) {
                Logger.getGlobal().log(Level.INFO, "Processing: " + amr);
                updateSample(amr);
            }

            // end of iteration
            if (dest != null) {
                Path modelPath = dest.resolve("iter" + (iterCount+1));
                Io.writeWeights(modelPath, ws, wsAvg, t);
            }
        }
    }

    public static List<State> stateSequence(State finalState) {
        List<State> res = new ArrayList<>();
        State state = finalState;
        while (state.prev != null) {
            res.add(state);
            state = state.prev;
        }
        Collections.reverse(res);
        return res;
    }

    private void updateSample(List<Token> goldAmr) throws IOException {
        State goldState = new State(goldAmr);
        while (!goldState.isFinal()) {
            Action a = goldState.goldAction();
            if (a == null) {
                // unable to produce gold
                Logger.getGlobal().log(Level.INFO, "Couldn't produce oracle.");
                return;
            }
            List<Action> actions = Arrays.asList(a);
            float actionScore = scorer.score(goldState, actions).get(0);
            float newScore = goldState.score + actionScore;
            goldState = goldState.next(a, newScore);
        }
        State predState = decoder.decode(new State(goldAmr), scorer);

        // get sequences of states
        List<State> predStates = stateSequence(predState);
        List<State> goldStates = stateSequence(goldState);

        // find max-violation
        float maxv = Float.NEGATIVE_INFINITY;
        int maxk = 0;
        for (int k = 1; k < goldStates.size(); k++) {
            float v = predStates.get(k).score - goldStates.get(k).score;
            if (v >= maxv) {
                maxv = v;
                maxk = k;
            }
        }

        // update until max-violation point
        for (int k = maxk; k >= 0; k--) {
            State ps = predStates.get(k);
            State gs = goldStates.get(k);
            update(gs, ps);
        }
        t++;
    }

    /**
     * Update with previous actions of gold and pred.
     * @param gold
     * @param pred 
     */
    private void update(State gold, State pred) {
        State goldPrev = gold.prev;
        Action goldPrevAct = gold.prevAct;
        State predPrev = pred.prev;
        Action predPrevAct = pred.prevAct;

        for (FeatureTemplate ft: fts) {
            List<Integer> goldFs = ft.extractFeatures(goldPrev, goldPrevAct);
            List<Integer> predFs = ft.extractFeatures(predPrev, predPrevAct);

            //System.err.printf("pred: %s [%s] %s\n", predPrevAct, predPrev.score, predFs);
            //System.err.printf("gold: %s [%s] %s\n\n", goldPrevAct, goldPrev.score, goldFs);

            // add 1 to weights of gold features
            for (int f: goldFs) {
                ws.add(f, 1);       // add 1
                wsAvg.add(f, t);    // add t
            }

            // subtract 1 from weights of pred features
            for (int f: predFs) {
                ws.add(f, -1);       // subtract 1
                wsAvg.add(f, -t);    // subtract t
            }
        }

        //System.err.printf("pred: %s [%s]\n", predPrevAct, predPrev.score);
        //System.err.printf("gold: %s [%s]\n\n", goldPrevAct, goldPrev.score);
    }
    
}
