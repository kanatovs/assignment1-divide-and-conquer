/** Counters belong to one algorithm execution. Root recursive depth is 1. */
public final class Metrics {
	public long comparisons;
	public long swaps;
	public long recursiveCalls;
	public long distanceEvaluations;
	public int maxDepth;

	public void reset() {
		comparisons=swaps=recursiveCalls=distanceEvaluations=0;
		maxDepth=0;
	}

	public void enter(int depth) {
		recursiveCalls++;
		maxDepth=Math.max(maxDepth,depth);
	}

	public int compare(int first,int second) {
		comparisons++;
		return Integer.compare(first,second);
	}

	public int compare(double first,double second) {
		comparisons++;
		return Double.compare(first,second);
	}
}
