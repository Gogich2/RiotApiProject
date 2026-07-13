---
type: "query"
date: "2026-07-13T19:21:50.098245+00:00"
question: "check the api key separately, if it works look into the crawler functionality"
contributor: "graphify"
outcome: "corrected"
correction: "The key is valid. The 401 came from a stale inherited RIOT_API_KEY overriding .env, not from Riot rejecting the current .env key."
source_nodes: ["RiotApiHttpClient", "CrawlerServiceImpl", "CrawlerController", "RiotRateLimiter"]
---

# Q: check the api key separately, if it works look into the crawler functionality

## Answer

Expanded via graph vocabulary: [riot, api, http, client, key, header, config, crawler, token, property]. Direct EUW status and Europe Match V5 calls both returned 200 using the .env key. Root cause of application 401 was a stale process-level RIOT_API_KEY overriding .env; fingerprints differed. Starting Spring with RIOT_API_KEY explicitly loaded from .env produced a successful 20-match crawl with 20 timelines and no warnings or 429s. A second latest-player call selected a different moving player and saved 20 more; a fixed-PUUID repeat saved 0, confirming idempotency. Response issue: PUUID/latest endpoints return summonerName null.

## Outcome

- Signal: corrected
- Correction: The key is valid. The 401 came from a stale inherited RIOT_API_KEY overriding .env, not from Riot rejecting the current .env key.

## Source Nodes

- RiotApiHttpClient
- CrawlerServiceImpl
- CrawlerController
- RiotRateLimiter