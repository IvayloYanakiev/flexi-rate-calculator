# All invalid events still produce a successful batch

null, an empty event object and an array-valued duration are all invalid. Return HTTP 200, a zero bill, and invalid count 3.

Expected HTTP **200**. `X-Invalid-Event-Count: 3`.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
