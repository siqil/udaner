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

public class SegmentationEvaluatorLeft extends SegmentationEvaluator {

	public SegmentationEvaluatorLeft(InstanceList instanceList1,
			String description1) {
		super(instanceList1, description1);
		this.setSegmentEndTag(new Object() {
			public boolean equals(Object o) {
				return false;
			}
		});
	}
}
