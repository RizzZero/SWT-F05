
package autopilot.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.ValueSource;

class AdaptiveCruiseTest {

    @Test
    void safeDistanceFor_whenSpeedIs80_returns40() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        double result = cruise.safeDistanceFor(80);

        assertEquals(40, result);
    }

    @Test
    void safeDistanceFor_whenSpeedIsZero_returnsZero() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        double result = cruise.safeDistanceFor(0);

        assertEquals(0, result);
    }

    @Test
    void safeDistanceFor_whenSpeedIsNegativeZero_preservesNegativeZero() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        double result = cruise.safeDistanceFor(-0.0);

        assertEquals(
                Double.doubleToRawLongBits(-0.0),
                Double.doubleToRawLongBits(result)
        );
    }

    @Test
    void update_whenSpeedIs96_acceleratesBy4() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(96, 100);

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, result.mode());
        assertEquals(4, result.amount());
    }

    @Test
    void update_whenSpeedIs95_acceleratesBy5() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(95, 100);

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, result.mode());
        assertEquals(5, result.amount());
    }

    @Test
    void update_whenSpeedIs94_limitsAccelerationTo5() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(94, 100);

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, result.mode());
        assertEquals(5, result.amount());
    }

    @Test
    void update_whenSpeedEqualsTarget_holds() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(100, 100);

        assertEquals(AdaptiveCruise.Mode.HOLD, result.mode());
        assertEquals(0, result.amount());
    }

    @Test
    void update_whenSpeedExceedsTarget_holds() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(120, 100);

        assertEquals(AdaptiveCruise.Mode.HOLD, result.mode());
        assertEquals(0, result.amount());
    }

    @Test
    void update_whenSpeedIsZeroAndDistancePositive_accelerates() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(0, 1);

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, result.mode());
        assertEquals(5, result.amount());
    }

    @Test
    void update_whenDistanceBelowEmergency_emergencyBrakes() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(80, 19.9);

        assertEquals(AdaptiveCruise.Mode.EMERGENCY_BRAKE, result.mode());
        assertEquals(100, result.amount());
    }

    @Test
    void update_whenDistanceEqualsEmergency_emergencyBrakes() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(80, 20);

        assertEquals(AdaptiveCruise.Mode.EMERGENCY_BRAKE, result.mode());
        assertEquals(100, result.amount());
    }

    @Test
    void update_whenDistanceAboveEmergency_brakesNormally() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(80, 20.1);

        assertEquals(AdaptiveCruise.Mode.BRAKE, result.mode());
        assertEquals(49.75, result.amount(), 0.0001);
    }

    @Test
    void update_whenDistanceBelowSafeDistance_brakes() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(80, 39.9);

        assertEquals(AdaptiveCruise.Mode.BRAKE, result.mode());
        assertEquals(0.25, result.amount(), 0.0001);
    }

    @Test
    void update_whenDistanceEqualsSafeDistance_holds() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(80, 40);

        assertEquals(AdaptiveCruise.Mode.HOLD, result.mode());
        assertEquals(0, result.amount());
    }

    @Test
    void update_whenDistanceAboveSafeDistance_accelerates() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(80, 40.1);

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, result.mode());
        assertEquals(5, result.amount());
    }

    @Test
    void update_whenSpeedAndDistanceAreZero_emergencyBrakes() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(0, 0);

        assertEquals(AdaptiveCruise.Mode.EMERGENCY_BRAKE, result.mode());
        assertEquals(100, result.amount());
    }

    @Test
    void update_whenOverspeedingNearObstacle_emergencyBrakes() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        AdaptiveCruise.Decision result = cruise.update(120, 20);

        assertEquals(AdaptiveCruise.Mode.EMERGENCY_BRAKE, result.mode());
        assertEquals(100, result.amount());
    }

    @Test
    void update_whenAlreadyBraking_continuesBraking() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        cruise.update(80, 30);

        AdaptiveCruise.Decision result = cruise.update(80, 45);

        assertEquals(AdaptiveCruise.Mode.BRAKE, result.mode());
        assertEquals(10, result.amount());
    }

    @Test
    void update_whenAlreadyBrakingAtSafeDistance_continuesBraking() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        cruise.update(80, 30);

        AdaptiveCruise.Decision result = cruise.update(80, 40);

        assertEquals(AdaptiveCruise.Mode.BRAKE, result.mode());
        assertEquals(10, result.amount());
    }

    @Test
    void update_whenDistanceBelowReleaseThreshold_continuesBraking() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        cruise.update(80, 30);

        AdaptiveCruise.Decision result = cruise.update(80, 47.9);

        assertEquals(AdaptiveCruise.Mode.BRAKE, result.mode());
        assertEquals(10, result.amount());
    }

    @Test
    void update_whenDistanceReachesReleaseThreshold_accelerates() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        cruise.update(80, 30);

        AdaptiveCruise.Decision result = cruise.update(80, 48);

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, result.mode());
        assertEquals(5, result.amount());
    }

    @Test
    void update_whenBrakingIsReleased_staysReleased() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        cruise.update(80, 30);
        cruise.update(80, 48);

        AdaptiveCruise.Decision result = cruise.update(80, 45);

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, result.mode());
        assertEquals(5, result.amount());
    }

    @Test
    void update_whenEmergencyEnds_usesNormalBraking() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        cruise.update(80, 10);

        AdaptiveCruise.Decision result = cruise.update(80, 35);

        assertEquals(AdaptiveCruise.Mode.BRAKE, result.mode());
        assertEquals(12.5, result.amount());
    }

    @Test
    void update_whenDistanceReachesSafeDistance_usesHysteresisPressure() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        cruise.update(80, 10);
        cruise.update(80, 35);

        AdaptiveCruise.Decision result = cruise.update(80, 40);

        assertEquals(AdaptiveCruise.Mode.BRAKE, result.mode());
        assertEquals(10, result.amount());
    }

    @Test
    void update_whenDistanceBecomesCriticalAgain_emergencyBrakes() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        cruise.update(80, 30);

        AdaptiveCruise.Decision result = cruise.update(80, 10);

        assertEquals(AdaptiveCruise.Mode.EMERGENCY_BRAKE, result.mode());
        assertEquals(100, result.amount());
    }

    @Test
    void update_whenSpeedIsInvalid_throwsException() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        double[] invalidSpeeds = {
                -1,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        for (double speed : invalidSpeeds) {
            assertThrows(IllegalArgumentException.class,
                    () -> cruise.update(speed, 50));
        }
    }

    @Test
    void update_whenDistanceIsInvalid_throwsException() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        double[] invalidDistances = {
                -1,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        for (double distance : invalidDistances) {
            assertThrows(IllegalArgumentException.class,
                    () -> cruise.update(80, distance));
        }
    }

    @Test
    void update_whenInvalidDistanceOccurs_keepsBrakingState() {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        cruise.update(80, 30);

        assertThrows(IllegalArgumentException.class,
                () -> cruise.update(80, -5));

        AdaptiveCruise.Decision result = cruise.update(80, 45);

        assertEquals(AdaptiveCruise.Mode.BRAKE, result.mode());
        assertEquals(10, result.amount());
    }

    @ParameterizedTest
    @ValueSource(doubles = {
            -1,
            -0.1,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    void safeDistanceFor_whenSpeedIsInvalid_throwsException(double speed) {
        AdaptiveCruise cruise = new AdaptiveCruise(100);

        assertThrows(IllegalArgumentException.class,
                () -> cruise.safeDistanceFor(speed));
    }

}
