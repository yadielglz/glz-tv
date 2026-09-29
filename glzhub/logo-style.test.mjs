import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
import ts from 'typescript';
const source = readFileSync(new URL('./src/index.ts', import.meta.url), 'utf8');
const js = ts.transpileModule(source + '\nexports.validateStyle = validateBetaLogoStyle; exports.attributes = betaLogoAttributes;', {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText;
const context = { exports: {}, URL, console };
vm.runInNewContext(js, context);
const { validateStyle, attributes } = context.exports;
const metadata = { beta_logo_style: { background: '#E60012', foreground: '#FFFFFF', mode: 'monochrome', removeBackground: false } };
assert.equal(attributes(metadata, false), '');
assert.match(attributes(metadata, true), /glz-logo-background="#E60012"/);
assert.match(attributes(metadata, true), /glz-logo-remove-background="false"/);
assert.equal(attributes({}, true), '');
assert.throws(() => validateStyle({ beta_logo_style: { background: '#fff" injected="true' } }));
assert.throws(() => validateStyle({ beta_logo_style: { mode: 'invalid' } }));
assert.throws(() => validateStyle({ beta_logo_style: { logoUrl: 'javascript:alert(1)' } }));
assert.equal(attributes({ beta_logo_style: { background: 'bad' } }, true), '');
validateStyle({ beta_logo_style: { logoUrl: 'https://example.com/logo.png', mode: 'original' } });
console.log('GlzHub logo metadata validation and Beta isolation checks passed.');
