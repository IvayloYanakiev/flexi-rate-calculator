# Omitted cycle uses the user's current month

billing_cycle is absent. 41 valid events use the generation month's dates and cost $0.30. The returned cycle uses the server clock. The Postman pre-request script refreshes event dates to the current UTC month automatically; standalone JSON/cURL files are dated snapshots and can be refreshed by rerunning the generator.

Expected HTTP **200**. `X-Invalid-Event-Count: 0`.

Files: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.
