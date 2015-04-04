package np2amr.feature;

import np2amr.Token;

public class PosFeature extends TokenFeature {

    @Override
    public String getName() {
        return "pos";
    }

    @Override
    public int getTokenFeature(Token tok) {
        return tok.posId;
    }

}
