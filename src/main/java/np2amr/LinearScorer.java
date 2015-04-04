package np2amr;

import java.util.ArrayList;
import java.util.List;
import np2amr.action.Action;
import np2amr.feature.FeatureTemplate;

public class LinearScorer {
    public final List<FeatureTemplate> fts;
    public final ArrayWeights weights;

    public LinearScorer(List<FeatureTemplate> fts, ArrayWeights weights) {
        this.fts = fts;
        this.weights = weights;
    }

    public List<Float> score(State state, List<Action> actions) {
        int size = actions.size();

        List<Float> scores = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            scores.add(0f);
        }

        for (FeatureTemplate ft: fts) {
            List<List<Integer>> fs = ft.extractFeatures(state, actions);
            for (int i = 0; i < size; i++) {
                List<Integer> features = fs.get(i);
                float tmp = 0f;
                for (int f: features) {
                    tmp += weights.get(f);
                }
                scores.set(i, scores.get(i) + tmp);

                //System.err.format("[%.2f]\t%s\t%s\n", scores.get(i), actions.get(i), features);
            }
            //System.err.println();
        }

        return scores;
    }
    
}
