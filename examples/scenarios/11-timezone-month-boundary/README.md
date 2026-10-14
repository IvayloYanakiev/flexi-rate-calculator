# The same UTC instant belongs to different local months

2026-09-30T21:00Z is October 1 in Sofia but September 30 in UTC. For billing_cycle 2026-10, only Sofia's event counts. The UTC user's valid out-of-month event does not increase the invalid count.

Expected HTTP **200**. `X-Invalid-Event-Count: 0`.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
