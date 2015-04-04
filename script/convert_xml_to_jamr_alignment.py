import sys
import os


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(
        description="Convert XML file defining noun compound AMR fragments to JAMR alignment format")
    parser.add_argument("xml", help="path to xml file")
    args = parser.parse_args()

