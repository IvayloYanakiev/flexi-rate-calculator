# The events container itself must be an array

An event may be malformed, but events itself must be an array. This request has an object instead and returns HTTP 400.

Expected HTTP **400**. No invalid-event count header on this rejected request.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
