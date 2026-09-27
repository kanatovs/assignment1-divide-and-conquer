import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

final class InputFactory {
	static final String[] TYPES={"random","sorted","reverse","duplicates"};
	private InputFactory() {}

	static int[] integers(int size,String type,long seed) {
		Random random=new Random(seed);
		int[] array=new int[size];
		for(int i=0;i<size;i++) array[i]=type.equals("duplicates") ? random.nextInt(8) : random.nextInt();
		if(type.equals("sorted") || type.equals("reverse")) Arrays.sort(array);
		if(type.equals("reverse")) {
			for(int i=0;i<size/2;i++) {
				int value=array[i];
				array[i]=array[size-1-i];
				array[size-1-i]=value;
			}
		}
		return array;
	}

	static Point[] points(int size,String type,long seed) {
		Random random=new Random(seed);
		Point[] points=new Point[size];
		for(int i=0;i<size;i++) {
			points[i]=type.equals("duplicates")
				? new Point(random.nextInt(8),random.nextInt(8))
				: new Point(random.nextDouble()*100000,random.nextDouble()*100000);
		}
		Comparator<Point> order=Comparator.comparingDouble(Point::x).thenComparingDouble(Point::y);
		if(type.equals("sorted")) Arrays.sort(points,order);
		if(type.equals("reverse")) Arrays.sort(points,order.reversed());
		return points;
	}
}
