import sys
import re
ifilename = sys.argv[1]
ofilename = sys.argv[2]
sep = "(\W)"
pattern = "%s\t%s"

def new_item(word, tag):
    """Create a new line"""
    return pattern % (word, tag)

def add_line(index, word, tag, lines):
    """Add a new line into lines"""
    if len(word)>0:
        if tag.startswith("B"):
            if len(lines) == 0:
                lines.append(new_item(word, tag))
            else:
                lines.append(new_item(word, "I"+tag[1:]))
        else:
            lines.append(new_item(word, tag))


with open(ifilename) as ifile:
    with open(ofilename,"w") as ofile:
        for line in ifile:
            line = line.strip()
            if len(line) > 0:
                word, tag = line.split("\t")
                if len(word) > 1:
                    lines = []
                    subs = re.split(sep, word)
                    if len(subs) > 0:
                        for i, subword in enumerate(subs):
                            add_line(i, subword, tag, lines)
                        line = "\n".join(lines)
            print >>ofile, line
