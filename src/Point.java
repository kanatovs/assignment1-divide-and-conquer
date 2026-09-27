/** Immutable planar point. NaN and infinite coordinates are rejected. */
public record Point(double x,double y) {
	public Point {
		if(!Double.isFinite(x) || !Double.isFinite(y)) {
			throw new IllegalArgumentException("Coordinates must be finite");
		}
	}
}
