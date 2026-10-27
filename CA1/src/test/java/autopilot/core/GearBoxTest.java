
package autopilot.core;

import autopilot.exception.IllegalGearChangeException;

import autopilot.sensor.SpeedSensor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.MethodSource;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

class GearBoxTest {

    @Test
    void changeTo_whenTransitionsAreValid_changesGear() {
        SpeedSensor sensor = () -> 0;
        GearBox gear = new GearBox(sensor);

        gear.changeTo(GearBox.Gear.R);
        assertEquals(GearBox.Gear.R, gear.getCurrentGear());

        gear.changeTo(GearBox.Gear.N);
        assertEquals(GearBox.Gear.N, gear.getCurrentGear());

        gear.changeTo(GearBox.Gear.D);
        assertEquals(GearBox.Gear.D, gear.getCurrentGear());
    }

    @Test
    void changeTo_betweenReverseAndDrive_requiresZeroSpeed() {
        double[] speeds = {0, 1};

        for (double speed : speeds) {
            SpeedSensor sensor = () -> speed;
            GearBox gear = new GearBox(sensor);

            gear.changeTo(GearBox.Gear.R);

            if (speed == 0) {
                gear.changeTo(GearBox.Gear.D);
                assertEquals(GearBox.Gear.D, gear.getCurrentGear());

                gear.changeTo(GearBox.Gear.R);
                assertEquals(GearBox.Gear.R, gear.getCurrentGear());
            } else {
                assertThrows(IllegalGearChangeException.class,
                        () -> gear.changeTo(GearBox.Gear.D));
            }
        }
    }

    @Test
    void changeTo_whenTransitionIsNotAllowed_throwsException() {
        SpeedSensor sensor = () -> 0;
        GearBox gear = new GearBox(sensor);

        assertThrows(IllegalGearChangeException.class,
                () -> gear.changeTo(GearBox.Gear.D));

        assertEquals(GearBox.Gear.P, gear.getCurrentGear());
    }

    @Test
    void changeTo_whenTargetIsCurrentGear_doesNothing() {
        SpeedSensor sensor = () -> 0;
        GearBox gear = new GearBox(sensor);

        gear.changeTo(GearBox.Gear.P);

        assertEquals(GearBox.Gear.P, gear.getCurrentGear());
    }

    @Test
    void changeTo_whenSpeedIsInvalid_throwsException() {
        double[] invalidSpeeds = {
                -1,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        for (double speed : invalidSpeeds) {
            SpeedSensor sensor = () -> speed;
            GearBox gear = new GearBox(sensor);

            assertThrows(IllegalArgumentException.class,
                    () -> gear.changeTo(GearBox.Gear.R));
        }
    }

    @Test
    void changeTo_whenTargetIsNull_throwsException() {
        SpeedSensor sensor = () -> 0;
        GearBox gear = new GearBox(sensor);

        assertThrows(NullPointerException.class,
                () -> gear.changeTo(null));
    }

    static Stream<Arguments> reverseSpeeds() {
        return Stream.of(
                Arguments.of(0.0, true),
                Arguments.of(4.9, true),
                Arguments.of(5.0, false),
                Arguments.of(5.1, false)
        );
    }

    @ParameterizedTest
    @MethodSource("reverseSpeeds")
    void changeTo_whenEnteringReverse_checksSpeedLimit(
            double speed, boolean allowed) {

        SpeedSensor sensor = () -> speed;
        GearBox gear = new GearBox(sensor);

        if (allowed) {
            gear.changeTo(GearBox.Gear.R);
            assertEquals(GearBox.Gear.R, gear.getCurrentGear());
        } else {
            assertThrows(IllegalGearChangeException.class,
                    () -> gear.changeTo(GearBox.Gear.R));
        }
    }

}
