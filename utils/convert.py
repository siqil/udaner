import sys
with open(sys.argv[1]) as in_file:
    with open(sys.argv[2],"w") as out_file:
        for line in in_file:
            token_seq = [token.rsplit("/",1) for token in line.strip().split(" ")[1:]]
            prev = None
            for i, token in enumerate(token_seq):
                word, tag = token
                tmp = tag
                if tag.startswith("NEWGENE"):
                    if tag == prev:
                        tag = "I-GENE"
                    else:
                        tag = "B-GENE"
                else:
                    tag = "O"
                prev = tmp
                token_seq[i] = "\t".join((word, tag))
            out_file.write("\n".join(token_seq))
            out_file.write("\n\n")
