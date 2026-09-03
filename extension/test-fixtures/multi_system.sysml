package multi_system {
    private import CSCore::*;
    private import CSRosBridge::*;
    private import multi_components::*;

    @RosSystemMapping { systemName = "multi_robot_system"; }
    part def MultiRobotSystem {
        part lidar : LidarEngine;

        perform action scanEnv : ScanEnvironment {
            in ref engine = lidar;
        }
    }
}
