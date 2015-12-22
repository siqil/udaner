package cbb;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class GenetagLine2TokenSequence extends Pipe {
	/**
	 * 
	 */
	private static final long serialVersionUID = 125642747650819983L;
	Pattern tokenPattern = Pattern.compile("(.+)/(.+)");
	Set<String> targets = new HashSet<String>(Arrays.asList("NEWGENE",
			"NEWGENE1"));

	public GenetagLine2TokenSequence() {
		super(null, new LabelAlphabet());
	}

	@Override
	public Instance pipe(Instance carrier) {
		String line = (String) carrier.getData();
		LabelAlphabet labels = (LabelAlphabet) getTargetAlphabet();
		TokenSequence ts = new TokenSequence();
		String[] tokens = line.split(" ");
		LabelSequence target = new LabelSequence(labels, tokens.length - 1);
		assert (tokens.length > 1);
		String prevTag = null;
		for (int i = 1; i < tokens.length; i++) {
			Matcher matcher = tokenPattern.matcher(tokens[i]);
			matcher.matches();
			String word = matcher.group(1);
			String tag = matcher.group(2);
			Token tok = new Token(word);
			tok.setFeatureValue(word, 1.0);
			ts.add(tok);
			String tmp = tag;
			if (targets.contains(tag)) {
				if (tag.equals(prevTag)) {
					tag = "I-GENE";
				} else {
					tag = "B-GENE";
				}
			} else {
				tag = "O";
			}
			target.add(tag);
			prevTag = tmp;
		}
		carrier.setData(ts);
		carrier.setTarget(target);
		return carrier;
	}

}
