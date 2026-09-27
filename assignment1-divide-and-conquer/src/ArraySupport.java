/** Shared insertion sort and in-place three-way partition. */
final class ArraySupport {
	private ArraySupport() {}

	record EqualRange(int first,int last) {}

	static void swap(int[] array,int first,int second,Metrics metrics) {
		if(first==second) return;
		int value=array[first];
		array[first]=array[second];
		array[second]=value;
		metrics.swaps++;
	}

	static void insertionSort(int[] array,int low,int high,Metrics metrics) {
		for(int i=low+1;i<=high;i++) {
			int value=array[i];
			int j=i-1;
			while(j>=low && metrics.compare(array[j],value)>0) {
				array[j+1]=array[j];
				j--;
			}
			array[j+1]=value;
		}
	}

	static EqualRange partition(int[] array,int low,int high,int pivot,Metrics metrics) {
		int less=low;
		int current=low;
		int greater=high;
		// [low,less) < pivot; [less,current) == pivot; (greater,high] > pivot.
		while(current<=greater) {
			int comparison=metrics.compare(array[current],pivot);
			if(comparison<0) {
				swap(array,less++,current++,metrics);
			} else if(comparison>0) {
				swap(array,current,greater--,metrics);
				// The replacement at current has not been examined yet.
			} else {
				current++;
			}
		}
		return new EqualRange(less,greater);
	}
}
