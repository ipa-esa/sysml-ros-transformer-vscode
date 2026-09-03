package other_system {
    private import CSCore::*;
    private import CSRosBridge::*;

    @RosArtifactMapping { rosPackage = "base_pkg"; rosArtifact = "base_driver"; }
    part def BaseEngine specializes Engine;

    @RosSystemMapping { systemName = "other_system"; }
    part def OtherSystem {
        part basePart : BaseEngine;
    }
}
