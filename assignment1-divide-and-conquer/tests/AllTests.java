import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** Dependency-free tests: a failed check throws AssertionError and fails the process. */
public final class AllTests {
	private static int checks;
	private static final StringBuilder log=new StringBuilder();

	public static void main(String[] args) throws IOException {
		checks=0;
		log.setLength(0);
		line("DIVIDE-AND-CONQUER CORRECTNESS TESTS");
		line("Reference methods: Arrays.sort and quadratic closest-pair search");
		sortingTests();
		selectionTests();
		closestPairTests();
		edgeAndMetricsTests();
		line("ALL TESTS PASSED | checks="+checks);
		Files.createDirectories(Path.of("results"));
		Files.writeString(Path.of("results","test-output.txt"),log.toString());
	}

	private static void sortingTests() {
		int[][] special={{},{4},{2,1},{7,7,7,7},{-2,0,-2,9},{Integer.MAX_VALUE,0,Integer.MIN_VALUE}};
		for(int[] array:special) checkSort(array);
		int[] sizes={2,5,23,24,25,48,101,1000,10000};
		for(String type:InputFactory.TYPES) {
			for(int size:sizes) checkSort(InputFactory.integers(size,type,12345L+size));
		}
		Random random=new Random(101);
		for(int test=0;test<300;test++) checkSort(InputFactory.integers(random.nextInt(1001),"random",random.nextLong()));
		for(String type:InputFactory.TYPES) checkSort(InputFactory.integers(100000,type,789));
		line("PASS sorting: empty, singleton, cutoff boundaries, all types, 300 random cases, large arrays");
	}

	private static void checkSort(int[] input) {
		int[] expected=input.clone();
		Arrays.sort(expected);
		int[] merge=input.clone();
		int[] quick=input.clone();
		Metrics metrics=new Metrics();
		new MergeSorter().sort(merge,metrics);
		check(Arrays.equals(expected,merge),"MergeSort mismatch");
		new QuickSorter().sort(quick,5678,metrics);
		check(Arrays.equals(expected,quick),"QuickSort mismatch");
		int bound=input.length==0 ? 0 : 1+(31-Integer.numberOfLeadingZeros(input.length));
		check(metrics.maxDepth<=bound,"QuickSort smaller-first depth bound");
	}

	private static void selectionTests() {
		DeterministicSelector selector=new DeterministicSelector();
		Random random=new Random(202);
		for(int test=0;test<500;test++) {
			int size=1+random.nextInt(2000);
			int[] input=InputFactory.integers(size,InputFactory.TYPES[test%4],random.nextLong());
			int[] expected=input.clone();
			Arrays.sort(expected);
			int[] ranks={0,size/2,size-1,random.nextInt(size)};
			for(int k:ranks) check(selector.select(input.clone(),k,new Metrics())==expected[k],"Selection rank "+k);
		}
		// All ranks for many short arrays catch index and last-group mistakes.
		for(int size=1;size<=70;size++) {
			int[] input=InputFactory.integers(size,"duplicates",size);
			int[] expected=input.clone();
			Arrays.sort(expected);
			for(int k=0;k<size;k++) check(selector.select(input.clone(),k,new Metrics())==expected[k],"Selection all ranks");
		}
		int[] extremes={Integer.MAX_VALUE,Integer.MIN_VALUE,0,Integer.MAX_VALUE};
		check(selector.select(extremes,0,new Metrics())==Integer.MIN_VALUE,"Selection integer extremes");
		line("PASS selection: 500 random datasets x 4 ranks; all ranks for n=1..70; integer extremes");
	}

