curl.exe --include --request POST http://localhost:8080/api/v1/billing/calculate --header 'Content-Type: application/json' --data-binary "@$PSScriptRoot/request.json"
