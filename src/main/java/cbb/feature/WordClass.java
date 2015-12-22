package cbb.feature;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class WordClass extends Pipe {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2180626179339491604L;

	@Override
	public Instance pipe(Instance carrier) {
		TokenSequence ts = (TokenSequence) carrier.getData();
		for (Token token : ts) {
			String text = token.getText();
			token.setFeatureValue("WC=" + getWordClass(text), 1);
			token.setFeatureValue("BWC=" + getBriefWordClass(text), 1);
		}
		carrier.setData(ts);
		return carrier;
	}

	private static String getWordClass(String text) {
		text = text.replaceAll("[A-Z]", "A");
		text = text.replaceAll("[a-z]", "a");
		text = text.replaceAll("[0-9]", "0");
		text = text.replaceAll("[^A-Za-z0-9]", "x");
		return text;
	}

	private static String getBriefWordClass(String text) {
		text = text.replaceAll("[A-Z]+", "A");
		text = text.replaceAll("[a-z]+", "a");
		text = text.replaceAll("[0-9]+", "0");
		text = text.replaceAll("[^A-Za-z0-9]+", "x");
		return text;
	}
}
