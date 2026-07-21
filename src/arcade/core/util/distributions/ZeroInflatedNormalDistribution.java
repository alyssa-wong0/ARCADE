package arcade.core.util.distributions;

import ec.util.MersenneTwisterFast;
import arcade.core.util.MiniBox;

public class ZeroInflatedNormalDistribution implements Distribution {
    final double zeroProb;
    final double mu;
    final double sigma;
    private final sim.util.distribution.Normal normal;
    private final MersenneTwisterFast random;
    final double value;

    public ZeroInflatedNormalDistribution(String name, MiniBox parameters, MersenneTwisterFast random) {
        this(parameters.getDouble(name + "_ZERO"), parameters.getDouble(name + "_MU"), parameters.getDouble(name + "_SIGMA"), random);
    }

    public ZeroInflatedNormalDistribution(double zeroProb, double mu, double sigma, MersenneTwisterFast random) {
        this.zeroProb = Math.min(1.0, Math.max(0.0, zeroProb));
        this.mu = mu;
        this.sigma = Math.abs(sigma);
        this.random = random;
        this.normal = new sim.util.distribution.Normal(mu, sigma, random);
        this.value = draw();
    }

    private double draw() {
        return (random.nextDouble() < zeroProb) ? 0.0 : normal.nextDouble();
    }

    @Override public double getDoubleValue() { return value; }
    @Override public int getIntValue() { return (int) Math.round(value); }
    @Override public double nextDouble() { return draw(); }
    @Override public int nextInt() { return (int) Math.round(nextDouble()); }
    @Override public MiniBox getParameters() { MiniBox p = new MiniBox(); p.put("ZERO", zeroProb); p.put("MU", mu); p.put("SIGMA", sigma); return p; }
    @Override public double getExpected() { return (1.0 - zeroProb) * mu; }
    @Override public Distribution rebase(MersenneTwisterFast random) { return new ZeroInflatedNormalDistribution(zeroProb, value, sigma, random); }
}