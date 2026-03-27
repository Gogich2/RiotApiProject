## Збірка проєкту
```bash
mvn clean package
```

У результаті буде створено `.jar` файл у папці `target`.

## Налаштування СУБД

### Створення бази даних
Після встановлення PostgreSQL потрібно створити базу даних:

```sql
CREATE DATABASE riot_api_project;
```

### Створення користувача
```sql
CREATE USER riot_user WITH PASSWORD 'strong_password';
GRANT ALL PRIVILEGES ON DATABASE riot_api_project TO riot_user;
```

### Налаштування `application.properties`
У файлі `src/main/resources/application.properties` потрібно вказати параметри підключення до БД.

Приклад:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/riot_api_project
spring.datasource.username=riot_user
spring.datasource.password=strong_password
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
```

## Розгортання коду

### Збірка застосунку
```bash
mvn clean package
```

### Запуск jar-файлу
```bash
java -jar target\RiotApiPractice-1.0-SNAPSHOT.jar
```

## Запуск у production
Для production можна використовувати `.bat`-скрипт або запуск через Планувальник завдань Windows.

Приклад запуску через `.bat`:

```bat
@echo off
cd /d D:\Projects\RiotApiProject
java -jar target\RiotApiPractice-1.0-SNAPSHOT.jar
pause
```

## Перевірка працездатності

### Перевірка запуску застосунку
Відкрити в браузері:

```text
http://localhost:8080
```

### Перевірка через curl
```bash
curl http://localhost:8080
```

### Перевірка PostgreSQL
Потрібно переконатися, що:

- застосунок запускається без помилок;
- у консолі немає помилок підключення до БД;
- таблиці створюються або оновлюються коректно.

## Ознаки успішного розгортання
Розгортання вважається успішним, якщо:

- застосунок запускається без критичних помилок;
- є стабільне підключення до PostgreSQL;
- застосунок відповідає на HTTP-запити;
- дані коректно зберігаються в базі даних.