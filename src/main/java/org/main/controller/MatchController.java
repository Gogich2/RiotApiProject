package org.main.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/matches")
public class MatchController {
    @Value("${riot.api.key}")
    private String riotApiKey;
    public String gameName = "Acoomer";
    public String tagline = "3595";

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/{name}")
    public ResponseEntity<?> getSummoner(@PathVariable String name) {
        String url = "https://europe.api.riotgames.com/riot/account/v1/accounts/by-riot-id/"+ gameName + "/" + tagline ;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return ResponseEntity.ok(response.getBody());
    }
}
