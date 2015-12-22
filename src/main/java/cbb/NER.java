package cbb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import cc.mallet.fst.CRF;
import cc.mallet.fst.PerClassAccuracyEvaluator;
import cc.mallet.fst.TransducerEvaluator;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;

public class NER {

	private CRF crf;
	private Pipe pipe;
	private TransducerTrainer trainer;
	// private double[][] limits;
	// private Map<String, Integer> vocab;

	// SCL parameters
	@Option(name = "-scl", usage = "Structural correspondence learning")
	private boolean scl;

	// @Option(name = "-normalize", depends = { "-scl" }, usage =
	// "Blockwise normalization for SCL features")
	// private boolean normalize;

	@Option(name = "-overlap", depends = { "-scl" }, usage = "Forcing SCL to only use overlapping features")
	private boolean overlap;

	@Option(name = "-w", depends = { "-scl" })
	private File W;

	@Option(name = "-h", depends = { "-scl" }, usage = "SCL parameter h")
	private int h = 25;

	@Option(name = "-pt", depends = { "-scl" }, usage = "Threshold for SCL pivot features (minimum number of occurrences)")
	private int pivotThreshold = 50;

	@Option(name = "-method")
	private TrainingMethod method = TrainingMethod.LIKELIHOOD;

	@Option(name = "-order")
	private int order = 2;

	@Option(name = "-threads")
	private int threads = 1;

	// either training sets or model file should be provided
	// either testing set or model file should be provided
	@Option(name = "-source-train", usage = "Source domain training set")
	private File sourceTrainFile;

	@Option(name = "-target-train", usage = "Target domain training set")
	private File targetTrainFile;

	@Option(name = "-read-model", usage = "Read CRF model from file")
	private File inModel;

	@Option(name = "-write-model", usage = "Write CRF model to file")
	private File outModel;

	@Option(name = "-test", usage = "Test set")
	private File testFile;

	@Option(name = "-pred", usage = "Data set for prediction")
	private File predFile;
	//
	// @Option(name = "-vt")
	// private int vocabThreshold = 0;

	@Option(name = "-k", usage = "Bootstrap: step size K")
	private int k = 10;

	@Option(name = "-gamma", usage = "Feature subsetting: norm of weight (1 or 2)")
	private int gamma = 1;

