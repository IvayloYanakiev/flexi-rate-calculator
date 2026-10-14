# Array instead of integer: skip one event

The exact client example: duration_minutes is [15]. That event is skipped, the valid event remains, and X-Invalid-Event-Count is 1.

Expected HTTP **200**. `X-Invalid-Event-Count: 1`.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
