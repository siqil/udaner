package cbb;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import cc.mallet.types.Instance;

/**
 * Modified LineGroupIterator. Returns arrays of strings instead of strings.
 * 
 * @author lsq
 * 
 */
public class StringArrayLineGroupIterator implements Iterator<Instance> {
	LineNumberReader reader;
	Pattern lineBoundaryRegex;
	boolean skipBoundary;
	// boolean putBoundaryLineAtEnd; // Not yet implemented
	ArrayList<String> nextLineGroup;
	String nextBoundary;
	String nextNextBoundary;
	int groupIndex = 0;
	boolean putBoundaryInSource = true;

	public StringArrayLineGroupIterator(Reader input,
			Pattern lineBoundaryRegex, boolean skipBoundary) {
		this.reader = new LineNumberReader(input);
		this.lineBoundaryRegex = lineBoundaryRegex;
		this.skipBoundary = skipBoundary;
		setNextLineGroup();
	}

	public ArrayList<String> peekLineGroup() {
		return nextLineGroup;
	}

	private void setNextLineGroup() {
		ArrayList<String> sb = new ArrayList<String>();
		String line;
		if (!skipBoundary && nextBoundary != null)
			sb.add(nextBoundary);
		while (true) {
			try {
				line = reader.readLine();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if (line == null) {
				break;
			} else if (lineBoundaryRegex.matcher(line).matches()) {
				if (sb.size() > 0) {
					this.nextBoundary = this.nextNextBoundary;
					this.nextNextBoundary = line;
					break;
				} else { // The first line of the file.
					if (!skipBoundary)
						sb.add(line);
					this.nextNextBoundary = line;
				}
			} else {
				sb.add(line);
			}
		}
		if (sb.size() == 0)
			this.nextLineGroup = null;
		else
			this.nextLineGroup = sb;
	}

	public Instance next() {
		assert (nextLineGroup != null);
		Instance carrier = new Instance(nextLineGroup, null, "linegroup"
				+ groupIndex++, putBoundaryInSource ? nextBoundary : null);
		setNextLineGroup();
		return carrier;
	}

	public boolean hasNext() {
		return nextLineGroup != null;
	}

	public void remove() {
		throw new IllegalStateException(
				"This Iterator<Instance> does not support remove().");
	}
}
