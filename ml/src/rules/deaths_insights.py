import pandas as pd

from db import engine


MIN_PLAYER_CHAMPION_MATCHES = 3
MIN_BASELINE_MATCHES = 5
MIN_AVG_DEATHS = 6.0
MIN_DEATHS_OVER_BASELINE_RATIO = 0.15


def load_data() -> pd.DataFrame:
    query = """
        select
            puuid,
            champion_id,
            champion_name,
            deaths
        from analyzed.v_player_match_stats
        where puuid is not null
          and champion_id is not null
          and deaths is not null
    """

    return pd.read_sql(query, engine)


def generate_deaths_insights() -> list[dict]:
    df = load_data()

    if df.empty:
        return []

    champion_baselines = (
        df.groupby("champion_id")["deaths"]
        .agg(["mean", "count"])
        .rename(columns={"mean": "baseline_deaths", "count": "baseline_games"})
    )

    insights = []

    for (puuid, champion_id), group in df.groupby(["puuid", "champion_id"], dropna=False):
        if len(group) < MIN_PLAYER_CHAMPION_MATCHES:
            continue

        baseline = champion_baselines.loc[champion_id]
        if baseline["baseline_games"] < MIN_BASELINE_MATCHES:
            continue

        avg_deaths = float(group["deaths"].mean())
        baseline_deaths = float(baseline["baseline_deaths"])
        if baseline_deaths <= 0:
            continue

        difference = avg_deaths - baseline_deaths
        percent_difference = difference / baseline_deaths

        if avg_deaths < MIN_AVG_DEATHS or percent_difference < MIN_DEATHS_OVER_BASELINE_RATIO:
            continue

        champion_name = get_champion_name(group)

        insights.append({
            "puuid": puuid,
            "champion_id": int(champion_id),
            "insight_type": "HIGH_DEATHS",
            "title": "Deaths are higher than expected",
            "description": (
                f"On {champion_name}, your deaths are above the champion baseline. "
                f"Slow down before objectives and wait for teammates before entering fog."
            ),
            "title_uk": "Забагато смертей",
            "description_uk": (
                f"На {champion_name} ти в середньому помираєш {avg_deaths:.1f} разів за гру. "
                f"Базовий показник для чемпіона - {baseline_deaths:.1f}, тому варто зменшити ризикові смерті."
            ),
            "language": "en+uk",
            "metric_name": "deaths",
            "metric_value": avg_deaths,
            "baseline_value": baseline_deaths,
            "difference_value": difference,
            "confidence": calculate_confidence(len(group), percent_difference),
            "sample_size": int(len(group)),
        })

    print(f"Generated deaths insights: {len(insights)}")
    return insights


def get_champion_name(group: pd.DataFrame) -> str:
    names = group["champion_name"].dropna().unique()
    return str(names[0]) if len(names) else "this champion"


def calculate_confidence(sample_size: int, percent_difference: float) -> float:
    sample_factor = min(0.30, sample_size / 100)
    difference_factor = min(0.20, float(percent_difference) / 2)

    return float(round(min(0.95, 0.50 + sample_factor + difference_factor), 2))
