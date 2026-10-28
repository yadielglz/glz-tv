import { spawnSync } from 'node:child_process';
import { readFileSync, mkdtempSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import assert from 'node:assert/strict';

// The off-device interpreter preserves unquoted AA key casing, unlike Roku.
// Require quoted keys for every JSON request payload before executing fixtures.
const hubSource = readFileSync('components/HubTask.brs', 'utf8');
const payloads = [...hubSource.matchAll(/\bpayload = \{([\s\S]*?)\n    \}/g)];
assert.equal(payloads.length, 2, 'enrollment and heartbeat request payloads are checked');
for (const [, body] of payloads) {
  assert.ok(!/(?:^|[\n{,])\s*[A-Za-z_]\w*\s*:/m.test(body), 'JSON request keys must be quoted to preserve case on Roku');
}
assert.match(payloads[1][1], /"installationId"\s*:/);
assert.match(payloads[1][1], /"appVersion"\s*:/);
console.log('PASS: Roku API payload keys preserve exact JSON casing');

function run(files, root, marker) {
  const result = spawnSync(process.execPath, ['node_modules/brs/bin/cli.js', '--root', root, ...files], { encoding: 'utf8' });
  process.stdout.write(result.stdout || '');
  process.stderr.write(result.stderr || '');
  if (result.status !== 0 || !result.stdout.includes(marker) || result.stdout.includes('FAIL:')) throw new Error('Roku offline tests failed');
}

// Isolate pure parser tests from device-only SceneGraph components.
run(['source/Catalog.brs', 'tests/catalog.brs'], 'tests', 'ALL CATALOG TESTS PASSED');

// Run the production Hub workflow against deterministic network/device fixtures.
// Only Roku I/O primitives are substituted; enrollment, credentials, catalog and
// heartbeat behavior execute from the actual Task source.
const scratch = mkdtempSync(join(tmpdir(), 'glz-roku-tests-'));
try {
  let source = readFileSync('components/HubTask.brs', 'utf8');
  source = source.replace(/function Http\([\s\S]*?end function/, `function Http(url as String, method as String, headers as Object, payload = "" as String) as Object\n    return TestHttp(url, method, headers, payload)\nend function`)
    .replaceAll('CreateObject("roAppInfo").GetValue("glz_version")', '"test-version"')
    .replaceAll('CreateObject("roRegistrySection", "GlzTV")', 'm.store')
    .replaceAll('CreateObject("roDeviceInfo").GetRandomUUID()', '"fixture-installation"')
    .replaceAll('CreateObject("roDeviceInfo").GetModel()', '"8102X"')
    .replaceAll('CreateObject("roDateTime").AsSeconds()', '1000');
  const task = join(scratch, 'HubTask.brs');
  writeFileSync(task, source);
  run([resolve('source/Catalog.brs'), task, resolve('tests/hub.brs')], scratch, 'ALL HUB TESTS PASSED');
} finally { rmSync(scratch, { recursive: true, force: true }); }
