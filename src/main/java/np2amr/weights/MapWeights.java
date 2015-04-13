package np2amr.weights;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of feature weights using Map. 
 */
public class MapWeights implements Weights<Integer> {

    public final Map<Integer, Float> weights;

    public MapWeights() {
        weights = new HashMap<>();
    }

    @Override
    public float get(Integer f) {
        if (weights.containsKey(f)) {
            return weights.get(f);
        } else {
            return 0;
        }
    }

    @Override
    public void add(Integer f, float num) {
        float newVal = num;
        if (weights.containsKey(f)) {
            newVal += weights.get(f);
        }
        weights.put(f, newVal);
    }

    @Override
    public void save(Path destPath, Weights<Integer> wsAvg, int t) throws IOException {
        int used = 0;

        try (FileChannel fc = new FileOutputStream(destPath.toString()).getChannel()) {
            ByteBuffer buf = ByteBuffer.allocate(1 << 28);
            buf.clear();
            for (Map.Entry<Integer, Float> e: weights.entrySet()) {
                int i = e.getKey();
                float w = e.getValue();
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
        Path loadFactorPath = destPath.resolveSibling(destPath.getFileName() + ".feat_num");
        try (BufferedWriter bw = Files.newBufferedWriter(loadFactorPath, Charset.defaultCharset())) {
            bw.write(String.format("%d\n", used));
        }
    }
    
}
