// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const source = readFileSync(new URL('../src/main/assets/portal-enrollment-demo.js', import.meta.url), 'utf8');
const origin = 'https://mysignins.microsoft.com';
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
        get href() { return this.attributes.href; }
        set href(value) { this.attributes.href = value; }
        setAttribute(key, value) { this.attributes[key] = String(value); }
        getAttribute(key) { return this.attributes[key] ?? null; }
        removeAttribute(key) { delete this.attributes[key]; }
        appendChild(child) { this.children.push(child); child.parentNode = this; return child; }
        append(...children) { children.forEach(child => this.appendChild(child)); }
        remove() {
            this.isConnected = false;
            if (this.parentNode) this.parentNode.children = this.parentNode.children.filter(child => child !== this);
        }
        contains(child) { return this === child || this.children.some(item => item.contains(child)); }
        getClientRects() { return this.isConnected && !this.hidden ? [{ width: 20, height: 20 }] : []; }
        addEventListener(name, callback) { this.listeners[name] = callback; }
        click() {
            const event = { isTrusted: true, preventDefault() { this.prevented = true; } };
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
    const sent = [];
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
        clearTimeout() {},
        mfaEnrollment: { postMessage: value => sent.push(JSON.parse(value)) }
    };
    context.window = context;
    context.top = options.subframe ? {} : context;
    const install = vm.runInNewContext(source, context);
    return { context, anchors, sent, observers, install,
        button: () => context.document.getElementById('mfa-custom-app-setup') };
}

test('installs only on supported top-level origin', () => {
    for (const options of [{ origin: 'https://evil.invalid' }, { subframe: true }]) {
        const p = page(undefined, options);
        assert.equal(p.install({ mode: 'webview' }), false);
        assert.equal(p.context.document.body.children.length, 0);
    }
});

test('browser click uses CURRENT pairing link and targeted String extra, not callback query', () => {
    const p = page();
    assert.equal(p.install({ mode: 'browser', packageName: 'example.test.host' }), true);
    const button = p.button();
    assert.equal(button.textContent, 'Set up using custom app');
    p.anchors[0].href = link('synthetic-current');
    const event = button.click();
    assert.notEqual(event.prevented, true);
    assert.equal(button.href, 'intent://activate#Intent;scheme=mfa-poc-live;' +
        'package=example.test.host;S.com.microsoft.identity.mfa.ACTIVATION_LINK=' +
        encodeURIComponent(link('synthetic-current')) + ';end');
    assert.equal(p.anchors[0].href, link('synthetic-current'));
    assert.equal(p.sent.length, 0);
});

test('WebView mode sends exact SDK message without navigating original pairing anchor', () => {
    const p = page();
    p.install({ mode: 'webview' });
    assert.equal(p.button().click().prevented, true);
    assert.deepEqual(p.sent, [{ version: 1, type: 'activation', activationLink: link('synthetic-one') }]);
    assert.equal(p.anchors[0].href, link('synthetic-one'));
});

test('click rejects missing, ambiguous, stale, TOTP and malformed current page input', () => {
    const invalid = ['', link('a') + '&code=b', link('a').replace('accountType=mfa', 'accountType=totp'),
        link('a').replace('login.microsoftonline.com', 'evil.invalid'), link(''),
        link('a').replace('poc.mobileappcommunicator.auth.microsoft.com', 'evil.invalid'),
        link('a').replace('https%3A', 'http%3A')];
    for (const hrefs of [[], [link('a'), link('b')], ...invalid.map(href => [href])]) {
        const p = page(hrefs);
        p.install({ mode: 'webview' });
        assert.equal(p.button().click().prevented, true);
        assert.equal(p.sent.length, 0);
    }
    for (const stale of ['detached', 'hidden', 'invisible']) {
        const p = page();
        p.install({ mode: 'webview' });
        if (stale === 'detached') p.anchors[0].isConnected = false;
        if (stale === 'hidden') p.context.document.visibilityState = 'hidden';
        if (stale === 'invisible') p.anchors[0].hidden = true;
        assert.equal(p.button().click().prevented, true);
        assert.equal(p.sent.length, 0);
    }
});

test('requires explicit browser package and rejects absent WebKit connector', () => {
    const p = page();
    assert.equal(p.install({ mode: 'browser' }), false);
    delete p.context.mfaEnrollment;
    assert.equal(p.install({ mode: 'webview' }), true);
    assert.equal(p.button().click().prevented, true);
    assert.equal(p.sent.length, 0);
});

test('reinjection removes only the prior demo observer and panel', () => {
    const p = page();
    p.install({ mode: 'webview' });
    const old = p.button();
    p.install({ mode: 'webview' });
    assert.notEqual(p.button(), old);
    assert.equal(p.observers[0].disconnected, true);
    assert.equal(p.context.document.body.children.length, 1);
});
