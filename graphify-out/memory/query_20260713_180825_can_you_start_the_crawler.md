---
type: "query"
date: "2026-07-13T18:08:25.453781+00:00"
question: "can you start the crawler?"
contributor: "graphify"
outcome: "useful"
source_nodes: ["CrawlerController", "CrawlerServiceImpl", "BalancedDatasetCrawlerServiceImpl", "RiotApiHttpClient"]
---

# Q: can you start the crawler?

## Answer

Expanded from original query via graph vocab: [crawler, crawl, controller, service, riot, matches, balanced, dataset, player, summoner]. The graph identified CrawlerController, CrawlerServiceImpl, and BalancedDatasetCrawlerServiceImpl. A bounded latest-player crawl was selected instead of the large balanced sweep. Runtime evidence showed the request reached RiotApiHttpClient but Riot returned HTTP 401 Unknown apikey, so no match IDs or new matches were saved.

## Outcome

- Signal: useful

## Source Nodes

- CrawlerController
- CrawlerServiceImpl
- BalancedDatasetCrawlerServiceImpl
- RiotApiHttpClient