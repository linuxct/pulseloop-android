## 2024-05-20 - Exposing Internals via Error Logs

**Vulnerability:** External API errors returning raw JSON response bodies were logged and bubbled up directly to the UI, risking exposure of internals, partial keys, or implementation details.
**Learning:** Returning exception stack traces or raw upstream provider responses directly in exceptions that bubble up to UI leaks internals, violates "Fail securely" principle.
**Prevention:** Catch external API exceptions and throw a generic error message (e.g. `IOException("OpenAI API error ${resp.code}")`) instead of including the raw upstream JSON body.
