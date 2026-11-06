package airplane;

import airplane.components.Engine;
import airplane.components.FuelSystem;
import airplane.components.LandingGear;
import airplane.components.NavigationSystem;
import airplane.exceptions.FlightException;
import airplane.models.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AircraftTest {
    private Engine engine;
    private FuelSystem fuelSystem;
    private LandingGear landingGear;
    private NavigationSystem navigationSystem;
    private Aircraft aircraft;

    @BeforeEach
    void setUp() {
        engine = mock(Engine.class);
        fuelSystem = mock(FuelSystem.class);
        landingGear = mock(LandingGear.class);
        navigationSystem = mock(NavigationSystem.class);
        aircraft = new Aircraft(engine, fuelSystem, landingGear, navigationSystem);
    }

    @Test
    void constructor_whenAircraftIsCreated_startsGrounded() {
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
    }

    @Test
    void prepareForTakeoff_whenAllRequirementsAreMet_becomesReady() {
        givenTakeoffRequirementsAreMet();

        aircraft.prepareForTakeoff();

        assertEquals(FlightStatus.READY, aircraft.getStatus());
        verify(engine).isOperational();
        verify(fuelSystem).hasEnoughFuel();
        verify(navigationSystem).isRouteAvailable();
    }

    @Test
    void prepareForTakeoff_whenEngineIsNotOperational_throwsAndStopsChecking() {
        when(engine.isOperational()).thenReturn(false);

        assertThrows(FlightException.class, aircraft::prepareForTakeoff);

        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(engine).isOperational();
        verifyNoInteractions(fuelSystem, navigationSystem);
    }

    @Test
    void prepareForTakeoff_whenFuelIsInsufficient_throwsAndStopsChecking() {
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(false);

        assertThrows(FlightException.class, aircraft::prepareForTakeoff);

        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(engine).isOperational();
        verify(fuelSystem).hasEnoughFuel();
        verifyNoInteractions(navigationSystem);
    }

    @Test
    void prepareForTakeoff_whenRouteIsUnavailable_throwsAndRemainsGrounded() {
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(true);
        when(navigationSystem.isRouteAvailable()).thenReturn(false);

        assertThrows(FlightException.class, aircraft::prepareForTakeoff);

        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(engine).isOperational();
        verify(fuelSystem).hasEnoughFuel();
        verify(navigationSystem).isRouteAvailable();
    }

    @Test
    void prepareForTakeoff_whenEngineCheckThrows_propagatesFailureAndRemainsGrounded() {
        IllegalStateException failure = new IllegalStateException("Engine status unavailable");
        when(engine.isOperational()).thenThrow(failure);

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                aircraft::prepareForTakeoff);

        assertSame(failure, thrown);
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verifyNoInteractions(fuelSystem, navigationSystem);
    }

    @Test
    void takeOff_whenAircraftIsReady_startsEngineAndRetractsGear() {
        givenAircraftIsReady();
        // Explicitly document the expected no-op response of this void collaborator.
        doNothing().when(landingGear).retract();

        aircraft.takeOff();

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verify(engine, times(1)).start();
        verify(landingGear, times(1)).retract();
    }

    @Test
    void takeOff_whenEngineStartFails_wrapsFailureAndAbortsFlight() {
        givenAircraftIsReady();
        IllegalStateException failure = new IllegalStateException("Starter failed");
        doThrow(failure).when(engine).start();

        FlightException thrown = assertThrows(FlightException.class, aircraft::takeOff);

        assertSame(failure, thrown.getCause());
        assertEquals(FlightStatus.ABORTED, aircraft.getStatus());
        verify(engine).start();
        verifyNoInteractions(landingGear);
    }

    @Test
    void cruise_whenAircraftIsInFlight_consumesTheRequestedFuelAmount() {
        givenAircraftIsInFlight();
        // Clear setup interactions so this assertion concerns only cruise().
        reset(fuelSystem);

        aircraft.cruise(25.0);

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verify(fuelSystem, times(1)).consumeFuel(25.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {
            0.0,
            -1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    void cruise_whenFuelAmountIsInvalid_throwsWithoutConsumingFuel(double amount) {
        givenAircraftIsInFlight();

        assertThrows(IllegalArgumentException.class, () -> aircraft.cruise(amount));

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verify(fuelSystem, never()).consumeFuel(anyDouble());
    }

    @Test
    void cruise_whenEngineIsNoLongerOperational_makesAnEmergencyLanding() {
        givenAircraftIsInFlight();
        when(engine.isOperational()).thenReturn(false);

        aircraft.cruise(10.0);

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(landingGear).deploy();
        verify(engine).stop();
        verify(fuelSystem, never()).consumeFuel(anyDouble());
    }

    @Test
    void cruise_whenFuelConsumptionFails_landsAndThrowsFlightException() {
        givenAircraftIsInFlight();
        IllegalStateException failure = new IllegalStateException("Fuel system failed");
        doThrow(failure).when(fuelSystem).consumeFuel(anyDouble());

        FlightException thrown = assertThrows(
                FlightException.class,
                () -> aircraft.cruise(10.0));

        assertSame(failure, thrown.getCause());
        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(landingGear).deploy();
        verify(engine).stop();
    }

    @Test
    void land_whenAircraftIsInFlight_deploysGearAndStopsEngine() {
        givenAircraftIsInFlight();

        aircraft.land();

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(landingGear).deploy();
        verify(engine).stop();
    }

    @Test
    void emergencyLand_whenAircraftIsInFlight_deploysGearAndStopsEngine() {
        givenAircraftIsInFlight();

        aircraft.emergencyLand();

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(landingGear).deploy();
        verify(engine).stop();
    }

    @Test
    void takeOff_whenAircraftIsGrounded_throwsAndRemainsGrounded() {
        assertThrows(FlightException.class, aircraft::takeOff);

        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verifyNoInteractions(engine, fuelSystem, landingGear, navigationSystem);
    }

    private void givenTakeoffRequirementsAreMet() {
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(true);
        when(navigationSystem.isRouteAvailable()).thenReturn(true);
    }

    private void givenAircraftIsReady() {
        givenTakeoffRequirementsAreMet();
        aircraft.prepareForTakeoff();
    }

    private void givenAircraftIsInFlight() {
        givenAircraftIsReady();
        aircraft.takeOff();
    }
}
