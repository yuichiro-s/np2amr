package np2amr;

import np2amr.amr.Concept;

public class Token {

    public static final String ROOT = "<ROOT>";

    public final int surfId;
    public final int lemmaId;
    public final int posId;
    public final int depHead;    // dependency head
    public final int depRelId;    // dependency relation

    public final boolean isVerb;
    public final boolean isNoun;
    public final boolean isAdj;

    // gold information
    public final Concept goldConcept;
    public final int goldHead;
    public final int goldLabelId;
    public final int goldPosition;

    // dummy-root
    public Token() {
        int rootId = Config.stringIdMap.getId(ROOT);
        this.surfId = rootId;
        this.lemmaId = rootId;
        this.posId = rootId;
        this.depHead = -1;
        this.depRelId = -1;
        this.isVerb = false;
        this.isNoun = false;
        this.isAdj = false;
        this.goldConcept = null;
        this.goldHead = -1;
        this.goldLabelId = -1;
        this.goldPosition = -1;
    }

    public Token(int surfId, int lemmaId, int posId, int depHead, int depRelId) {
        this.surfId = surfId;
        this.lemmaId = lemmaId;
        this.posId = posId;
        this.depHead = depHead;
        this.depRelId = depRelId;

        this.goldConcept = null;
        this.goldHead = -1;
        this.goldLabelId = -1;
        this.goldPosition = -1;

        this.isVerb = Config.stringIdMap.getString(posId).startsWith("VB");
        this.isNoun = Config.stringIdMap.getString(posId).startsWith("NN");
        this.isAdj = Config.stringIdMap.getString(posId).startsWith("JJ");
    }

    public Token(int surfId, int lemmaId, int posId, int depHead, int depRelId, Concept goldConcept, int goldHead, int goldLabelId, int goldPosition) {
        this.surfId = surfId;
        this.lemmaId = lemmaId;
        this.posId = posId;
        this.depHead = depHead;
        this.depRelId = depRelId;

        this.goldConcept = goldConcept;
        this.goldHead = goldHead;
        this.goldLabelId = goldLabelId;
        this.goldPosition = goldPosition;

        this.isVerb = Config.stringIdMap.getString(posId).startsWith("VB");
        this.isNoun = Config.stringIdMap.getString(posId).startsWith("NN");
        this.isAdj = Config.stringIdMap.getString(posId).startsWith("JJ");
    }

    @Override
    public String toString() {
        return Util.s(lemmaId);
    }

}
