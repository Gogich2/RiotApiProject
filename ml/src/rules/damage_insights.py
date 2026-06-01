import pandas as pd
from sqlalchemy import text

from db import engine


MIN_PLAYER_CHAMPION_MATCHES = 3
MIN_BASELINE_MATCHES = 5
MIN_BASELINE_DAMAGE = 8_000
MIN_DAMAGE_BELOW_BASELINE_RATIO = 0.15
DAMAGE_COLUMN = "total_damage_dealt_to_champions"


def load_data(puuid: str | None = None) -> pd.DataFrame:
    query = f"""
        select
            puuid,
            champion_id,
            champion_name,
            {DAMAGE_COLUMN}
        from analyzed.v_player_match_stats
        where puuid is not null
          and champion_id is not null
          and (
              :puuid is null
              or puuid = :puuid
              or champion_id in (
                  select distinct champion_id
                  from analyzed.v_player_match_stats
                  where puuid = :puuid
                    and champion_id is not null
              )
          )
          and {DAMAGE_COLUMN} is not null
          and {DAMAGE_COLUMN} > 0
    """

    return pd.read_sql(text(query), engine, params={"puuid": puuid})


def generate_damage_insights(puuid: str | None = None) -> list[dict]:
    df = load_data(puuid)

    if df.empty:
        return []

    champion_baselines = (
        df.groupby("champion_id")[DAMAGE_COLUMN]
        .agg(["mean", "count"])
        .rename(columns={"mean": "baseline_damage", "count": "baseline_games"})
    )

    insights = []

    player_df = df[df["puuid"] == puuid] if puuid is not None else df

    for (puuid, champion_id), group in player_df.groupby(["puuid", "champion_id"], dropna=False):
        if len(group) < MIN_PLAYER_CHAMPION_MATCHES:
            continue

        baseline = champion_baselines.loc[champion_id]
        if baseline["baseline_games"] < MIN_BASELINE_MATCHES:
            continue

        avg_damage = float(group[DAMAGE_COLUMN].mean())
        baseline_damage = float(baseline["baseline_damage"])
        if baseline_damage < MIN_BASELINE_DAMAGE:
            continue

        difference = avg_damage - baseline_damage
        percent_below = (baseline_damage - avg_damage) / baseline_damage

        if percent_below < MIN_DAMAGE_BELOW_BASELINE_RATIO:
            continue

        champion_name = get_champion_name(group)

        insights.append({
            "puuid": puuid,
            "champion_id": int(champion_id),
            "insight_type": "LOW_DAMAGE",
            "title": "Damage output is below the champion baseline",
            "description": (
                f"On {champion_name}, your champion damage is below baseline. "
                f"Look for safer trades and avoid dying before major fights."
            ),
            "title_uk": "Замало шкоди по чемпіонах",
            "description_uk": (
                f"На {champion_name} ти в середньому завдаєш {avg_damage:.0f} шкоди по чемпіонах. "
                f"Базовий показник для чемпіона - {baseline_damage:.0f}, тому варто шукати більше безпечних обмінів і участі в бійках."
            ),
            "language": "en+uk",
            "metric_name": DAMAGE_COLUMN,
            "metric_value": avg_damage,
            "baseline_value": baseline_damage,
            "difference_value": difference,
            "confidence": calculate_confidence(len(group), percent_below),
            "sample_size": int(len(group)),
        })

    print(f"Generated damage insights: {len(insights)}")
    return insights


def get_champion_name(group: pd.DataFrame) -> str:
    names = group["champion_name"].dropna().unique()
    return str(names[0]) if len(names) else "this champion"


def calculate_confidence(sample_size: int, percent_difference: float) -> float:
    sample_factor = min(0.30, sample_size / 100)
    difference_factor = min(0.20, float(percent_difference) / 2)

    return float(round(min(0.95, 0.50 + sample_factor + difference_factor), 2))
