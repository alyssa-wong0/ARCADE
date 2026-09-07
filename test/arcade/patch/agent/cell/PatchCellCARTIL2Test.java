package arcade.patch.agent.cell;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import arcade.core.env.lattice.Lattice;
import arcade.core.sim.Simulation;
import arcade.core.util.MiniBox;
import arcade.core.util.Parameters;
import arcade.patch.env.location.PatchLocation;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PatchCellCARTIL2Test {

    private Parameters parametersMock;
    private PatchLocation locationMock;
    private PatchCellContainer container;
    private PatchCellCARTIL2 cellMock;
    private Simulation simMock;
    private Lattice latticeMock;

    @BeforeEach
    public void setUp() {
        parametersMock = spy(new Parameters(new MiniBox(), null, null));
        locationMock = mock(PatchLocation.class);

        container =
                new PatchCellContainer(
                        1,
                        1,
                        1,
                        0,
                        0,
                        arcade.patch.util.PatchEnums.State.UNDEFINED,
                        100,
                        5,
                        100,
                        5);

        doReturn(1.0).when(parametersMock).getDouble(anyString());
        doReturn(1).when(parametersMock).getInt(anyString());

        cellMock = new PatchCellCARTIL2(container, locationMock, parametersMock);
        simMock = mock(Simulation.class);
        latticeMock = mock(Lattice.class);

        when(simMock.getLattice("IL2")).thenReturn(latticeMock);
    }

    @Test
    public void secreteIL2_increasesConcentration() {
        when(latticeMock.getAverageValue(locationMock)).thenReturn(100.00);

        cellMock.secreteIL2(simMock);

        verify(latticeMock).updateValue(locationMock, 1.01);
    }
}
