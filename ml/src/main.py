from insight_generator import generate_all_insights


def main() -> None:
    generated_count = generate_all_insights()
    print(f"Generated {generated_count} insights.")


if __name__ == "__main__":
    main()