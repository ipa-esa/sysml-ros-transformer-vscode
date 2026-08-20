# SysML ↔ ROS Transformer
![Funded by the European Union](images/EU_funded_en.jpg)

A lightweight Visual Studio Code extension providing bi-directional Model-to-Model (M2M) transformations between **SysML v2** cognitive architecture models and **RosTooling** (`.rossystem`) models for the CoreSense project.

---

## Features

- **Forward Transformation (SysML v2 $\to$ RosTooling)**:
  - Right-click any `.sysml` file in the File Explorer or active editor and select **"Generate ROS System (.rossystem)"**.
  - Automatically parses `@RosArtifactMapping`, `@RosSystemMapping`, `@RosTypeMapping`, `@RosInterfaceMapping`, and `@RosInterfaceHint` metadata annotations.
  - Generates ready-to-use `.rossystem` models in the workspace `src-gen/` folder.
- **Reverse Transformation (RosTooling $\to$ SysML v2)**:
  - Right-click any `.rossystem` file in the File Explorer or active editor and select **"Generate SysML Architecture (.sysml)"**.
  - Automatically resolves interface types from accompanying `.ros2` node definitions.
  - Produces valid SysML v2 architecture models importing `CSCore` and `CSRosBridge` with complete cognitive triad structures (`Modelet`, `Engine`, `Exert`).
- **SysML v2 Syntax Highlighting**:
  - TextMate grammar highlighting for SysML v2 keywords (`package`, `part def`, `action def`, `perform`, `flow`, etc.) and `@Ros*` bridge annotations.
- **Real-Time Output Streaming**:
  - Dedicated **SysML ↔ ROS Transformer** output channel providing real-time execution feedback and transformation logs.
- **Interactive Notifications**:
  - Quick action prompts upon completion to immediately open the generated file or reveal it in the file explorer.

---

## Requirements

- **Visual Studio Code**: `v1.85.0` or later.
- **Java Runtime**: **JDK 17** or **JDK 21** installed and accessible via system `PATH`, `JAVA_HOME`, or configured via `sysml-ros.java.home`.

---

## Extension Settings

This extension contributes the following settings (`Ctrl+,` / `Cmd+,` $\to$ search `SysML ↔ ROS`):

* `sysml-ros.outputDirectory`: Target folder name (relative to workspace root) for generated files. Default is `"src-gen"`. Leave empty to generate next to the source file.
* `sysml-ros.openGeneratedFile`: Automatically open the generated model file in the editor upon successful transformation. Default is `true`.
* `sysml-ros.java.home`: Absolute path to a JDK 17+ or JDK 21+ home directory. Leave empty to use system default Java.

---

## Known Issues

* If Java is not detected in your system `PATH` or `JAVA_HOME`, configure the JDK location using the `sysml-ros.java.home` setting.
* For reverse transformation (`.rossystem` $\to$ SysML), ensure accompanying `.ros2` node artifacts are located in the same directory or specified via configuration.

---

## Release Notes

### 1.0.0

* **Initial release** of the SysML ↔ ROS Transformer extension.
* **Bi-directional transformations**:
  * SysML v2 $\to$ `.rossystem` (Forward transformation with `@Ros*` bridge metadata support).
  * `.rossystem` $\to$ SysML v2 (Reverse transformation with `CSCore` and `CSRosBridge` mapping).
* **Context menu integration**: Right-click actions in Explorer and Editor context menus.
* **TextMate syntax highlighting**: Dedicated syntax coloring for SysML v2 and CoreSense ROS bridge annotations.
* **Output Channel & Notifications**: Integrated progress logging and auto-open functionality.
* **Configurable output directory**: Defaults to `src-gen/`.

---

## License

Copyright (c) 2022-2026 The CORESENSE Consortium.
Licensed under the [Apache License, Version 2.0](LICENSE).
