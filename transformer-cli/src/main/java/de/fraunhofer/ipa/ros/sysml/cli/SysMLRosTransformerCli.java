//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.sysml.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import de.fraunhofer.ipa.ros.sysml2rostooling.parser.SysMLParser;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLModel;
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer;
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer.RosSystemResult;
import de.fraunhofer.ipa.ros.sysml2rostooling.generator.RosSystemTextGenerator;

import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer;
import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer.SysMLResult;
import de.fraunhofer.ipa.ros.rostooling2sysml.generator.SysMLTextGenerator;

/**
 * Standalone Command-Line Interface for Bi-directional SysML ↔ RosTooling Transformations.
 */
public class SysMLRosTransformerCli {

    public static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args) {
        if (args == null || args.length == 0) {
            printHelp();
            return 1;
        }

        String mode = null; // "forward" or "reverse"
        String inputFile = null;
        String ros2File = null;
        String outputDir = null;
        boolean toStdout = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h":
                case "--help":
                    printHelp();
                    return 0;
                case "-v":
                case "--version":
                    System.out.println("SysML ↔ ROS Transformer CLI v" + VERSION);
                    return 0;
                case "-f":
                case "--forward":
                    mode = "forward";
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        inputFile = args[++i];
                    }
                    break;
                case "-r":
                case "--reverse":
                    mode = "reverse";
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        inputFile = args[++i];
                    }
                    break;
                case "--ros2":
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        ros2File = args[++i];
                    }
                    break;
                case "-o":
                case "--output":
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        outputDir = args[++i];
                    }
                    break;
                case "--stdout":
                    toStdout = true;
                    break;
                default:
                    if (inputFile == null && !arg.startsWith("-")) {
                        inputFile = arg;
                    } else {
                        System.err.println("[ERROR] Unrecognized argument: " + arg);
                        printHelp();
                        return 1;
                    }
                    break;
            }
        }

        if (mode == null) {
            if (inputFile != null) {
                if (inputFile.endsWith(".sysml")) {
                    mode = "forward";
                } else if (inputFile.endsWith(".rossystem")) {
                    mode = "reverse";
                } else {
                    System.err.println("[ERROR] Unable to determine mode from file extension. Specify --forward or --reverse.");
                    return 1;
                }
            } else {
                System.err.println("[ERROR] No input file specified.");
                printHelp();
                return 1;
            }
        }

        if (inputFile == null) {
            System.err.println("[ERROR] No input file specified for " + mode + " transformation.");
            return 1;
        }

        File inFile = new File(inputFile);
        if (!inFile.exists() || !inFile.isFile()) {
            System.err.println("[ERROR] Input file does not exist: " + inFile.getAbsolutePath());
            return 1;
        }

        try {
            if ("forward".equalsIgnoreCase(mode)) {
                return executeForward(inFile, outputDir, toStdout);
            } else if ("reverse".equalsIgnoreCase(mode)) {
                return executeReverse(inFile, ros2File, outputDir, toStdout);
            } else {
                System.err.println("[ERROR] Invalid mode: " + mode);
                return 1;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Transformation failed: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        }
    }

    private static int executeForward(File inputFile, String outputDir, boolean toStdout) throws IOException {
        SysMLParser parser = new SysMLParser();
        SysMLModel model = parser.parse(List.of(inputFile.getAbsolutePath()));

        SysML2RosSystemTransformer transformer = new SysML2RosSystemTransformer();
        List<RosSystemResult> results = transformer.transform(model);

        if (results.isEmpty()) {
            System.err.println("[WARNING] No systems found in SysML model.");
            return 0;
        }

        RosSystemTextGenerator generator = new RosSystemTextGenerator();

        for (RosSystemResult result : results) {
            CharSequence generatedText = generator.generate(result);
            String outputFileName = result.getSystemName() + ".rossystem";

            if (toStdout) {
                System.out.println(generatedText);
            } else {
                Path targetPath;
                if (outputDir != null && !outputDir.isBlank()) {
                    Path outDir = Paths.get(outputDir);
                    Files.createDirectories(outDir);
                    targetPath = outDir.resolve(outputFileName);
                } else {
                    targetPath = inputFile.toPath().getParent() != null
                            ? inputFile.toPath().getParent().resolve(outputFileName)
                            : Paths.get(outputFileName);
                }

                Files.writeString(targetPath, generatedText, StandardCharsets.UTF_8);
                System.out.println("[SUCCESS] Generated .rossystem file: " + targetPath.toAbsolutePath());
            }
        }

        return 0;
    }

    private static int executeReverse(File inputFile, String ros2FilePath, String outputDir, boolean toStdout) throws IOException {
        String rossystemContent = Files.readString(inputFile.toPath(), StandardCharsets.UTF_8);
        String ros2Content = null;

        if (ros2FilePath != null && !ros2FilePath.isBlank()) {
            File ros2File = new File(ros2FilePath);
            if (ros2File.exists() && ros2File.isFile()) {
                ros2Content = Files.readString(ros2File.toPath(), StandardCharsets.UTF_8);
            } else {
                System.err.println("[WARNING] Specified .ros2 file not found: " + ros2FilePath);
            }
        } else {
            // Check for adjacent or nearby .ros2 files in parent or siblings
            File parentDir = inputFile.getParentFile();
            if (parentDir != null) {
                File[] matchingRos2 = parentDir.listFiles((dir, name) -> name.endsWith(".ros2"));
                if (matchingRos2 != null && matchingRos2.length > 0) {
                    ros2Content = Files.readString(matchingRos2[0].toPath(), StandardCharsets.UTF_8);
                }
            }
        }

        RosSystem2SysMLTransformer transformer = new RosSystem2SysMLTransformer();
        SysMLResult result = transformer.transformText(rossystemContent, ros2Content);

        SysMLTextGenerator generator = new SysMLTextGenerator();
        CharSequence generatedText = generator.generate(result);

        String outputFileName = (result.systemName != null ? result.systemName : "system") + "_architecture.sysml";

        if (toStdout) {
            System.out.println(generatedText);
        } else {
            Path targetPath;
            if (outputDir != null && !outputDir.isBlank()) {
                Path outDir = Paths.get(outputDir);
                Files.createDirectories(outDir);
                targetPath = outDir.resolve(outputFileName);
            } else {
                targetPath = inputFile.toPath().getParent() != null
                        ? inputFile.toPath().getParent().resolve(outputFileName)
                        : Paths.get(outputFileName);
            }

            Files.writeString(targetPath, generatedText, StandardCharsets.UTF_8);
            System.out.println("[SUCCESS] Generated SysML architecture file: " + targetPath.toAbsolutePath());
        }

        return 0;
    }

    private static void printHelp() {
        System.out.println("SysML ↔ ROS Model Transformer CLI (Approach A)");
        System.out.println("Usage:");
        System.out.println("  java -jar sysml-ros-transformer-cli.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -f, --forward <file.sysml>       Perform forward transformation (SysML -> .rossystem)");
        System.out.println("  -r, --reverse <file.rossystem>   Perform reverse transformation (.rossystem -> SysML)");
        System.out.println("      --ros2 <file.ros2>           Optional .ros2 model for reverse transformation type resolution");
        System.out.println("  -o, --output <directory>         Target directory for output files (default: adjacent)");
        System.out.println("      --stdout                     Output generated text directly to stdout");
        System.out.println("  -v, --version                    Display version information");
        System.out.println("  -h, --help                       Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  sysml-ros-transformer-cli --forward path/to/model.sysml -o src-gen/");
        System.out.println("  sysml-ros-transformer-cli --reverse path/to/system.rossystem --ros2 path/to/nodes.ros2 -o src-gen/");
    }
}
