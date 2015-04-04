package np2amr.amr;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import np2amr.Config;
import np2amr.Util;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public final class Concept {

    public final static String INDENT = "     ";

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

    /**
     * Deep copy of Concept. Parent is set to null;
     * @param orig Concept to copy from.
     */
    public Concept(Concept orig) {
        this.conceptId = orig.conceptId;
        this.children = new ArrayList<>();
        this.parent = null;
        for (Pair<Integer, Concept> p: orig.children) {
            int labelId = p.getLeft();
            Concept origChild = p.getRight();
            Concept newChild = new Concept(origChild);
            addChild(newChild, labelId);    // set parent of child
        }
    }

    public void addChild(Concept c, int labelId) {
        this.children.add(new ImmutablePair<>(labelId, c));
        c.parent = this;
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

    public String toSexp() {
        return toSexp(0, new HashMap<>(), false);
    }
    
    public String toSexp(int indent, Map<String, Integer> vars, boolean noIndent) {
        String name = Util.s(conceptId);

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
                String rel = Config.stringIdMap.getString(relId);
                Concept child = p.getRight();
                if (noIndent) {
                    sb.append(" ");
                } else {
                    sb.append("\n");
                    for (int i = 0; i < indent+1; i++) {
                        sb.append(INDENT);
                    }
                }
                
                sb.append(":");
                sb.append(rel);
                sb.append(" ");
                sb.append(child.toSexp(indent+1, vars, noIndent));
            }
            sb.append(")");
        } else {
            sb.append(name);
        }

        return sb.toString();
    }

    /**
     * Constructs Concept from String.
     * @param e
     * @return null if input string is not parseable
     */
    public static Concept fromSexp(String e) {
        try {
            String[] toks = e.replaceAll("\\(", " \\( ").replaceAll("\\)", " \\) ").trim().split("\\s+");

            // remove variables
            List<String> toksWithOutVars = new ArrayList<>();
            for (int i = 0; i < toks.length; i++) {
                if (i+1 < toks.length && toks[i+1].equals("/")) {
                    i++;    // skip two tokens
                } else {
                    toksWithOutVars.add(toks[i]);
                }
            }
            
            List<Object> stack = new ArrayList<>();
            for (String tok: toksWithOutVars) {
                if (tok.startsWith(":")) {
                    int relId = Util.i(tok.substring(1));
                    stack.add(relId);
                } else if (tok.equals("(") || tok.equals(")")) {
                    stack.add(tok);
                } else {
                    int id = Util.i(tok);
                    stack.add(id);
                }

                // check if it matches the pattern of "(" id (id Concept)* ")"
                if (tok.equals(")")) {
                    boolean flag = true;
                    int removeSize = 1;

                    int id = -1;
                    List<Pair<Integer, Concept>> children = new ArrayList<>();
                    for (int i = stack.size()-3; i >= 0; i -= 2) {
                        removeSize += 2;

                        Object o0 = stack.get(i);
                        Object o1 = stack.get(i+1);
                        if (o0 instanceof Integer && o1 instanceof Concept) {
                            // ok
                            int labelId = (Integer)o0;
                            Concept child = (Concept)o1;
                            children.add(new ImmutablePair<>(labelId, child));
                        } else {
                            if (o0.equals("(") && o1 instanceof Integer) {
                                id = (Integer)o1;
                            } else {
                                flag = false;
                            }
                            break;
                        }
                    }
                    if (id >= 0 && flag) {
                        // matched pattern
                        Concept c = new Concept(id);
                        for (Pair<Integer, Concept> p: children) {
                            int labelId = p.getLeft();
                            Concept child = p.getRight();
                            c.addChild(child, labelId);
                        }

                        for (int i = 0; i < removeSize; i++) {
                            stack.remove(stack.size()-1);
                        }
                        stack.add(c);
                    }
                }
            }
            return (Concept)stack.get(0);
        } catch (Exception ex) {
            return null;
        }
    }
    

    public int size() {
        int res = 1;
        for (Pair<Integer, Concept> p: children) {
            res += p.getRight().size();
        }
        return res;
    }

    /**
     * Finds the position of cTrg (0-indexed pre-order traversal).
     * @param cTrg
     * @return -1 if not found
     */
    public int getPosition(Concept cTrg) {
        int res = 0;
        Deque<Concept> stack = new ArrayDeque<>();
        stack.push(this);
        while (!stack.isEmpty()) {
            Concept top = stack.pop();
            if (top == cTrg) {
                return res;
            } else {
                res++;
                for (int i = top.children.size() - 1; i >= 0; i--) {
                    Concept child = top.children.get(i).getRight();
                    stack.push(child);
                }
            }
        }
        return -1;
    }

    /**
     * Returns the concept at pos (0-indexed pre-order traversal).
     * @param position
     * @return null if pos is out of bounds
     */
    public Concept getConceptAtPosition(int position) {
        int count = position;
        Deque<Concept> stack = new ArrayDeque<>();
        stack.push(this);
        while (!stack.isEmpty()) {
            Concept top = stack.pop();
            if (count == 0) {
                return top;
            } else {
                for (int i = top.children.size() - 1; i >= 0; i--) {
                    Concept child = top.children.get(i).getRight();
                    stack.push(child);
                }
            }
            count--;
        }
        return null;
    }

    @Override
    public String toString() {
        return toSexp(0, new HashMap<>(), true);
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
