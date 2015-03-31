package np2amr.amr;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

public class ConceptFragment {

    // these attributes are null, when fragment is empty
    public final Set<Concept> concepts; // concepts belonging to the fragment
    public final Concept root;

    /**
     * Creates an empty fragment.
     */
    public ConceptFragment() {
        this.concepts = null;
        this.root = null;
    }

    public ConceptFragment(ConceptFragment orig) {
        throw new UnsupportedOperationException();
    }

    public ConceptFragment(Set<Concept> concepts) {
        this.concepts = concepts;

        assert concepts != null;
        Set<Concept> noParent = new HashSet<>(concepts);
        for (Concept c: concepts) {
            for (Pair<Integer, Concept> p: c.children) {
                noParent.remove(p.getRight());
            }
        }
        assert noParent.size() == 1;
        root = (Concept)noParent.toArray()[0];
    }

    public boolean isEmptyFragment() {
        return concepts == null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConceptFragment) {
            ConceptFragment cf = (ConceptFragment)obj;
            return Objects.equals(cf.root, this.root);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.root);
        return hash;
    }
    
}
