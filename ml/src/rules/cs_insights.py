import pandas as pd

from db import engine


MIN_PLAYER_CHAMPION_MATCHES = 3
MIN_BASELINE_MATCHES = 5
MIN_CS_PER_MINUTE = 4.5
MIN_CS_BELOW_BASELINE_RATIO = 0.15
MILLISECONDS_PER_MINUTE = 60_000


def load_data() -> pd.DataFrame:
    query = """
        select
            puuid,
            champion_id,
            champion_name,
            total_minions_killed,
            neutral_minions_killed,
            game_duration_ms
        from analyzed.v_player_match_stats
        where puuid is not null
          and champion_id is not null
          and total_minions_killed is not null
          and game_duration_ms is not null
          and game_duration_ms > 0
    """

    df = pd.read_sql(query, engine)

    if df.empty:
        return df

    df["neutral_minions_killed"] = df["neutral_minions_killed"].fillna(0)
    df["total_cs"] = df["total_minions_killed"] + df["neutral_minions_killed"]
    df["game_minutes"] = df["game_duration_ms"] / MILLISECONDS_PER_MINUTE
    df["cs_per_minute"] = df["total_cs"] / df["game_minutes"]

    return df[df["cs_per_minute"].notna()]


def generate_cs_insights() -> list[dict]:
    df = load_data()

    if df.empty:
        return []

    champion_baselines = (
        df.groupby("champion_id")["cs_per_minute"]
        .agg(["mean", "count"])
        .rename(columns={"mean": "baseline_cs_per_minute", "count": "baseline_games"})
    )

    insights = []

    for (puuid, champion_id), group in df.groupby(["puuid", "champion_id"], dropna=False):
        if len(group) < MIN_PLAYER_CHAMPION_MATCHES:
            continue

        baseline = champion_baselines.loc[champion_id]
        if baseline["baseline_games"] < MIN_BASELINE_MATCHES:
            continue

        avg_cs = float(group["cs_per_minute"].mean())
        baseline_cs = float(baseline["baseline_cs_per_minute"])
        if baseline_cs <= 0:
            continue

        difference = avg_cs - baseline_cs
        percent_below = (baseline_cs - avg_cs) / baseline_cs

        if avg_cs >= MIN_CS_PER_MINUTE and percent_below < MIN_CS_BELOW_BASELINE_RATIO:
            continue

        champion_name = get_champion_name(group)

        insights.append({
            "puuid": puuid,
            "champion_id": int(champion_id),
            "insight_type": "CS_WEAKNESS",
            "title": "CS pace is lower than expected",
            "description": (
                f"On {champion_name}, you average {avg_cs:.1f} CS per minute. "
                f"The champion baseline is {baseline_cs:.1f}, so improving wave and jungle camp collection may help."
            ),
            "title_uk": "Темп фарму нижчий за очікуваний",
            "description_uk": (
                f"На {champion_name} ти в середньому маєш {avg_cs:.1f} CS за хвилину. "
                f"Базовий показник для чемпіона - {baseline_cs:.1f}, тому варто покращити збір хвиль і таборів."
            ),
            "language": "en+uk",
            "metric_name": "cs_per_minute",
            "metric_value": avg_cs,
            "baseline_value": baseline_cs,
            "difference_value": difference,
            "confidence": calculate_confidence(len(group), max(percent_below, 0)),
            "sample_size": int(len(group)),
        })

    print(f"Generated CS insights: {len(insights)}")
    return insights


def get_champion_name(group: pd.DataFrame) -> str:
    names = group["champion_name"].dropna().unique()
    return str(names[0]) if len(names) else "this champion"


def calculate_confidence(sample_size: int, percent_difference: float) -> float:
    sample_factor = min(0.30, sample_size / 100)
    difference_factor = min(0.20, float(percent_difference) / 2)

    return float(round(min(0.95, 0.50 + sample_factor + difference_factor), 2))
