Feature: Crawl summoner matches

  Scenario: Crawl EUW summoner matches
    Given EUW summoner "acoomer" exists
    When I crawl last 5 matches
    Then matches are saved
