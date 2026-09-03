package multi_components {
    private import CSCore::*;
    private import CSRosBridge::*;

    @RosTypeMapping { rosType = "sensor_msgs/msg/LaserScan"; }
    part def LaserScan specializes Modelet;

    @RosArtifactMapping { rosPackage = "robot_drivers"; rosArtifact = "lidar_driver"; }
    part def LidarEngine specializes Engine;

    action def ScanEnvironment specializes Exert {
        in ref modelets : Modelet[*] redefines Exert::modelets;
        in ref engine : LidarEngine redefines Exert::engine;
        out scan : LaserScan subsets Exert::output;
    }
}
