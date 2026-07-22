/**
 * Client-side CSV export.
 *
 * <p>Done in the browser because the data is already loaded - a server-side
 * report endpoint would be a second source of truth for numbers the admin is
 * looking at right now.
 */
export function exportCsv(filename, rows, columns) {
  if (!rows || rows.length === 0) {
    return false
  }

  const cols = columns || Object.keys(rows[0]).map((key) => ({ key, label: key }))

  const escape = (value) => {
    if (value === null || value === undefined) return ''
    const text = String(value)
    // Quote anything containing a delimiter, quote or newline; double up any
    // embedded quotes. Without this a doctor's name with a comma in it would
    // silently shift every later column.
    return /[",\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
  }

  const header = cols.map((c) => escape(c.label)).join(',')
  const body = rows
    .map((row) => cols.map((c) => escape(row[c.key])).join(','))
    .join('\n')

  // The BOM makes Excel read it as UTF-8; without it Indian names and the
  // rupee sign come out mangled.
  const blob = new Blob(['﻿' + header + '\n' + body],
    { type: 'text/csv;charset=utf-8;' })

  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename.endsWith('.csv') ? filename : `${filename}.csv`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
  return true
}

/** 'appointments-2026-07-23.csv' */
export function datedFilename(prefix) {
  return `${prefix}-${new Date().toISOString().split('T')[0]}.csv`
}
