# ML Insights Module

This module generates rule-based player insights and writes them for the Java application to read.

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

The module loads these values with `python-dotenv`.

## Run

From the repository root:

```bash
python ml/src/main.py
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

The Java Spring Boot application only reads generated rows from `analyzed.player_insights`; insight generation stays in this Python module.
