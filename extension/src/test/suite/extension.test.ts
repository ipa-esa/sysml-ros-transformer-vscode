import * as assert from 'assert';
import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import { ExtensionApi } from '../../extension';

suite('SysML ↔ ROS Transformer Extension Test Suite', () => {
    vscode.window.showInformationMessage('Starting SysML ↔ ROS Transformer tests.');

    let extensionApi: ExtensionApi;

    suiteSetup(async () => {
        const ext = vscode.extensions.getExtension<ExtensionApi>('ipa-esa.sysml-ros-transformer-vscode');
        assert.ok(ext, 'Extension ipa-esa.sysml-ros-transformer-vscode should be found');
        extensionApi = await ext.activate();
        assert.ok(extensionApi, 'Extension API should be returned on activation');
    });

    test('Extension should register commands', async () => {
        const allCommands = await vscode.commands.getCommands(true);

        assert.ok(
            allCommands.includes('sysml2ros.generateRosSystem'),
            'sysml2ros.generateRosSystem command should be registered'
        );

        assert.ok(
            allCommands.includes('ros2sysml.generateSysML'),
            'ros2sysml.generateSysML command should be registered'
        );
    });

    test('Configuration settings should have default values', () => {
        const config = vscode.workspace.getConfiguration('sysml-ros');
        assert.strictEqual(config.get('outputDirectory'), 'src-gen');
        assert.strictEqual(config.get('openGeneratedFile'), true);
        assert.strictEqual(config.get('java.home'), '');
    });

    test('TransformerRunner should resolve valid Java executable', async () => {
        assert.ok(extensionApi.runner, 'TransformerRunner should be available');
        const javaExec = await extensionApi.runner.getJavaExecutable();
        assert.ok(javaExec, 'Java executable path should be resolved');
    });

    test('sysml2ros.generateRosSystem command transforms single-file SysML model', async () => {
        const workspaceFolders = vscode.workspace.workspaceFolders;
        assert.ok(workspaceFolders && workspaceFolders.length > 0, 'Workspace folder should be open');
        const root = workspaceFolders[0].uri.fsPath;

        const sampleSysml = vscode.Uri.file(path.join(root, 'sample.sysml'));
        await vscode.commands.executeCommand('sysml2ros.generateRosSystem', sampleSysml);

        const expectedFile = path.join(root, 'src-gen', 'sample_robot_system.rossystem');
        assert.ok(fs.existsSync(expectedFile), 'Generated sample_robot_system.rossystem should exist');

        const content = fs.readFileSync(expectedFile, 'utf8');
        assert.ok(content.includes('sample_robot_system:'), 'Generated rossystem should contain system name');
        assert.ok(content.includes('robot_drivers.lidar_driver'), 'Generated rossystem should contain lidar_driver node');
    });

    test('sysml2ros.generateRosSystem command transforms multi-file SysML with selective output', async () => {
        const workspaceFolders = vscode.workspace.workspaceFolders;
        assert.ok(workspaceFolders && workspaceFolders.length > 0, 'Workspace folder should be open');
        const root = workspaceFolders[0].uri.fsPath;

        const multiSysml = vscode.Uri.file(path.join(root, 'multi_system.sysml'));
        await vscode.commands.executeCommand('sysml2ros.generateRosSystem', multiSysml);

        const expectedFile = path.join(root, 'src-gen', 'multi_robot_system.rossystem');
        assert.ok(fs.existsSync(expectedFile), 'Generated multi_robot_system.rossystem should exist');

        const content = fs.readFileSync(expectedFile, 'utf8');
        assert.ok(content.includes('multi_robot_system:'), 'Should contain multi_robot_system name');
        assert.ok(content.includes('robot_drivers.lidar_driver'), 'Should resolve component from companion multi_components.sysml');

        // CRITICAL CHECK: Verify other_system.rossystem was NOT generated even though other_system.sysml exists in workspace
        const unrelatedFile = path.join(root, 'src-gen', 'other_system.rossystem');
        assert.strictEqual(
            fs.existsSync(unrelatedFile),
            false,
            'other_system.rossystem must NOT be generated when multi_system.sysml was selected!'
        );
    });

    test('ros2sysml.generateSysML command transforms rossystem with ROS 2 models', async () => {
        const workspaceFolders = vscode.workspace.workspaceFolders;
        assert.ok(workspaceFolders && workspaceFolders.length > 0, 'Workspace folder should be open');
        const root = workspaceFolders[0].uri.fsPath;

        const sampleRosSystem = vscode.Uri.file(path.join(root, 'sample.rossystem'));
        await vscode.commands.executeCommand('ros2sysml.generateSysML', sampleRosSystem);

        const expectedFile = path.join(root, 'src-gen', 'sample_robot_system_architecture.sysml');
        assert.ok(fs.existsSync(expectedFile), 'Generated sample_robot_system_architecture.sysml should exist');

        const content = fs.readFileSync(expectedFile, 'utf8');
        assert.ok(content.includes('package sample_robot_system_architecture'), 'Should define architecture package');
        assert.ok(content.includes('LaserScan'), 'Should resolve LaserScan message type from sample_nodes.ros2');
    });
});
