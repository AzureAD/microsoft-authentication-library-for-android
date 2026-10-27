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
    if (typeof packageName !== 'string' ||
        !/^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$/.test(packageName) ||
        packageName.length > 255) return false;

    if (window.__mfaPortalDemo) window.__mfaPortalDemo.dispose();
    const panel = document.createElement('div');
    panel.id = 'mfa-custom-app-demo';
    panel.hidden = true;
    panel.style.cssText = 'margin:16px 0;flex-shrink:0';
    const button = document.createElement('a');
    button.id = 'mfa-custom-app-setup';
    button.textContent = 'Set up using this device';
    button.href = '#';
    button.setAttribute('role', 'button');
    button.setAttribute('tabindex', '0');
    button.style.cssText = 'display:inline-block;padding:10px;background:#005a9e;color:white;' +
        'text-decoration:none;border-radius:4px;cursor:pointer';
    panel.appendChild(button);
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
            if (keys.length < 3 || keys.length > 4 || new Set(keys).size !== keys.length ||
                !keys.every(key => ['accountType', 'code', 'url', 'source'].includes(key)) ||
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
            return candidates[0];
        } catch (_) { return null; }
    }

    function refresh() {
        scheduled = false;
        if (disposed) return;
        const pairingLink = !sent && currentLink();
        const ready = Boolean(pairingLink);
        panel.hidden = !sent && !ready;
        const content = pairingLink && pairingLink.parentElement;
        if (content && panel.parentNode !== content) content.appendChild(panel);
        button.setAttribute('aria-disabled', String(!ready));
        button.style.opacity = ready ? '1' : '0.6';
    }

    button.addEventListener('click', event => {
        // Read the current page on every click, never a value captured while installing the button.
        const pairingLink = !sent && event.isTrusted && currentLink();
        const activationLink = pairingLink && pairingLink.getAttribute('href');
        if (!activationLink) {
            event.preventDefault();
            refresh();
            return;
        }
        // Both hosts consume the same user-clicked link; no native JavaScript bridge is required.
        button.href = 'intent://activate#Intent;scheme=mfa-poc-live;package=' + packageName +
            ';S.com.microsoft.identity.mfa.ACTIVATION_LINK=' + encodeURIComponent(activationLink) + ';end';
        clearIntentTimer = setTimeout(() => button.removeAttribute('href'), 0);
        sent = true;
        refresh();
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
