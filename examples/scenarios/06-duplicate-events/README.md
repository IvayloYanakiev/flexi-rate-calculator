# Duplicate event IDs are counted once

The first valid event wins. The repeated ID is skipped, so usage is 15 minutes rather than 30 minutes; invalid count 1.

Expected HTTP **200**. `X-Invalid-Event-Count: 1`.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
