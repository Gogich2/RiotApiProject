import pandas as pd
from sqlalchemy import text

from db import engine


MIN_STRONG_CHAMPION_MATCHES = 3
MIN_CONSISTENT_MATCHES = 5
MIN_STRONG_WINRATE = 0.60
MIN_WINRATE_OVER_PLAYER_BASELINE = 0.10
MIN_CONSISTENT_WINRATE = 0.50
MIN_AVG_KDA = 2.5
MAX_KDA_COEFFICIENT_OF_VARIATION = 0.55


def load_data(puuid: str | None = None) -> pd.DataFrame:
    query = """
        select
            puuid,
            champion_id,
            champion_name,
            win,
            kills,
            deaths,
            assists
        from analyzed.v_player_match_stats
        where puuid is not null
          and (:puuid is null or puuid = :puuid)
          and champion_id is not null
          and win is not null
          and kills is not null
          and deaths is not null
          and assists is not null
    """

    df = pd.read_sql(text(query), engine, params={"puuid": puuid})

    if df.empty:
        return df

    df["kda"] = (df["kills"] + df["assists"]) / df["deaths"].replace(0, 1)
    return df


def generate_champion_strength_insights(puuid: str | None = None) -> list[dict]:
    df = load_data(puuid)

    if df.empty:
        return []

    insights = []
    player_winrate_baselines = df.groupby("puuid")["win"].mean()

    for (puuid, champion_id), group in df.groupby(["puuid", "champion_id"], dropna=False):
        insights.extend(generate_strong_champion_insight(
            puuid,
            champion_id,
            group,
            float(player_winrate_baselines.loc[puuid]),
        ))
        insights.extend(generate_consistent_performer_insight(puuid, champion_id, group))

    print(f"Generated champion strength insights: {len(insights)}")
    return insights


def generate_strong_champion_insight(
    puuid: str,
    champion_id: int,
    group: pd.DataFrame,
    player_winrate: float,
) -> list[dict]:
    if len(group) < MIN_STRONG_CHAMPION_MATCHES:
        return []

    champion_winrate = float(group["win"].mean())
    difference = champion_winrate - player_winrate

    if champion_winrate < MIN_STRONG_WINRATE or difference < MIN_WINRATE_OVER_PLAYER_BASELINE:
        return []

    champion_name = get_champion_name(group)

    return [{
        "puuid": puuid,
        "champion_id": int(champion_id),
        "insight_type": "STRONG_CHAMPION",
        "title": "This champion is one of your stronger picks",
        "description": (
            f"On {champion_name}, your winrate is meaningfully above your overall baseline. "
            f"Consider using this pick more often in similar matchups."
        ),
        "title_uk": "Цей чемпіон є одним із твоїх сильніших виборів",
        "description_uk": (
            f"На {champion_name} твій вінрейт становить {champion_winrate * 100:.0f}%. "
            f"Твій загальний базовий показник - {player_winrate * 100:.0f}%, тож цей вибір працює добре."
        ),
        "language": "en+uk",
        "metric_name": "winrate",
        "metric_value": champion_winrate * 100,
        "baseline_value": player_winrate * 100,
        "difference_value": difference * 100,
        "confidence": calculate_confidence(len(group), difference),
        "sample_size": int(len(group)),
    }]


def generate_consistent_performer_insight(
    puuid: str,
    champion_id: int,
    group: pd.DataFrame,
) -> list[dict]:
    if len(group) < MIN_CONSISTENT_MATCHES:
        return []

    avg_kda = float(group["kda"].mean())
    kda_std = float(group["kda"].std(ddof=0))
    coefficient_of_variation = kda_std / avg_kda if avg_kda > 0 else None
    winrate = float(group["win"].mean())

    if (
        coefficient_of_variation is None
        or coefficient_of_variation > MAX_KDA_COEFFICIENT_OF_VARIATION
        or avg_kda < MIN_AVG_KDA
        or winrate < MIN_CONSISTENT_WINRATE
    ):
        return []

    champion_name = get_champion_name(group)

    return [{
        "puuid": puuid,
        "champion_id": int(champion_id),
        "insight_type": "CONSISTENT_PERFORMER",
        "title": "Your performance is consistent on this champion",
        "description": (
            f"On {champion_name}, your KDA is stable across matches. "
            f"This looks like a reliable pick when you need consistency."
        ),
        "title_uk": "Ти стабільно граєш на цьому чемпіоні",
        "description_uk": (
            f"На {champion_name} твій середній KDA становить {avg_kda:.1f}, а результати між матчами стабільні. "
            f"Це надійний вибір, коли потрібна передбачувана гра."
        ),
        "language": "en+uk",
        "metric_name": "kda_consistency",
        "metric_value": avg_kda,
        "baseline_value": MAX_KDA_COEFFICIENT_OF_VARIATION,
        "difference_value": coefficient_of_variation,
        "confidence": calculate_confidence(len(group), 1 - coefficient_of_variation),
        "sample_size": int(len(group)),
    }]


def get_champion_name(group: pd.DataFrame) -> str:
    names = group["champion_name"].dropna().unique()
    return str(names[0]) if len(names) else "this champion"


def calculate_confidence(sample_size: int, signal_strength: float) -> float:
    sample_factor = min(0.30, sample_size / 100)
    signal_factor = min(0.20, max(float(signal_strength), 0) / 2)

    return float(round(min(0.95, 0.50 + sample_factor + signal_factor), 2))
