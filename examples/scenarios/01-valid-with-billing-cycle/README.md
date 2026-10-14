# Valid snapshot with an explicit billing cycle

41 consecutive quarter-hour chunks = 10.25 hours. The first 10 hours are free; the last paid chunk is peak usage. Base $0.25 + surcharge $0.05 = $0.30.

Expected HTTP **200**. `X-Invalid-Event-Count: 0`.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
