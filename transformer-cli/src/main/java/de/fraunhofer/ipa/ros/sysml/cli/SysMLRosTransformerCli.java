//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.sysml.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
 * Supports multi-file SysML parsing and project-wide ROS 2 model discovery.
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
        List<String> extraSysmlFiles = new ArrayList<>();
        List<String> ros2Files = new ArrayList<>();
        String workspaceDir = null;
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
                case "--sysml":
                case "--models":
                    while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        extraSysmlFiles.add(args[++i]);
                    }
                    break;
                case "--ros2":
                    while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        ros2Files.add(args[++i]);
                    }
                    break;
                case "-w":
                case "--workspace":
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        workspaceDir = args[++i];
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
                return executeForward(inFile, extraSysmlFiles, workspaceDir, outputDir, toStdout);
            } else if ("reverse".equalsIgnoreCase(mode)) {
                return executeReverse(inFile, ros2Files, workspaceDir, outputDir, toStdout);
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

    private static int executeForward(File inputFile, List<String> extraSysmlFiles, String workspaceDir,
                                      String outputDir, boolean toStdout) throws IOException {
        Set<String> allSysmlFiles = new LinkedHashSet<>();
        allSysmlFiles.add(inputFile.getAbsolutePath());

        // Add explicitly specified companion files
        for (String extra : extraSysmlFiles) {
            File f = new File(extra);
            if (f.exists() && f.isFile() && extra.endsWith(".sysml")) {
                allSysmlFiles.add(f.getAbsolutePath());
            }
        }

        // Discover companion .sysml files in workspace or parent directory if needed
        File searchRoot = (workspaceDir != null && !workspaceDir.isBlank())
                ? new File(workspaceDir)
                : inputFile.getParentFile();

        if (searchRoot != null && searchRoot.exists()) {
            List<String> discovered = discoverFiles(searchRoot, ".sysml");
            allSysmlFiles.addAll(discovered);
        }

        System.out.println("[INFO] Parsing " + allSysmlFiles.size() + " SysML file(s) for model resolution.");

        SysMLParser parser = new SysMLParser();
        SysMLModel model = parser.parse(new ArrayList<>(allSysmlFiles));

        // Transform ONLY the system defined in the selected inputFile
        SysML2RosSystemTransformer transformer = new SysML2RosSystemTransformer();
        List<RosSystemResult> results = transformer.transform(model, inputFile.getAbsolutePath());

        if (results.isEmpty()) {
            System.err.println("[WARNING] No @RosSystemMapping found in selected SysML file: " + inputFile.getName());
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

    private static int executeReverse(File inputFile, List<String> ros2Files, String workspaceDir,
                                      String outputDir, boolean toStdout) throws IOException {
        String rossystemContent = Files.readString(inputFile.toPath(), StandardCharsets.UTF_8);

        Set<String> allRosModelFiles = new LinkedHashSet<>();

        // Add explicitly specified ROS model files
        for (String r2 : ros2Files) {
            File f = new File(r2);
            if (f.exists() && f.isFile()) {
                allRosModelFiles.add(f.getAbsolutePath());
            }
        }

        // Discover all .ros2 and .ros files in workspace or parent directory
        File searchRoot = (workspaceDir != null && !workspaceDir.isBlank())
                ? new File(workspaceDir)
                : (inputFile.getParentFile() != null ? inputFile.getParentFile() : new File("."));

        if (searchRoot.exists()) {
            List<String> discovered = discoverFiles(searchRoot, ".ros2", ".ros");
            allRosModelFiles.addAll(discovered);
        }

        // Combine contents of all discovered ROS model files
        StringBuilder ros2Combined = new StringBuilder();
        for (String modelPath : allRosModelFiles) {
            try {
                ros2Combined.append(Files.readString(Paths.get(modelPath), StandardCharsets.UTF_8)).append("\n");
            } catch (Exception ignored) {}
        }

        if (!allRosModelFiles.isEmpty()) {
            System.out.println("[INFO] Loaded " + allRosModelFiles.size() + " ROS 2 / ROS model file(s) for type resolution.");
        }

        RosSystem2SysMLTransformer transformer = new RosSystem2SysMLTransformer();
        SysMLResult result = transformer.transformText(rossystemContent, ros2Combined.toString());

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

    /**
     * Recursively discovers files matching given extensions while skipping build, dot, and dependency folders.
     */
    public static List<String> discoverFiles(File searchDir, String... extensions) {
        List<String> results = new ArrayList<>();
        if (searchDir == null || !searchDir.exists() || !searchDir.isDirectory()) {
            return results;
        }

        try {
            Files.walkFileTree(searchDir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (name.startsWith(".") || "build".equals(name) || "target".equals(name)
                            || "bin".equals(name) || "node_modules".equals(name) || "out".equals(name)
                            || "dist".equals(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    for (String ext : extensions) {
                        if (fileName.endsWith(ext)) {
                            results.add(file.toAbsolutePath().toString());
                            break;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {}

        return results;
    }

    private static void printHelp() {
        System.out.println("SysML ↔ ROS Model Transformer CLI (Approach A)");
        System.out.println("Usage:");
        System.out.println("  java -jar sysml-ros-transformer-cli.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -f, --forward <file.sysml>       Target SysML model file to transform (.sysml -> .rossystem)");
        System.out.println("  -r, --reverse <file.rossystem>   Target RosSystem model file to transform (.rossystem -> SysML)");
        System.out.println("      --models, --sysml <files...> Additional companion .sysml files for cross-file resolution");
        System.out.println("      --ros2 <files...>            .ros2 / .ros model files for reverse transformation type resolution");
        System.out.println("  -w, --workspace <directory>      Workspace root directory for automatic model discovery");
        System.out.println("  -o, --output <directory>         Target directory for output files (default: adjacent)");
        System.out.println("      --stdout                     Output generated text directly to stdout");
        System.out.println("  -v, --version                    Display version information");
        System.out.println("  -h, --help                       Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  sysml-ros-transformer-cli --forward system.sysml --models components.sysml -o src-gen/");
        System.out.println("  sysml-ros-transformer-cli --reverse system.rossystem --ros2 nodes.ros2 -o src-gen/");
    }
}
