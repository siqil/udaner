import sys
with open(sys.argv[1]) as f:
    print sum(1 for line in f if line == "\n")
