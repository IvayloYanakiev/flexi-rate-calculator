"""Generate documented API cases, standalone cURL imports and a local Postman collection.

Expected results below come from the confirmed billing rules, not recorded service output.
Run with --verify-base-url to compare every example with a real running service.
"""
import argparse
import copy
import datetime as dt
import json
from pathlib import Path
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parent
URL_PATH = "/api/v1/billing/calculate"
CYCLE = "2026-10"
CURRENT_CYCLE = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m")


def user(user_id="alice", enterprise=False, timezone="UTC"):
    return {"user_id": user_id, "is_enterprise": enterprise, "timezone": timezone}


def event(event_id, user_id="alice", timestamp="2026-10-02T10:00:00Z", duration=15):
    return {"event_id": event_id, "user_id": user_id, "timestamp": timestamp, "duration_minutes": duration}


def chunks(user_id="alice", count=41, off_peak_only=False, billing_cycle=CYCLE):
    year, month = map(int, billing_cycle.split("-"))
    timestamp = dt.datetime(year, month, 2, tzinfo=dt.timezone.utc)
    events = []
    while len(events) < count:
        if not off_peak_only or not 9 <= timestamp.hour < 17:
            events.append(event(f"{user_id}-{len(events) + 1}", user_id, timestamp.isoformat().replace("+00:00", "Z")))
        timestamp += dt.timedelta(minutes=15)
    return events


def request(users=None, events=None, billing_cycle=CYCLE):
    payload = {"users": users if users is not None else [user()], "events": events if events is not None else []}
    if billing_cycle is not None:
        payload["billing_cycle"] = billing_cycle
    return payload


def bill(user_id="alice", total_hours=0, tier=1, hourly_rate=1.0, peak_hours=0, enterprise=False, cycle=CYCLE):
    free_hours = min(total_hours, 10)
    paid_hours = total_hours - free_hours
    base_cost = round(paid_hours * hourly_rate, 2)
    surcharge_cost = round(peak_hours * hourly_rate * 0.20, 2)
    return {
        "user_id": user_id, "billing_cycle": cycle, "total_cost": round(base_cost + surcharge_cost, 2),
        "tier_breakdown": [{"tier": tier, "hourly_rate": hourly_rate,
                            "eligible_hours": total_hours if enterprise else paid_hours,
                            "free_hours": free_hours, "billable_hours": paid_hours, "cost": base_cost}],
        "applied_surcharges": [] if not peak_hours else [
            {"type": "UTC_PEAK", "percentage": 0.20, "billable_peak_hours": peak_hours, "cost": surcharge_cost}]
    }


GENERIC_ERROR = {"code": "INVALID_REQUEST", "message": "Provide valid JSON, users with IDs/enterprise flags/timezones, and an events array."}
CASES = []


def add(case_id, title, description, payload, bills=None, invalid_count=0, status=200, error=None, dynamic_cycle=False):
    CASES.append({"id": case_id, "title": title, "description": description, "payload": payload,
                  "expected_body": {"users": bills or []} if status == 200 else error,
                  "status": status, "invalid_count": invalid_count if status == 200 else None,
                  "dynamic_cycle": dynamic_cycle})


add("01-valid-with-billing-cycle", "Valid snapshot with an explicit billing cycle",
    "41 consecutive quarter-hour chunks = 10.25 hours. The first 10 hours are free; the last paid chunk is peak usage. Base $0.25 + surcharge $0.05 = $0.30.",
    request(events=chunks()), [bill(total_hours=10.25, peak_hours=0.25)])

add("02-malformed-event-type", "Array instead of integer: skip one event",
    "The exact client example: duration_minutes is [15]. That event is skipped, the valid event remains, and X-Invalid-Event-Count is 1.",
    request(events=[event("bad-array", duration=[15]), event("good")]), [bill(total_hours=0.25)], invalid_count=1)

add("03-duplicate-users", "Duplicate user IDs reject the request",
    "Two users have user_id alice. This is a request-level error, unlike duplicate events; HTTP 400 and no invalid-event header.",
    request(users=[user(), user(timezone="Europe/Sofia")]), status=400,
    error={"code": "INVALID_REQUEST", "message": "Duplicate user_id."})

