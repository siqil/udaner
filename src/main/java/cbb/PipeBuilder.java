package cbb;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cbb.feature.Word;
import cbb.feature.WordClass;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTaggerSentence2TokenSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.tsf.ModOffsetConjunctions;
import cc.mallet.pipe.tsf.TokenTextCharPrefix;
import cc.mallet.pipe.tsf.TokenTextCharSuffix;
import cc.mallet.scl.SCL;
import cc.mallet.scl.SCLAugment;
import cc.mallet.scl.SCLTrainer;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class PipeBuilder {
	static final String GREEK = "(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)";
	private LinkedList<Pipe> pipes;
	private Iterator<Instance> sclSourceIt;
	private Iterator<Instance> sclTargetIt;
	private boolean useSCL = false;
	private File W = null;
	private int h = 25;
	private int threads = 1;
	private int minOccurPivot = 50;
	private int maxNumPivots = 1000;
	private boolean useMI = true;

	protected void addFeatures() {

		pipes.add(new Word(null));
		pipes.add(new WordClass());

		// copied from BANNER CRFTagger
		// pipes.add(new RegexMatches("ALPHA", Pattern.compile("[A-Za-z]+")));
		// pipes.add(new RegexMatches("INITCAPS", Pattern.compile("[A-Z].*")));
		// pipes.add(new RegexMatches("UPPER-LOWER", Pattern
		// .compile("[A-Z][a-z].*")));
		// pipes.add(new RegexMatches("LOWER-UPPER", Pattern
		// .compile("[a-z]+[A-Z]+.*")));
		// pipes.add(new RegexMatches("ALLCAPS", Pattern.compile("[A-Z]+")));
		// pipes.add(new RegexMatches("MIXEDCAPS", Pattern
		// .compile("[A-Z][a-z]+[A-Z][A-Za-z]*")));
		// pipes.add(new RegexMatches("SINGLECHAR",
		// Pattern.compile("[A-Za-z]")));
		// pipes.add(new RegexMatches("SINGLEDIGIT", Pattern.compile("[0-9]")));
		// pipes.add(new RegexMatches("DOUBLEDIGIT",
		// Pattern.compile("[0-9][0-9]")));
		// pipes.add(new RegexMatches("NUMBER", Pattern.compile("[0-9,]+")));
		// pipes.add(new RegexMatches("HASDIGIT",
		// Pattern.compile(".*[0-9].*")));
		// pipes.add(new RegexMatches("ALPHANUMERIC", Pattern
		// .compile(".*[0-9].*[A-Za-z].*")));
		// pipes.add(new RegexMatches("ALPHANUMERIC", Pattern
		// .compile(".*[A-Za-z].*[0-9].*")));
		// pipes.add(new RegexMatches("LETTERS_NUMBERS", Pattern
		// .compile("[0-9]+[A-Za-z]+")));
		// pipes.add(new RegexMatches("NUMBERS_LETTERS", Pattern
		// .compile("[A-Za-z]+[0-9]+")));
		//
		// pipes.add(new RegexMatches("HAS_DASH", Pattern.compile(".*-.*")));
		// pipes.add(new RegexMatches("HAS_QUOTE", Pattern.compile(".*'.*")));
		// pipes.add(new RegexMatches("HAS_SLASH", Pattern.compile(".*/.*")));

		// Start second set of new features (to handle improvements in
		// BaseTokenizer)
		// pipes.add(new RegexMatches("REALNUMBER", Pattern
		// .compile("(-|\\+)?[0-9,]+(\\.[0-9]*)?%?")));
		// pipes.add(new RegexMatches("REALNUMBER", Pattern
		// .compile("(-|\\+)?[0-9,]*(\\.[0-9]+)?%?")));
		// pipes.add(new RegexMatches("START_MINUS", Pattern.compile("-.*")));
		// pipes.add(new RegexMatches("START_PLUS", Pattern.compile("\\+.*")));
		// pipes.add(new RegexMatches("END_PERCENT", Pattern.compile(".*%")));
		// End second set

		pipes.add(new TokenTextCharPrefix("2PREFIX=", 2));
		pipes.add(new TokenTextCharPrefix("3PREFIX=", 3));
		pipes.add(new TokenTextCharPrefix("4PREFIX=", 4));
		pipes.add(new TokenTextCharSuffix("2SUFFIX=", 2));
		pipes.add(new TokenTextCharSuffix("3SUFFIX=", 3));
		pipes.add(new TokenTextCharSuffix("4SUFFIX=", 4));
		// pipes.add(new TokenTextCharNGrams("CHARNGRAM=", new int[] { 2, 3 },
		// true));
		// pipes.add(new RegexMatches("ROMAN", Pattern.compile("[IVXDLCM]+",
		// Pattern.CASE_INSENSITIVE)));
		// pipes.add(new RegexMatches("GREEK", Pattern.compile(GREEK,
		// Pattern.CASE_INSENSITIVE)));
		// pipes.add(new RegexMatches("ISPUNCT", Pattern
		// .compile("[`~!@#$%^&*()-=_+\\[\\]\\\\{}|;\':\\\",./<>?]+")));
		pipes.add(new ModOffsetConjunctions(new int[][] { { -2 }, { 2 },
				{ -1 }, { 1 }, { 0 } }));
	}

	public PipeBuilder useSCL(Iterator<Instance> sourceIt,
			Iterator<Instance> targetIt) {
		this.sclSourceIt = sourceIt;
		this.sclTargetIt = targetIt;
		this.useSCL = true;
		return this;
	}

	public PipeBuilder setW(File W) {
		this.W = W;
		return this;
	}

	public PipeBuilder setH(int h) {
		this.h = h;
		return this;
	}

	public PipeBuilder setThreads(int n) {
		this.threads = n;
		return this;
	}

	public PipeBuilder setMaxNumPivots(int k) {
		this.maxNumPivots = k;
		return this;
	}

	private SCL trainSCL(InstanceList sourceTrainInstances,
			InstanceList targetTrainInstances) {
		SCL scl;
		if (W != null) {
			SCLTrainer sclTrainer = new SCLTrainer();
			scl = sclTrainer
					.train(W, h, sourceTrainInstances.getDataAlphabet());
		} else {
			SCLTrainer sclTrainer = new SCLTrainer();
			sclTrainer.setH(h);
			sclTrainer.setThreads(threads);
			sclTrainer.setMinOccurPivot(minOccurPivot);
			sclTrainer.setMaxNumPivots(maxNumPivots);
			sclTrainer.setUseMI(useMI);
			scl = sclTrainer.train(sourceTrainInstances, targetTrainInstances);
		}
		return scl;
	}

	public Pipe build() {
		pipes = new LinkedList<Pipe>();
		pipes.add(new SimpleTaggerSentence2TokenSequence(false));
		addFeatures();
		pipes.add(new TokenSequence2FeatureVectorSequence(true, false));
		if (useSCL) {
			Pipe pipe = new SerialPipes(pipes);
			InstanceList sourceTrainInstances = new InstanceList(pipe);
			sourceTrainInstances.addThruPipe(sclSourceIt);
			InstanceList targetTrainInstances = new InstanceList(pipe);
			targetTrainInstances.addThruPipe(sclTargetIt);
			SCL scl = trainSCL(sourceTrainInstances, targetTrainInstances);
			pipes.removeLast();
			pipes.add(new TokenSequence2FeatureVectorSequence(false, true));
			pipes.add(new SCLAugment(scl));
		}
		return new SerialPipes(pipes);
	}

	public PipeBuilder setMinOccurPivot(int threshold) {
		this.minOccurPivot = threshold;
		return this;
	}

	public PipeBuilder useMI(boolean useMI) {
		this.useMI = useMI;
		return this;
	}
}
