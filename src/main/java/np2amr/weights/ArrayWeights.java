package np2amr.weights;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Override
    public void save(Path destPath, Weights<Integer> wsAvg, int t) throws IOException {
        int used = 0;

        try (FileChannel fc = new FileOutputStream(destPath.toString()).getChannel()) {
            ByteBuffer buf = ByteBuffer.allocate(1 << 28);
            buf.clear();
            buf.putInt(size);
            for (int i = 0; i < size; i++) {
                float w = weights[i];
                float wAvg = wsAvg.get(i);
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
        Path loadFactorPath = destPath.resolveSibling(destPath.getFileName() + ".load_factor");
        try (BufferedWriter bw = Files.newBufferedWriter(loadFactorPath, Charset.defaultCharset())) {
            bw.write(String.format("%.3f%% [%d/%d]\n", ((double)used*100)/size, used, size));
        }
    }
    
}
