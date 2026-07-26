import { existsSync, readFileSync, statSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const failures = []

function fail(message) {
  failures.push(message)
}

function read(relativePath) {
  const absolutePath = resolve(root, relativePath)
  if (!existsSync(absolutePath)) {
    fail(`missing: ${relativePath}`)
    return ''
  }
  return readFileSync(absolutePath, 'utf8')
}

function checkManifest(relativePath) {
  const absolutePath = resolve(root, relativePath)
  let manifest
  try {
    manifest = JSON.parse(read(relativePath))
  } catch (error) {
    fail(`invalid JSON: ${relativePath} (${error.message})`)
    return
  }
  const base = dirname(absolutePath)
  function visit(value) {
    if (typeof value === 'string' && (/\.(md|json|html)$/.test(value) || value.endsWith('/'))) {
      const target = resolve(base, value)
      if (!existsSync(target)) fail(`manifest path missing: ${relativePath} -> ${value}`)
      if (value.endsWith('/') && existsSync(target) && !statSync(target).isDirectory()) {
        fail(`manifest path is not a directory: ${relativePath} -> ${value}`)
      }
    } else if (Array.isArray(value)) {
      value.forEach(visit)
    } else if (value && typeof value === 'object') {
      Object.values(value).forEach(visit)
    }
  }
  visit(manifest)
}

function checkMarkdownLinks(relativePath) {
  const source = read(relativePath)
  const base = dirname(resolve(root, relativePath))
  for (const match of source.matchAll(/\[[^\]]*\]\(([^)]+)\)/g)) {
    const raw = match[1].replace(/^<|>$/g, '').split('#')[0]
    if (!raw || /^(https?:|mailto:)/.test(raw)) continue
    if (!existsSync(resolve(base, decodeURIComponent(raw)))) {
      fail(`broken link: ${relativePath} -> ${raw}`)
    }
  }
}

checkManifest('docs/product-specs/product.manifest.json')
checkManifest('docs/product-specs/versions/mvp/version.manifest.json')

for (const path of [
  'AGENTS.md',
  'README.md',
  'ARCHITECTURE.md',
  'docs/README.md',
  'docs/product-specs/current.md',
  'docs/architecture/index.md',
  'docs/architecture/versions/mvp/README.md'
]) checkMarkdownLinks(path)

const staleEntries = ['AGENTS.md', 'ARCHITECTURE.md', 'docs/QUALITY_SCORE.md', 'docs/SECURITY.md', 'docs/RELIABILITY.md']
const stalePhrases = ['尚无应用代码', '当前仓库只包含文档', '旧 MVP 工程设计内容已清空']
for (const path of staleEntries) {
  const source = read(path)
  for (const phrase of stalePhrases) {
    if (source.includes(phrase)) fail(`stale entry text in ${path}: ${phrase}`)
  }
}

const prototype = read('docs/design-docs/versions/mvp/prototype.html')
if (!prototype.startsWith('<!doctype html>') || !prototype.includes('</html>')) fail('prototype HTML structure is incomplete')
const prototypeLabels = [...prototype.matchAll(/class="frame-label">([^<]+)/g)].map((match) => match[1])
const frameCount = prototypeLabels.length
if (frameCount !== 74) fail(`prototype frame count expected 74, received ${frameCount}`)

const implementationMapPath = 'docs/architecture/versions/mvp/prototype-implementation-map.json'
try {
  const implementationMap = JSON.parse(read(implementationMapPath))
  const mappedLabels = implementationMap.frames?.map((frame) => frame.label) || []
  if (implementationMap.frameCount !== frameCount || mappedLabels.length !== frameCount) {
    fail(`prototype implementation map expected ${frameCount} frames, received ${mappedLabels.length}`)
  }
  if (new Set(mappedLabels).size !== mappedLabels.length) fail('prototype implementation map contains duplicate labels')
  prototypeLabels.forEach((label, index) => {
    if (mappedLabels[index] !== label) fail(`prototype implementation map drift at frame ${index + 1}: ${mappedLabels[index] || 'missing'} != ${label}`)
  })
  implementationMap.frames?.forEach((frame) => {
    if (!['implemented', 'pilot_implemented', 'backend_ready', 'planned'].includes(frame.status)) {
      fail(`prototype implementation map has invalid status at frame ${frame.index}: ${frame.status}`)
    }
  })
} catch (error) {
  fail(`invalid JSON: ${implementationMapPath} (${error.message})`)
}

if (failures.length) {
  failures.forEach((message) => console.error(`ERROR ${message}`))
  process.exit(1)
}
console.log(`Documentation checks passed: 2 manifests, core links, stale-entry guard, ${frameCount} mapped prototype frames.`)
