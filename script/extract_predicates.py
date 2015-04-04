import sys
import os
from xml.etree import ElementTree as ET


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(
        description="Extract predicates from Ontonotes frame directory.")
    parser.add_argument("frames", help="path to Ontonotes frame directory")
    args = parser.parse_args()


    for filename in os.listdir(args.frames):
        if filename.endswith("-v.xml"):
            # only process frame definition of verbs
            path = os.path.join(args.frames, filename)
            try:
                tree = ET.parse(path)

                for e_pred in tree.findall(".//predicate"):
                    lemma = e_pred.get("lemma")
                    for e_roleset in e_pred.findall(".//roleset"):
                        pred_id = e_roleset.get("id").replace(".", "-")
                        arg_nums = []
                        for e_role in e_roleset.findall(".//role"):
                            arg_str = e_role.get("n")
                            try:
                                argn = int(arg_str)
                                arg_nums.append(arg_str)
                            except:
                                pass
                        print "\t".join([lemma, pred_id, ",".join(arg_nums)])
            except ET.ParseError as e:
                print >> sys.stderr, "Couldn't parse: " + path
