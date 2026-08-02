// http-proxy-middleware streams request/response bodies by default.
// The only way to break that is registering a body-parser (express.json,
// multer, etc.) on a route *before* the proxy runs — that consumes the
// stream and the proxy forwards an empty body. Routes in proxy.routes.js
// must stay parser-free ahead of the proxy for this reason.
export const STREAMING_NOTE =
  'Do not add express.json()/multer() before proxy routes — see comment above.';
