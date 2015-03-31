package np2amr.amr;

import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConceptTest {
    
    public ConceptTest() {
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
    public void testEquality() {
        Concept c1 = new Concept(1);
        Concept c1_ = new Concept(1);
        Concept c2 = new Concept(2);
        Concept c2_ = new Concept(2);

        assertEquals(c1, c1_);
        assertEquals(c2, c2_);
        assertNotEquals(c1, c2);
        assertNotEquals(c1, c2_);
        assertNotEquals(c2, c1);
        assertNotEquals(c2, c1_);

        // same child, same label
        Concept c3 = new Concept(3);
        Concept c3_ = new Concept(3);
        Concept c4 = new Concept(4);
        Concept c4_ = new Concept(4);
        c3.addChild(c4, 0);
        c3_.addChild(c4_, 0);
        assertEquals(c3, c3_);
        // different number of children
        Concept c4__ = new Concept(4);
        c3_.addChild(c4__, 0);
        assertNotEquals(c3, c3_);

        // same child, different labels
        Concept c5 = new Concept(5);
        Concept c5_ = new Concept(5);
        Concept c6 = new Concept(6);
        Concept c6_ = new Concept(6);
        c5.addChild(c6, 0);
        c5_.addChild(c6_, 1);
        assertNotEquals(c5, c5_);

        // different children, same label
        Concept c7 = new Concept(7);
        Concept c7_ = new Concept(7);
        Concept c8 = new Concept(8);
        Concept c9 = new Concept(9);
        c7.addChild(c8, 0);
        c7_.addChild(c9, 0);
        assertNotEquals(c7, c7_);


    }
    
}
