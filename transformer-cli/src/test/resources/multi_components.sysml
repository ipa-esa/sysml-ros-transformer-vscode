package MultiComponents {
    private import CSCore::*;
    private import CSRosBridge::*;

    @RosTypeMapping { rosType = "sensor_msgs/msg/Image"; }
    part def SensorImage specializes Modelet;

    @RosArtifactMapping { rosPackage = "sensor_drivers"; rosArtifact = "sensor_camera_driver"; }
    part def SensorCameraEngine specializes Engine;

    action def SensorCameraCapture specializes Exert {
        in ref engine : SensorCameraEngine redefines Exert::engine;
        out image : SensorImage subsets Exert::output;
    }
}
