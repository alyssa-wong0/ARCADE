package arcade.core.util.distributions;

import ec.util.MersenneTwisterFast;
import arcade.core.util.MiniBox;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ZeroInflatedNormalDistributionTest {
    private static final double EPSILON = 1E-5;
    private static final MersenneTwisterFast RANDOM = new MersenneTwisterFast();

    @Test public void constructor_givenParametersBox_setsFields() {
        MiniBox p = new MiniBox(); p.put("t_ZERO", 0.4); p.put("t_MU", 100.0); p.put("t_SIGMA", 20.0);
        ZeroInflatedNormalDistribution d = new ZeroInflatedNormalDistribution("t", p, RANDOM);
        assertAll(() -> assertEquals(0.4, d.zeroProb, EPSILON), () -> assertEquals(100.0, d.mu, EPSILON), () -> assertEquals(20.0, d.sigma, EPSILON));
    }

    @Test public void constructor_givenValues_setsFields() {
        ZeroInflatedNormalDistribution d = new ZeroInflatedNormalDistribution(0.4, 100.0, 20.0, RANDOM);
        assertAll(() -> assertEquals(0.4, d.zeroProb, EPSILON), () -> assertEquals(100.0, d.mu, EPSILON), () -> assertEquals(20.0, d.sigma, EPSILON));
    }

    @Test public void getDoubleValue_returnsSameValue() {
        ZeroInflatedNormalDistribution d = new ZeroInflatedNormalDistribution(0.4, 100.0, 20.0, RANDOM);
        assertEquals(d.value, d.getDoubleValue(), EPSILON);
    }

    @Test public void getIntValue_roundsCorrectly() {
        ZeroInflatedNormalDistribution d = new ZeroInflatedNormalDistribution(0.0, 100.0, 0.0, RANDOM);
        assertEquals((int) Math.round(d.value), d.getIntValue());
    }

    @Test public void getExpected_matchesFormula() {
        ZeroInflatedNormalDistribution d = new ZeroInflatedNormalDistribution(0.4, 100.0, 20.0, RANDOM);
        assertEquals(60.0, d.getExpected(), EPSILON);
    }

    @Test public void nextDouble_producesZerosAndNonzeros() {
        ZeroInflatedNormalDistribution d = new ZeroInflatedNormalDistribution(0.5, 50.0, 10.0, RANDOM);
        int zeros = 0; for (int i = 0; i < 10000; i++) if (d.nextDouble() == 0.0) zeros++;
        assertTrue(zeros > 1000 && zeros < 9000);
    }

    @Test public void rebase_keepsParameters() {
        ZeroInflatedNormalDistribution old = new ZeroInflatedNormalDistribution(0.4, 100.0, 20.0, RANDOM);
        ZeroInflatedNormalDistribution rebased = (ZeroInflatedNormalDistribution) old.rebase(RANDOM);
        assertAll(() -> assertEquals(old.zeroProb, rebased.zeroProb, EPSILON), () -> assertEquals(old.sigma, rebased.sigma, EPSILON));
    }
}