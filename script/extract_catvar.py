import sys
import os
import difflib


def find_american_counterpart(word, words):
    """Finds Americanized version of word in words"""
    for w2 in words:
        if word != w2 and word.replace("is", "iz") == w2:
            return True
    return False


# tests
assert find_american_counterpart("oxidisation", ["oxide", "oxidization"])
assert not find_american_counterpart("oxidisation", ["oxide"])
assert not find_american_counterpart("oxidization", ["oxide", "oxidisation"])
assert not find_american_counterpart("oxidization", ["oxide"])
assert not find_american_counterpart("agonist", ["agonized"])


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(
        description="Extract noun to verb translations from catvar file.")
    parser.add_argument("catvar", help="path to catvar file")
    parser.add_argument("--pos-from", default="N", help="POS to map from")
    parser.add_argument("--pos-to", default="V", help="POS to map to")
    args = parser.parse_args()

    for line in open(args.catvar):
        line = line.rstrip()
        verbs = []
        nouns = []
        adjs = []
        for word_pos in line.split("#"):
            word, pos = word_pos.split("_")
            if pos == "N":
                nouns.append(word)
            elif pos == "V":
                # ignore variation of British spelling
                if not word.endswith("ise"):
                    verbs.append(word)
            elif pos == "AJ":
                adjs.append(word)

        # remove British spellings from nouns
        for noun in nouns[:]:
            if find_american_counterpart(noun, nouns):
                nouns.remove(noun)

        def pos_to_list(pos, noun, verbs, adjs):
            if pos == "N":
                # do not convert to nouns when can be converted into verbs
                if not verbs:
                    return noun
                else:
                    return []
            elif pos == "V":
                return verbs
            elif pos == "AJ":
                return adjs
            assert False

        for f in pos_to_list(args.pos_from, nouns, verbs, adjs):
            for t in pos_to_list(args.pos_to, nouns, verbs, adjs):
                print "\t".join([f, t])

