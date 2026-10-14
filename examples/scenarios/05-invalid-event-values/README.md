# Wrong values and missing fields are skipped

Skips duration 20, start time 10:07, an invalid date, a missing duration, and null. One valid 15-minute event remains; invalid count 5.

Expected HTTP **200**. `X-Invalid-Event-Count: 5`.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
