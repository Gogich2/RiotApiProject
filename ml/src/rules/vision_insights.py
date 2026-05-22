import pandas as pd

from db import engine


def load_data() -> pd.DataFrame:
    query = """
        select
            puuid,
            champion_id,
            champion_name,
            win,
            vision_score
        from analyzed.v_player_match_stats
        where puuid is not null
          and vision_score is not null
    """

    return pd.read_sql(query, engine)


def generate_vision_insights() -> list[dict]:
    df = load_data()

    print(f"Loaded rows: {len(df)}")

    if df.empty:
        return []

    insights = []

    grouped = df.groupby(["puuid", "champion_id"], dropna=False)
    print(f"Groups: {len(grouped)}")

    checked = 0
    skipped_no_win_loss = 0
    skipped_low_difference = 0

    for (puuid, champion_id), group in grouped:
        checked += 1

        wins = group[group["win"] == True]
        losses = group[group["win"] == False]

        if len(wins) < 1 or len(losses) < 1:
            skipped_no_win_loss += 1
            continue

        win_vision = wins["vision_score"].mean()
        loss_vision = losses["vision_score"].mean()

        if loss_vision <= 0:
            continue

        difference = win_vision - loss_vision
        percent_difference = difference / loss_vision

        if percent_difference < 0.05:
            skipped_low_difference += 1
            continue

        champion_name = get_champion_name(group)

        title_en = "Improve vision around objective setup"
        description_en = (
            f"On {champion_name}, your wins show better vision control. "
            f"Place river wards earlier and swap to Oracle Lens after lane phase."
        )

        title_uk = "Огляд мапи може впливати на твої результати"
        description_uk = (
            f"На {champion_name} твій середній vision score у перемогах становить {win_vision:.1f}, "
            f"а в поразках — {loss_vision:.1f}. Варто звернути увагу на варди та контроль ключових зон."
        )

        insights.append({
            "puuid": puuid,
            "champion_id": int(champion_id) if pd.notna(champion_id) else None,
            "insight_type": "VISION_WEAKNESS",

            "title": title_en,
            "description": description_en,

            "title_uk": title_uk,
            "description_uk": description_uk,
            "language": "en+uk",

            "metric_name": "vision_score",
            "metric_value": float(win_vision),
            "baseline_value": float(loss_vision),
            "difference_value": float(difference),
            "confidence": float(calculate_confidence(len(group), percent_difference)),
            "sample_size": int(len(group))
        })

    print(f"Checked groups: {checked}")
    print(f"Skipped no win/loss pair: {skipped_no_win_loss}")
    print(f"Skipped low difference: {skipped_low_difference}")
    print(f"Generated vision insights: {len(insights)}")

    return insights


def get_champion_name(group: pd.DataFrame) -> str:
    if "champion_name" not in group.columns:
        return "this champion"

    names = group["champion_name"].dropna().unique()

    if len(names) == 0:
        return "this champion"

    return str(names[0])


def calculate_confidence(sample_size: int, percent_difference: float) -> float:
    sample_factor = min(0.30, sample_size / 100)
    difference_factor = min(0.20, float(percent_difference) / 2)

    return float(round(min(0.95, 0.50 + sample_factor + difference_factor), 2))
