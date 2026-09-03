package UnrelatedSystemPkg {
    private import CSCore::*;
    private import CSRosBridge::*;

    @RosArtifactMapping { rosPackage = "base_pkg"; rosArtifact = "base_node"; }
    part def BaseEngine specializes Engine;

    @RosSystemMapping { systemName = "unrelated_system"; }
    part def UnrelatedSystem {
        part basePart : BaseEngine;
    }
}