	@Option(name = "-lambda", usage = "Feature subsetting: regularization weight (default 1)")
	private double lambda = 1;

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		new NER().doMain(args);
		// NamedEntityRecognizer ner = new NamedEntityRecognizer();
		// ner.setThreads(8);
		// // ner.setSCL(true);
		// // ner.setNormalize(true);
		// // ner.setModel(new File("std.model"));
		// ner.setMethod(TrainingMethod.BOOTSTRAPPING);
		// ner.evaluate("genetag_train", "genetag_test", "genia_train",
		// "genia_test");
	}

	public void doMain(String[] args) throws IOException,
			ClassNotFoundException {
		CmdLineParser parser = new CmdLineParser(this);
		parser.setUsageWidth(80);
		try {
			// parse the arguments.
			parser.parseArgument(args);
			if (args.length < 1)
				throw new CmdLineException(parser, "No argument is given");
			run();
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
		}
	}

	// public TransducerTrainer trainByEntropyRegularization(
	// InstanceList sourceTrainInstances, InstanceList targetTrainInstances) {
	// CRFTrainerByEntropyRegularization trainer = new
	// CRFTrainerByEntropyRegularization(
	// crf);
	// trainer.setEntropyWeight(lambda);
	// trainer.train(sourceTrainInstances, targetTrainInstances,
	// Integer.MAX_VALUE);
	// return trainer;
	// }

	// public TransducerTrainer trainByFeatureSubsetting(
	// InstanceList sourceTrainInstances, InstanceList targetTrainInstances) {
	// CRFTrainerByFeatureSubsetting trainer = new
	// CRFTrainerByFeatureSubsetting(
	// crf);
	// trainer.setNorm(norm);
	// trainer.setRegWeight(lambda);
	// trainer.train(sourceTrainInstances, targetTrainInstances,
	// Integer.MAX_VALUE);
	// return trainer;
	// }

	// public TransducerTrainer trainByLabelLikelihood(
	// InstanceList sourceTrainInstances, InstanceList targetTrainInstances) {
	// if (isOverlap()) {
	// CRFTrainerByOverlapFeatures trainer = new CRFTrainerByOverlapFeatures(
	// crf);
	// trainer.train(sourceTrainInstances, targetTrainInstances,
	// Integer.MAX_VALUE);
	// return trainer;
	// } else if (getThreads() <= 1) {
	// CRFTrainerByLabelLikelihood trainer = new CRFTrainerByLabelLikelihood(
	// crf);
	// trainer.train(sourceTrainInstances);
	// return trainer;
	// } else {
	// CRFTrainerByThreadedLabelLikelihood trainer = new
	// CRFTrainerByThreadedLabelLikelihood(
	// crf, getThreads());
	// trainer.train(sourceTrainInstances);
	// trainer.shutdown();
	// return trainer;
	// }
	//
	// }

	// public TransducerTrainer trainByBootstrap(
	// InstanceList sourceTrainInstances, InstanceList targetTrainInstances) {
	// Bootstrap trainer = new Bootstrap(crf, sourceTrainInstances,
	// targetTrainInstances);
	// // trainer.addEvaluator(new
	// // PerClassAccuracyEvaluator(targetTestInstances,
	// // "boostrapping evaluation (target test)"));
	// trainer.setThreads(threads);
	// trainer.setK(k);
	// crf = trainer.train();
	// return trainer;
	//
	// }

	protected Iterator<Instance> iteratorForFile(File file)
			throws FileNotFoundException {
		return new LineGroupIterator(new FileReader(file),
				Pattern.compile("^\\s*$"), true);
	}

	static void log(String msg) {
		System.err.println(msg);
	}

	public void train(File sourceTrainFile, File targetTrainFile)
			throws IOException, ClassNotFoundException {
		log("Source domain training set: " + sourceTrainFile);
		log("Target domain training set: " + targetTrainFile);
		log("Force using overlapping features: " + overlap);
		log("Threads: " + threads);

		PipeBuilder pb = new PipeBuilder();
		pb.setThreads(threads);
		if (scl) {
			log("\th: " + h);
			log("\tpivot threshold: " + pivotThreshold);
			pb.setH(h);
			pb.setMinOccurPivot(pivotThreshold);
			Iterator<Instance> sourceIt = iteratorForFile(sourceTrainFile);
			Iterator<Instance> targetIt = iteratorForFile(targetTrainFile);
			if (W != null)
				pb.setW(W);
			pb.useSCL(sourceIt, targetIt);
		}
		pipe = pb.build();
		train(iteratorForFile(sourceTrainFile),
				iteratorForFile(targetTrainFile));
	}

	public void train(Iterator<Instance> source, Iterator<Instance> target) {

		// if (normalize) {
		// limits = normalizeSCLBlock(new InstanceList[] {
		// sourceTrainInstances, targetTrainInstances }, null);
		// }
		log("Method: " + method);
		CRFBuilder builder = null;
		switch (getMethod()) {
		case BOOTSTRAPPING:
			log("\tK: " + k);
			builder = new CRFBuilderByBootstrapping(pipe).setK(k);
			break;
		case ENTROPY_REGULARIZATION:
			builder = new CRFBuilderByEntropyRegularization(pipe)
					.setLambda(lambda);
			break;
		case FEATURE_SUBSETTING:
			log("\tGamma: " + gamma);
			log("\tLambda: " + lambda);
			builder = new CRFBuilderByFeatureSubsetting(pipe).setLambda(lambda)
					.setGamma(gamma);
			break;
		case LIKELIHOOD:
			builder = new CRFBuilder(pipe).setOverlap(overlap);
			break;
		}
		crf = builder.setOrder(order).setThreads(threads)
				.byInstanceIterator(source, target).build();
		trainer = builder.getTrainer();
	}

	public void evaluate(File file) throws FileNotFoundException {
		// if (normalize) {
		// normalizeSCLBlock(new InstanceList[] { instances }, limits);
		// }
		InstanceList instances = new InstanceList(pipe);
		instances.addThruPipe(iteratorForFile(file));
		TransducerEvaluator evaluator = new PerClassAccuracyEvaluator(
				instances, "evaluation");
		evaluator.evaluate(trainer);
	}

	public void predict(File file) throws FileNotFoundException {
		Iterator<Instance> it = iteratorForFile(file);
		CRFLabeler labeler = new CRFLabeler(crf);
		while (it.hasNext()) {
			Instance instance = labeler.predict(it.next());
			Sequence<?> labels = (Sequence<?>) instance.getData();
			for (int i = 0; i < labels.size(); i++) {
				System.out.println(labels.get(i));
			}
			System.out.println();
		}
	}

	public void run() throws IOException, ClassNotFoundException {
		checkParams();
		if (crf == null) {
			if (inModel != null && inModel.exists()) {
				CRFBuilder builder = new CRFBuilder(pipe);
				crf = builder.byFile(inModel).build();
				trainer = builder.getTrainer();
				pipe = crf.getInputPipe();
			} else {
				train(sourceTrainFile, targetTrainFile);
			}
		}
		if (outModel != null) {
			writeModel(outModel);
		}
		if (testFile != null) {
			evaluate(testFile);
		}
		if (predFile != null) {
			predict(predFile);
		}
	}

	public void writeModel(File outModel) throws FileNotFoundException,
			IOException {
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
				outModel));
		out.writeObject(crf);
		out.close();
	}

	private void checkParams() {
		if (sourceTrainFile != null && targetTrainFile != null
				|| inModel != null) {
			if (outModel == null && testFile == null && predFile == null) {
				throw new IllegalArgumentException(
						"No output specified (either -write-model or -test should be provided.");
			}
		} else {
			throw new IllegalArgumentException(
					"No input specified (either -read-model or -source-train and -target-train should be provided.");
		}
	}

	public void setW(File W) {
		this.W = W;
	}

	public void setH(int h) {
		this.h = h;
	}

	public void setSCL(boolean scl) {
		this.scl = scl;
	}

	// public void setNormalize(boolean normalize) {
	// this.normalize = normalize;
	// }
	//
	// public int getVocabThreshold() {
	// return vocabThreshold;
	// }
	//
	// public void setVocabThreshold(int vocabThreshold) {
	// this.vocabThreshold = vocabThreshold;
	// }

	public int getPivotThreshold() {
		return pivotThreshold;
	}

	public void setPivotThreshold(int pivotThreshold) {
		this.pivotThreshold = pivotThreshold;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	public TrainingMethod getMethod() {
		return method;
	}

	public void setMethod(TrainingMethod method) {
		this.method = method;
	}

	public boolean isOverlap() {
		return overlap;
	}

	public void setOverlap(boolean overlap) {
		this.overlap = overlap;
	}

	// protected double[][] normalize(InstanceList[] ilists, double[][] limits)
	// {
	// int n = ilists[0].getDataAlphabet().size();
	// double[] min;
	// double[] max;
	// if (limits == null) {
	// limits = new double[2][n];
	// min = new double[n];
	// max = new double[n];
	// Arrays.fill(min, Double.MAX_VALUE);
	// Arrays.fill(max, Double.MIN_VALUE);
	// for (InstanceList ilist : ilists) {
	// for (Instance ins : ilist) {
	// FeatureVectorSequence vs = (FeatureVectorSequence) ins
	// .getData();
	// for (int i = 0; i < vs.size(); i++) {
	// FeatureVector v = vs.get(i);
	// for (int l = 0; l < v.numLocations(); l++) {
	// int j = v.indexAtLocation(l);
	// double val = v.valueAtLocation(l);
	// if (val < min[j]) {
	// min[j] = val;
	// }
	// if (val > max[j]) {
	// max[j] = val;
	// }
	// }
	// }
	// }
	// }
	// for (int j = 0; j < n; j++) {
	// max[j] = max[j] - min[j];
	// }
	// limits[0] = min;
	// limits[1] = max;
	// } else {
	// min = limits[0];
	// max = limits[1];
	// }
	// for (InstanceList ilist : ilists) {
	// for (Instance ins : ilist) {
	// FeatureVectorSequence vs = (FeatureVectorSequence) ins
	// .getData();
	// for (int i = 0; i < vs.size(); i++) {
	// FeatureVector v = vs.get(i);
	// for (int l = 0; l < v.numLocations(); l++) {
	// int j = v.indexAtLocation(l);
	// if (j < max.length && max[j] != 0) {
	// double val = v.valueAtLocation(l);
	// v.setValueAtLocation(l, (val - min[j]) / max[j]);
	// }
	// }
	// }
	// }
	// }
	// return limits;
	// }
	//
	// protected double[][] normalizeSCLBlock(InstanceList[] ilists,
	// double[][] limits) {
	// Alphabet features = ilists[0].getDataAlphabet();
	// Pattern pat = Pattern.compile("SCL_AUG.*");
	// double min;
	// double max;
	// if (limits == null) {
	// limits = new double[2][1];
	// min = Double.MAX_VALUE;
	// max = Double.MIN_VALUE;
	// for (InstanceList ilist : ilists) {
	// for (Instance ins : ilist) {
	// FeatureVectorSequence vs = (FeatureVectorSequence) ins
	// .getData();
	// for (int i = 0; i < vs.size(); i++) {
	// FeatureVector v = vs.get(i);
	// for (int l = 0; l < v.numLocations(); l++) {
	// int j = v.indexAtLocation(l);
	// String key = (String) features.lookupObject(j);
	// if (pat.matcher(key).matches()) {
	// double val = v.valueAtLocation(l);
	// if (min > val)
	// min = val;
	// if (max < val)
	// max = val;
	// }
	// }
	// }
	// }
	// }
	// max = max - min;
	// limits[0][0] = min;
	// limits[1][0] = max;
	// } else {
	// min = limits[0][0];
	// max = limits[1][0];
	// }
	// System.out.println("normalize block: min: " + min + " range: " + max);
	// for (InstanceList ilist : ilists) {
	// for (Instance ins : ilist) {
	// FeatureVectorSequence vs = (FeatureVectorSequence) ins
	// .getData();
	// for (int i = 0; i < vs.size(); i++) {
	// FeatureVector v = vs.get(i);
	// for (int l = 0; l < v.numLocations(); l++) {
	// int j = v.indexAtLocation(l);
	// String key = (String) features.lookupObject(j);
	// if (pat.matcher(key).matches()) {
	// double val = v.valueAtLocation(l);
	// v.setValueAtLocation(l, (val - min) / max);
	// }
	// }
	// }
	// }
	// }
	// return limits;
	// }

	// private void diagnose(File sourceTrainFile, File sourceTestFile,
	// File targetTrainFile, File targetTestFile) throws IOException,
	// ClassNotFoundException {
	// if (trainer == null)
	// train();
	//
	// InstanceList diag = new InstanceList(
	// new SimpleTaggerSentence2TokenSequence(false));
	// addInstances(diag, targetTestFile);
	//
	// // List<Pipe> pipes = new LinkedList<Pipe>();
	// // pipes.add(new SimpleTaggerSentence2TokenSequence(false));
	// // addFeatures(pipes, vocab.keySet());
	// // pipes.add(new TokenSequence2FeatureVectorSequence(true, false));
	// // SerialPipes pipe = new SerialPipes(pipes);
	// InstanceList pipeDiag = new InstanceList(pipe);
	// addInstances(pipeDiag, targetTestFile);
	// for (int i = 0; i < diag.size(); i++) {
	// Instance inst = diag.get(i);
	// Sequence trueTarget = (Sequence) inst.getTarget();
	// FeatureVectorSequence input = (FeatureVectorSequence) pipeDiag.get(
	// i).getData();
	// MaxLatticeDefault lattice = new MaxLatticeDefault(crf, input);
	// SumLatticeDefault slattice = new SumLatticeDefault(crf, input);
	// Sequence predTarget = lattice.bestOutputSequence();
	// SumLatticeDefault slattice2 = new SumLatticeDefault(crf, input,
	// predTarget);
	// double prob = Math.exp(slattice2.getTotalWeight()
	// - slattice.getTotalWeight());
	//
	// int correct = 0;
	// int n = predTarget.size();
	// for (int j = 0; j < n; j++) {
	// if (trueTarget.get(j).equals(predTarget.get(j))) {
	// correct++;
	// }
	// }
	// Sequence data = (Sequence) inst.getData();
	// for (int j = 0; j < data.size(); j++) {
	// System.out.println(data.get(j) + "\t" + predTarget.get(j)
	// + "\t" + trueTarget.get(j));
	// }
	// double acc = ((double) correct) / n;
	// System.out.println("accuracy: " + acc + " prob: " + prob
	// + " entropy: " + getEntropy(crf, input) + " correct: "
	// + (correct == n));
	// System.out.println();
	// }
	// }

	//
	// private double getEntropy(CRF crf, FeatureVectorSequence input) {
	// SumLattice lattice = new SumLatticeDefault(crf, input, true);
	// // udpate the expectations
	// EntropyLattice entropyLattice = new EntropyLattice(input,
	// lattice.getGammas(), lattice.getXis(), crf, null, 1);
	// double entropy = -entropyLattice.getEntropy();
	// return entropy;
	// }
}
