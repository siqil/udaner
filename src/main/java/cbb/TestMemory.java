package cbb;

import java.util.ArrayList;
import java.util.Iterator;

import no.uib.cipr.matrix.NotConvergedException;

/**
 * Class: TestMemory
 * 
 * @author: Viral Patel
 * @description: Prints JVM memory utilization statistics
 */
public class TestMemory {
	public static void main(String[] args) throws NotConvergedException {
		print();
	}

	public static void wrongRemove() {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < 100; i++) {
			list.add(i);
		}
		for (int i = 0; i < 50; i++) {
			list.remove(i);
		}
		System.out.println(list);
	}

	public static void rightRemove() {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < 100; i++) {
			list.add(i);
		}
		Iterator<Integer> it = list.iterator();
		int c = 0;
		while (it.hasNext()) {
			it.next();
			it.remove();
			if (++c >= 50)
				break;
		}
		System.out.println(list);
	}

	public static void print() {

		int mb = 1024 * 1024;

		// Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();

		System.out.println("##### Heap utilization statistics [MB] #####");

		// Print used memory
		System.out.println("Used Memory:"
				+ (runtime.totalMemory() - runtime.freeMemory()) / mb);

		// Print free memory
		System.out.println("Free Memory:" + runtime.freeMemory() / mb);

		// Print total available memory
		System.out.println("Total Memory:" + runtime.totalMemory() / mb);

		// Print Maximum available memory
		System.out.println("Max Memory:" + runtime.maxMemory() / mb);
	}
}