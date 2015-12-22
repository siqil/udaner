import sys
import random

with open(sys.argv[1]) as f:
    sentences = []
    tokens = []
    for line in f:
        line = line.strip()
        if len(line) == 0:
            sentences.append("\n".join(tokens))
            tokens = []
        else:
            tokens.append(line)
    if len(tokens) > 0:
        sentences.append("\n".join(tokens))
    random.shuffle(sentences)
    p = int(len(sentences) * 0.3)
    test = sentences[:p]
    train = sentences[p:]

with open(sys.argv[2], "w") as f:
    f.write("\n\n".join(train))
    f.write("\n")


with open(sys.argv[3], "w") as f:
    f.write("\n\n".join(test))
    f.write("\n")
