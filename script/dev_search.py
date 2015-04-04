import sys
import os


JAR_PATH = "code/np2amr/target/np2amr-1.0-SNAPSHOT.jar"
SMATCH_PATH = "tools/smatch/smatch.py"
DATA_PATH = "data"


def print_cmd(cmd_lst):
    print >> sys.stderr, " ".join(cmd_lst)


def parse_and_eval(model_path, test, beam, rm=True, pr=False):
    tmp_path = "parse.{}.{}.{}".format(uuid, os.path.basename(test),
            ".".join(model_path.split("/")[-2:]))  # parse result is stored in this file

    # parse dev set
    #with open(tmp_path, "w") as f, open(os.devnull) as devnull:
    with open(tmp_path, "w") as f:
        # concept identification
        parse_cmd = ["java", "-jar", JAR_PATH,
                "-test", test,
                "-model", os.path.dirname(model_path),
                "-ws", os.path.basename(model_path),
                "-beam", str(beam),
                ]
        parse_p = subprocess.Popen(parse_cmd, stdout=f)
        print_cmd(parse_cmd)
        parse_p.wait()

    # calculate smatch
    smatch_cmd = ["python", SMATCH_PATH, "-f", tmp_path, test]
    if pr:
        smatch_cmd.append("--pr")   # report also precision and recall
    smatch_p = subprocess.Popen(smatch_cmd, stdout=subprocess.PIPE)
    print_cmd(smatch_cmd)
    smatch_p.wait()

    res = []
    for line in smatch_p.stdout.readlines():
        line = line.rstrip()
        print >> sys.stderr, line
        res.append(line)

    if rm:
        # clean up the temporary file
        rm_cmd = ["rm", tmp_path]
        rm_p = subprocess.Popen(rm_cmd)
        print_cmd(rm_cmd)
        rm_p.wait()

    return res


if __name__ == "__main__":
    import argparse
    import subprocess
    import uuid
    import os
    import re

    parser = argparse.ArgumentParser(
        description="Calculate score of each model on development set, take the best model, evaluate on test set")
    parser.add_argument("dev", help="gold data for dev set")
    parser.add_argument("test", help="gold data for test set")
    parser.add_argument("model", nargs="+", help="models to compare")
    parser.add_argument("--beam", type=int, default=1, help="beam width")
    parser.add_argument("--norm", action="store_true", help="don't remove intermediate files")
    parser.add_argument("--jar", help="path to jar file of parser")
    parser.add_argument("--smatch", help="path to smatch file")
    args = parser.parse_args()

    # overwrite path
    if not args.jar is None:
        JAR_PATH = args.jar
    if not args.smatch is None:
        SMATCH_PATH = args.smatch

    best_score = 0
    best_model_path = None

    uuid = uuid.uuid4()     # needed for naming tmp file

    models = args.model
    pat = re.compile("[0-9]*$")
    models.sort(key=lambda n: int(pat.search(n).group()))
    for model_path in models:
        res = parse_and_eval(model_path, args.dev, args.beam, rm=not args.norm)
        score = float(res[-1].split()[-1])
        if score >= best_score:
            best_score = score
            best_model_path = model_path

    # parse test set using best scoring model
    res = parse_and_eval(best_model_path, args.test, args.beam, rm=not args.norm, pr=True)
    print best_model_path
    for line in res:
        print line

