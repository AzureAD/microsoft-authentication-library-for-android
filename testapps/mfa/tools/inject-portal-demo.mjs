// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Opt-in USB demo helper: never returns page data, activation links or credentials.
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { parseArgs } from 'node:util';

const supportedOrigin = 'https://mysignins.microsoft.com';
const controller = new AbortController();
let port;
let adb;
let deviceArgs;
let deadline;
const connections = new Set();

function runAdb(args) {
    const result = spawnSync(adb, [...deviceArgs, ...args], {
        encoding: 'utf8', timeout: 10000, maxBuffer: 65536, windowsHide: true, shell: false
    });
    if (result.error || result.status !== 0) throw new Error('ADB_FAILED');
    return result.stdout.trim();
}

function allowedTarget(target) {
    if (target.type !== 'page' || typeof target.url !== 'string') return false;
    try { return new URL(target.url).origin === supportedOrigin; } catch (_) { return false; }
}

async function connect(url) {
    controller.signal.throwIfAborted();
    const endpoint = new URL(url);
    if (endpoint.protocol !== 'ws:' || endpoint.hostname !== '127.0.0.1' ||
        endpoint.port !== String(port) || endpoint.username || endpoint.password ||
        endpoint.search || endpoint.hash || !/^\/devtools\/page\/[A-Za-z0-9_-]+$/.test(endpoint.pathname)) {
        throw new Error('UNEXPECTED_DEBUG_ENDPOINT');
    }
    const socket = new WebSocket(endpoint);
    connections.add(socket);
    let nextId = 0;
    const requests = new Map();
    const fail = () => {
        for (const request of requests.values()) {
            clearTimeout(request.timer);
            request.reject(new Error('DEBUG_CONNECTION_CLOSED'));
        }
        requests.clear();
    };
    const abort = () => { fail(); socket.close(); };
    controller.signal.addEventListener('abort', abort, { once: true });
    socket.addEventListener('close', () => {
        fail();
        connections.delete(socket);
        controller.signal.removeEventListener('abort', abort);
    });
    socket.addEventListener('error', fail);
    socket.addEventListener('message', event => {
        // No event forwarding or logging. CDP domains with credential-bearing events are never enabled.
        if (typeof event.data !== 'string' || event.data.length > 1048576) { abort(); return; }
        let message;
        try { message = JSON.parse(event.data); } catch (_) { abort(); return; }
        const request = requests.get(message.id);
        if (!request) return;
        requests.delete(message.id);
        clearTimeout(request.timer);
        if (message.error || message.result?.exceptionDetails) request.reject(new Error('EVALUATION_FAILED'));
        else request.resolve(message.result);
    });
    await new Promise((resolve, reject) => {
        const timer = setTimeout(() => { reject(new Error('CONNECT_TIMEOUT')); socket.close(); }, 5000);
        socket.addEventListener('open', () => { clearTimeout(timer); resolve(); }, { once: true });
        socket.addEventListener('error', () => { clearTimeout(timer); reject(new Error('CONNECT_FAILED')); }, { once: true });
        socket.addEventListener('close', () => { clearTimeout(timer); reject(new Error('CONNECT_CLOSED')); }, { once: true });
    });
    return {
        close: () => socket.close(),
        async evaluateBoolean(expression) {
            controller.signal.throwIfAborted();
            const id = ++nextId;
            const result = await new Promise((resolve, reject) => {
                const timer = setTimeout(() => {
                    requests.delete(id);
                    reject(new Error('EVALUATION_TIMEOUT'));
                }, 6000);
                requests.set(id, { resolve, reject, timer });
                socket.send(JSON.stringify({
                    id, method: 'Runtime.evaluate', params: {
                        expression, returnByValue: true, silent: true, timeout: 5000, awaitPromise: false
                    }
                }));
            });
            if (result?.result?.type !== 'boolean') throw new Error('NON_BOOLEAN_RESULT');
            return result.result.value === true;
        }
    };
}

