package np2amr;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of feature weights using Map.
 * @param <Feature> 
 */
public class MapWeights<Feature> implements Weights<Feature> {

    Map<Feature, Float> weights;

    public MapWeights() {
        weights = new HashMap<>();
    }

    @Override
    public float get(Feature f) {
        if (weights.containsKey(f)) {
            return weights.get(f);
        } else {
            return 0;
        }
    }

    @Override
    public void add(Feature f, float num) {
        throw new UnsupportedOperationException();
    }
    
}
