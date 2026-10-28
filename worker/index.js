// 图片防盗链：允许页面内的 <img> / CSS 背景（Sec-Fetch-Dest: image）、
// 同站来源的 Referer，以及社交平台的预览抓取 UA；其余直接访问一律 403。
const IMG = /\.(png|webp|jpe?g|gif|svg|ico|bmp|avif)$/i;
const PREVIEW_BOT =
  /(facebookexternalhit|twitterbot|telegrambot|discordbot|slackbot|whatsapp|linkedinbot|pinterest|qq|qzone|wechat|weixin|microMessenger|bingbot|googlebot|yandex|baiduspider|applebot)/i;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (IMG.test(url.pathname)) {
      const dest = request.headers.get("Sec-Fetch-Dest") || "";
      const referer = request.headers.get("Referer") || "";
      const ua = request.headers.get("User-Agent") || "";

      const fromPage = dest === "image" || referer.startsWith(url.origin);
      const isPreviewBot = PREVIEW_BOT.test(ua);

      if (!fromPage && !isPreviewBot) {
        return new Response("Forbidden", {
          status: 403,
          headers: {
            "content-type": "text/plain; charset=utf-8",
            "cache-control": "no-store",
            "x-robots-tag": "noindex",
          },
        });
      }
    }

    return env.ASSETS.fetch(request);
  },
};