try {
    const { values } = parseArgs({ options: {
        serial: { type: 'string' }, package: { type: 'string' }, adb: { type: 'string', default: 'adb' },
        socket: { type: 'string', default: 'chrome_devtools_remote' }, help: { type: 'boolean' }
    }, strict: true });
    if (values.help) {
        console.log('Usage: node inject-portal-demo.mjs --package <installed-app-package> ' +
            '[--serial <USB-device-serial>] [--adb <adb-executable>] [--socket <Chrome-debug-socket>]');
        console.log('Requires built-in Node WebSocket, authorized USB debugging and one visible supported registration tab.');
    } else {
        if (typeof WebSocket !== 'function' || !values.package ||
            !/^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$/.test(values.package) ||
            values.package.length > 255 || !/^[A-Za-z0-9_.-]{1,128}$/.test(values.socket) ||
            (values.serial && !/^[A-Za-z0-9_.:-]{1,256}$/.test(values.serial))) throw new Error('INVALID_OPTIONS');
        adb = values.adb;
        deviceArgs = values.serial ? ['-s', values.serial] : [];
        process.once('SIGINT', () => controller.abort());
        process.once('SIGTERM', () => controller.abort());
        deadline = setTimeout(() => controller.abort(), 60000);
        const assigned = runAdb(['forward', 'tcp:0', `localabstract:${values.socket}`]);
        if (!/^[0-9]{1,5}$/.test(assigned) || Number(assigned) < 1 || Number(assigned) > 65535) {
            throw new Error('FORWARD_FAILED');
        }
        port = Number(assigned);
        const response = await fetch(`http://127.0.0.1:${port}/json/list`, {
            signal: AbortSignal.any([controller.signal, AbortSignal.timeout(10000)]),
            redirect: 'error'
        });
        if (!response.ok) throw new Error('TARGETS_UNAVAILABLE');
        const chunks = [];
        let size = 0;
        for await (const chunk of response.body) {
            size += chunk.length;
            if (size > 1048576) throw new Error('TARGET_LIST_TOO_LARGE');
            chunks.push(chunk);
        }
        const targets = JSON.parse(Buffer.concat(chunks).toString('utf8'));
        if (!Array.isArray(targets) || targets.length > 128) throw new Error('INVALID_TARGET_LIST');
        const candidates = targets.filter(allowedTarget);
        if (candidates.length < 1 || candidates.length > 16) throw new Error('SUPPORTED_TAB_NOT_FOUND');
        let visible = null;
        for (const target of candidates) {
            const session = await connect(target.webSocketDebuggerUrl);
            const isVisible = await session.evaluateBoolean(
                `Boolean(window.top === window && location.origin === ${JSON.stringify(supportedOrigin)} ` +
                `&& document.visibilityState === 'visible' && !document.prerendering)`);
            if (!isVisible) session.close();
            else if (visible) throw new Error('AMBIGUOUS_VISIBLE_TAB');
            else visible = session;
        }
        if (!visible) throw new Error('VISIBLE_SUPPORTED_TAB_NOT_FOUND');
        const script = readFileSync(new URL('../src/main/assets/portal-enrollment-demo.js', import.meta.url), 'utf8');
        const configuration = JSON.stringify({ mode: 'browser', packageName: values.package });
        const installed = await visible.evaluateBoolean(`Boolean((${script})(${configuration}))`);
        if (!installed) throw new Error('INJECTION_REJECTED');
        console.log('Injected: true. Keep the original tab open. Click Set up using custom app and confirm natively.');
        console.log('The observer lasts only for this document; reinject after a full reload. No enrollment was submitted by this helper.');
    }
} catch (_) {
    console.error('Injection failed. Check options, built-in Node WebSocket, authorized adb and a single visible supported Chrome tab.');
    process.exitCode = 1;
} finally {
    clearTimeout(deadline);
    controller.abort();
    for (const socket of connections) socket.close();
    if (port !== undefined) {
        try { runAdb(['forward', '--remove', `tcp:${port}`]); }
        catch (_) {
            console.error('Owned debug forward cleanup failed; remove the helper-created forward manually.');
            process.exitCode = 1;
        }
    }
}
