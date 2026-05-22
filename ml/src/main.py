import argparse

from insight_generator import (
    generate_all_insights,
    generate_insights_for_player,
    refresh_stale_insights,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate player insights.")
    parser.add_argument(
        "--mode",
        choices=["all", "refresh-stale", "player"],
        default="all",
        help="Generation mode. Default: all.",
    )
    parser.add_argument("--puuid", help="Player PUUID for --mode player.")
    parser.add_argument(
        "--limit",
        type=int,
        default=20,
        help="Maximum players to process in refresh-stale mode. Default: 20.",
    )
    parser.add_argument(
        "--min-matches",
        type=int,
        default=5,
        help="Minimum analyzed match rows required for refresh-stale candidates. Default: 5.",
    )
    parser.add_argument(
        "--stale-hours",
        type=float,
        help="Refresh generated insights older than this many hours.",
    )
    parser.add_argument(
        "--stale-days",
        type=float,
        default=1,
        help="Refresh generated insights older than this many days. Default: 1.",
    )

    args = parser.parse_args()

    if args.mode == "player" and not args.puuid:
        parser.error("--puuid is required when --mode player is used")

    if args.mode == "refresh-stale" and args.limit < 1:
        parser.error("--limit must be greater than 0")

    if args.mode == "refresh-stale" and args.min_matches < 1:
        parser.error("--min-matches must be greater than 0")

    if args.stale_hours is not None and args.stale_days != 1:
        parser.error("Use either --stale-hours or --stale-days, not both")

    return args


def main() -> None:
    args = parse_args()
    stale_hours = args.stale_hours if args.stale_hours is not None else args.stale_days * 24

    print(f"Mode: {args.mode}")

    if args.mode == "all":
        generated_count = generate_all_insights()
        print("Candidate player count: all")
        print("Refreshed player count: all")
        print(f"Generated insight count: {generated_count}")
        print("Skipped/error count: 0")
        return

    if args.mode == "player":
        generated_count = generate_insights_for_player(args.puuid)
        print("Candidate player count: 1")
        print("Refreshed player count: 1")
        print(f"Generated insight count: {generated_count}")
        print("Skipped/error count: 0")
        return

    result = refresh_stale_insights(args.limit, stale_hours, args.min_matches)
    print(f"Refresh limit: {args.limit}")
    print(f"Stale threshold hours: {stale_hours}")
    print(f"Minimum match rows: {args.min_matches}")
    print(f"Candidate player count: {result['candidate_count']}")
    print(f"Refreshed player count: {result['refreshed_count']}")
    print(f"Generated insight count: {result['generated_count']}")
    print(f"Skipped/error count: {result['error_count']}")


if __name__ == "__main__":
    main()
