
package autopilot.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.CsvFileSource;

import org.junit.jupiter.params.provider.EnumSource;

class BrakeControllerTest {


    @Test
    void apply_whenPressureIsInvalid_throwsException() {
        BrakeController brake = new BrakeController();

        double[] invalidPressures = {
                -1,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        for (double pressure : invalidPressures) {
            assertThrows(IllegalArgumentException.class,
                    () -> brake.apply(pressure, 50, GearBox.Gear.D));
        }
    }

    @Test
    void apply_whenSpeedIsInvalid_throwsException() {
        BrakeController brake = new BrakeController();

        double[] invalidSpeeds = {
                -1,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        for (double speed : invalidSpeeds) {
            assertThrows(IllegalArgumentException.class,
                    () -> brake.apply(50, speed, GearBox.Gear.D));
        }
    }

    @Test
    void apply_whenGearIsPark_throwsException() {
        BrakeController brake = new BrakeController();

        assertThrows(IllegalStateException.class,
                () -> brake.apply(50, 50, GearBox.Gear.P));
    }

    @Test
    void apply_whenGearIsNull_throwsException() {
        BrakeController brake = new BrakeController();

        assertThrows(NullPointerException.class,
                () -> brake.apply(50, 50, null));
    }

    @Test
    void apply_whenAbsStarts_firstPulseIsHigh() {
        BrakeController brake = new BrakeController();

        double result = brake.apply(100, 50, GearBox.Gear.D);

        assertEquals(90, result);
    }

    @Test
    void apply_whenAbsContinues_secondPulseIsLow() {
        BrakeController brake = new BrakeController();

        brake.apply(100, 50, GearBox.Gear.D);

        double result = brake.apply(100, 50, GearBox.Gear.D);

        assertEquals(60, result);
    }

    @Test
    void apply_whenAbsContinues_thirdPulseIsHigh() {
        BrakeController brake = new BrakeController();

        brake.apply(100, 50, GearBox.Gear.D);
        brake.apply(100, 50, GearBox.Gear.D);

        double result = brake.apply(100, 50, GearBox.Gear.D);

        assertEquals(90, result);
    }

    @Test
    void apply_whenAbsStops_nextPulseResetsToHigh() {
        BrakeController brake = new BrakeController();

        brake.apply(100, 50, GearBox.Gear.D);
        brake.apply(50, 50, GearBox.Gear.D);

        double result = brake.apply(100, 50, GearBox.Gear.D);

        assertEquals(90, result);
    }

    @Test
    void apply_whenSpeedDropsBelowThreshold_stopsAbs() {
        BrakeController brake = new BrakeController();

        brake.apply(100, 50, GearBox.Gear.D);

        double result = brake.apply(85, 29, GearBox.Gear.D);

        assertEquals(85, result);
    }

    @Test
    void apply_whenPressureDropsToThreshold_stopsAbs() {
        BrakeController brake = new BrakeController();

        brake.apply(100, 50, GearBox.Gear.D);

        double result = brake.apply(80, 50, GearBox.Gear.D);

        assertEquals(80, result);
    }

    @Test
    void emergencyBrake_whenGearIsPark_throwsException() {
        BrakeController brake = new BrakeController();

        assertThrows(IllegalStateException.class,
                () -> brake.emergencyBrake(GearBox.Gear.P));
    }

    @Test
    void emergencyBrake_whenGearIsNull_throwsException() {
        BrakeController brake = new BrakeController();

        assertThrows(NullPointerException.class,
                () -> brake.emergencyBrake(null));
    }

    @Test
    void emergencyBrake_whenAbsPulseIsLow_resetsToHigh() {
        BrakeController brake = new BrakeController();

        brake.apply(100, 50, GearBox.Gear.D);
        brake.apply(100, 50, GearBox.Gear.D);

        brake.emergencyBrake(GearBox.Gear.D);

        double result = brake.apply(100, 50, GearBox.Gear.D);

        assertEquals(90, result);
    }

    @Test
    void apply_afterEmergencyBrake_warningRemainsActive() {
        BrakeController brake = new BrakeController();

        brake.emergencyBrake(GearBox.Gear.D);
        brake.apply(40, 20, GearBox.Gear.D);

        assertTrue(brake.isWarningActive());
    }

    @Test
    void apply_whenInputIsInvalid_keepsPreviousPressure() {
        BrakeController brake = new BrakeController();

        brake.apply(50, 40, GearBox.Gear.D);

        assertThrows(IllegalArgumentException.class,
                () -> brake.apply(-10, 40, GearBox.Gear.D));

        assertEquals(50, brake.getPressure());
    }

    @Test
    void apply_whenInputIsInvalid_keepsAbsPulseState() {
        BrakeController brake = new BrakeController();

        brake.apply(100, 50, GearBox.Gear.D);

        assertThrows(IllegalArgumentException.class,
                () -> brake.apply(-1, 50, GearBox.Gear.D));

        double result = brake.apply(100, 50, GearBox.Gear.D);

        assertEquals(60, result);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/brake-data.csv", numLinesToSkip = 1)
    void apply_whenPressureAndSpeedChange_returnsExpectedPressure(
            double pressure, double speed, double expected) {

        BrakeController brake = new BrakeController();

        double result = brake.apply(pressure, speed, GearBox.Gear.D);

        assertEquals(expected, result);
    }

    @ParameterizedTest
    @EnumSource(
            value = GearBox.Gear.class,
            names = {"R", "N", "D"}
    )
    void emergencyBrake_whenGearIsAllowed_activatesBraking(
            GearBox.Gear gear) {

        BrakeController brake = new BrakeController();

        brake.emergencyBrake(gear);

        assertEquals(100, brake.getPressure());
        assertTrue(brake.isWarningActive());
    }
}
