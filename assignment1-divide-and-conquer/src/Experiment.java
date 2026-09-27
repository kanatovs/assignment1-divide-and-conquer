import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

public final class Experiment {
	public static final int[] SIZES={100,1000,10000,100000};
	public static final String[] ALGORITHMS={"MergeSort","QuickSort","DeterministicSelect","ClosestPair"};
	public static final int WARMUPS=3;
	public static final int TRIALS=5;
	private static volatile long blackhole;
	private final MergeSorter merge=new MergeSorter();
	private final QuickSorter quick=new QuickSorter();
	private final DeterministicSelector selector=new DeterministicSelector();
	private final ClosestPairSolver closest=new ClosestPairSolver();

	public void run() throws IOException {
		Files.createDirectories(Path.of("results"));
		StringBuilder log=new StringBuilder("EXPERIMENTS: 4 algorithms, 4 input types, 4 sizes, 5 measured trials\n");
		System.out.print(log);
		try(BufferedWriter output=Files.newBufferedWriter(Path.of("results","results.csv"))) {
			output.write("algorithm,input_type,n,trial,seed,time_ns,max_depth,comparisons,swaps,recursive_calls,distance_evaluations\n");
			for(int sizeIndex=0;sizeIndex<SIZES.length;sizeIndex++) {
				int size=SIZES[sizeIndex];
				for(int typeIndex=0;typeIndex<InputFactory.TYPES.length;typeIndex++) {
					String type=InputFactory.TYPES[typeIndex];
					for(int trial=-WARMUPS;trial<TRIALS;trial++) {
						// Same seed across types: sorted/reverse inputs reorder the same data.
						long seed=20260000L+size*101L+trial+WARMUPS;
						int[] source=InputFactory.integers(size,type,seed);
						int[] expected=source.clone();
						Arrays.sort(expected);
						Point[] points=InputFactory.points(size,type,seed);
						// Rotate algorithm order to reduce a fixed-order timing bias.
						for(int offset=0;offset<ALGORITHMS.length;offset++) {
							String algorithm=ALGORITHMS[Math.floorMod(offset+trial+typeIndex+sizeIndex,ALGORITHMS.length)];
							Metrics metrics=new Metrics();
							long elapsed=algorithm.equals("ClosestPair")
								? runPoints(points,metrics) : runIntegers(algorithm,source,expected,seed,metrics);
							if(trial>=0) {
								output.write(String.format(Locale.ROOT,"%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
									algorithm,type,size,trial+1,seed,elapsed,metrics.maxDepth,metrics.comparisons,
									metrics.swaps,metrics.recursiveCalls,metrics.distanceEvaluations));
							}
						}
					}
					String line=String.format(Locale.ROOT,"PASS experiments | n=%-6d | input=%s%n",size,type);
					System.out.print(line);
					log.append(line);
				}
			}
		}
		log.append("Saved results/results.csv: 320 measured rows; warmups excluded.\n");
		System.out.println("Saved results/results.csv: 320 measured rows; warmups excluded.");
		Files.writeString(Path.of("results","experiment-output.txt"),log.toString());
		String environment="# Measurement environment\n\n"
			+"- Run timestamp (UTC): "+Instant.now()+"\n"
			+"- Java: "+System.getProperty("java.version")+"\n"
			+"- JVM: "+System.getProperty("java.vm.name")+"\n"
			+"- Operating system: "+System.getProperty("os.name")+" "+System.getProperty("os.version")+"\n"
			+"- Architecture: "+System.getProperty("os.arch")+""
			+"\n- Available processors reported to JVM: "+Runtime.getRuntime().availableProcessors()+"\n"
			+"- Maximum JVM heap (bytes): "+Runtime.getRuntime().maxMemory()+"\n"
			+"- Warmups per scenario: "+WARMUPS+"\n- Measured trials per scenario: "+TRIALS+"\n"
			+"- Algorithms are instrumented; timing includes counter updates.\n"
			+"- Re-running experiments replaces this environment record and the CSV.\n";
		Files.writeString(Path.of("results","environment.md"),environment);
	}

	private long runIntegers(String algorithm,int[] source,int[] expected,long seed,Metrics metrics) {
		int[] working=source.clone(); // Input preparation is outside the timed section.
		int selected=0;
		long start=System.nanoTime();
		switch(algorithm) {
			case "MergeSort" -> merge.sort(working,metrics);
			case "QuickSort" -> quick.sort(working,seed^0x5DEECE66DL,metrics);
			case "DeterministicSelect" -> selected=selector.select(working,working.length/2,metrics);
			default -> throw new IllegalArgumentException("Unknown algorithm: "+algorithm);
		}
		long elapsed=System.nanoTime()-start;
		if(algorithm.equals("DeterministicSelect")) {
			if(selected!=expected[expected.length/2]) throw new AssertionError("Selection experiment mismatch");
			blackhole=blackhole^selected;
		} else {
			if(!Arrays.equals(working,expected)) throw new AssertionError("Sorting experiment mismatch");
			blackhole=blackhole^working[working.length/2];
		}
		return elapsed;
	}

	private long runPoints(Point[] points,Metrics metrics) {
		long start=System.nanoTime();
		ClosestPairSolver.Result result=closest.solve(points,metrics);
		long elapsed=System.nanoTime()-start;
		if(!result.exists() || !Double.isFinite(result.distance())) throw new AssertionError("Invalid closest pair");
		if(points.length<=2000) {
			double expected=closest.bruteForce(points,new Metrics()).distance();
			if(Math.abs(result.distance()-expected)>1e-10*Math.max(1.0,expected)) {
				throw new AssertionError("Closest-pair experiment mismatch");
			}
		}
		blackhole=blackhole^Double.doubleToLongBits(result.distance());
		return elapsed;
	}
}
