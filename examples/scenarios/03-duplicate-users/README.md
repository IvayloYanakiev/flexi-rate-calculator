# Duplicate user IDs reject the request

Two users have user_id alice. This is a request-level error, unlike duplicate events; HTTP 400 and no invalid-event header.

Expected HTTP **400**. No invalid-event count header on this rejected request.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
