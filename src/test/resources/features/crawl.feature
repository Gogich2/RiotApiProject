Feature: Crawl EUW summoner matches

  Scenario: Crawl EUW summoner matches
    Given EUW summoner "acoomer" exists
    When I crawl last 5 matches
    Then matches are saved

  Scenario: No matches returned from Riot API
    Given EUW summoner "acoomer" exists
    When I crawl last 5 matches and no matches exist
    Then no matches are saved

  Scenario: All matches already exist in database
    Given EUW summoner "acoomer" exists
    When I crawl last 5 matches but all already exist
    Then no matches are saved
