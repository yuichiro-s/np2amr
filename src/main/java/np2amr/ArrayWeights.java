package np2amr;

/**
 * Implementation of feature weights using an array.
 */
public class ArrayWeights implements Weights<Integer> {

    public final float[] weights;
    public final int size;

    public ArrayWeights(int size) {
        this.size = size;
        this.weights = new float[size];
    }

    public int getIdx(int f) {
        return Math.abs(f) % size;
    }

    @Override
    public float get(Integer f) {
        return weights[getIdx(f)];
    }

    @Override
    public void add(Integer f, float num) {
        weights[getIdx(f)] += num;
    }
    
}
