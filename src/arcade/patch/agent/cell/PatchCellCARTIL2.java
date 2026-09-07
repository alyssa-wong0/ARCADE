package arcade.patch.agent.cell;

import sim.engine.SimState;
import ec.util.MersenneTwisterFast;
import arcade.core.agent.cell.CellState;
import arcade.core.env.location.Location;
import arcade.core.sim.Simulation;
import arcade.core.util.GrabBag;
import arcade.core.util.Parameters;
import arcade.patch.util.PatchEnums.AntigenFlag;
import arcade.patch.util.PatchEnums.Domain;
import arcade.patch.util.PatchEnums.State;

/** Extension of {@link PatchCellCART} for IL-2 CART-cells with selected module versions. */
public class PatchCellCARTIL2 extends PatchCellCART {

    /** Whether the synthetic IL-2 circuit has been activated by antigen recognition. */
    private boolean il2CircuitActivated = false;

    /** Rate of IL-2 secretion per timestep. */
    private double il2SecretionRate;

    /**
     * Creates a T cell {@code PatchCellCARTIL2} agent.
     *
     * @param container the cell container
     * @param location the {@link Location} of the cell
     * @param parameters the {@link Parameters} of the cell
     */
    public PatchCellCARTIL2(
            PatchCellContainer container, Location location, Parameters parameters) {
        this(container, location, parameters, null);
    }

    /**
     * Creates a T cell {@code PatchCellCARTIL2} agent.
     *
     * @param container the cell container
     * @param location the {@link Location} of the cell
     * @param parameters the {@link Parameters} of the cell
     * @param links the map of population links
     */
    public PatchCellCARTIL2(
            PatchCellContainer container, Location location, Parameters parameters, GrabBag links) {
        super(container, location, parameters, links);
        this.il2SecretionRate = parameters.getDouble("IL2_SECRETION_RATE");
    }

    @Override
    public PatchCellContainer make(int newID, CellState newState, MersenneTwisterFast random) {
        divisions++;
        return new PatchCellContainer(
                newID,
                id,
                pop,
                age,
                divisions,
                newState,
                volume,
                height,
                criticalVolume,
                criticalHeight);
    }

    @Override
    public void step(SimState simstate) {
        Simulation sim = (Simulation) simstate;

        super.age++;

        if (state != State.APOPTOTIC && age > apoptosisAge) {
            setState(State.APOPTOTIC);
            super.setBindingFlag(AntigenFlag.UNBOUND);
            this.activated = false;
        }

        super.lastActiveTicker++;
        if (super.lastActiveTicker != 0 && super.lastActiveTicker % MINUTES_IN_DAY == 0) {
            if (super.boundCARAntigensCount != 0) {
                super.boundCARAntigensCount--;
            }
        }
        if (super.lastActiveTicker / MINUTES_IN_DAY >= 7) {
            super.activated = false;
        }

        super.processes.get(Domain.METABOLISM).step(simstate.random, sim);

        if (state != State.APOPTOTIC) {
            if (super.energy < super.energyThreshold) {

                super.setState(State.APOPTOTIC);
                super.unbind();
                this.activated = false;
            } else if (state != State.ANERGIC
                    && state != State.SENESCENT
                    && state != State.EXHAUSTED
                    && state != State.STARVED
                    && energy < 0) {

                super.setState(State.STARVED);
                super.unbind();
            } else if (state == State.STARVED && energy >= 0) {
                super.setState(State.UNDEFINED);
            }
        }

        super.processes.get(Domain.INFLAMMATION).step(simstate.random, sim);

        // Change state from undefined.
        if (super.state == State.UNDEFINED || super.state == State.PAUSED) {
            if (divisions == divisionPotential) {
                if (simstate.random.nextDouble() > super.senescentFraction) {
                    super.setState(State.APOPTOTIC);
                } else {
                    super.setState(State.SENESCENT);
                }
                super.unbind();
                this.activated = false;
            } else {
                PatchCellTissue target = super.bindTarget(sim, location, simstate.random);
                super.boundTarget = target;

                if (super.getBindingFlag() == AntigenFlag.BOUND_ANTIGEN_CELL_RECEPTOR) {
                    if (simstate.random.nextDouble() > super.anergicFraction) {
                        super.setState(State.APOPTOTIC);
                    } else {
                        super.setState(State.ANERGIC);
                    }
                    super.unbind();
                    this.activated = false;
                    this.il2CircuitActivated = false;
                } else if (super.getBindingFlag() == AntigenFlag.BOUND_ANTIGEN) {

                    this.il2CircuitActivated = true;

                    if (boundCARAntigensCount > maxAntigenBinding) {
                        if (simstate.random.nextDouble() > super.exhaustedFraction) {
                            super.setState(State.APOPTOTIC);
                        } else {
                            super.setState(State.EXHAUSTED);
                        }
                        super.unbind();
                        this.activated = false;
                    } else {
                        super.setState(State.STIMULATORY);
                        this.lastActiveTicker = 0;
                        this.activated = true;
                    }
                } else {
                    if (super.getBindingFlag() == AntigenFlag.BOUND_CELL_RECEPTOR) {
                        super.unbind();
                    }
                    if (activated) {
                        super.setState(State.PROLIFERATIVE);
                    } else {
                        if (simstate.random.nextDouble() > super.proliferativeFraction) {
                            super.setState(State.MIGRATORY);
                        } else {
                            super.setState(State.PROLIFERATIVE);
                        }
                    }
                }
            }
        }

        // Turn of the IL-2 circuit if cell is apoptotic.
        if (state == State.APOPTOTIC) {
            this.il2CircuitActivated = false;
        }

        // If the IL-2 circuit is activated,
        // the CART IL-2 cell will continuously secrete IL-2 at a constant rate.
        if (il2CircuitActivated) {
            secreteIL2(sim);
        }

        // Step the module for the cell state.
        if (super.module != null) {
            super.module.step(simstate.random, sim);
        }
    }

    protected void secreteIL2(Simulation sim) {
        double il2Concentration = sim.getLattice("IL2").getAverageValue(location);

        if (il2Concentration > 0) {
            double newConcentration = il2Concentration + il2SecretionRate;
            double fraction = newConcentration / il2Concentration;
            sim.getLattice("IL2").updateValue(location, fraction);
        }
    }
}
