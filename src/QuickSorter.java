import java.util.Objects;
import java.util.Random;

public final class QuickSorter {
	public void sort(int[] array,long seed,Metrics metrics) {
		Objects.requireNonNull(array,"array");
		Objects.requireNonNull(metrics,"metrics").reset();
		if(array.length==0) return;
		sortRange(array,0,array.length-1,new Random(seed),1,metrics);
	}

	private void sortRange(int[] array,int low,int high,Random random,int depth,Metrics metrics) {
		metrics.enter(depth);
		while(low<high) {
			int pivot=array[low+random.nextInt(high-low+1)];
			ArraySupport.EqualRange equal=ArraySupport.partition(array,low,high,pivot,metrics);
			int leftSize=equal.first()-low;
			int rightSize=high-equal.last();
			// Only the smaller side creates another recursive frame.
			if(leftSize<rightSize) {
				if(leftSize>1) sortRange(array,low,equal.first()-1,random,depth+1,metrics);
				low=equal.last()+1;
			} else {
				if(rightSize>1) sortRange(array,equal.last()+1,high,random,depth+1,metrics);
				high=equal.first()-1;
			}
		}
	}
}
