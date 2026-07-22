import { readFile, readdir } from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const workspaceRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const productionRoot = path.join(workspaceRoot, 'apps/api/src/main/java')
const forbiddenPersistencePatterns = [
  ['JdbcTemplate', /\b(?:NamedParameter)?JdbcTemplate\b/],
  ['direct JDBC connection', /\bjava\.sql\.(?:Connection|PreparedStatement|Statement)\b/],
]

const violations = []

async function walk(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const resolved = path.join(directory, entry.name)
    if (entry.isDirectory()) {
      files.push(...await walk(resolved))
    } else if (entry.isFile() && entry.name.endsWith('.java')) {
      files.push(resolved)
    }
  }
  return files
}

for (const file of await walk(productionRoot)) {
  const source = await readFile(file, 'utf8')
  const relative = path.relative(workspaceRoot, file)
  for (const [label, pattern] of forbiddenPersistencePatterns) {
    if (pattern.test(source)) {
      violations.push(`${relative}: production persistence must use a MyBatis-Plus Mapper (${label} found)`)
    }
  }
  if (/^import\s+[^;]+\.\*;/m.test(source)) {
    violations.push(`${relative}: wildcard imports are forbidden by the Java coding standard`)
  }
  if (/System\.(?:out|err)\./.test(source)) {
    violations.push(`${relative}: use structured logging instead of System.out/System.err`)
  }
}

if (violations.length > 0) {
  console.error(violations.join('\n'))
  process.exit(1)
}

console.log('Java persistence and coding boundaries passed')
