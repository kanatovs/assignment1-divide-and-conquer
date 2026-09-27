import java.util.Objects;

public final class MergeSorter {
	public static final int CUTOFF=24;

	public void sort(int[] array,Metrics metrics) {
		Objects.requireNonNull(array,"array");
		Objects.requireNonNull(metrics,"metrics").reset();
		if(array.length==0) return;
		int[] buffer=new int[array.length];
		sortRange(array,buffer,0,array.length-1,1,metrics);
	}

	private void sortRange(int[] array,int[] buffer,int low,int high,int depth,Metrics metrics) {
		metrics.enter(depth);
		if(high-low+1<=CUTOFF) {
			ArraySupport.insertionSort(array,low,high,metrics);
			return;
		}
		int middle=low+(high-low)/2;
		sortRange(array,buffer,low,middle,depth+1,metrics);
		sortRange(array,buffer,middle+1,high,depth+1,metrics);
		merge(array,buffer,low,middle,high,metrics);
	}

	private void merge(int[] array,int[] buffer,int low,int middle,int high,Metrics metrics) {
		System.arraycopy(array,low,buffer,low,high-low+1);
		int left=low;
		int right=middle+1;
		for(int target=low;target<=high;target++) {
			if(left>middle) array[target]=buffer[right++];
			else if(right>high) array[target]=buffer[left++];
			else if(metrics.compare(buffer[left],buffer[right])<=0) array[target]=buffer[left++];
			else array[target]=buffer[right++];
		}
	}
}
