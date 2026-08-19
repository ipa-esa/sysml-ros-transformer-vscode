import * as assert from 'assert';
import * as vscode from 'vscode';

suite('SysML ↔ ROS Transformer Extension Test Suite', () => {
    vscode.window.showInformationMessage('Start all tests.');

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
});
