package cbb;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SimpleTaggerSentence2TokenSequence;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.TokenSequence;

public class VocabProcessor {
	InstanceList[] ilists;
	Map<String, Integer>[] vocabs;

	public VocabProcessor(String... filenames) throws FileNotFoundException {
		Pipe pipe = new SimpleTaggerSentence2TokenSequence();
		ilists = new InstanceList[filenames.length];
		vocabs = new Map[filenames.length];
		for (int i = 0; i < filenames.length; i++) {
			ilists[i] = new InstanceList(pipe);
			ilists[i].addThruPipe(new LineGroupIterator(new FileReader(
					filenames[i]), Pattern.compile("^\\s*$"), true));
			vocabs[i] = getVocab(ilists[i]);
			System.out.println("Vocab " + i + " : " + vocabs[i].size());
		}

	}

	public Map<String, Integer>[] getVocabs() {
		return vocabs;
	}

	public Map<String, Integer> getVocab(int i) {
		return vocabs[i];
	}

	public static Map<String, Integer> filter(Map<String, Integer> original,
			int threshold) {
		Map<String, Integer> filtered = new HashMap<String, Integer>();
		for (String word : original.keySet()) {
			Integer value = original.get(word);
			if (value > threshold) {
				filtered.put(word, value);
			}
		}
		return filtered;
	}

	public Map<String, Integer> getFilteredVocab(int i, int threshold) {
		return filter(vocabs[i], threshold);
	}

	public Map<String, Integer>[] getFilteredVocabs(int threshold) {
		Map<String, Integer>[] results = new Map[vocabs.length];
		for (int i = 0; i < vocabs.length; i++) {
			results[i] = filter(vocabs[i], threshold);
		}
		return results;
	}

	public Map<String, Integer> getVocab(InstanceList instList) {
		Map<String, Integer> vocab = new HashMap<String, Integer>();
		for (Instance sent : instList) {
			TokenSequence ts = (TokenSequence) sent.getData();
			for (int i = 0; i < ts.size(); i++) {
				String word = ts.get(i).getText().toLowerCase();
				if (vocab.containsKey(word)) {
					vocab.put(word, vocab.get(word) + 1);
				} else {
					vocab.put(word, 1);
				}
			}
		}
		return vocab;
	}

	public static Map<String, Integer> union(Map<String, Integer>... vocabs) {
		Set<String> words = new HashSet<String>(vocabs[0].keySet());
		for (int i = 1; i < vocabs.length; i++) {
			words.addAll(vocabs[i].keySet());
		}
		Map<String, Integer> result = new HashMap<String, Integer>();
		for (String word : words) {
			int count = 0;
			for (Map<String, Integer> vocab : vocabs) {
				if(vocab.get(word)!=null){
					count += vocab.get(word);
				}
			}
			result.put(word, count);
		}
		System.out.println("Union size : " + result.size());
		return result;
	}

	public static Map<String, Integer> intersect(Map<String, Integer>... vocabs) {
		Set<String> words = new HashSet<String>(vocabs[0].keySet());
		for (int i = 1; i < vocabs.length; i++) {
			words.retainAll(vocabs[i].keySet());
		}
		Map<String, Integer> result = new HashMap<String, Integer>();
		for (String word : words) {
			int count = 0;
			for (Map<String, Integer> vocab : vocabs) {
				count += vocab.get(word);
			}
			result.put(word, count);
		}
		System.out.println("Intersection size : " + result.size());
		return result;
	}

}
