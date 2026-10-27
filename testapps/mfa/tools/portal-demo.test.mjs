// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const source = readFileSync(new URL('../src/main/assets/portal-enrollment-demo.js', import.meta.url), 'utf8');
const origin = 'https://mysignins.microsoft.com';
const packageName = 'example.test.host';
const link = code => 'https://login.microsoftonline.com/authenticatorApp/activateAccount' +
    `?accountType=mfa&code=${code}&url=https%3A%2F%2Fpoc.mobileappcommunicator.auth.microsoft.com`;

function page(hrefs = [link('synthetic-one')], options = {}) {
    class Element {
        constructor(tag) {
            this.tagName = tag.toUpperCase();
            this.children = [];
            this.style = {};
            this.attributes = {};
            this.isConnected = true;
            this.textContent = '';
            this.listeners = {};
        }
        get innerText() { return this.textContent; }
        get parentElement() { return this.parentNode ?? null; }
        get href() { return this.attributes.href; }
        set href(value) { this.attributes.href = value; }
        setAttribute(key, value) { this.attributes[key] = String(value); }
        getAttribute(key) { return this.attributes[key] ?? null; }
        removeAttribute(key) { delete this.attributes[key]; }
        appendChild(child) {
            if (child.parentNode) child.parentNode.children = child.parentNode.children.filter(item => item !== child);
            this.children.push(child);
            child.parentNode = this;
            return child;
        }
        append(...children) { children.forEach(child => this.appendChild(child)); }
        remove() {
            this.isConnected = false;
            if (this.parentNode) this.parentNode.children = this.parentNode.children.filter(child => child !== this);
        }
        contains(child) { return this === child || this.children.some(item => item.contains(child)); }
        getClientRects() { return this.isConnected && !this.hidden ? [{ width: 20, height: 20 }] : []; }
        addEventListener(name, callback) { this.listeners[name] = callback; }
        click(isTrusted = true) {
            const event = { isTrusted, preventDefault() { this.prevented = true; } };
            this.listeners.click(event);
            return event;
        }
    }
    const anchors = hrefs.map(href => {
        const anchor = new Element('a');
        anchor.textContent = 'Pair your account';
        anchor.href = href;
        return anchor;
    });
    const body = new Element('body');
    const observers = [];
    const context = {
        URL, JSON, Set, TextEncoder,
        location: { origin: options.origin ?? origin },
        document: {
            body, documentElement: body, visibilityState: 'visible',
            createElement: tag => new Element(tag),
            querySelectorAll: () => anchors,
            getElementById: id => {
                function find(node) { return node.id === id ? node : node.children.map(find).find(Boolean); }
                return find(body);
            }
        },
        getComputedStyle: () => ({ display: 'block', visibility: 'visible' }),
        MutationObserver: class {
            constructor(callback) { this.callback = callback; observers.push(this); }
            observe() {}
            disconnect() { this.disconnected = true; }
        },
        requestAnimationFrame: callback => { callback(); return 1; },
        cancelAnimationFrame() {},
        setTimeout: () => 1,
        clearTimeout() {}
    };
    context.window = context;
    context.top = options.subframe ? {} : context;
    const install = vm.runInNewContext(source, context);
    return { context, anchors, observers, install,
        button: () => context.document.getElementById('mfa-custom-app-setup') };
}

test('installs only on supported top-level origin', () => {
    for (const options of [{ origin: 'https://evil.invalid' }, { subframe: true }]) {
        const p = page(undefined, options);
        assert.equal(p.install({ mode: 'webview', packageName }), false);
        assert.equal(p.context.document.body.children.length, 0);
    }
});

test('shows the demo only when the page reaches supported pairing input', () => {
    const p = page();
    const pairingAnchor = p.anchors.pop();
    p.install({ mode: 'webview', packageName });
    const panel = p.context.document.getElementById('mfa-custom-app-demo');
    assert.equal(panel.hidden, true);

    p.anchors.push(pairingAnchor);
    p.observers[0].callback([{ target: p.context.document.body }]);
    assert.equal(panel.hidden, false);

    pairingAnchor.hidden = true;
    p.observers[0].callback([{ target: p.context.document.body }]);
    assert.equal(panel.hidden, true);

    pairingAnchor.hidden = false;
    p.observers[0].callback([{ target: p.context.document.body }]);
    p.button().click();
    p.anchors.pop();
    p.observers[0].callback([{ target: p.context.document.body }]);
    assert.equal(panel.hidden, false);
    assert.equal(p.button().getAttribute('aria-disabled'), 'true');
    assert.equal(p.button().click().prevented, true);
});