	private static void closestPairTests() {
		checkPoints(new Point[]{});
		checkPoints(new Point[]{new Point(0,0)});
		checkPoints(new Point[]{new Point(0,0),new Point(3,4)});
		checkPoints(new Point[]{new Point(1,2),new Point(1,2),new Point(100,100)});
		checkPoints(new Point[]{new Point(-1,0),new Point(-0.001,30),new Point(0.001,30),new Point(1,0)});
		checkPoints(new Point[]{new Point(1e150,1e150),new Point(1e150+1e140,1e150),new Point(-1e150,0)});
		Random random=new Random(303);
		for(int test=0;test<200;test++) {
			checkPoints(InputFactory.points(2+random.nextInt(149),InputFactory.TYPES[test%4],random.nextLong()));
		}
		for(String type:InputFactory.TYPES) checkPoints(InputFactory.points(2000,type,909));
		Point[] vertical=new Point[400];
		Point[] horizontal=new Point[400];
		for(int i=0;i<400;i++) {
			vertical[i]=new Point(0,i*3);
			horizontal[i]=new Point(i*3,0);
		}
		checkPoints(vertical);
		checkPoints(horizontal);
		Point[] large=InputFactory.points(100000,"random",999);
		ClosestPairSolver.Result result=new ClosestPairSolver().solve(large,new Metrics());
		check(result.exists() && Double.isFinite(result.distance()),"Large closest-pair smoke test");
		line("PASS closest pair: 200 random small datasets, all types at n=2000, duplicates, tied x/y, cross-strip, n=100000 smoke");
	}

	private static void checkPoints(Point[] points) {
		ClosestPairSolver solver=new ClosestPairSolver();
		Point[] original=points.clone();
		ClosestPairSolver.Result fast=solver.solve(points,new Metrics());
		ClosestPairSolver.Result brute=solver.bruteForce(points,new Metrics());
		check(Arrays.equals(points,original),"ClosestPair must preserve caller's array");
		check(fast.exists()==brute.exists(),"ClosestPair existence mismatch");
		if(!fast.exists()) {
			check(fast.distance()==Double.POSITIVE_INFINITY,"No pair must have infinite distance");
			return;
		}
		double tolerance=1e-10*Math.max(1.0,brute.distance());
		check(Math.abs(fast.distance()-brute.distance())<=tolerance,"ClosestPair distance mismatch");
		check(Arrays.asList(points).contains(fast.first()) && Arrays.asList(points).contains(fast.second()),"Returned pair must belong to input");
		double actual=Math.hypot(fast.first().x()-fast.second().x(),fast.first().y()-fast.second().y());
		check(Double.compare(actual,fast.distance())==0,"Returned points and reported distance disagree");
	}

	private static void edgeAndMetricsTests() {
		DeterministicSelector selector=new DeterministicSelector();
		expect(IllegalArgumentException.class,()->selector.select(new int[]{},0,new Metrics()));
		expect(IllegalArgumentException.class,()->selector.select(new int[]{1},-1,new Metrics()));
		expect(IllegalArgumentException.class,()->selector.select(new int[]{1},1,new Metrics()));
		expect(NullPointerException.class,()->new MergeSorter().sort(null,new Metrics()));
		expect(NullPointerException.class,()->new QuickSorter().sort(null,0,new Metrics()));
		expect(NullPointerException.class,()->selector.select(null,0,new Metrics()));
		expect(NullPointerException.class,()->new ClosestPairSolver().solve(new Point[]{null},new Metrics()));
		expect(IllegalArgumentException.class,()->new Point(Double.NaN,0));
		expect(IllegalArgumentException.class,()->new Point(0,Double.POSITIVE_INFINITY));
		Metrics metrics=new Metrics();
		new MergeSorter().sort(new int[]{3,2,1},metrics);
		check(metrics.comparisons>0 && metrics.maxDepth==1,"MergeSort small-input metrics");
		new MergeSorter().sort(new int[]{},metrics);
		check(metrics.comparisons==0 && metrics.maxDepth==0,"Metrics must reset between runs");
		int[] equal=new int[100000];
		new QuickSorter().sort(equal,42,metrics);
		check(metrics.maxDepth==1 && metrics.comparisons==equal.length,"All-equal three-way partition");
		line("PASS validation and metrics: invalid k/null/NaN/infinity, reset, all-equal quicksort depth");
	}

	private static void expect(Class<? extends Throwable> type,Runnable action) {
		try { action.run(); }
		catch(Throwable failure) {
			check(type.isInstance(failure),"Wrong exception: "+failure);
			return;
		}
		throw new AssertionError("Expected "+type.getSimpleName());
	}

	private static void check(boolean condition,String message) {
		checks++;
		if(!condition) throw new AssertionError(message);
	}

	private static void line(String text) {
		System.out.println(text);
		log.append(text).append('\n');
	}
}
