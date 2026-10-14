# Invalid user timezone rejects the request

The user timezone is needed for month boundaries; an invalid timezone is a request-level HTTP 400 error.

Expected HTTP **400**. No invalid-event count header on this rejected request.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