add("04-without-billing-cycle", "Omitted cycle uses the user's current month",
    "billing_cycle is absent. 41 valid events use the generation month's dates and cost $0.30. The returned cycle uses the server clock. The Postman pre-request script refreshes event dates to the current UTC month automatically; standalone JSON/cURL files are dated snapshots and can be refreshed by rerunning the generator.",
    request(events=chunks(billing_cycle=CURRENT_CYCLE), billing_cycle=None),
    [bill(total_hours=10.25, peak_hours=0.25, cycle=CURRENT_CYCLE)], dynamic_cycle=True)

add("05-invalid-event-values", "Wrong values and missing fields are skipped",
    "Skips duration 20, start time 10:07, an invalid date, a missing duration, and null. One valid 15-minute event remains; invalid count 5.",
    request(events=[event("wrong-duration", duration=20), event("off-quarter", timestamp="2026-10-02T10:07:00Z"),
                    event("bad-date", timestamp="2026-02-30T10:00:00Z"),
                    {"event_id": "missing-duration", "user_id": "alice", "timestamp": "2026-10-02T10:00:00Z"},
                    None, event("good")]), [bill(total_hours=0.25)], invalid_count=5)

add("06-duplicate-events", "Duplicate event IDs are counted once",
    "The first valid event wins. The repeated ID is skipped, so usage is 15 minutes rather than 30 minutes; invalid count 1.",
    request(events=[event("same-id"), event("same-id")]), [bill(total_hours=0.25)], invalid_count=1)

add("07-unknown-user", "Unknown event user is skipped",
    "An event references a user absent from users. That event is skipped; the known user's event is still billed.",
    request(events=[event("unknown", user_id="missing-user"), event("known")]), [bill(total_hours=0.25)], invalid_count=1)

add("08-empty-events", "Users with no events receive zero bills",
    "Both listed users receive a bill in input order even though events is empty.",
    request(users=[user(), user("bob", enterprise=True)]), [bill(), bill("bob", enterprise=True)])

add("09-invalid-timezone", "Invalid user timezone rejects the request",
    "The user timezone is needed for month boundaries; an invalid timezone is a request-level HTTP 400 error.",
    request(users=[user(timezone="Invalid/Timezone")]), status=400,
    error={"code": "INVALID_REQUEST", "message": "Invalid user timezone."})

add("10-invalid-billing-cycle", "Invalid explicit month rejects the request",
    "Month 13 does not follow the required YYYY-MM cycle format; HTTP 400.",
    request(billing_cycle="2026-13"), status=400,
    error={"code": "INVALID_REQUEST", "message": "billing_cycle must be YYYY-MM."})

add("11-timezone-month-boundary", "The same UTC instant belongs to different local months",
    "2026-09-30T21:00Z is October 1 in Sofia but September 30 in UTC. For billing_cycle 2026-10, only Sofia's event counts. The UTC user's valid out-of-month event does not increase the invalid count.",
    request(users=[user("sofia", timezone="Europe/Sofia"), user("utc")], events=[
        event("sofia-boundary", "sofia", "2026-09-30T21:00:00Z"), event("utc-boundary", "utc", "2026-09-30T21:00:00Z")]),
    [bill("sofia", total_hours=0.25), bill("utc")])

add("12-enterprise-tier-comparison", "Enterprise grace counts toward the rate threshold",
    "Each user has 100 hours from 400 chronological off-peak chunks. Regular: 90 eligible/paid hours at $1 = $90. Enterprise: 100 eligible hours selects $0.80, but only 90 paid hours = $72. The free allowance is per user. This is intentionally the larger request; import the collection to avoid copying its long cURL.",
    request(users=[user("regular"), user("enterprise", enterprise=True)], events=
            chunks("regular", 400, True) + chunks("enterprise", 400, True)),
    [bill("regular", total_hours=100), bill("enterprise", total_hours=100, tier=2, hourly_rate=0.8, enterprise=True)])

add("13-peak-versus-off-peak", "16:45 is peak; 17:00 is off-peak",
    "Both users get their own 10 free hours. Each then has one paid quarter hour: 16:45 UTC costs $0.30 with surcharge; 17:00 UTC costs $0.25 without surcharge.",
    request(users=[user("peak"), user("off-peak")], events=chunks("peak", 40) + chunks("off-peak", 40) + [
        event("paid-peak", "peak", "2026-10-02T16:45:00Z"), event("paid-off-peak", "off-peak", "2026-10-02T17:00:00Z")]),
    [bill("peak", 10.25, peak_hours=0.25), bill("off-peak", 10.25)])

