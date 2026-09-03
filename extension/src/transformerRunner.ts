//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import * as cp from 'child_process';

export interface TransformOptions {
    mode: 'forward' | 'reverse';
    inputUri: vscode.Uri;
    ros2Uri?: vscode.Uri;
}

export class TransformerRunner {
    private outputChannel: vscode.OutputChannel;
    private context: vscode.ExtensionContext;

    constructor(context: vscode.ExtensionContext, outputChannel: vscode.OutputChannel) {
        this.context = context;
        this.outputChannel = outputChannel;
    }

    /**
     * Resolves the path to the bundled transformer CLI Fat JAR.
     */
    private getJarPath(): string {
        const jarPath = this.context.asAbsolutePath(path.join('server', 'sysml-ros-transformer-cli.jar'));
        if (!fs.existsSync(jarPath)) {
            // Fallback for development mode
            const devJarPath = path.resolve(
                this.context.extensionPath,
                '..',
                'transformer-cli',
                'build',
                'libs',
                'sysml-ros-transformer-cli-1.0.1.jar'
            );
            if (fs.existsSync(devJarPath)) {
                return devJarPath;
            }
        }
        return jarPath;
    }

    /**
     * Finds and verifies the Java executable path (JDK 17+ or 21+).
     */
    public async getJavaExecutable(): Promise<string> {
        const config = vscode.workspace.getConfiguration('sysml-ros');
        const configuredJavaHome = config.get<string>('java.home');

        let javaExec = 'java';
        if (configuredJavaHome && configuredJavaHome.trim().length > 0) {
            javaExec = path.join(configuredJavaHome.trim(), 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
        } else if (process.env.JAVA_HOME) {
            javaExec = path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
        }

        const isValid = await this.verifyJavaVersion(javaExec);
        if (!isValid) {
            const msg = 'Java 17 or higher is required to run the SysML ↔ ROS Transformer. Please install JDK 17/21 or set "sysml-ros.java.home".';
            vscode.window.showErrorMessage(msg, 'Open Settings').then(selection => {
                if (selection === 'Open Settings') {
                    vscode.commands.executeCommand('workbench.action.openSettings', 'sysml-ros.java.home');
                }
            });
            throw new Error(msg);
        }

        return javaExec;
    }

    private verifyJavaVersion(javaExecutable: string): Promise<boolean> {
        return new Promise((resolve) => {
            cp.exec(`"${javaExecutable}" -version`, (error, stdout, stderr) => {
                if (error) {
                    resolve(false);
                    return;
                }
                const output = stdout.toString() + stderr.toString();
                // Match version strings like "21.0.1", "17.0.2", "1.8.0"
                const match = output.match(/version "(?:1\.)?(\d+)/);
                if (match && match[1]) {
                    const major = parseInt(match[1], 10);
                    resolve(major >= 17);
                    return;
                }
                resolve(true); // Fallback if pattern matching fails but execution succeeded
            });
        });
    }

    /**
     * Executes the transformation CLI on the given input file.
     */
    public async runTransformation(options: TransformOptions): Promise<void> {
        const jarPath = this.getJarPath();
        if (!fs.existsSync(jarPath)) {
            const err = `Transformer CLI JAR not found at: ${jarPath}. Please run the Gradle build first.`;
            this.outputChannel.appendLine(`[ERROR] ${err}`);
            vscode.window.showErrorMessage(err);
            return;
        }

        const javaExec = await this.getJavaExecutable();
        const inputFilePath = options.inputUri.fsPath;

        // Determine destination output directory
        const config = vscode.workspace.getConfiguration('sysml-ros');
        const outputDirSetting = config.get<string>('outputDirectory', 'src-gen');
        const openGenerated = config.get<boolean>('openGeneratedFile', true);

        let targetOutputDir: string;
        const workspaceFolder = vscode.workspace.getWorkspaceFolder(options.inputUri);

        if (outputDirSetting && outputDirSetting.trim().length > 0) {
            if (workspaceFolder) {
                targetOutputDir = path.join(workspaceFolder.uri.fsPath, outputDirSetting.trim());
            } else {
                targetOutputDir = path.join(path.dirname(inputFilePath), outputDirSetting.trim());
            }
        } else {
            targetOutputDir = path.dirname(inputFilePath);
        }

        // Build CLI arguments
        const args: string[] = ['-jar', jarPath];
        if (options.mode === 'forward') {
            args.push('--forward', inputFilePath);

            // Discover companion .sysml files in workspace
            const sysmlFiles = await vscode.workspace.findFiles(
                '**/*.sysml',
                '{**/node_modules/**,**/dist/**,**/out/**,**/build/**,**/target/**}'
            );
            const companionFiles = sysmlFiles
                .map(uri => uri.fsPath)
                .filter(p => path.resolve(p) !== path.resolve(inputFilePath));

            if (companionFiles.length > 0) {
                args.push('--models', ...companionFiles);
            }
            if (workspaceFolder) {
                args.push('--workspace', workspaceFolder.uri.fsPath);
            }
        } else {
            args.push('--reverse', inputFilePath);

            // Discover all .ros2 and .ros files in workspace
            const rosFiles = await vscode.workspace.findFiles(
                '**/*.{ros2,ros}',
                '{**/node_modules/**,**/dist/**,**/out/**,**/build/**,**/target/**}'
            );
            const rosFilePaths = new Set<string>();
            if (options.ros2Uri) {
                rosFilePaths.add(options.ros2Uri.fsPath);
            }
            for (const file of rosFiles) {
                rosFilePaths.add(file.fsPath);
            }
            if (rosFilePaths.size > 0) {
                args.push('--ros2', ...Array.from(rosFilePaths));
            }
            if (workspaceFolder) {
                args.push('--workspace', workspaceFolder.uri.fsPath);
            }
        }
        args.push('--output', targetOutputDir);

        this.outputChannel.show(true);
        this.outputChannel.appendLine(`\n------------------------------------------------------------`);
        this.outputChannel.appendLine(`[${new Date().toLocaleTimeString()}] Starting ${options.mode.toUpperCase()} transformation...`);
        this.outputChannel.appendLine(`Input file: ${inputFilePath}`);
        this.outputChannel.appendLine(`Output dir: ${targetOutputDir}`);
        this.outputChannel.appendLine(`Executing: ${javaExec} ${args.join(' ')}`);

        return new Promise<void>((resolve, reject) => {
            const process = cp.spawn(javaExec, args);

            let stdoutData = '';
            let stderrData = '';

            process.stdout.on('data', (chunk) => {
                const text = chunk.toString();
                stdoutData += text;
                this.outputChannel.append(text);
            });

            process.stderr.on('data', (chunk) => {
                const text = chunk.toString();
                stderrData += text;
                this.outputChannel.append(text);
            });

            process.on('close', async (code) => {
                if (code === 0) {
                    this.outputChannel.appendLine(`[SUCCESS] Transformation completed successfully!`);

                    // Try to extract generated file path from CLI output
                    const match = stdoutData.match(/Generated (?:[^\s]+) file: (.+)/);
                    let generatedFilePath: string | undefined;
                    if (match && match[1]) {
                        generatedFilePath = match[1].trim();
                    }

                    const actionOpen = 'Open File';
                    const actionReveal = 'Reveal in Explorer';
                    const prompt = generatedFilePath
                        ? `Transformation complete! Generated: ${path.basename(generatedFilePath)}`
                        : `Transformation complete! Output saved to: ${targetOutputDir}`;

                    vscode.window.showInformationMessage(prompt, actionOpen, actionReveal).then(async (selection) => {
                        if (selection === actionOpen && generatedFilePath && fs.existsSync(generatedFilePath)) {
                            const doc = await vscode.workspace.openTextDocument(generatedFilePath);
                            await vscode.window.showTextDocument(doc);
                        } else if (selection === actionReveal && generatedFilePath && fs.existsSync(generatedFilePath)) {
                            await vscode.commands.executeCommand('revealInExplorer', vscode.Uri.file(generatedFilePath));
                        }
                    });

                    if (openGenerated && generatedFilePath && fs.existsSync(generatedFilePath)) {
                        try {
                            const doc = await vscode.workspace.openTextDocument(generatedFilePath);
                            await vscode.window.showTextDocument(doc, { preview: false });
                        } catch {
                            // Non-critical if auto-open fails
                        }
                    }

                    resolve();
                } else {
                    const errMsg = `Transformation failed with exit code ${code}. Check output channel for details.`;
                    this.outputChannel.appendLine(`[ERROR] ${errMsg}`);
                    vscode.window.showErrorMessage(errMsg);
                    reject(new Error(errMsg));
                }
            });

            process.on('error', (err) => {
                const errMsg = `Failed to start Java process: ${err.message}`;
                this.outputChannel.appendLine(`[ERROR] ${errMsg}`);
                vscode.window.showErrorMessage(errMsg);
                reject(err);
            });
        });
    }
}
