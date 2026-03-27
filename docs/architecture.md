# Архітектура проєкту

## Короткий опис
RiotApiProject — це серверний застосунок для отримання, обробки та збереження даних з Riot API.  
Основна логіка реалізована на Java з використанням Spring Boot.  
Проєкт також використовує PostgreSQL для збереження даних і містить окремий лендінг у папці `docs`.

## Основні структурні елементи проєкту
Проєкт включає такі компоненти:

- **Web server / Application server**  
  Використовується Spring Boot із вбудованим вебсервером для обробки HTTP-запитів і запуску REST API.

- **REST API**  
  Основна точка взаємодії із системою.  
  У проєкті є контролер `CrawlerController`, який приймає запити та передає їх у сервісний шар.

- **Service layer**  
  Бізнес-логіка винесена в `CrawlerService` та `CrawlerServiceImpl`.  
  Цей шар відповідає за виклики Riot API, обробку даних і координацію збереження.

- **Riot API client**  
  Для роботи із зовнішнім Riot API використовуються `RiotApiClient` та `RiotApiHttpClient`.

- **Persistence layer**  
  Для доступу до бази даних використовуються JPA-сутності та репозиторії:
    - `MatchEntity`
    - `PlayerEntity`
    - `SummonerEntity`
    - `MatchRepository`
    - `PlayerRepository`

- **СУБД**  
  Використовується **PostgreSQL**.

- **Файлове сховище**  
  Окреме файлове сховище у проєкті не використовується.  
  Статичні файли лендінгу зберігаються безпосередньо в репозиторії у папці `docs`.

- **Сервіси кешування**  
  На поточному етапі окремі сервіси кешування не використовуються.

- **Тестова підсистема**  
  У проєкті є unit, integration і cucumber тести.

- **CI**  
  У репозиторії налаштовано GitHub Actions workflow для запуску тестів і перевірок.

## Структура проєкту
- `src/main/java` — основний код застосунку
- `src/main/resources` — конфігураційні файли
- `src/test/java` — тести
- `src/test/resources` — feature-файли для Cucumber
- `docs` — лендінг і додаткова документація
- `.github/workflows` — CI

## Діаграма архітектури

    Користувач / клієнт
            |
            v
     REST API (CrawlerController)
            |
            v
     Service layer (CrawlerService, CrawlerServiceImpl)
            |
            +----------------------+
            |                      |
            v                      v
     Riot API client         Persistence layer
     (RiotApiClient,         (JPA entities,
      RiotApiHttpClient)      repositories)
            |                      |
            v                      v
        Riot API               PostgreSQL

## Висновок
Архітектура проєкту побудована за багатошаровим підходом: контролер, сервісний шар, клієнт зовнішнього API та шар доступу до даних. Це спрощує підтримку, тестування й подальше розширення функціоналу.