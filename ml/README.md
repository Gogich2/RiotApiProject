# ML Insights Module

This module generates rule-based player insights and writes them for the Java application to read. Java only reads these generated insights through `GET /api/players/{puuid}/insights`; recommendation generation stays in Python.

## Setup

Create a virtual environment if desired, then install dependencies:

```bash
pip install -r ml/requirements.txt
```

Create `ml/.env` from `ml/.env.example`:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=your_database_name
DB_USER=postgres
DB_PASSWORD=your_password
```

The module loads these values with `python-dotenv`. `ml/.env` is loaded relative to the module location, so the same config is used whether the generator is run from the repository root or from `ml`.

## Run

From the repository root:

```bash
python ml/src/main.py
```

From the `ml` directory:

```bash
python src/main.py
```

If your Windows `python` launcher is not configured, use:

```bash
py ml/src/main.py
```

Running without arguments keeps the existing full generation behavior. It deletes and rewrites only the generated insight types listed below.

### Full generation

```bash
python ml/src/main.py --mode all
```

`--mode all` is also the default:

```bash
python ml/src/main.py
```

### Refresh stale players gradually

Refresh at most 20 players whose generated insights are missing, stale, or older than their latest match data. By default, candidates must have at least 5 analyzed rows in `analyzed.v_player_match_stats`:

```bash
python ml/src/main.py --mode refresh-stale --limit 20 --stale-hours 24 --min-matches 5
```

You can use days instead of hours:

```bash
python ml/src/main.py --mode refresh-stale --limit 20 --stale-days 1 --min-matches 5
```

This mode processes a limited, stable batch ordered with missing generated insights first, then oldest generated insights first. It does not run a full all-player refresh, and it skips low-sample players so a batch is not spent on one-match players that cannot produce useful recommendations.

### Regenerate one player

```bash
python ml/src/main.py --mode player --puuid SOME_PUUID
```

This regenerates insights only for that `puuid`. It deletes old generated insight types only for that player and leaves manual or unrelated insight types untouched.

Player mode does not enforce `--min-matches`; it may be used for any specific `puuid`.

### Scheduled refresh examples

Windows Task Scheduler action:

```text
Program/script: C:\Path\To\Python\python.exe
Arguments: ml/src/main.py --mode refresh-stale --limit 20 --stale-hours 24 --min-matches 5
Start in: D:\Games\RiotApiPractice
```

Cron example:

```cron
*/30 * * * * cd /path/to/RiotApiPractice && python ml/src/main.py --mode refresh-stale --limit 20 --stale-hours 24 --min-matches 5
```

## Data Contract

Reads from:

```text
analyzed.v_player_match_stats
```

Writes to:

```text
analyzed.player_insights
```

Generated insight types are:

```text
VISION_WEAKNESS
HIGH_DEATHS
LOW_DAMAGE
CS_WEAKNESS
STRONG_CHAMPION
CONSISTENT_PERFORMER
```

Before inserting new generated rows, the module deletes only these generated insight types. It does not delete unrelated or manually created insight rows.

Generated candidates are ranked before saving. For each player, the generator limits repeated recommendations per insight type, then keeps only the strongest overall insights so one category, such as vision, does not flood the recommendations page.
