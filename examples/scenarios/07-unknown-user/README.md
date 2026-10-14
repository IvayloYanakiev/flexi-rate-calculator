# Unknown event user is skipped

An event references a user absent from users. That event is skipped; the known user's event is still billed.

Expected HTTP **200**. `X-Invalid-Event-Count: 1`.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
