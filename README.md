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

### Option 1: Install Pre-built `.vsix` from GitHub Actions / Releases

1. Download the latest `sysml-ros-transformer-vscode-1.0.0.vsix` from the GitHub Actions Artifacts or Releases page.
2. In VS Code, open the Extensions view (`Ctrl+Shift+X` / `Cmd+Shift+X`).
3. Click the **`...`** (Views and More Actions) menu in the top-right corner of the Extensions pane.
4. Select **Install from VSIX...** and choose the downloaded file.

Alternatively, from the command line:
```bash
code --install-extension sysml-ros-transformer-vscode-1.0.0.vsix
```

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
extension/build/vscode/sysml-ros-transformer-vscode-1.0.0.vsix
```

The standalone CLI Fat JAR will be located at:
```
transformer-cli/build/libs/sysml-ros-transformer-cli-1.0.0.jar
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
java -jar transformer-cli/build/libs/sysml-ros-transformer-cli-1.0.0.jar \
  --forward path/to/model.sysml \
  --output src-gen/

# Reverse transformation: .rossystem -> SysML
java -jar transformer-cli/build/libs/sysml-ros-transformer-cli-1.0.0.jar \
  --reverse path/to/system.rossystem \
  --ros2 path/to/nodes.ros2 \
  --output src-gen/
```

---

## GitHub Actions CI/CD Pipeline

- **Continuous Integration (`.github/workflows/ci.yml`)**:
  - Automatically compiles the Java CLI wrapper.
  - Runs JUnit 5 integration tests against SysML and RosSystem test fixtures.
  - Compiles the TypeScript extension and packages `sysml-ros-transformer-vscode-1.0.0.vsix`.
  - Publishes both the `.vsix` extension package and the CLI `.jar` as downloadable build artifacts on every push and pull request.
- **Automated Releases (`.github/workflows/release.yml`)**:
  - Automatically attaches the `.vsix` installer and standalone CLI JAR to GitHub Releases upon tag creation (`v*`).

---

## License

Copyright (c) 2022-2026 The CORESENSE Consortium.
Licensed under the [Apache License, Version 2.0](LICENSE).
