# SysML ↔ ROS Model Transformer — VS Code Extension
[![Build & Package SysML ↔ ROS VS Code Extension](https://github.com/ipa-esa/sysml-ros-transformer-vscode/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/ipa-esa/sysml-ros-transformer-vscode/actions/workflows/ci.yml)
![Funded by the European Union](extension/images/EU_funded_en.jpg)

A lightweight, high-performance Visual Studio Code extension for bi-directional model-to-model (M2M) transformations between **SysML v2** cognitive architecture models and **RosTooling** (`.rossystem`) models.

---

## Features

- **Forward Transformation (SysML v2 $\to$ RosTooling)**:
  - Right-click any `.sysml` file in the Explorer or active editor $\to$ select **"Generate ROS System (.rossystem)"**.
  - Parses `@RosArtifactMapping`, `@RosSystemMapping`, `@RosTypeMapping`, `@RosInterfaceMapping`, and `@RosInterfaceHint` metadata annotations.
  - Produces complete, syntactically valid `.rossystem` models in `src-gen/`.
- **Reverse Transformation (RosTooling $\to$ SysML v2)**:
  - Right-click any `.rossystem` file $\to$ select **"Generate SysML Architecture (.sysml)"**.
  - Automatically resolves interface types from accompanying `.ros2` node definitions.
  - Generates SysML v2 models importing `CSCore` and `CSRosBridge` with full cognitive triad (`Modelet`, `Engine`, `Exert`) structures.
- **Syntax Highlighting**:
  - TextMate grammar highlighting for SysML v2 keywords, types, relations, and `@Ros*` bridge metadata annotations.
- **Real-Time Output Streaming**:
  - Dedicated **SysML ↔ ROS Transformer** output channel providing real-time execution feedback and transformation logs.
- **Interactive Notifications**:
  - Quick action prompts to instantly open generated files or reveal them in the file explorer.

---

## Architecture (Approach A)

This extension implements **Approach A** (Direct VS Code Extension via Lightweight CLI / Fat JAR):

```
sysml-ros-transformer-vscode/
├── transformer-cli/               # Standalone Java CLI Module
│   └── SysMLRosTransformerCli     # Entrypoint invoking Eclipse transformer dependencies
└── extension/                     # VS Code Extension Subproject (TypeScript)
    ├── src/extension.ts           # Extension activation & command handlers
    ├── src/transformerRunner.ts   # Child process manager & JDK detection
    └── server/
        └── sysml-ros-transformer-cli.jar  # Bundled standalone Fat JAR
```

- **Zero Code Duplication**: Directly consumes `de.fraunhofer.ipa.ros.sysml2rostooling` and `de.fraunhofer.ipa.ros.rostooling2sysml`. Any new feature added to the Eclipse transformer is immediately available.
- **Zero OSGi Overhead at Runtime**: Runs via lightweight Java CLI process without launching an Eclipse desktop instance or OSGi framework.

---

## Requirements

- **Visual Studio Code**: `v1.85.0` or later.
- **Java Runtime**: **JDK 17** or **JDK 21** installed and accessible via system `PATH` or configured via `sysml-ros.java.home`.

---

## Installation

You can install the pre-packaged `.vsix` extension directly without needing to build from source.

### 1. Download the `.vsix` Package

- **GitHub Releases (Recommended)**: Download `sysml-ros-transformer-vscode-*.vsix` attached to the latest release on the [GitHub Releases](https://github.com/ipa-esa/sysml-ros-transformer-vscode/releases) page.
- **GitHub Actions CI Artifacts**: For the latest development builds:
  1. Go to the [GitHub Actions](https://github.com/ipa-esa/sysml-ros-transformer-vscode/actions/workflows/ci.yml) tab.
  2. Click on the latest successful workflow run on `main` or your branch.
  3. Scroll down to the **Artifacts** section and download `sysml-ros-transformer-vscode` (extract the `.zip` archive to obtain the `.vsix` file).

---

### 2. Install into VS Code

#### Method A: Graphical Interface (GUI)

Works in **VS Code**, **VSCodium**, **Cursor**, **Eclipse Theia**, **Windsurf**, and other VS Code distributions:

1. Open your editor.
2. Open the **Extensions** view (`Ctrl+Shift+X` / `Cmd+Shift+X` or click the Extensions icon in the Activity Bar).
3. Click the **`···`** (Views and More Actions) menu in the top-right corner of the Extensions pane.
4. Select **Install from VSIX...** and select the downloaded `.vsix` file.

> **Tip:** Alternatively, open the Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`), type **`Extensions: Install from VSIX...`**, press Enter, and choose the `.vsix` file.

#### Method B: Command Line Interface (CLI)

Run the command corresponding to your editor from the terminal:

```bash
# Visual Studio Code
code --install-extension sysml-ros-transformer-vscode-1.0.1.vsix
```

>***Note:*** This plugin can be installed in other forks of VS Code, for example **VSCodium**, **Cursor**, **Antigravity**, etc. However, the extension has **only** been tested on VS Code and Antigravity.
---

### 3. Verify Installation

1. Verify that **SysML ↔ ROS Transformer** appears in your editor under **Installed Extensions**.
2. Ensure a compatible Java Runtime (JDK 17 or JDK 21) is installed on your machine.
3. Open any `.sysml` or `.rossystem` model file; right-click in the editor or Explorer to access the transformation commands.

---

## Building from Source

### Prerequisites
- JDK 21
- Node.js 20+ and npm 10+
- Gradle (or use the included `./gradlew`)

### Build Command
To build both the standalone Java CLI and package the VS Code `.vsix` extension:

```bash
./gradlew build vscodeExtension
```

The compiled extension `.vsix` will be located at:
```
extension/build/vscode/sysml-ros-transformer-vscode-1.0.1.vsix
```

The standalone CLI Fat JAR will be located at:
```
transformer-cli/build/libs/sysml-ros-transformer-cli-1.0.1.jar
```

---

## Running Tests

### Java CLI & Transformation Unit Tests
```bash
./gradlew :transformer-cli:test
```

### TypeScript Extension Compilation & Linting
```bash
./gradlew :extension:compileExtension
```

---

## Configuration Settings

You can customize the extension via VS Code Settings (`Ctrl+,` / `Cmd+,` $\to$ search `SysML ↔ ROS`):

| Setting | Default | Description |
| :--- | :--- | :--- |
| `sysml-ros.outputDirectory` | `"src-gen"` | Target folder (relative to workspace root) for generated files. Leave empty to generate next to source file. |
| `sysml-ros.openGeneratedFile` | `true` | Automatically open the generated model file in the editor upon successful transformation. |
| `sysml-ros.java.home` | `""` | Optional absolute path to JDK 17+ or 21+ home directory. Uses system Java if empty. |

---

## Standalone CLI Usage

The bundled CLI can also be run directly from any terminal or CI script:

```bash
# Forward transformation: SysML -> .rossystem
java -jar transformer-cli/build/libs/sysml-ros-transformer-cli-1.0.1.jar \
  --forward path/to/model.sysml \
  --output src-gen/

# Reverse transformation: .rossystem -> SysML
java -jar transformer-cli/build/libs/sysml-ros-transformer-cli-1.0.1.jar \
  --reverse path/to/system.rossystem \
  --ros2 path/to/nodes.ros2 \
  --output src-gen/
```

---

## GitHub Actions CI/CD Pipeline

- **Continuous Integration (`.github/workflows/ci.yml`)**:
  - Automatically compiles the Java CLI wrapper.
  - Runs JUnit 5 integration tests against SysML and RosSystem test fixtures.
  - Compiles the TypeScript extension and packages `sysml-ros-transformer-vscode-1.0.1.vsix`.
  - Publishes both the `.vsix` extension package and the CLI `.jar` as downloadable build artifacts on every push and pull request.
- **Automated Releases (`.github/workflows/release.yml`)**:
  - Automatically attaches the `.vsix` installer and standalone CLI JAR to GitHub Releases upon tag creation (`v*`).

---

## Changelog

### [1.0.1] - 2026-09-03

- **Backend CLI Updates (`SysMLRosTransformerCli.java`)**:
  - **Multi-File & Workspace Support**: Added support for passing multiple companion model files (`--models`, `--sysml <files...>`) and workspace paths (`--workspace <dir>`).
  - **Recursive Discovery**: Implemented recursive workspace and directory file discovery for `.sysml` and `.ros2`/`.ros` files.
  - **Strict Selective Output Guarantee**: In forward mode, all workspace `.sysml` files are parsed into the unified `SysMLModel`, but `transformer.transform(model, inputFile.getAbsolutePath())` filters strictly for the root annotated with `@RosSystemMapping` inside only the selected target file. Any other systems in the workspace are ignored for generation.
  - **Unified ROS 2 Resolution**: In reverse mode, all `.ros2` and `.ros` files across the project tree are combined into a unified model for full ROS 2 type resolution.
- **Extension Runner Updates (`transformerRunner.ts` & `extension.ts`)**:
  - **Automatic Workspace Discovery**:
    - In forward mode: automatically discovers all workspace `.sysml` files via `vscode.workspace.findFiles` and passes them to the CLI along with the workspace directory.
    - In reverse mode: automatically discovers all workspace `.ros2` and `.ros` files and passes them to the CLI.
  - **Harness Inspection**: Exported `ExtensionApi` from `activate()` for test harness inspection.
- **Automated Test Suites**:
  - **JUnit 5 CLI Backend Tests (`SysMLRosTransformerCliTest.java`)**:
    - Tested multi-file SysML parsing (`multi_system.sysml` referencing `multi_components.sysml`).
    - Verified that unrelated system models in the same directory (`unrelated_system.sysml`) are not generated.
    - Tested multi-file ROS 2 reverse transformation.
  - **VS Code Extension Integration Tests (`extension.test.ts`)**:
    - Runs directly inside the VS Code Electron test runner.
    - Verifies command registration and default configuration.
    - Verifies Java runtime detection via `TransformerRunner`.
    - Executes `sysml2ros.generateRosSystem` and confirms `.rossystem` generation with cross-file resolution and selective output filtering.
    - Executes `ros2sysml.generateSysML` and confirms reverse transformation with ROS 2 models.
- **Gradle & Build Integration**:
  - Added `testExtension` task to `extension/build.gradle` wired to `check.dependsOn testExtension`.
  - Bumped project version to `1.0.1` across root Gradle build, standalone Java CLI, and VS Code extension manifest.

### [1.0.0] - Initial Release

- **Initial release** of the SysML ↔ ROS Transformer VS Code extension and standalone Java CLI.
- **Bi-directional transformations**:
  - **Forward Transformation (SysML v2 $\to$ RosTooling)**: Generates `.rossystem` models from `.sysml` files with full support for `@Ros*` bridge metadata annotations (`@RosArtifactMapping`, `@RosSystemMapping`, `@RosTypeMapping`, `@RosInterfaceMapping`, `@RosInterfaceHint`).
  - **Reverse Transformation (RosTooling $\to$ SysML v2)**: Converts `.rossystem` models and `.ros2` node definitions into SysML v2 models importing `CSCore` and `CSRosBridge` with complete cognitive triad structures (`Modelet`, `Engine`, `Exert`).
- **VS Code Extension Features**:
  - Context menu integration for `.sysml` and `.rossystem` files in File Explorer and Editor.
  - Dedicated TextMate syntax highlighting for SysML v2 and CoreSense ROS bridge annotations.
  - Streaming output channel for real-time transformation logging and feedback.
  - Interactive notification prompts to open generated models immediately.
  - Configurable settings for output directories, auto-open behavior, and custom JDK path.
- **Standalone Java CLI**:
  - Standalone Shadow Fat JAR for command-line and automated execution without OSGi / Eclipse desktop overhead.
- **CI/CD Integration**:
  - GitHub Actions workflows for automated build, JUnit test validation, packaging, and release artifact distribution.

---

## License

Copyright (c) 2022-2026 The CORESENSE Consortium.
Licensed under the [Apache License, Version 2.0](LICENSE).
