import { execFileSync } from 'node:child_process'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const temporary = mkdtempSync(join(tmpdir(), 'seren-openapi-'))
const generated = join(temporary, 'generated.ts')

try {
  execFileSync(resolve(root, 'node_modules/.bin/openapi-typescript'), [
    resolve(root, 'packages/shared-types/openapi.json'),
    '-o',
    generated
  ], { stdio: 'ignore' })
  const expected = readFileSync(resolve(root, 'packages/shared-types/src/generated.ts'), 'utf8')
  const actual = readFileSync(generated, 'utf8')
  if (expected !== actual) {
    throw new Error('OpenAPI generated types drifted. Run npm run generate:api.')
  }
  console.log('OpenAPI generated types are current.')
} finally {
  rmSync(temporary, { recursive: true, force: true })
}
