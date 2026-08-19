package sample_robot {
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

    @RosSystemMapping { systemName = "sample_robot_system"; }
    part def SampleRobotSystem {
        part lidar : LidarEngine;

        perform action scanEnv : ScanEnvironment {
            in ref engine = lidar;
        }
    }
}
