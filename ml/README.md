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
