package np2amr;

import java.util.ArrayList;
import java.util.List;
import np2amr.action.Action;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

public class BeamDecoder {

    public final int beamSize;

    public BeamDecoder(int beamSize) {
        this.beamSize = beamSize;
    }
    
    public State decode(State initState, LinearScorer scorer) {
        List<State> kBest = new ArrayList<>();
        kBest.add(initState);
        while (!kBest.get(0).isFinal()) {
            List<Triple<State, Action, Float>> cands = new ArrayList<>();
            for (State s: kBest) {
                List<Action> actions = s.validActions();
                List<Float> scores = scorer.score(s, actions);
                for (int i = 0; i < scores.size(); i++) {
                    Action a = actions.get(i);
                    float newScore = s.score + scores.get(i);
                    //System.err.format("%s\t%s\n", a, newScore);
                    cands.add(new ImmutableTriple<>(s, a, newScore));
                }
            }

            // sort candidates in decreasing order
            cands.sort((t1, t2) -> Double.compare(t2.getRight(), t1.getRight()));

            int count = Math.min(beamSize, cands.size());

            for (Triple<State, Action, Float> c: cands) {
                State s = c.getLeft();
                Action a = c.getMiddle();
                float score = c.getRight();
                //System.err.format("%s\t%s\t%s\n", s.concept, a, score);
            }
            //System.err.println();
            
            kBest = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Triple<State, Action, Float> t = cands.get(i);
                State s = t.getLeft();
                Action a = t.getMiddle();
                float newScore = t.getRight();
                State nextState = s.next(a, newScore);
                kBest.add(nextState);
            }
        }
            
        return kBest.get(0);
    }
}
