//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

import * as vscode from 'vscode';
import { TransformerRunner } from './transformerRunner';

export interface ExtensionApi {
    runner: TransformerRunner;
    context: vscode.ExtensionContext;
}

export function activate(context: vscode.ExtensionContext): ExtensionApi {
    console.log('[SysML ↔ ROS Transformer] Activating extension...');
    const outputChannel = vscode.window.createOutputChannel('SysML ↔ ROS Transformer');
    context.subscriptions.push(outputChannel);

    const runner = new TransformerRunner(context, outputChannel);

    // Command: SysML -> .rossystem (Forward transformation)
    const forwardCmd = vscode.commands.registerCommand(
        'sysml2ros.generateRosSystem',
        async (uri?: vscode.Uri) => {
            const targetUri = getTargetUri(uri, '.sysml');
            if (!targetUri) {
                vscode.window.showWarningMessage('Please select a valid .sysml file to transform.');
                return;
            }

            try {
                await runner.runTransformation({
                    mode: 'forward',
                    inputUri: targetUri
                });
            } catch (err) {
                // Errors already logged in runner
            }
        }
    );

    // Command: .rossystem -> SysML (Reverse transformation)
    const reverseCmd = vscode.commands.registerCommand(
        'ros2sysml.generateSysML',
        async (uri?: vscode.Uri) => {
            const targetUri = getTargetUri(uri, '.rossystem');
            if (!targetUri) {
                vscode.window.showWarningMessage('Please select a valid .rossystem file to transform.');
                return;
            }

            try {
                await runner.runTransformation({
                    mode: 'reverse',
                    inputUri: targetUri
                });
            } catch (err) {
                // Errors already logged in runner
            }
        }
    );

    context.subscriptions.push(forwardCmd);
    context.subscriptions.push(reverseCmd);

    outputChannel.appendLine('SysML ↔ ROS Transformer extension is now active.');
    return { runner, context };
}

export function deactivate() {
    // Cleanup if needed
}

function getTargetUri(uri: vscode.Uri | undefined, expectedExt: string): vscode.Uri | undefined {
    if (uri && uri.fsPath && uri.fsPath.endsWith(expectedExt)) {
        return uri;
    }

    const activeEditor = vscode.window.activeTextEditor;
    if (activeEditor && activeEditor.document.uri.fsPath.endsWith(expectedExt)) {
        return activeEditor.document.uri;
    }

    return undefined;
}
