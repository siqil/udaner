import sys
ifilename = sys.argv[1]
ofilename = sys.argv[2]
retain_tags = ["protein", "DNA", "RNA"]
replace_tag = "GENE"

with open(ifilename) as ifile:
    with open(ofilename,"w") as ofile:
        for line in ifile:
            line = line.strip()
            if len(line) > 0:
                word, tag = line.split("\t")
                if tag != "O":
                    ind, tag = tag.split("-")
                    if tag in retain_tags:
                        tag = ind + "-" + replace_tag
                    else:
                        tag = "O"
                line = word + "\t" + tag
            print >>ofile, line
