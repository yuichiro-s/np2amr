package np2amr.weights;

import java.io.IOException;
import java.nio.file.Path;

public interface Weights<Feature> {
    public float get(Feature f);
    public void add(Feature f, float num);

    public void save(Path destPath, Weights<Feature> wsAvg, int t) throws IOException;
}
