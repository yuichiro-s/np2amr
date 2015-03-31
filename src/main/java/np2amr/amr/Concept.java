package np2amr.amr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import np2amr.Config;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class Concept {

    public final static String INDENT = "    ";
    
    public final int conceptId;
    public List<Pair<Integer, Concept>> children;   // list of (label id, concept)
    public Concept parent;

    public Concept(int conceptId) {
        this.conceptId = conceptId;
        this.children = new ArrayList<>();
        this.parent = null;
    }

    public Concept(int conceptId, List<Pair<Integer, Concept>> children, Concept parent) {
        this.conceptId = conceptId;
        this.children = children;
        this.parent = parent;
    }

    public void addChild(Concept c, int labelId) {
        this.children.add(new ImmutablePair<>(labelId, c));
        c.parent = this;
    }

    // keep children only in concept fragment
    public void keepChildrenInsideFragment(ConceptFragment cf) {
        List<Pair<Integer, Concept>> newChildren = new ArrayList<>();
        for (Pair<Integer, Concept> p: children) {
            Concept child = p.getRight();
            if (cf.concepts.contains(child)) {
                newChildren.add(p);
            } else {
                child.parent = null;
            }
        }
        children = newChildren;
    }

    // if child exists in children, return the label to the child
    // if not, return -1
    public int childLabel(Concept child) {
        for (Pair<Integer, Concept> c: children) {
            if (c.getRight() == child) {
                return c.getLeft();
            }
        }
        return -1;
    }
    
    public String toSexp(int indent, Map<String, Integer> vars, Config config) {
        String name = config.conceptIdMap.getString(conceptId);

        StringBuilder sb = new StringBuilder();
        if (!name.startsWith("\"")) {
            String var = name.substring(0, 1);
            sb.append("(");
            sb.append(var);
            int cnt = 1;
            if (vars.containsKey(var)) {
                cnt = vars.get(var) + 1;
                sb.append(cnt);
            }
            vars.put(var, cnt);
            sb.append(" / ");
            sb.append(name);
            for (Pair<Integer, Concept> p: children) {
                int relId = p.getLeft();
                String rel = config.labelIdMap.getString(relId);
                Concept child = p.getRight();
                sb.append("\n");
                for (int i = 0; i < indent+1; i++) {
                    sb.append(INDENT);
                }
                
                sb.append(":");
                sb.append(rel);
                sb.append(" ");
                sb.append(child.toSexp(indent+1, vars, config));
            }
            sb.append(")");
        } else {
            sb.append(name);
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Concept) {
            Concept c2 = (Concept)obj;
            if (conceptId == c2.conceptId && children.size() == c2.children.size()) {
                for (int i = 0; i < children.size(); i++) {
                    Pair<Integer, Concept> p = children.get(i);
                    Pair<Integer, Concept> p2 = c2.children.get(i);
                    if (!Objects.equals(p.getLeft(), p2.getLeft()) || !p.getRight().equals(p2.getRight())) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.conceptId;
        for (Pair<Integer, Concept> p: this.children) {
            int labelId = p.getLeft();
            hash = 97 * hash + labelId;
            hash = 97 * hash + Objects.hashCode(p.getRight());
        }
        return hash;
    }

}
