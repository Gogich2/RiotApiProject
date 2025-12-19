package org.main.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/summoner")
public class SummonerController {

    @Value("${riot.api.key}")
    private String riotApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/{name}")
    public ResponseEntity<?> getSummoner(@PathVariable String name) {
        String url = "https://europe.api.riotgames.com/lol/match/v5/matches/EUW1_7491582034/timeline";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        // Save response body to a JSON file
        try {
            // Create file path, e.g., projectRoot/data/summoner_<name>.json
            String filePath = Paths.get("data", "summoner_" + name + ".json").toString();
            java.nio.file.Files.createDirectories(Paths.get("data")); // ensure "data" folder exists

            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(response.getBody());
            }

            System.out.println("✅ Data saved to: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save data to file");
        }

        // Also return the response back to API caller
        return ResponseEntity.ok(response.getBody());
    }
}
