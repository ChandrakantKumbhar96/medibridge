import { createProxyMiddleware } from 'http-proxy-middleware';

// Plain passthrough: forwards method, headers (incl. Authorization), body,
// and streams the response back untouched. No buffering — required for
// prescription PDF downloads and multipart file uploads.
//
// pathPrefix restores the mount prefix Express strips before this middleware
// ever sees the request: router.use('/api', proxy) hands the proxy a req.url
// with '/api' already removed, and http-proxy-middleware forwards req.url
// as-is (it does not fall back to req.originalUrl) — so without this, every
// request would reach the upstream missing its '/api' context path.
export function createUpstreamProxy(target, { pathPrefix, ...options } = {}) {
  return createProxyMiddleware({
    target,
    changeOrigin: true,
    logger: console,
    ...(pathPrefix ? { pathRewrite: (path) => pathPrefix + path } : {}),
    ...options,
  });
}
