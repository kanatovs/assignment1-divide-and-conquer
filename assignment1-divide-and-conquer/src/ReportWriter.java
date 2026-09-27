import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Summarizes real CSV measurements; does not invent experimental values. */
public final class ReportWriter {
	public record Summary(String algorithm,String type,int n,double timeMs,int depth,long comparisons) {}
	private record Row(long timeNs,int depth,long comparisons) {}
	private static final String BEGIN="<!-- RESULTS_START -->";
	private static final String END="<!-- RESULTS_END -->";

	public static void generate() throws IOException {
		Path csv=Path.of("results","results.csv");
		if(!Files.exists(csv)) throw new IOException("Run experiments before generating the report");
		Map<String,List<Row>> groups=new TreeMap<>();
		List<String> lines=Files.readAllLines(csv);
		for(int i=1;i<lines.size();i++) {
			if(lines.get(i).isBlank()) continue;
			String[] fields=lines.get(i).split(",");
			if(fields.length!=11) throw new IOException("Invalid CSV row "+(i+1));
			String key=fields[0]+","+fields[1]+","+fields[2];
			groups.computeIfAbsent(key,ignored->new ArrayList<>()).add(
				new Row(Long.parseLong(fields[5]),Integer.parseInt(fields[6]),Long.parseLong(fields[7])));
		}
		List<Summary> summaries=new ArrayList<>();
		for(var entry:groups.entrySet()) {
			String[] key=entry.getKey().split(",");
			List<Row> values=entry.getValue();
			long[] times=values.stream().mapToLong(Row::timeNs).sorted().toArray();
			long[] comparisons=values.stream().mapToLong(Row::comparisons).sorted().toArray();
			int depth=values.stream().mapToInt(Row::depth).max().orElse(0);
			summaries.add(new Summary(key[0],key[1],Integer.parseInt(key[2]),median(times)/1e6,depth,(long)median(comparisons)));
		}
		summaries.sort(Comparator.comparing(Summary::algorithm).thenComparing(Summary::type).thenComparingInt(Summary::n));
		String full="# All experimental summaries\n\nMedian of five times; maximum recorded depth; median comparisons.\n\n"+table(summaries);
		Files.writeString(Path.of("results","summary.md"),full);
		String content="### Random inputs: all tested sizes\n\n"
			+table(summaries.stream().filter(s->s.type().equals("random")).toList())
			+"\n### Input-structure comparison: n = 100,000\n\n"
			+table(summaries.stream().filter(s->s.n()==100000).toList())
			+"\nAll 64 size/type/algorithm summaries are in [results/summary.md](results/summary.md). "
			+"Raw data: [results.csv](results/results.csv). Runtime: [environment.md](results/environment.md).\n";
		Path readme=Path.of("README.md");
		if(Files.exists(readme)) {
			String original=Files.readString(readme);
			int start=original.indexOf(BEGIN);
			int end=original.indexOf(END);
			if(start<0 || end<start) throw new IOException("README results markers are missing");
			Files.writeString(readme,original.substring(0,start+BEGIN.length())+"\n"+content+original.substring(end));
		}
		PlotWriter.write(summaries,"time","Time vs. input size","Median execution time (ms)",true);
		PlotWriter.write(summaries,"depth","Recursion depth vs. input size","Maximum recorded recursion depth",false);
		PlotWriter.write(summaries,"comparisons","Comparisons vs. input size","Median counted comparisons",true);
		System.out.println("Updated README tables, results/summary.md, and 3 PNG plots in docs/plots.");
	}

	private static double median(long[] values) {
		if(values.length==0) return 0;
		int middle=values.length/2;
		return values.length%2==1 ? values[middle] : values[middle-1]/2.0+values[middle]/2.0;
	}

	private static String table(List<Summary> summaries) {
		StringBuilder text=new StringBuilder("| Algorithm | Input | n | Median ms | Max depth | Median comparisons |\n");
		text.append("|---|---|---:|---:|---:|---:|\n");
		for(Summary item:summaries) text.append(String.format(Locale.ROOT,"| %s | %s | %d | %.6f | %d | %d |%n",
			item.algorithm(),item.type(),item.n(),item.timeMs(),item.depth(),item.comparisons()));
		return text.toString();
	}
}
