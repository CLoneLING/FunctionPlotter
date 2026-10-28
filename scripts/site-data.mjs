// 生成 docs/release.json：从 GitHub 最新 release 读取版本、APK 大小、蓝奏云链接与提取码
// 用法：node scripts/site-data.mjs
// 环境变量 GITHUB_TOKEN 可选（CI 里能提高速率上限）

import { writeFile, readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { execFileSync } from 'node:child_process';
import dns from 'node:dns';

// 某些网络环境 IPv6 不可达，Node 的 fetch 会直接 fetch failed；优先 IPv4。
dns.setDefaultResultOrder('ipv4first');

const REPO = 'CLoneLING/FunctionPlotter';
const __dirname = dirname(fileURLToPath(import.meta.url));
const OUT = join(__dirname, '..', 'docs', 'release.json');

function parseLanzou(body) {
  if (!body) return null;
  const urls = body.match(/https?:\/\/[^\s)<>"']*lanzou[a-z0-9.-]*\/[A-Za-z0-9]+/gi);
  if (!urls || urls.length === 0) return null;
  const url = urls[urls.length - 1].replace(/[.,，。]+$/, '');
  const codeMatch = body.match(/(?:提取码|访问码|密码)\s*[:：]?\s*([A-Za-z0-9]{2,12})/);
  return { url, code: codeMatch ? codeMatch[1] : null };
}

function parseSha(body) {
  if (!body) return null;
  const m = body.match(/\b([A-Fa-f0-9]{64})\b/);
  return m ? m[1].toUpperCase() : null;
}

function humanSize(bytes) {
  if (!bytes) return null;
  return (bytes / 1e6).toFixed(1) + ' MB';
}

async function main() {
  const headers = {
    Accept: 'application/vnd.github+json',
    'User-Agent': 'functionplotter-site-builder',
  };
  if (process.env.GITHUB_TOKEN) headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;

  const apiUrl = `https://api.github.com/repos/${REPO}/releases/latest`;

  // 先试原生 fetch；某些网络（企业/加速器 TLS 中间人）下 Node 的根证书校验会失败，
  // 此时退回 curl（走系统证书库）。CI 里两者都可用。
  async function fetchRelease() {
    try {
      const res = await fetch(apiUrl, { headers, signal: AbortSignal.timeout(20000) });
      if (!res.ok) throw new Error(`GitHub API ${res.status} ${res.statusText}`);
      return await res.json();
    } catch (err) {
      const viaCurl = readViaCurl();
      if (viaCurl) return viaCurl;
      throw err;
    }
  }

  function readViaCurl() {
    try {
      const args = ['-sS', '--max-time', '25', '-H', `Accept: ${headers.Accept}`, '-H', `User-Agent: ${headers['User-Agent']}`];
      if (headers.Authorization) args.push('-H', `Authorization: ${headers.Authorization}`);
      args.push(apiUrl);
      const out = execFileSync('curl', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
      const json = JSON.parse(out);
      if (!json || !json.tag_name) return null;
      console.log('[site-data] 已通过 curl 获取 release');
      return json;
    } catch {
      return null;
    }
  }

  let rel = null;
  try {
    rel = await fetchRelease();
  } catch (err) {
    console.warn(`[site-data] 获取 release 失败：${err.message}`);
    try {
      const old = JSON.parse(await readFile(OUT, 'utf8'));
      console.warn(`[site-data] 保留已有 release.json（v${old.version}）`);
      return;
    } catch {
      console.warn('[site-data] 无可用旧数据，跳过');
      return;
    }
  }

  const body = rel.body || '';
  const apk = (rel.assets || []).find((a) => a.name && a.name.endsWith('.apk'));
  const data = {
    version: (rel.tag_name || '').replace(/^v/i, ''),
    tag: rel.tag_name || '',
    publishedAt: rel.published_at || null,
    htmlUrl: rel.html_url || null,
    apkName: apk ? apk.name : null,
    apkSize: apk ? apk.size : null,
    apkSizeText: apk ? humanSize(apk.size) : null,
    apkDownloadUrl: apk ? apk.browser_download_url : null,
    sha256: parseSha(body),
    lanzou: parseLanzou(body),
    updatedAt: new Date().toISOString(),
  };

  await writeFile(OUT, JSON.stringify(data, null, 2) + '\n', 'utf8');
  console.log(`[site-data] v${data.version} · ${data.apkSizeText || '?'} · 蓝奏云 ${data.lanzou ? 'ok' : '无'}`);
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
