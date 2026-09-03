package MultiSystem {
    private import CSCore::*;
    private import CSRosBridge::*;
    private import MultiComponents::*;

    @RosSystemMapping { systemName = "multi_robot_system"; }
    part def MultiRobotSystem {
        part camera : SensorCameraEngine;

        perform action cameraCapture : SensorCameraCapture {
            in ref engine = camera;
        }
    }
}
