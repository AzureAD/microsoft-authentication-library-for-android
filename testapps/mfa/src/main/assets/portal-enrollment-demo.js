// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Demo DOM integration only; native validation/enrollment belongs to the MFA SDK.
(function installMfaPortalDemo(configuration) {
    'use strict';
    const supportedOrigin = 'https://mysignins.microsoft.com';
    if (window.top !== window || location.origin !== supportedOrigin || !document.body) return false;
    const mode = configuration && configuration.mode;
    if (mode !== 'webview' && mode !== 'browser') return false;
    const packageName = configuration.packageName;
    if (mode === 'browser' && (typeof packageName !== 'string' ||
        !/^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$/.test(packageName) ||
        packageName.length > 255)) return false;

    if (window.__mfaPortalDemo) window.__mfaPortalDemo.dispose();
    const panel = document.createElement('div');
    panel.id = 'mfa-custom-app-demo';
    panel.style.cssText = 'position:fixed;bottom:16px;left:16px;right:16px;z-index:2147483647;' +
        'padding:12px;background:#fff;color:#111;border:2px solid #005a9e;border-radius:6px;' +
        'font:14px sans-serif;box-shadow:0 2px 8px #555';
    const button = document.createElement('a');
    button.id = 'mfa-custom-app-setup';
    button.textContent = 'Set up using custom app';
    button.href = '#';
    button.setAttribute('role', 'button');
    button.setAttribute('tabindex', '0');
    button.style.cssText = 'display:inline-block;padding:10px;background:#005a9e;color:white;' +
        'text-decoration:none;border-radius:4px;cursor:pointer';
    const status = document.createElement('div');
    status.setAttribute('aria-live', 'polite');
    panel.append(button, status);
    document.body.appendChild(panel);
    let sent = false;
    let scheduled = false;
    let disposed = false;
    let clearIntentTimer = null;

    function visible(element) {
        const style = getComputedStyle(element);
        return element.isConnected && element.getClientRects().length > 0 &&
            style.display !== 'none' && style.visibility !== 'hidden' && style.visibility !== 'collapse';
    }

    function currentLink() {
        if (location.origin !== supportedOrigin || window.top !== window ||
            document.visibilityState !== 'visible') return null;
        const anchors = document.querySelectorAll('a[href]');
        if (anchors.length > 2000) return null;
        const candidates = Array.from(anchors).filter(anchor =>
            !panel.contains(anchor) && visible(anchor) &&
            /pair your account/i.test((anchor.innerText || '').replace(/\s+/g, ' ')));
        if (candidates.length !== 1) return null;
        const raw = candidates[0].getAttribute('href');
        if (!raw || !/^https:\/\/login\.microsoftonline\.com(?::443)?\/authenticatorApp\/activateAccount\?/.test(raw) ||
            raw.length > 8192 || new TextEncoder().encode(raw).length > 8192 ||
            /[\s\\\u0000-\u001f\u007f]/.test(raw)) return null;
        try {
            const link = new URL(raw);
            if (link.protocol !== 'https:' || link.hostname !== 'login.microsoftonline.com' ||
                link.port || link.username || link.password || link.hash ||
                link.pathname !== '/authenticatorApp/activateAccount') return null;
            const keys = Array.from(link.searchParams.keys());
            if (keys.length !== 3 || new Set(keys).size !== 3 ||
                !keys.every(key => ['accountType', 'code', 'url'].includes(key)) ||
                link.searchParams.get('accountType') !== 'mfa') return null;
            const code = link.searchParams.get('code');
            if (!code || !code.trim() || new TextEncoder().encode(code).length > 1024 ||
                /[\u0000-\u001f\u007f]/.test(code)) return null;
            const endpointValue = link.searchParams.get('url');
            if (!endpointValue || endpointValue.length > 4096 ||
                /[%\\\s\u0000-\u001f\u007f]/.test(endpointValue)) return null;
            const endpoint = new URL(endpointValue);
            if (endpoint.protocol !== 'https:' || endpoint.port || endpoint.username || endpoint.password ||
                endpoint.search || endpoint.hash || endpointValue.includes('?') || endpointValue.includes('#') ||
                endpoint.hostname.endsWith('.') ||
                !['mobileappcommunicator.auth.microsoft.com', 'phonefactor.net'].some(zone =>
                    endpoint.hostname === zone || endpoint.hostname.endsWith('.' + zone)) ||
                /\/\.{1,2}(\/|$)/.test(endpointValue)) return null;
            return raw;
        } catch (_) { return null; }
    }

    function say(message) {
        if (status.textContent !== message) status.textContent = message;
    }

    function refresh() {
        scheduled = false;
        if (disposed) return;
        const ready = !sent && currentLink() !== null;
        button.setAttribute('aria-disabled', String(!ready));
        if (!sent) say(ready ?
            'Demo: native confirmation is required. Keep this page open.' :
            'Open the visible Pair your account step. Missing, ambiguous or different-authenticator (TOTP) input is not supported.');
    }

    button.addEventListener('click', event => {
        // Read the current page on every click, never a value captured while installing the button.
        const activationLink = !sent && event.isTrusted && currentLink();
        if (!activationLink) {
            event.preventDefault();
            say('No fresh supported pairing link is available. Nothing was submitted.');
            return;
        }
        if (mode === 'webview') {
            event.preventDefault();
            if (!window.mfaEnrollment || typeof window.mfaEnrollment.postMessage !== 'function') {
                say('The secure SDK WebView connector is unavailable. Nothing was submitted.');
                return;
            }
            try {
                window.mfaEnrollment.postMessage(JSON.stringify({ version: 1, type: 'activation', activationLink }));
            } catch (_) {
                say('The SDK handoff could not be delivered. Check the native app.');
                return;
            }
        } else {
            // A real user-clicked anchor keeps Chrome's intent navigation within its gesture policy.
            button.href = 'intent://activate#Intent;scheme=mfa-poc-live;package=' + packageName +
                ';S.com.microsoft.identity.mfa.ACTIVATION_LINK=' + encodeURIComponent(activationLink) + ';end';
            clearIntentTimer = setTimeout(() => button.removeAttribute('href'), 0);
        }
        sent = true;
        button.setAttribute('aria-disabled', 'true');
        say('Handoff requested, not enrollment success. Confirm in the native app, then return here and let the website verify normally. Do not repeat this request.');
    });
    const observer = new MutationObserver(records => {
        if (disposed || scheduled || records.every(record => panel.contains(record.target))) return;
        scheduled = true;
        requestAnimationFrame(refresh);
    });
    observer.observe(document.documentElement, {
        childList: true, subtree: true, characterData: true, attributes: true,
        attributeFilter: ['href', 'style', 'class', 'hidden', 'aria-hidden']
    });
    window.__mfaPortalDemo = {
        dispose() {
            disposed = true;
            observer.disconnect();
            clearTimeout(clearIntentTimer);
            button.removeAttribute('href');
            panel.remove();
        }
    };
    refresh();
    return true;
})
