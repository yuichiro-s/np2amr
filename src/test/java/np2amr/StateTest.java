/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package np2amr;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import np2amr.action.Action;
import np2amr.action.ReduceAction;
import np2amr.action.ShiftAction;
import np2amr.amr.Concept;
import np2amr.amr.Io;
import np2amr.amr.OntoPredicate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author yuichiro
 */
public class StateTest {
    
    public StateTest() {
    }

    private String s(int id) {
        return Config.stringIdMap.getString(id);
    }

    private static void addP(Map<Integer, List<OntoPredicate>> m, String fromStr, String toStr)  {
        int from = Config.stringIdMap.getId(fromStr);
        int to = Config.stringIdMap.getId(toStr);
        OntoPredicate pred = new OntoPredicate(to, new ArrayList<>());
        if (m.containsKey(from)) {
            m.get(from).add(pred);
        } else {
            List<OntoPredicate> preds = new ArrayList<>();
            m.put(from, preds);
            preds.add(pred);
        }
    }

    private static void add(Map<Integer, List<Integer>> m, int from, int to)  {
        if (m.containsKey(from)) {
            m.get(from).add(to);
        } else {
            List<Integer> ids = new ArrayList<>();
            m.put(from, ids);
            ids.add(to);
        }
    }
    
    @BeforeClass
    public static void setUpClass() {
        Config.noun2pred = new HashMap<>();
        Config.adj2pred = new HashMap<>();
        Config.verb2pred = new HashMap<>();
        Config.adj2noun = new HashMap<>();
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    private List<Token> loadToks(Path path) throws IOException {
        Triple<List<List<Token>>, Map<Integer, Set<Concept>>, Set<Integer>> t = Io.loadAlignment(path);
        Config.conceptTable = t.getMiddle();
        return t.getLeft().get(0);
    }

    private List<Action> goldActions(State initState) {
        List<Action> res = new ArrayList<>();
        State state = initState;
        while (!state.isFinal()) {
            Action a = state.goldAction();
            if (a == null) {
                return null;
            }
            System.err.println(a);
            state = state.next(a, 0);
            res.add(a);
        }
        return res;
    }

    @Test
    public void testGoldActionIndustrialInnovation() throws IOException {
        Config.noun2pred = new HashMap<>();
        addP(Config.noun2pred, "innovation", "innovate-01");
        addP(Config.noun2pred, "innovation", "innovate-02");
        Config.adj2noun = new HashMap<>();
        addP(Config.adj2pred, "industrial", "industry");
        List<Token> toks = loadToks(Paths.get("testdata", "industrial_innovation.align"));
        State state = new State(toks);
        List<Action> acts = goldActions(state);
        assertEquals(4, acts.size());

        int shiftCount = 0;
        for (Action a: acts) {
            if (a instanceof ShiftAction) {
                shiftCount++;
            }
        }
        assertEquals(2, shiftCount);
    }

    @Test
    public void testGoldActionAnAdvancedLevelInTheWorld() throws IOException {
        List<Token> toks = loadToks(Paths.get("testdata", "an_advanced_level_in_the_world.align"));
        State state = new State(toks);
        List<Action> acts = goldActions(state);
        assertNull(acts);    // non-projective
    }

    @Test
    public void testGoldActionEarthquakeWorkers() throws IOException {
        List<Token> toks = loadToks(Paths.get("testdata", "earthquake_workers.align"));
        State state = new State(toks);
        List<Action> acts = goldActions(state);
        assertTrue(acts.get(0) instanceof ShiftAction);
        assertTrue(acts.get(1) instanceof ShiftAction);
        assertEquals(2, ((ShiftAction)acts.get(1)).concept.size());
        assertTrue(acts.get(2) instanceof ReduceAction);
        assertEquals(1, ((ReduceAction)acts.get(2)).position);
        assertTrue(acts.get(3) instanceof ReduceAction);
    }

    @Test
    public void testToAmrEarthquakeWorkers() throws IOException {
        List<Token> toks = loadToks(Paths.get("testdata", "earthquake_workers.align"));
        State state = new State(toks);
        List<Action> acts = goldActions(state);
        while (!state.isFinal()) {
            Action a = state.goldAction();
            state = state.next(a, 0);
        }
        Concept amr = state.toAmr();

        assertEquals(3, amr.size());
        assertEquals("person", s(amr.conceptId));
        Pair<Integer, Concept> e1 = amr.children.get(0);
        assertEquals("ARG0-of", s(e1.getLeft()));
        Concept c2 = e1.getRight();
        assertEquals("work-01", s(c2.conceptId));
        Pair<Integer, Concept> e2 = c2.children.get(0);
        assertEquals("mod", s(e2.getLeft()));
        assertEquals("earthquake", s(e2.getRight().conceptId));
    }
    
}
