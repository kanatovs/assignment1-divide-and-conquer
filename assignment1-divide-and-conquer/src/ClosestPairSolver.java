import java.util.Arrays;
import java.util.Objects;

public final class ClosestPairSolver {
	public record Result(Point first,Point second,double distance) {
		public boolean exists() { return first!=null; }
	}

	private static final Result NONE=new Result(null,null,Double.POSITIVE_INFINITY);

	public Result solve(Point[] points,Metrics metrics) {
		validate(points,metrics);
		if(points.length<2) return NONE;
		Point[] working=points.clone();
		Arrays.sort(working,(first,second)->compareX(first,second,metrics));
		Point[] buffer=new Point[points.length];
		return solveRange(working,buffer,0,working.length,1,metrics);
	}

	// Entry: this range is x-ordered. Exit: exactly the same points are y-ordered.
	private Result solveRange(Point[] points,Point[] buffer,int low,int high,int depth,Metrics metrics) {
		metrics.enter(depth);
		if(high-low<=3) {
			Result best=NONE;
			for(int i=low;i<high;i++) {
				for(int j=i+1;j<high;j++) best=consider(points[i],points[j],best,metrics);
			}
			insertionByY(points,low,high,metrics);
			return best;
		}
		int middle=low+(high-low)/2;
		double middleX=points[middle].x(); // Save BEFORE recursion changes the order.
		Result left=solveRange(points,buffer,low,middle,depth+1,metrics);
		Result right=solveRange(points,buffer,middle,high,depth+1,metrics);
		Result best=metrics.compare(left.distance(),right.distance())<=0 ? left : right;
		mergeByY(points,buffer,low,middle,high,metrics);
		int count=0;
		for(int i=low;i<high;i++) {
			if(metrics.compare(Math.abs(points[i].x()-middleX),best.distance())<0) {
				buffer[low+count++]=points[i];
			}
		}
		for(int i=0;i<count;i++) {
			// Geometry bounds the number of useful next neighbors by seven.
			for(int j=i+1;j<count && j<=i+7;j++) {
				Point first=buffer[low+i];
				Point second=buffer[low+j];
				if(metrics.compare(second.y()-first.y(),best.distance())>=0) break;
				best=consider(first,second,best,metrics);
			}
		}
		return best;
	}

	public Result bruteForce(Point[] points,Metrics metrics) {
		validate(points,metrics);
		Result best=NONE;
		for(int i=0;i<points.length;i++) {
			for(int j=i+1;j<points.length;j++) best=consider(points[i],points[j],best,metrics);
		}
		return best;
	}

	private Result consider(Point first,Point second,Result best,Metrics metrics) {
		metrics.distanceEvaluations++;
		double distance=Math.hypot(first.x()-second.x(),first.y()-second.y());
		if(!best.exists() || metrics.compare(distance,best.distance())<0) {
			return new Result(first,second,distance);
		}
		return best;
	}

	private static void validate(Point[] points,Metrics metrics) {
		Objects.requireNonNull(points,"points");
		Objects.requireNonNull(metrics,"metrics").reset();
		for(Point point:points) Objects.requireNonNull(point,"point");
	}

	private static int compareX(Point first,Point second,Metrics metrics) {
		int result=metrics.compare(first.x(),second.x());
		return result!=0 ? result : metrics.compare(first.y(),second.y());
	}

	private static int compareY(Point first,Point second,Metrics metrics) {
		int result=metrics.compare(first.y(),second.y());
		return result!=0 ? result : metrics.compare(first.x(),second.x());
	}

	private void insertionByY(Point[] points,int low,int high,Metrics metrics) {
		for(int i=low+1;i<high;i++) {
			Point value=points[i];
			int j=i-1;
			while(j>=low && compareY(points[j],value,metrics)>0) {
				points[j+1]=points[j];
				j--;
			}
			points[j+1]=value;
		}
	}

	private void mergeByY(Point[] points,Point[] buffer,int low,int middle,int high,Metrics metrics) {
		System.arraycopy(points,low,buffer,low,high-low);
		int left=low;
		int right=middle;
		for(int target=low;target<high;target++) {
			if(left>=middle) points[target]=buffer[right++];
			else if(right>=high) points[target]=buffer[left++];
			else if(compareY(buffer[left],buffer[right],metrics)<=0) points[target]=buffer[left++];
			else points[target]=buffer[right++];
		}
	}
}
