package np2amr;

public interface Weights<Feature> {
    public float get(Feature f);
    public void add(Feature f, float num);
}
