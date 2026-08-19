//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.sysml.cli.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.fraunhofer.ipa.ros.sysml.cli.SysMLRosTransformerCli;

public class SysMLRosTransformerCliTest {

    @TempDir
    Path tempDir;

    private Path sysmlFile;
    private Path rossystemFile;
    private Path ros2File;

    @BeforeEach
    void setUp() throws Exception {
        sysmlFile = tempDir.resolve("test_annotated.sysml");
        rossystemFile = tempDir.resolve("test_system.rossystem");
        ros2File = tempDir.resolve("test_nodes.ros2");

        copyResource("/test_annotated.sysml", sysmlFile);
        copyResource("/test_system.rossystem", rossystemFile);
        copyResource("/test_nodes.ros2", ros2File);
    }

    private void copyResource(String resourcePath, Path target) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(in, "Resource not found: " + resourcePath);
            Files.write(target, in.readAllBytes());
        }
    }

    @Test
    void testVersionAndHelp() {
        assertEquals(0, SysMLRosTransformerCli.run(new String[]{"--version"}));
        assertEquals(0, SysMLRosTransformerCli.run(new String[]{"-v"}));
        assertEquals(0, SysMLRosTransformerCli.run(new String[]{"--help"}));
        assertEquals(0, SysMLRosTransformerCli.run(new String[]{"-h"}));
    }

    @Test
    void testForwardTransformation() throws Exception {
        Path outDir = tempDir.resolve("forward-out");
        int exitCode = SysMLRosTransformerCli.run(new String[]{
            "--forward", sysmlFile.toString(),
            "--output", outDir.toString()
        });

        assertEquals(0, exitCode, "Forward transformation should succeed");

        Path generatedFile = outDir.resolve("test_system.rossystem");
        assertTrue(Files.exists(generatedFile), "Generated .rossystem file should exist");

        String content = Files.readString(generatedFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("test_system:"), "Should contain system name");
        assertTrue(content.contains("test_drivers.camera_driver"), "Should contain camera node");
        assertTrue(content.contains("test_perception.object_detector"), "Should contain detector node");
        assertTrue(content.contains("camera_image"), "Should contain camera interface");
        assertTrue(content.contains("detector_image"), "Should contain detector input interface");
        assertTrue(content.contains("[\"camera_image\", \"detector_image\"]"), "Should contain connection");
    }

    @Test
    void testReverseTransformation() throws Exception {
        Path outDir = tempDir.resolve("reverse-out");
        int exitCode = SysMLRosTransformerCli.run(new String[]{
            "--reverse", rossystemFile.toString(),
            "--ros2", ros2File.toString(),
            "--output", outDir.toString()
        });

        assertEquals(0, exitCode, "Reverse transformation should succeed");

        Path generatedFile = outDir.resolve("test_system_architecture.sysml");
        assertTrue(Files.exists(generatedFile), "Generated .sysml file should exist");

        String content = Files.readString(generatedFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("package test_system_architecture"), "Should define package");
        assertTrue(content.contains("private import CSCore::*;"), "Should import CSCore");
        assertTrue(content.contains("private import CSRosBridge::*;"), "Should import CSRosBridge");
        assertTrue(content.contains("@RosArtifactMapping"), "Should contain @RosArtifactMapping");
        assertTrue(content.contains("@RosSystemMapping"), "Should contain @RosSystemMapping");
        assertTrue(content.contains("@RosTypeMapping"), "Should contain @RosTypeMapping");
        assertTrue(content.contains("specializes Engine"), "Should contain Engine specialization");
        assertTrue(content.contains("specializes Exert"), "Should contain Exert specialization");
        assertTrue(content.contains("flow from cameraExert.camera_image to detectorExert.detector_image;"), "Should contain flow connection");
    }
}
