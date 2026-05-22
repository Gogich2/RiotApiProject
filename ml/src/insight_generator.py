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

MAX_INSIGHTS_PER_TYPE = {
    "VISION_WEAKNESS": 2,
    "HIGH_DEATHS": 2,
    "LOW_DAMAGE": 2,
    "CS_WEAKNESS": 2,
    "STRONG_CHAMPION": 2,
    "CONSISTENT_PERFORMER": 2,
}
MAX_INSIGHTS_PER_PLAYER = 10
MIN_POSITIVE_INSIGHTS_WHEN_AVAILABLE = 1
POSITIVE_INSIGHT_TYPES = {
    "STRONG_CHAMPION",
    "CONSISTENT_PERFORMER",
}
ACTIONABLE_NEGATIVE_TYPES = {
    "VISION_WEAKNESS",
    "HIGH_DEATHS",
    "LOW_DAMAGE",
    "CS_WEAKNESS",
}


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


def rank_and_limit_insights(insights: list[dict]) -> list[dict]:
    by_player: dict[str, list[dict]] = {}

    for insight in insights:
        puuid = insight.get("puuid")
        if not puuid:
            continue

        by_player.setdefault(str(puuid), []).append(insight)

    curated = []

    for player_insights in by_player.values():
        per_type_limited = limit_per_type(player_insights)
        curated.extend(limit_player_total(per_type_limited))

    return sorted(curated, key=insight_sort_key)


def limit_per_type(insights: list[dict]) -> list[dict]:
    by_type: dict[str, list[dict]] = {}

    for insight in insights:
        insight_type = str(insight.get("insight_type", ""))
        by_type.setdefault(insight_type, []).append(insight)

    limited = []

    for insight_type, type_insights in by_type.items():
        limit = MAX_INSIGHTS_PER_TYPE.get(insight_type, 2)
        limited.extend(sorted(type_insights, key=insight_sort_key)[:limit])

    return limited


def limit_player_total(insights: list[dict]) -> list[dict]:
    sorted_insights = sorted(insights, key=insight_sort_key)

    if len(sorted_insights) <= MAX_INSIGHTS_PER_PLAYER:
        return sorted_insights

    selected = sorted_insights[:MAX_INSIGHTS_PER_PLAYER]
    positive_candidates = [
        insight for insight in sorted_insights
        if insight.get("insight_type") in POSITIVE_INSIGHT_TYPES
    ]

    if positive_candidates and not any(
        insight.get("insight_type") in POSITIVE_INSIGHT_TYPES
        for insight in selected
    ):
        selected = selected[:MAX_INSIGHTS_PER_PLAYER - MIN_POSITIVE_INSIGHTS_WHEN_AVAILABLE]
        selected.extend(positive_candidates[:MIN_POSITIVE_INSIGHTS_WHEN_AVAILABLE])

    return sorted(selected, key=insight_sort_key)


def insight_sort_key(insight: dict) -> tuple:
    insight_type = str(insight.get("insight_type", ""))
    priority = 0 if insight_type in ACTIONABLE_NEGATIVE_TYPES else 1

    return (
        priority,
        -safe_float(insight.get("confidence")),
        -safe_int(insight.get("sample_size")),
        -abs(safe_float(insight.get("difference_value"))),
    )


def safe_float(value) -> float:
    if value is None:
        return 0.0

    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def safe_int(value) -> int:
    if value is None:
        return 0

    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def generate_all_insights() -> int:
    clear_old_insights()

    insights = []
    insights.extend(generate_vision_insights())
    insights.extend(generate_deaths_insights())
    insights.extend(generate_damage_insights())
    insights.extend(generate_cs_insights())
    insights.extend(generate_champion_strength_insights())

    curated_insights = rank_and_limit_insights(insights)
    print(f"Curated insights after ranking and limits: {len(curated_insights)}")

    return save_insights(curated_insights)
