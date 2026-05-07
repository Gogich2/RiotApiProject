package org.main.service;

import org.main.dto.CrawlResultDto;

/**
 * Сервіс для запуску процесу отримання, обробки та збереження даних з Riot API.
 * <p>
 * Використовується як основний рівень бізнес-логіки для роботи з даними про матчі
 * та гравців.
 */
public interface CrawlerService {

    /**
     * Виконує отримання матчів для заданого PUUID у регіоні EUW,
     * обробляє отримані дані та зберігає нові матчі в базі даних.
     *
     * @param puuid унікальний ідентифікатор гравця в Riot API
     * @param limitRaw максимальна кількість матчів для обробки
     * @return результат виконання операції, включаючи кількість збережених матчів
     */
    CrawlResultDto crawlPuuidEUW(String puuid, int limitRaw);

    CrawlResultDto crawlRiotIdEUW(String gameNameRaw, String tagLineRaw, int limitRaw);

    CrawlResultDto crawlLatestPlayerEUW(int limitRaw);

    void crawlSummonerEUW(String acoomer, int limit);
}