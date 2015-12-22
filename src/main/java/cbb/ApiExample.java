package cbb;

import java.util.ArrayList;
import java.util.List;

import cc.mallet.fst.CRF;
import cc.mallet.types.Instance;

public class ApiExample {
	public static void main(String[] args) {
		String[][][] train1 = new String[][][] {
				{ { "Biologists", "O" }, { "have", "O" }, { "recently", "O" },
						{ "found", "O" }, { "a", "O" }, { "new", "O" },
						{ "gene", "O" }, { "called", "O" },
						{ "semidwarf", "B-GENE" }, { "gene", "I-GENE" },
						{ ".", "O" } },
				{ { "The", "O" }, { "semidwarf", "B-GENE" },
						{ "gene", "I-GENE" }, { "is", "O" }, { "very", "O" },
						{ "important", "O" }, { ".", "O" } } };
		String[][][] train2 = new String[][][] {
				{ { "Biologists" }, { "have" }, { "recently" }, { "found" },
						{ "a" }, { "new" }, { "gene" }, { "called" },
						{ "SD1" }, { "gene" }, { "." } },
				{ { "The" }, { "SD1" }, { "gene" }, { "is" }, { "very" },
						{ "important" }, { "." } } };
		String[][] test = new String[][] { { "For" }, { "rice" }, { "," },
				{ "SD1" }, { "gene" }, { "has" }, { "significant" },
				{ "influences" }, { "." } };
		List<Instance> instances1 = new ArrayList<Instance>();
		for (String[][] sentence : train1) {
			instances1.add(new Instance(sentence, null, null, null));
		}
		List<Instance> instances2 = new ArrayList<Instance>();
		for (String[][] sentence : train2) {
			instances2.add(new Instance(sentence, null, null, null));
		}
		List<Instance> instances11 = new ArrayList<Instance>();
		for (String[][] sentence : train1) {
			instances11.add(new Instance(sentence, null, null, null));
		}
		List<Instance> instances22 = new ArrayList<Instance>();
		for (String[][] sentence : train2) {
			instances22.add(new Instance(sentence, null, null, null));
		}
		CRF crf = new CRFBuilder(new PipeBuilder()
				.useSCL(instances11.iterator(), instances22.iterator()).setH(2)
				.setMinOccurPivot(1).useMI(false).build()).byInstanceIterator(
				instances1.iterator(), instances2.iterator()).build();
//		 CRF crf = new CRFBuilderByFeatureSubsetting(new PipeBuilder().build())
//		 .byInstanceIterator(instances1.iterator(),
//		 instances2.iterator()).build();
		CRFLabeler labeler = new CRFLabeler(crf);
		String[] labels = labeler.predict(test);
		for (int i = 0; i < test.length; i++) {
			System.out.println(test[i][0] + "\t" + labels[i]);
		}
	}
}
