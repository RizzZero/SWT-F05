
package autopilot.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.CsvSource;

class SteeringControllerTest {

    @Test
    void update_whenInputIsInvalid_throwsException() {
        SteeringController steering = new SteeringController();

        double[] invalidAngles = {
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        double[] invalidSpeeds = {
                -1,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        for (double angle : invalidAngles) {
            assertThrows(IllegalArgumentException.class,
                    () -> steering.update(angle, 50));
        }

        for (double speed : invalidSpeeds) {
            assertThrows(IllegalArgumentException.class,
                    () -> steering.update(30, speed));
        }
    }

    @Test
    void update_whenAngleExceedsPhysicalLimit_clampsTarget() {
        SteeringController steering = new SteeringController();

        steering.update(500, 50);
        assertEquals(450, steering.getTargetAngleDegrees());

        steering.update(-500, 50);
        assertEquals(-450, steering.getTargetAngleDegrees());
    }

    @Test
    void update_whenSpeedExceedsThreshold_limitsAngle() {
        SteeringController steering = new SteeringController();

        steering.update(200, 121);
        assertEquals(90, steering.getTargetAngleDegrees());

        steering.update(-200, 121);
        assertEquals(-90, steering.getTargetAngleDegrees());
    }

    @Test
    void update_whenSpeedEqualsThreshold_usesPhysicalLimit() {
        SteeringController steering = new SteeringController();

        steering.update(200, 120);

        assertEquals(200, steering.getTargetAngleDegrees());
    }

    @Test
    void update_whenAngleChangeExceeds60_limitsMovement() {
        SteeringController steering = new SteeringController();

        double first = steering.update(200, 50);
        double second = steering.update(200, 50);
        double third = steering.update(-200, 50);

        assertEquals(60, first);
        assertEquals(120, second);
        assertEquals(60, third);
    }

    @Test
    void update_whenDifferenceIs60_movesExactly60() {
        SteeringController steering = new SteeringController();

        double result = steering.update(60, 50);

        assertEquals(60, result);
    }

    @Test
    void selfCenter_whenAngleIsNotZero_movesTowardZero() {
        SteeringController steering = new SteeringController();

        steering.update(180, 50);
        steering.update(180, 50);

        double first = steering.selfCenter();
        double second = steering.selfCenter();

        assertEquals(60, first);
        assertEquals(0, second);
        assertEquals(0, steering.getTargetAngleDegrees());
    }

    @ParameterizedTest
    @CsvSource({
            "30, 50, 30",
            "60, 50, 60",
            "100, 50, 60",
            "-30, 50, -30",
            "-100, 50, -60",
            "200, 121, 60"
    })
    void update_whenAngleChanges_returnsExpectedAngle(
            double angle, double speed, double expected) {

        SteeringController steering = new SteeringController();

        double result = steering.update(angle, speed);

        assertEquals(expected, result);
    }

}
