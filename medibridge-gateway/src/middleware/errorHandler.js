export function notFoundHandler(req, res) {
  res.status(404).json({ error: 'Not Found', path: req.originalUrl });
}

export function errorHandler(err, req, res, _next) {
  req.log?.error({ err }, 'unhandled error');
  const status = err.status ?? 502;
  res.status(status).json({ error: err.message ?? 'Bad Gateway' });
}
