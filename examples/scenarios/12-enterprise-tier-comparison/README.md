# Enterprise grace counts toward the rate threshold

Each user has 100 hours from 400 chronological off-peak chunks. Regular: 90 eligible/paid hours at $1 = $90. Enterprise: 100 eligible hours selects $0.80, but only 90 paid hours = $72. The free allowance is per user. This is intentionally the larger request; import the collection to avoid copying its long cURL.

Expected HTTP **200**. `X-Invalid-Event-Count: 0`.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
