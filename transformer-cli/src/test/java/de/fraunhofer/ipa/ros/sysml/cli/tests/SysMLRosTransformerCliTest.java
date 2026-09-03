//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.sysml.cli.tests;

import static org.junit.jupiter.api.Assertions.*;

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
    void testMultiFileForwardTransformationWithSelectiveOutput() throws Exception {
        Path multiComp = tempDir.resolve("multi_components.sysml");
        Path multiSys = tempDir.resolve("multi_system.sysml");
        Path unrelatedSys = tempDir.resolve("unrelated_system.sysml");

        copyResource("/multi_components.sysml", multiComp);
        copyResource("/multi_system.sysml", multiSys);
        copyResource("/unrelated_system.sysml", unrelatedSys);

        Path outDir = tempDir.resolve("multi-forward-out");

        // Execute forward transform targeting ONLY multi_system.sysml, with tempDir as workspace
        int exitCode = SysMLRosTransformerCli.run(new String[]{
            "--forward", multiSys.toString(),
            "--workspace", tempDir.toString(),
            "--output", outDir.toString()
        });

        assertEquals(0, exitCode, "Multi-file forward transformation should succeed");

        // Verify that multi_robot_system.rossystem was generated
        Path generatedFile = outDir.resolve("multi_robot_system.rossystem");
        assertTrue(Files.exists(generatedFile), "Generated multi_robot_system.rossystem should exist");

        String content = Files.readString(generatedFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("multi_robot_system:"), "Should contain multi_robot_system");
        assertTrue(content.contains("sensor_drivers.sensor_camera_driver"), "Should contain sensor_camera_driver node from companion file");
        assertTrue(content.contains("camera_image"), "Should contain camera_image interface from companion exert");

        // CRITICAL CHECK: unrelated_system.rossystem must NOT be generated even though unrelated_system.sysml exists in workspace
        Path unrelatedGeneratedFile = outDir.resolve("unrelated_system.rossystem");
        assertFalse(Files.exists(unrelatedGeneratedFile),
                "unrelated_system.rossystem MUST NOT be generated when multi_system.sysml was selected!");
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

    @Test
    void testMultiRos2ReverseTransformation() throws Exception {
        Path sensorsRos2 = tempDir.resolve("test_sensors.ros2");
        copyResource("/test_sensors.ros2", sensorsRos2);

        Path outDir = tempDir.resolve("multi-reverse-out");

        // Execute reverse transform with multiple .ros2 files passed
        int exitCode = SysMLRosTransformerCli.run(new String[]{
            "--reverse", rossystemFile.toString(),
            "--ros2", ros2File.toString(), sensorsRos2.toString(),
            "--output", outDir.toString()
        });

        assertEquals(0, exitCode, "Multi-ros2 reverse transformation should succeed");

        Path generatedFile = outDir.resolve("test_system_architecture.sysml");
        assertTrue(Files.exists(generatedFile), "Generated architecture .sysml should exist");

        String content = Files.readString(generatedFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("package test_system_architecture"), "Should define package");
        assertTrue(content.contains("Image"), "Should resolve Image message type");
    }
}
