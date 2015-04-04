package np2amr.amr;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import np2amr.Token;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class IoTest {
    
    public IoTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
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

    @Test
    public void testLoadAlignmentIndustrialInnovation() throws IOException {
        Path path = Paths.get("testdata", "industrial_innovation.align");
        Triple<List<List<Token>>, Map<Integer, Set<Concept>>, Set<Integer>> t = Io.loadAlignment(path);
        List<Token> toks = t.getLeft().get(0);
        Map<Integer, Set<Concept>> conceptTable = t.getMiddle();
        Set<Integer> labelIds = t.getRight();

        assertEquals(3, toks.size());
        assertEquals(-1, toks.get(0).depHead);
        assertEquals(2, toks.get(1).depHead);
        assertEquals(0, toks.get(2).depHead);
        assertEquals(2, conceptTable.size());
        assertEquals(2, labelIds.size());
    }

    @Test
    public void testLoadAlignmentATrojanHorse() throws IOException {
        Path path = Paths.get("testdata", "a_trojan_horse.align");  // there's a corrupt node line
        Triple<List<List<Token>>, Map<Integer, Set<Concept>>, Set<Integer>> t = Io.loadAlignment(path);
        assertEquals(0, t.getLeft().size());
    }

    @Test
    public void testLoadAlignmentTheStudentUnion() throws IOException {
        Path path = Paths.get("testdata", "the_student_union.align");
        Triple<List<List<Token>>, Map<Integer, Set<Concept>>, Set<Integer>> t = Io.loadAlignment(path);
        List<Token> toks = t.getLeft().get(0);
        Map<Integer, Set<Concept>> conceptTable = t.getMiddle();
        Set<Integer> labelIds = t.getRight();

        assertEquals(4, toks.size());
        assertEquals(-1, toks.get(1).goldPosition);
        assertEquals(0, toks.get(2).goldPosition);
        assertEquals(0, toks.get(3).goldPosition);
        assertNull(toks.get(1).goldConcept);
        assertEquals(2, toks.get(2).goldConcept.size());
        assertEquals(1, toks.get(3).goldConcept.size());
        assertEquals(2, conceptTable.size());
        assertEquals(3, labelIds.size());
    }

    @Test
    public void testLoadAlignmentEarthquakeWorkers() throws IOException {
        Path path = Paths.get("testdata", "earthquake_workers.align");
        Triple<List<List<Token>>, Map<Integer, Set<Concept>>, Set<Integer>> t = Io.loadAlignment(path);
        List<Token> toks = t.getLeft().get(0);

        assertEquals(3, toks.size());
        assertEquals(2, toks.get(1).goldHead);
        assertEquals(0, toks.get(2).goldHead);
        assertEquals(1, toks.get(1).goldPosition);
        assertEquals(0, toks.get(2).goldPosition);
        assertEquals(1, toks.get(1).goldConcept.size());
        assertEquals(2, toks.get(2).goldConcept.size());
    }

    @Test
    public void testLoadAlignmentAnAdvancedLevelInTheWorld() throws IOException {
        Path path = Paths.get("testdata", "an_advanced_level_in_the_world.align");
        Triple<List<List<Token>>, Map<Integer, Set<Concept>>, Set<Integer>> t = Io.loadAlignment(path);
        List<Token> toks = t.getLeft().get(0);

        assertEquals(7, toks.size());
        assertEquals(-1, toks.get(1).goldHead);
        assertEquals(3, toks.get(2).goldHead);
        assertEquals(0, toks.get(3).goldHead);
        assertEquals(-1, toks.get(4).goldHead);
        assertEquals(-1, toks.get(5).goldHead);
        assertEquals(2, toks.get(6).goldHead);
    }

}
