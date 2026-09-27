import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class Main {
	public static void main(String[] args) throws IOException {
		String command=args.length==0 ? "demo" : args[0];
		switch(command) {
			case "demo" -> demo();
			case "experiments" -> { new Experiment().run(); ReportWriter.generate(); }
			case "report" -> ReportWriter.generate();
			case "all" -> { demo(); new Experiment().run(); ReportWriter.generate(); }
			default -> throw new IllegalArgumentException("Use: demo | experiments | report | all");
		}
	}

	private static void demo() throws IOException {
		StringBuilder output=new StringBuilder("DIVIDE-AND-CONQUER DEMO\n");
		int[] input={9,3,7,3,1,8,2};
		output.append("Input: ").append(Arrays.toString(input)).append('\n');
		Metrics metrics=new Metrics();
		int[] first=input.clone();
		new MergeSorter().sort(first,metrics);
		output.append("MergeSort: ").append(Arrays.toString(first)).append('\n');
		output.append("  comparisons=").append(metrics.comparisons).append(", maxDepth=").append(metrics.maxDepth).append('\n');
		int[] second=input.clone();
		new QuickSorter().sort(second,42,metrics);
		output.append("QuickSort: ").append(Arrays.toString(second)).append('\n');
		output.append("  comparisons=").append(metrics.comparisons).append(", maxDepth=").append(metrics.maxDepth).append('\n');
		int k=3;
		int selected=new DeterministicSelector().select(input.clone(),k,metrics);
		output.append("DeterministicSelect: k=3 (4th smallest), value=").append(selected).append('\n');
		output.append("  comparisons=").append(metrics.comparisons).append(", maxDepth=").append(metrics.maxDepth).append('\n');
		Point[] points={new Point(0,0),new Point(3,4),new Point(1,1),new Point(10,10)};
		ClosestPairSolver.Result pair=new ClosestPairSolver().solve(points,metrics);
		output.append("ClosestPair: ").append(pair.first()).append(" and ").append(pair.second()).append('\n');
		output.append("  distance=").append(pair.distance()).append(", maxDepth=").append(metrics.maxDepth).append('\n');
		output.append("  distance evaluations=").append(metrics.distanceEvaluations).append('\n');
		System.out.print(output);
		Files.createDirectories(Path.of("results"));
		Files.writeString(Path.of("results","demo-output.txt"),output.toString());
	}
}
