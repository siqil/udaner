package cc.mallet.scl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import cc.mallet.types.IndexedSparseVector;
import cc.mallet.types.SparseVector;

public class SVDLIBC {
	public static String SVD = "svd";
	public static String input = "mat";
	public static String Ut = "-Ut";
	public static String Vt = "-Vt";
	public static String S = "-S";

	private File dir;
	private File fUt;
	private File fVt;
	private File fS;

	public SVDLIBC() throws IOException {
	}

	public SVDLIBC(File dir) {
		this.setDir(dir);
	}

	public void svd(List<SparseVector> cols, int nrows, int ncols, int dim)
			throws IOException {
		File mat = newFile(input);
		writeSparse(mat, cols, nrows, ncols);
		svd(mat, dim);
	}

	public void svd(File mat, int dim) throws IOException {
		Map<String, String> options = new HashMap<String, String>();
		options.put("-d", Integer.toString(dim));
		File[] results = svd(mat, getDir(), options);
		fUt = results[0];
		fS = results[1];
		fVt = results[2];
	}

	public File newFile(String filename) {
		return getDir().toPath().resolve(filename).toFile();
	}

	public File[] svd(File mat, File out, Map<String, String> options)
			throws IOException {
		System.out.println("SVD input: " + mat.toString());
		System.out.println("SVD output: " + out.toString());
		List<String> cmd = new ArrayList<String>();
		cmd.add(SVD);
		for (String key : options.keySet()) {
			cmd.add(key);
			cmd.add(options.get(key));
		}
		cmd.add("-o");
		cmd.add(out.toString() + File.separator);
		cmd.add(mat.toString());
		ProcessBuilder pb = new ProcessBuilder(cmd);
		Process p = pb.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}
		Path oPath = out.toPath();
		File[] results = new File[3];
		results[0] = oPath.resolve(Ut).toFile();
		results[1] = oPath.resolve(S).toFile();
		results[2] = oPath.resolve(Vt).toFile();
		return results;
	}

	public void writeSparse(File f, List<SparseVector> cols, int nrows,
			int ncols) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(f);
		int nz = 0;
		int[] nzs = new int[ncols];
		for (int j = 0; j < ncols; j++) {
			SparseVector col = cols.get(j);
			for (int l = 0; l < col.numLocations(); l++) {
				if (col.valueAtLocation(l) != 0) {
					nz++;
					nzs[j]++;
				}
			}
		}
		writer.printf("%d %d %d\n", nrows, ncols, nz);
		for (int j = 0; j < ncols; j++) {
			writer.printf("%d\n", nzs[j]);
			SparseVector col = cols.get(j);
			for (int l = 0; l < col.numLocations(); l++) {
				if (col.valueAtLocation(l) != 0) {
					writer.print(col.indexAtLocation(l));
					writer.print(" ");
					writer.println(col.valueAtLocation(l));
				}
			}
		}
		writer.close();
	}

	public List<SparseVector> readSparse(File mat) throws IOException {
		Scanner sc = new Scanner(mat);
		List<SparseVector> ret = new ArrayList<SparseVector>();
		int nrows = sc.nextInt();
		int ncols = sc.nextInt();
		int nz = sc.nextInt();
		for (int j = 0; j < ncols; j++) {
			int cnz = sc.nextInt();
			int[] indices = new int[cnz];
			double[] values = new double[cnz];
			for (int i = 0; i < cnz; i++) {
				indices[i] = sc.nextInt();
				values[i] = sc.nextDouble();
			}
			ret.add(new IndexedSparseVector(indices, values));
		}
		sc.close();
		return ret;
	}

	public List<SparseVector> readDense(File mat) throws IOException {
		Scanner sc = new Scanner(mat);
		List<SparseVector> ret = new ArrayList<SparseVector>();

		int nrows = sc.nextInt();
		int ncols = sc.nextInt();
		for (int i = 0; i < nrows; i++) {
			List<Double> row = new ArrayList<Double>();
			int nz = 0;
			for (int j = 0; j < ncols; j++) {
				double val = sc.nextDouble();
				row.add(val);
				if (val != 0)
					nz++;
			}
			int[] indices = new int[nz];
			double[] values = new double[nz];
			int p = 0;
			for (int j = 0; j < ncols; j++) {
				double val = row.get(j);
				if (val != 0) {
					indices[p] = j;
					values[p] = val;
					p++;
				}
			}
			ret.add(new IndexedSparseVector(indices, values));
		}
		sc.close();
		return ret;
	}

	public List<SparseVector> getUt() throws IOException {
		return readDense(fUt);
	}

	public static void main(String[] args) throws IOException {
		int row = 3000, col = 1000;
		List<SparseVector> mat = new ArrayList<SparseVector>();
		for (int i = 0; i < col; i++) {
			int nz = 0;
			List<Double> c = new ArrayList<Double>();
			for (int j = 0; j < row; j++) {
				double val = Math.random() - 0.1;
				c.add(val);
				if (val > 0) {
					nz++;
				}
			}
			int[] indices = new int[nz];
			double[] values = new double[nz];
			int p = 0;
			for (int j = 0; j < row; j++) {
				double val = c.get(j);
				if (val > 0) {
					indices[p] = j;
					values[p] = val;
					p++;
				}
			}
			mat.add(new SparseVector(indices, values));
		}
		SVDLIBC svd = new SVDLIBC();
		// File f = svd.newFile("mat1");
		// System.out.println(f.toString());
		// svd.writeSparse(f, mat, row, col);
		// svd = new SVDLIBC();
		// List<SparseVector> mat2 = svd.readSparse(f);
		// f = svd.newFile("mat2");
		// System.out.println(f.toString());
		// svd.writeSparse(f, mat2, row, col);
		svd.svd(mat, row, col, 25);
		List<SparseVector> ut = svd.getUt();
		for (SparseVector v : ut) {
			System.out.println(v);
		}
	}

	public File getDir() {
		if (dir == null) {
			try {
				dir = Files.createTempDirectory("svdlibc").toFile();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Creating temp dir failed.");
			}
		}
		return dir;
	}

	public void setDir(File dir) {
		this.dir = dir;
	}
}
