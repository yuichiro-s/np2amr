package np2amr.feature;

import np2amr.Token;

public class LemmaFeature extends TokenFeature {

    @Override
    public String getName() {
        return "lemma";
    }

    @Override
    public int getTokenFeature(Token tok) {
        return tok.lemmaId;
    }

}
