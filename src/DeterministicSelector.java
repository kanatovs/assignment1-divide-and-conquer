import java.util.Objects;

/** select(array,k) returns the value at zero-based rank k and may reorder array. */
public final class DeterministicSelector {
	public int select(int[] array,int k,Metrics metrics) {
		Objects.requireNonNull(array,"array");
		Objects.requireNonNull(metrics,"metrics").reset();
		if(k<0 || k>=array.length) throw new IllegalArgumentException("k must satisfy 0 <= k < n");
		return selectRange(array,0,array.length-1,k,1,metrics);
	}

	private int selectRange(int[] array,int low,int high,int k,int depth,Metrics metrics) {
		metrics.enter(depth);
		if(high-low+1<=5) {
			ArraySupport.insertionSort(array,low,high,metrics);
			return array[k];
		}
		int pivot=medianOfMedians(array,low,high,depth,metrics);
		ArraySupport.EqualRange equal=ArraySupport.partition(array,low,high,pivot,metrics);
		if(k<equal.first()) return selectRange(array,low,equal.first()-1,k,depth+1,metrics);
		if(k>equal.last()) return selectRange(array,equal.last()+1,high,k,depth+1,metrics);
		return pivot;
	}

	private int medianOfMedians(int[] array,int low,int high,int depth,Metrics metrics) {
		int count=0;
		for(int start=low;start<=high;start+=5) {
			int end=Math.min(start+4,high);
			ArraySupport.insertionSort(array,start,end,metrics);
			int median=start+(end-start)/2;
			// Store group medians at the beginning of the same array range.
			ArraySupport.swap(array,low+count,median,metrics);
			count++;
		}
		return selectRange(array,low,low+count-1,low+count/2,depth+1,metrics);
	}
}
