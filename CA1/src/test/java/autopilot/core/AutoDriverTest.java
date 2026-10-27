
package autopilot.core;

import autopilot.exception.SensorFailureException;
import autopilot.sensor.LaneSensor;
import autopilot.sensor.ObstacleSensor;
import autopilot.sensor.SpeedSensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AutoDriverTest {

    private static final double TARGET_SPEED = 80.0;
    private static final double OBSTACLE_DISTANCE = 100.0;
    private static final double LANE_OFFSET = 1.0;
    private static final double SAFE_BRAKE_PRESSURE = 30.0;

    private SpeedSensor speedSensor;
    private ObstacleSensor obstacleSensor;
    private LaneSensor laneSensor;
    private AutoDriver autoDriver;

    @BeforeEach
    void setUp() {
        speedSensor = () -> TARGET_SPEED;
        obstacleSensor = () -> OBSTACLE_DISTANCE;
        laneSensor = () -> 0.0;

        autoDriver = new AutoDriver(
                speedSensor,
                obstacleSensor,
                laneSensor,
                TARGET_SPEED
        );
    }

    @Test
    void tick_whenSensorsAreHealthy_doesNotEnterSafeMode() {
        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertFalse(result.safeMode());
        assertEquals(0.0, result.brakePressure());
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, -1.0})
    void tick_whenLaneOffsetChanges_steersInOppositeDirection(double offset) {
        laneSensor = () -> offset;

        autoDriver = new AutoDriver(
                speedSensor, obstacleSensor, laneSensor, TARGET_SPEED
        );

        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertTrue(result.steeringAngleDegrees() * offset < 0.0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"speed", "obstacle"})
    void tick_whenSensorFails_entersSafeMode(String failedSensor) {
        if (failedSensor.equals("speed")) {
            speedSensor = () -> {
                throw new SensorFailureException("Speed sensor failed");
            };
        } else {
            obstacleSensor = () -> {
                throw new SensorFailureException("Obstacle sensor failed");
            };
        }

        autoDriver = new AutoDriver(
                speedSensor, obstacleSensor, laneSensor, TARGET_SPEED
        );

        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertAll(
                () -> assertTrue(result.safeMode()),
                () -> assertEquals(SAFE_BRAKE_PRESSURE, result.brakePressure()),
                () -> assertEquals(0.0, result.steeringAngleDegrees()),
                () -> assertEquals(AdaptiveCruise.Mode.BRAKE, result.cruiseMode()),
                () -> assertFalse(result.warningActive())
        );
    }
}
