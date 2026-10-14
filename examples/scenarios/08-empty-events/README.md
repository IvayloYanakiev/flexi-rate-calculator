# Users with no events receive zero bills

Both listed users receive a bill in input order even though events is empty.

Expected HTTP **200**. `X-Invalid-Event-Count: 0`.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
