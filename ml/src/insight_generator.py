from sqlalchemy import text

from db import engine
from rules.champion_strength_insights import generate_champion_strength_insights
from rules.cs_insights import generate_cs_insights
from rules.damage_insights import generate_damage_insights
from rules.deaths_insights import generate_deaths_insights
from rules.vision_insights import generate_vision_insights


GENERATED_INSIGHT_TYPES = (
    "VISION_WEAKNESS",
    "HIGH_DEATHS",
    "LOW_DAMAGE",
    "CS_WEAKNESS",
    "STRONG_CHAMPION",
    "CONSISTENT_PERFORMER",
)


def clear_old_insights() -> None:
    with engine.begin() as conn:
        conn.execute(text("""
            delete from analyzed.player_insights
            where insight_type = any(:insight_types)
        """), {"insight_types": list(GENERATED_INSIGHT_TYPES)})


def normalize_insight(insight: dict) -> dict:
    normalized = dict(insight)

    for key in ["metric_value", "baseline_value", "difference_value", "confidence"]:
        if normalized.get(key) is not None:
            normalized[key] = float(normalized[key])

    if normalized.get("sample_size") is not None:
        normalized["sample_size"] = int(normalized["sample_size"])

    if normalized.get("champion_id") is not None:
        normalized["champion_id"] = int(normalized["champion_id"])

    return normalized


def save_insights(insights: list[dict]) -> int:
    if not insights:
        return 0

    with engine.begin() as conn:
        for insight in insights:
            insight = normalize_insight(insight)

            conn.execute(text("""
                insert into analyzed.player_insights
                (
                    puuid,
                    champion_id,
                    insight_type,
                    title,
                    description,
                    title_uk,
                    description_uk,
                    language,
                    metric_name,
                    metric_value,
                    baseline_value,
                    difference_value,
                    confidence,
                    sample_size,
                    created_at
                )
                values
                (
                    :puuid,
                    :champion_id,
                    :insight_type,
                    :title,
                    :description,
                    :title_uk,
                    :description_uk,
                    :language,
                    :metric_name,
                    :metric_value,
                    :baseline_value,
                    :difference_value,
                    :confidence,
                    :sample_size,
                    now()
                )
            """), insight)

    return len(insights)


def generate_all_insights() -> int:
    clear_old_insights()

    insights = []
    insights.extend(generate_vision_insights())
    insights.extend(generate_deaths_insights())
    insights.extend(generate_damage_insights())
    insights.extend(generate_cs_insights())
    insights.extend(generate_champion_strength_insights())

    return save_insights(insights)