add("14-all-events-invalid", "All invalid events still produce a successful batch",
    "null, an empty event object and an array-valued duration are all invalid. Return HTTP 200, a zero bill, and invalid count 3.",
    request(events=[None, {}, event("bad-array", duration=[15])]), [bill()], invalid_count=3)

add("15-invalid-request-shape", "The events container itself must be an array",
    "An event may be malformed, but events itself must be an array. This request has an object instead and returns HTTP 400.",
    {"users": [user()], "events": {}, "billing_cycle": CYCLE}, status=400, error=GENERIC_ERROR)

add("16-malformed-json-document", "Truncated JSON rejects the entire document",
    "request.json is intentionally invalid JSON (the closing brace is missing). Item recovery cannot repair a broken document; HTTP 400.",
    '{"users":[],"events":[]', status=400, error=GENERIC_ERROR)


def dump(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def payload_text(case):
    return case["payload"] if isinstance(case["payload"], str) else json.dumps(case["payload"], indent=2)


def expected_body(case):
    expected = copy.deepcopy(case["expected_body"])
    if case["dynamic_cycle"]:
        for user_bill in expected["users"]:
            user_bill["billing_cycle"] = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m")
    return expected


def postman_tests(case):
    expected = json.dumps(case["expected_body"], separators=(",", ":"))
    lines = [f"const expected = {expected};"]
    if case["dynamic_cycle"]:
        lines.append("expected.users.forEach(userBill => userBill.billing_cycle = new Date().toISOString().slice(0, 7));")
    lines += [f"pm.test('HTTP {case['status']}', () => pm.response.to.have.status({case['status']}));",
              "pm.test('JSON content type', () => pm.expect(pm.response.headers.get('Content-Type')).to.include('application/json'));",
              "pm.test('Complete response matches the billing contract', () => pm.expect(pm.response.json()).to.deep.equal(expected));"]
    if case["invalid_count"] is None:
        lines.append("pm.test('Rejected request has no invalid-event count', () => pm.expect(pm.response.headers.has('X-Invalid-Event-Count')).to.equal(false));")
    else:
        lines.append(f"pm.test('Invalid-event count {case['invalid_count']}', () => pm.expect(pm.response.headers.get('X-Invalid-Event-Count')).to.equal('{case['invalid_count']}'));")
    return lines


def generate():
    collection = {"info": {"name": "Flexi-Rate Calculator - verified examples",
                            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
                            "description": "Local billing examples with status, header and full-body assertions. No authentication required."},
                  "variable": [{"key": "baseUrl", "value": "http://localhost:8080", "type": "string"}], "item": []}
    manifest = []
    curl_sections = ["# Copy/paste cURL requests for Postman", "",
                     "Start the service first. In Postman choose Import and paste an entire cURL command, including the inline body.",
                     "These commands contain JSON directly; no local payload files are needed for import. Change localhost:8080 if necessary.", ""]
    for case in CASES:
        directory = ROOT / "scenarios" / case["id"]
        directory.mkdir(parents=True, exist_ok=True)
        body = payload_text(case)
        (directory / "request.json").write_text(body + "\n", encoding="utf-8")
        dump(directory / "response.json", case["expected_body"])
        headers = {"Content-Type": "application/json"}
        if case["invalid_count"] is not None:
            headers["X-Invalid-Event-Count"] = str(case["invalid_count"])
        dump(directory / "response-headers.json", headers)
        compact_body = body if isinstance(case["payload"], str) else json.dumps(case["payload"], separators=(",", ":"))
        assert "'" not in compact_body, "The inline POSIX cURL body requires shell-safe quoting."
        curl = ("curl --include --request POST 'http://localhost:8080" + URL_PATH
                + "' --header 'Content-Type: application/json' --data-raw '" + compact_body + "'\n")
        (directory / "curl.txt").write_text(curl, encoding="utf-8")
        powershell = "curl.exe --include --request POST http://localhost:8080" + URL_PATH + " --header 'Content-Type: application/json' --data-binary \"@$PSScriptRoot/request.json\"\n"
        (directory / "curl.ps1").write_text(powershell, encoding="utf-8")
        readme = f"# {case['title']}\n\n{case['description']}\n\nExpected HTTP **{case['status']}**. "
        readme += "No invalid-event count header on this rejected request.\n" if case["invalid_count"] is None else f"`X-Invalid-Event-Count: {case['invalid_count']}`.\n"
        readme += "\nFiles: [request.json](request.json), [response.json](response.json), [response-headers.json](response-headers.json), [curl.txt](curl.txt) (paste into Postman Import), and [curl.ps1](curl.ps1) (run from PowerShell). Header samples contain stable fields only.\n"
        (directory / "README.md").write_text(readme, encoding="utf-8")
        curl_sections += [f"## {case['id']}: {case['title']}", "", case["description"], "",
                          f"Expected HTTP {case['status']}; invalid-event count: {case['invalid_count'] if case['invalid_count'] is not None else 'header absent'}.",
                          "", "```sh", curl.rstrip(), "```", ""]
        saved_request = {"method": "POST", "header": [{"key": "Content-Type", "value": "application/json"}],
                         "body": {"mode": "raw", "raw": body, "options": {"raw": {"language": "json"}}},
                         "url": {"raw": "{{baseUrl}}" + URL_PATH, "host": ["{{baseUrl}}"], "path": URL_PATH.strip("/").split("/")},
                         "description": case["description"]}
        collection["item"].append({"name": case["id"] + " - " + case["title"], "request": saved_request,
                                   "event": [{"listen": "test", "script": {"type": "text/javascript", "exec": postman_tests(case)}}],
                                   "response": [{"name": "Expected response", "originalRequest": copy.deepcopy(saved_request),
                                                 "status": "OK" if case["status"] == 200 else "Bad Request", "code": case["status"],
                                                 "header": [{"key": key, "value": value} for key, value in headers.items()],
                                                 "body": json.dumps(case["expected_body"], indent=2)}]})
        if case["dynamic_cycle"]:
            collection["item"][-1]["event"].insert(0, {"listen": "prerequest", "script": {
                "type": "text/javascript", "exec": [
                    "const body = JSON.parse(pm.request.body.raw);",
                    "const now = new Date();",
                    "const start = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 2);",
                    "body.events = Array.from({length: 41}, (_, index) => ({event_id: `alice-${index + 1}`, user_id: 'alice', timestamp: new Date(start + index * 15 * 60 * 1000).toISOString(), duration_minutes: 15}));",
                    "delete body.billing_cycle;",
                    "pm.request.body.update(JSON.stringify(body));"
                ]}})
        manifest.append({key: case[key] for key in ("id", "title", "description", "status", "invalid_count", "dynamic_cycle")})
    dump(ROOT / "flexi-rate.postman_collection.json", collection)
    dump(ROOT / "scenarios.json", manifest)
    (ROOT / "CURL-REQUESTS.md").write_text("\n".join(curl_sections), encoding="utf-8")
    print(f"Generated {len(CASES)} request/response/header/cURL scenarios and a Postman collection.")


def verify(base_url):
    results = []
    for case in CASES:
        http_request = urllib.request.Request(base_url.rstrip("/") + URL_PATH,
                data=payload_text(case).encode("utf-8"), headers={"Content-Type": "application/json"}, method="POST")
        try:
            response = urllib.request.urlopen(http_request, timeout=30)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            actual_body = json.loads(response.read())
            assert response.status == case["status"], (case["id"], response.status, case["status"])
            expected_header = None if case["invalid_count"] is None else str(case["invalid_count"])
            assert response.headers.get("X-Invalid-Event-Count") == expected_header, case["id"]
            assert "application/json" in response.headers.get("Content-Type", ""), case["id"]
            assert actual_body == expected_body(case), (case["id"], actual_body, expected_body(case))
            results.append({"id": case["id"], "status": response.status, "invalid_count": expected_header, "assertions": 4, "passed": True})
            print(f"PASS {case['id']}: HTTP {response.status}, invalid-count={expected_header}")
    directory = ROOT / "verification"
    directory.mkdir(exist_ok=True)
    dump(directory / "http-results.json", {"verified_at_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
         "target": base_url, "requests": len(results), "assertions": len(results) * 4, "results": results})


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--verify-base-url", help="Also assert every case against a running real service.")
    arguments = parser.parse_args()
    generate()
    if arguments.verify_base_url:
        verify(arguments.verify_base_url)