test('renders only the button in normal page flow without a footer', () => {
    const p = page();
    const setup = p.context.document.createElement('div');
    const next = p.context.document.createElement('button');
    setup.append(p.anchors[0], next);
    p.context.document.body.appendChild(setup);
    p.install({ mode: 'webview', packageName });
    const panel = p.context.document.getElementById('mfa-custom-app-demo');
    assert.doesNotMatch(panel.style.cssText, /position\s*:\s*(fixed|absolute)|z-index/i);
    assert.deepEqual(panel.children, [p.button()]);
    assert.equal(panel.parentElement, setup);
    assert.equal(setup.children.at(-1), panel);
});

test('both modes use the identical CURRENT package-targeted link without a bridge', () => {
    const outputs = [];
    for (const mode of ['webview', 'browser']) {
        const p = page();
        assert.equal(p.context.mfaEnrollment, undefined);
        assert.equal(p.install({ mode, packageName }), true);
        const button = p.button();
        assert.equal(button.textContent, 'Set up using this device');
        p.anchors[0].href = link('synthetic-current');
        assert.notEqual(button.click().prevented, true);
        outputs.push(button.href);
        assert.equal(button.href, 'intent://activate#Intent;scheme=mfa-poc-live;' +
            'package=example.test.host;S.com.microsoft.identity.mfa.ACTIVATION_LINK=' +
            encodeURIComponent(link('synthetic-current')) + ';end');
        assert.equal(p.anchors[0].href, link('synthetic-current'));
        assert.equal(button.click().prevented, true);
    }
    assert.equal(outputs[0], outputs[1]);
    assert.doesNotMatch(source, /postMessage|mfaEnrollment|addJavascriptInterface|addWebMessageListener/);
});

test('accepts the optional source parameter on the Microsoft pairing link', () => {
    const href = link('synthetic-source') + '&source=synthetic-portal';
    for (const mode of ['webview', 'browser']) {
        const p = page([href]);
        p.install({ mode, packageName });
        assert.equal(p.button().getAttribute('aria-disabled'), 'false');
        p.button().click();
        assert.ok(p.button().href.includes(encodeURIComponent(href)));
    }
});

test('click rejects missing, ambiguous, stale, TOTP and malformed current page input', () => {
    const invalid = ['', link('a') + '&code=b', link('a').replace('accountType=mfa', 'accountType=totp'),
        link('a') + '&source=one&source=two',
        link('a').replace('login.microsoftonline.com', 'evil.invalid'), link(''),
        link('a').replace('poc.mobileappcommunicator.auth.microsoft.com', 'evil.invalid'),
        link('a').replace('https%3A', 'http%3A')];
    for (const mode of ['webview', 'browser']) {
        for (const hrefs of [[], [link('a'), link('b')], ...invalid.map(href => [href])]) {
            const p = page(hrefs);
            p.install({ mode, packageName });
            assert.equal(p.button().click().prevented, true);
            assert.equal(p.button().href, '#');
        }
        for (const stale of ['detached', 'hidden', 'invisible', 'changed']) {
            const p = page();
            p.install({ mode, packageName });
            if (stale === 'detached') p.anchors[0].isConnected = false;
            if (stale === 'hidden') p.context.document.visibilityState = 'hidden';
            if (stale === 'invisible') p.anchors[0].hidden = true;
            if (stale === 'changed') p.anchors[0].href = 'https://evil.invalid';
            assert.equal(p.button().click().prevented, true);
            assert.equal(p.button().href, '#');
        }
    }
});

test('both modes reject a missing or invalid package before inserting a link', () => {
    for (const mode of ['webview', 'browser']) {
        for (const invalid of [undefined, null, '', 1, 'one', 'a.b;end', 'a..b',
            'a.b c', '1a.b', 'a.' + 'b'.repeat(254)]) {
            const p = page();
            assert.equal(p.install({ mode, packageName: invalid }), false);
            assert.equal(p.context.document.body.children.length, 0);
        }
    }
});

test('untrusted clicks cannot consume the one permitted user click in either mode', () => {
    for (const mode of ['webview', 'browser']) {
        const p = page();
        p.install({ mode, packageName });
        assert.equal(p.button().click(false).prevented, true);
        assert.equal(p.button().href, '#');
        assert.notEqual(p.button().click().prevented, true);
        assert.equal(p.button().click().prevented, true);
    }
});

test('reinjection removes only the prior demo observer and panel', () => {
    const p = page();
    p.install({ mode: 'webview', packageName });
    const old = p.button();
    p.install({ mode: 'webview', packageName });
    assert.notEqual(p.button(), old);
    assert.equal(p.observers[0].disconnected, true);
    assert.equal(p.context.document.body.children.length, 1);
});
