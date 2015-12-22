package cbb;

/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
 This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
 http://www.cs.umass.edu/~mccallum/mallet
 This software is provided under the terms of the Common Public License,
 version 1.0, as published by http://www.opensource.org.  For further
 information, see the file `LICENSE' included with this distribution. */

/** 
 @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

import java.util.logging.Logger;
import java.util.regex.Pattern;

import cc.mallet.fst.Transducer;
import cc.mallet.fst.TransducerEvaluator;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
import cc.mallet.types.TokenSequence;
import cc.mallet.util.MalletLogger;

public class SegmentationEvaluatorRight extends SegmentationEvaluator {
	private static Logger logger = MalletLogger
			.getLogger(SegmentationEvaluator.class.getName());

	public SegmentationEvaluatorRight(InstanceList instanceList1,
			String description1) {
		super(instanceList1, description1);
		this.setSegmentStartTag(new Object() {
			public boolean equals(Object o) {
				return false;
			}
		});
	}

	public void evaluateInstanceList(TransducerTrainer tt, InstanceList data,
			String description) {
		Transducer model = tt.getTransducer();
		int numCorrectTokens, totalTokens;
		int numTrueSegments, numPredictedSegments, numCorrectSegments;
		int numCorrectSegmentsInAlphabet, numCorrectSegmentsOOV;
		int numIncorrectSegmentsInAlphabet, numIncorrectSegmentsOOV;
		TokenSequence sourceTokenSequence = null;

		totalTokens = numCorrectTokens = 0;
		numTrueSegments = numPredictedSegments = numCorrectSegments = 0;
		numCorrectSegmentsInAlphabet = numCorrectSegmentsOOV = 0;
		numIncorrectSegmentsInAlphabet = numIncorrectSegmentsOOV = 0;
		for (int i = 0; i < data.size(); i++) {
			Instance instance = data.get(i);
			Sequence input = (Sequence) instance.getData();
			// String tokens = null;
			// if (instance.getSource() != null)
			// tokens = (String) instance.getSource().toString();
			Sequence trueOutput = (Sequence) instance.getTarget();
			assert (input.size() == trueOutput.size());
			Sequence predOutput = model.transduce(input);
			assert (predOutput.size() == trueOutput.size());
			boolean trueStart, predStart;
			for (int j = trueOutput.size() - 1; j >= 0; j--) {
				totalTokens++;
				trueStart = predStart = false;
				if (segmentEndTag.equals(trueOutput.get(j))) {
					numTrueSegments++;
					trueStart = true;
				}
				if (segmentEndTag.equals(predOutput.get(j))) {
					predStart = true;
					numPredictedSegments++;
				}
				if (trueStart && predStart) {
					int m;
					// StringBuffer sb = new StringBuffer();
					// sb.append (tokens.charAt(j));
					for (m = j - 1; m >= 0; m--) {
						trueStart = predStart = false; // Here, these actually
														// mean "end", not
														// "start"
						if (segmentStartTag.equals(trueOutput.get(m)))
							trueStart = true;
						if (segmentStartTag.equals(predOutput.get(m)))
							predStart = true;
						if (trueStart || predStart) {
							if (trueStart && predStart) {
								// It is a correct segment
								numCorrectSegments++;
								// if
								// (HashFile.allLexicons.contains(sb.toString()))
								// numCorrectSegmentsInAlphabet++;
								// else
								// numCorrectSegmentsOOV++;
							} else {
								// It is an incorrect segment; let's find out if
								// it was in the lexicon
								// for (int mm = m; mm < trueOutput.size();
								// mm++) {
								// if (segmentEndTag.equals(predOutput.get(mm)))
								// break;
								// sb.append (tokens.charAt(mm));
								// }
								// if
								// (HashFile.allLexicons.contains(sb.toString()))
								// numIncorrectSegmentsInAlphabet++;
								// else
								// numIncorrectSegmentsOOV++;
							}
							break;
						}
						// sb.append (tokens.charAt(m));
					}
					// for the case of the end of the sequence
					if (m == 0) {
						if (trueStart == predStart) {
							numCorrectSegments++;
							// if (HashFile.allLexicons.contains(sb.toString()))
							// numCorrectSegmentsInAlphabet++;
							// else
							// numCorrectSegmentsOOV++;
						} else {
							// if (HashFile.allLexicons.contains(sb.toString()))
							// numIncorrectSegmentsInAlphabet++;
							// else
							// numIncorrectSegmentsOOV++;
						}
					}
				} else if (predStart) {
					// Here is an incorrect predicted start, find out if the
					// word is in the lexicon
					// StringBuffer sb = new StringBuffer();
					// sb.append (tokens.charAt(j));
					// for (int mm = j+1; mm < trueOutput.size(); mm++) {
					// if (segmentEndTag.equals(predOutput.get(mm)))
					// break;
					// sb.append (tokens.charAt(mm));
					// }
					// if (HashFile.allLexicons.contains(sb.toString()))
					// numIncorrectSegmentsInAlphabet++;
					// else
					// numIncorrectSegmentsOOV++;
				}
				if (trueOutput.get(j).equals(predOutput.get(j)))
					numCorrectTokens++;
			}
		}
		logger.info(description + " accuracy=" + ((double) numCorrectTokens)
				/ totalTokens);
		double precision = numPredictedSegments == 0 ? 1
				: ((double) numCorrectSegments) / numPredictedSegments;
		double recall = numTrueSegments == 0 ? 1
				: ((double) numCorrectSegments) / numTrueSegments;
		double f1 = recall + precision == 0.0 ? 0.0
				: (2.0 * recall * precision) / (recall + precision);
		logger.info(" precision=" + precision + " recall=" + recall + " f1="
				+ f1);
		logger.info("segments true=" + numTrueSegments + " pred="
				+ numPredictedSegments + " correct=" + numCorrectSegments
				+ " misses=" + (numTrueSegments - numCorrectSegments)
				+ " alarms=" + (numPredictedSegments - numCorrectSegments));
		// System.out.println
		// ("correct segments OOV="+numCorrectSegmentsOOV+" IV="+numCorrectSegmentsInAlphabet);
		// System.out.println
		// ("incorrect segments OOV="+numIncorrectSegmentsOOV+" IV="+numIncorrectSegmentsInAlphabet);
	}

}
