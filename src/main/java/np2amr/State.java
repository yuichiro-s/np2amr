package np2amr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import np2amr.action.Action;
import java.util.List;
import java.util.Map;
import np2amr.action.DummyAction;
import np2amr.action.EmptyAction;
import np2amr.action.ReduceAction;
import np2amr.action.ShiftAction;
import np2amr.amr.Concept;
import np2amr.amr.OntoPredicate;
import org.apache.commons.lang3.tuple.Pair;

public class State {

    public final int top;
    public final Concept concept;
    public final int lmost;
    public final int rmost;
    public final State left;
    public final int right;
    public final List<Token> toks;
    public final State prev;
    public final Action prevAct;

    public final float score;

    public State(List<Token> toks) {
        this.top = 0;
        this.concept = null;
        this.lmost = -1;
        this.rmost = -1;
        this.left = null;
        this.right = 1;
        this.toks = toks;
        this.prev = null;
        this.prevAct = null;
        this.score = 0;
    }

    public State(int top, Concept concept, int lmost, int rmost, State left, int right, List<Token> toks, State prev, Action prevAct, float score) {
        this.top = top;
        this.concept = concept;
        this.lmost = lmost;
        this.rmost = rmost;
        this.left = left;
        this.right = right;
        this.toks = toks;
        this.prev = prev;
        this.prevAct = prevAct;
        this.score = score;
    }

    public State next(Action action, float nextScore) {
        if (action instanceof ShiftAction) {
            ShiftAction a = (ShiftAction)action;
            return new State(right, a.concept, -1, -1, this, right+1, toks, this, action, nextScore);
        } else if (action instanceof EmptyAction) {
            return new State(top, concept, lmost, rmost, left, right+1, toks, this, action, nextScore);
        } else if (action instanceof ReduceAction) {
            ReduceAction a = (ReduceAction)action;
            if (a.dir == ReduceAction.Dir.LEFT) {
                // left-reduce
                return new State(top, concept, left.top, rmost, left.left, right, toks, this, action, nextScore);
            } else {
                // right-reduce
                return new State(left.top, left.concept, left.lmost, top, left.left, right, toks, this, action, nextScore);
            }
        } else if (action instanceof DummyAction) {
            return new State(top, concept, lmost, rmost, left, right, toks, this, action, nextScore);
        } else {
            throw new RuntimeException("Unknown action: " + action);
        }
    }


    private ShiftAction goldShiftAction() {
        Token tok = toks.get(right);

        Concept goldConcept = tok.goldConcept;
        assert goldConcept != null;

        for (Pair<ShiftAction.Type, Concept> p: Config.identifyConcepts(tok)) {
            ShiftAction.Type type = p.getLeft();
            Concept c = p.getRight();
            if (c.equals(goldConcept)) {
                return new ShiftAction(type, c);
            }
        }

        throw new RuntimeException("Unable to generate a gold action.");
    }

    /**
     * Returns an action that leads to the correct parse.
     * @return null if impossible to parse (non-projective)
     */
    public Action goldAction() {
        if (prevAct instanceof EmptyAction) {
            return new DummyAction();   // only DUMMY is allowed after EMPTY
        }

        if (right < toks.size() && toks.get(right).goldConcept == null) {
            // EMPTY as soon as possible
            return new EmptyAction();
        } else if (left == null) {
            // SHIFT
            return goldShiftAction();
        } else {
            Token s0 = toks.get(top);
            Token s1 = toks.get(left.top);

            if (s1.goldHead == top) {
                return new ReduceAction(ReduceAction.Dir.LEFT, s1.goldLabelId, s1.goldPosition);
            } else if (s0.goldHead == left.top) {
                boolean reducible = true;
                for (int i = right; i < toks.size(); i++) {
                    if (toks.get(i).goldHead == top) {
                        reducible = false;
                        break;
                    }
                }
                if (reducible) {
                    return new ReduceAction(ReduceAction.Dir.RIGHT, s0.goldLabelId, s0.goldPosition);
                }
            }
        }
        if (right < toks.size()) {
            return goldShiftAction();
        } else {
            return null;
        }
    }

    /**
     * Lists all valid actions from this state.
     * @return list of all valid actions from this state
     */
    public List<Action> validActions() {
        if (prevAct instanceof EmptyAction) {
            return Arrays.asList(new DummyAction());
        }

        List<Action> res = new ArrayList<>();

        // check shift
        if (right < toks.size()) {
            // there's something left in buffer

            if (!(prevAct instanceof ReduceAction)) {
                // EMPTY is applicable except for immediately after REDUCE
                res.add(new EmptyAction());
            }

            // token to identify concept
            Token tok = toks.get(right);

            Config.identifyConcepts(tok).stream().forEach((p) -> {
                ShiftAction.Type type = p.getLeft();
                Concept newConcept = p.getRight();
                res.add(new ShiftAction(type, newConcept));
            });
        }

        // check reduce
        if (left != null) {
            int size = concept.size();
            int leftSize = left.concept == null ? 1 : left.concept.size();
            for (int labelId: Config.labelIds) {
                // left-reduce
                if (left.top != 0) {    // if left is not ROOT
                    for (int i = 0; i < size; i++) {
                        ReduceAction a = new ReduceAction(ReduceAction.Dir.LEFT, labelId, i);
                        if (checkLabelConstraints(a)) {
                            res.add(a);
                        }
                    }
                }
                // right-reduce
                if (left.top != 0 || right == toks.size()) {    // if left is ROOT, buffer must be empty
                    for (int i = 0; i < leftSize; i++) {
                        ReduceAction a = new ReduceAction(ReduceAction.Dir.RIGHT, labelId, i);
                        if (checkLabelConstraints(a)) {
                            res.add(a);
                        }
                    }
                }
            }
        }

        return res;
    }

    /**
     * Checks whether ReduceAction satisfies hard-constraints of allowed relations.
     * @param a 
     * @return  
     */
    public boolean checkLabelConstraints(ReduceAction a) {
        if (left.concept == null) {
            // reduce to ROOT
            return a.dir == ReduceAction.Dir.RIGHT && a.position == 0 && a.labelId == Util.i("root");
        } else {
            Concept cTail;
            Concept cHead;
            if (a.dir == ReduceAction.Dir.RIGHT) {
                cTail = concept;
                cHead = left.concept.getConceptAtPosition(a.position);
            } else {
                cTail = left.concept;
                cHead = concept.getConceptAtPosition(a.position);
            }

            // resolve reversed label
            int labelId = a.labelId;
            int predId = cHead.conceptId;
            if (Config.reverseLabelMap.containsKey(a.labelId)) {
                labelId = Config.reverseLabelMap.get(a.labelId);
                predId = cTail.conceptId;
            }

            if (isPred(predId)) {
                return allowedLabel(predId, labelId);
            } else {
                // it's not a prediacte
                // not allowed to take ARGn
                return !isArgLabel(labelId);
            }
        }
    }

    private static Map<Integer, Boolean> isPredMem = new HashMap<>();
    /**
     * Returns whether this is predicate or not.
     */
    private static boolean isPred(int id) {
        if (isPredMem.containsKey(id)) {
            return isPredMem.get(id);
        } else {
            String str = Util.s(id);
            boolean b = str.contains("-") && str.length() >= 3;
            isPredMem.put(id, b);
            return b;
        }
    }


    private static Map<Integer, Boolean> isArgLabelMem = new HashMap<>();
    /**
     * Returns whether this label is ARGn.
     */
    private static boolean isArgLabel(int labelId) {
        if (isArgLabelMem.containsKey(labelId)) {
            return isArgLabelMem.get(labelId);
        } else {
            boolean b = Util.s(labelId).startsWith("ARG");
            isArgLabelMem.put(labelId, b);
            return b;
        }
    }

    private static Map<OntoPredicate, List<Integer>> predArgMem = new HashMap<>();
    /**
     * Returns whether labelId is allowed to be attached to pred.
     * @param pred
     * @param labelId
     * @return 
     */
    private static boolean allowedLabel(int predId, int labelId) {
        if (!Config.preds.containsKey(predId)) {
            // non-registered predicate
            return true;
        } else {
            OntoPredicate pred = Config.preds.get(predId);
            if (isArgLabel(labelId)) {
                // label is ARGn
                List<Integer> allowedLabels;
                if (predArgMem.containsKey(pred)) {
                    allowedLabels = predArgMem.get(pred);
                } else {
                    allowedLabels = new ArrayList<>();
                    predArgMem.put(pred, allowedLabels);
                    for (int i: pred.args) {
                        String argName = "ARG" + Integer.toString(i);
                        allowedLabels.add(Util.i(argName));
                    }
                }
                return allowedLabels.contains(labelId);
            } else {
                // other labels: always ok
                return true;
            }
        }
    }

    public boolean isFinal() {
        // DUMMY must follow after EMPTY
        return left == null && right == toks.size() && !(prevAct instanceof EmptyAction);
    }

    /**
     * Transform the state to a concept where all fragments are connected according to the actions.
     * If there's no concept in the resulting AMR, returns (amr-empty).
     * @return 
     */
    public Concept toAmr() {
        assert isFinal();

        int size = toks.size();

        State s = prev;  // before attachment of root
        Concept root = s.concept;
        while (s != null) {
            if (s.prevAct instanceof ReduceAction) {
                ReduceAction a = (ReduceAction)s.prevAct;
                Concept s0 = s.prev.concept;
                Concept s1 = s.prev.left.concept;
                Concept cTail, cHead;
                if (a.dir == ReduceAction.Dir.LEFT) {
                    cTail = s1;
                    cHead = s0.getConceptAtPosition(a.position);
                } else {
                    cTail = s0;
                    cHead = s1.getConceptAtPosition(a.position);
                }
                cHead.addChild(cTail, a.labelId);
            }
            s = s.prev;
        }
        if (root == null) {
            return new Concept(Util.i("amr-empty"));
        } else {
            return root;
        }
    }

}
