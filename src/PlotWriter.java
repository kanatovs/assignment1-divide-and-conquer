import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Headless Java2D plots: no Python, spreadsheet application, or extra JAR needed. */
public final class PlotWriter {
	private static final int WIDTH=1280,HEIGHT=800,LEFT=155,RIGHT=1190,TOP=205,BOTTOM=685;
	private static final float[][] DASHES={null,{13,6},{3,6},{13,5,3,5}};
	private PlotWriter() {}

	public static void write(List<ReportWriter.Summary> data,String metric,String title,String axis,boolean logY) throws IOException {
		List<ReportWriter.Summary> random=data.stream().filter(s->s.type().equals("random")).toList();
		double smallest=random.stream().mapToDouble(s->value(s,metric)).filter(v->v>0).min().orElse(1);
		double largest=random.stream().mapToDouble(s->value(s,metric)).max().orElse(1);
		double minimum=logY ? Math.floor(Math.log10(smallest)) : 0;
		double maximum=logY ? Math.ceil(Math.log10(largest)) : Math.ceil(largest/2)*2+2;
		if(maximum<=minimum) maximum=minimum+1;
		BufferedImage image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics=image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0,0,WIDTH,HEIGHT);
		graphics.setColor(Color.BLACK);
		graphics.setFont(new Font(Font.SANS_SERIF,Font.BOLD,30));
		graphics.drawString(title,LEFT,48);
		graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,18));
		graphics.drawString("Random inputs | 5 measured trials per size | "+(logY ? "logarithmic x and y axes" : "logarithmic x axis"),LEFT,80);
		for(int series=0;series<Experiment.ALGORITHMS.length;series++) {
			int x=LEFT+(series%2)*490;
			int y=120+(series/2)*37;
			graphics.setStroke(stroke(series));
			graphics.drawLine(x,y,x+65,y);
			marker(graphics,x+32,y,series);
			graphics.drawString(Experiment.ALGORITHMS[series],x+80,y+6);
		}
		graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,17));
		int ticks=logY ? (int)(maximum-minimum) : (int)(maximum/2);
		for(int tick=0;tick<=ticks;tick++) {
			double scaled=logY ? minimum+tick : tick*2;
			int y=(int)(BOTTOM-(scaled-minimum)/(maximum-minimum)*(BOTTOM-TOP));
			graphics.setStroke(new BasicStroke(1));
			graphics.setColor(Color.LIGHT_GRAY);
			graphics.drawLine(LEFT,y,RIGHT,y);
			graphics.setColor(Color.BLACK);
			String label=logY ? formatPower((int)scaled) : String.format(Locale.ROOT,"%.0f",scaled);
			graphics.drawString(label,LEFT-15-graphics.getFontMetrics().stringWidth(label),y+6);
		}
		for(int n:Experiment.SIZES) {
			int x=x(n);
			graphics.setColor(Color.LIGHT_GRAY);
			graphics.drawLine(x,TOP,x,BOTTOM);
			graphics.setColor(Color.BLACK);
			String label=String.format(Locale.ROOT,"%,d",n);
			graphics.drawString(label,x-graphics.getFontMetrics().stringWidth(label)/2,BOTTOM+30);
		}
		graphics.setStroke(new BasicStroke(1.5f));
		graphics.drawLine(LEFT,TOP,LEFT,BOTTOM);
		graphics.drawLine(LEFT,BOTTOM,RIGHT,BOTTOM);
		for(int series=0;series<Experiment.ALGORITHMS.length;series++) {
			String name=Experiment.ALGORITHMS[series];
			List<ReportWriter.Summary> values=random.stream().filter(s->s.algorithm().equals(name)).sorted(Comparator.comparingInt(ReportWriter.Summary::n)).toList();
			graphics.setStroke(stroke(series));
			int previousX=0,previousY=0;
			for(int i=0;i<values.size();i++) {
				ReportWriter.Summary item=values.get(i);
				double raw=value(item,metric);
				double transformed=logY ? Math.log10(Math.max(raw,1e-12)) : raw;
				int x=x(item.n());
				int y=(int)(BOTTOM-(transformed-minimum)/(maximum-minimum)*(BOTTOM-TOP));
				if(i>0) graphics.drawLine(previousX,previousY,x,y);
				marker(graphics,x,y,series);
				previousX=x;
				previousY=y;
			}
		}
		graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,20));
		graphics.drawString("Input size n",(LEFT+RIGHT)/2-55,HEIGHT-38);
		graphics.rotate(-Math.PI/2);
		graphics.drawString(axis,-(TOP+BOTTOM)/2-graphics.getFontMetrics().stringWidth(axis)/2,42);
		graphics.rotate(Math.PI/2);
		graphics.dispose();
		Files.createDirectories(Path.of("docs","plots"));
		ImageIO.write(image,"png",Path.of("docs","plots",metric+"-vs-n.png").toFile());
	}

	private static int x(int n) { return (int)(LEFT+(Math.log10(n)-2)/3*(RIGHT-LEFT)); }
	private static double value(ReportWriter.Summary item,String metric) {
		return switch(metric) { case "time"->item.timeMs(); case "depth"->item.depth(); default->item.comparisons(); };
	}
	private static BasicStroke stroke(int series) {
		return new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1,DASHES[series],0);
	}
	private static String formatPower(int exponent) {
		return exponent>=0 && exponent<=6 ? String.format(Locale.ROOT,"%,.0f",Math.pow(10,exponent))
			: String.format(Locale.ROOT,"%s",Math.pow(10,exponent));
	}
	private static void marker(Graphics2D graphics,int x,int y,int series) {
		switch(series) {
			case 0 -> graphics.fillOval(x-5,y-5,10,10);
			case 1 -> graphics.fillRect(x-5,y-5,10,10);
			case 2 -> graphics.fillPolygon(new Polygon(new int[]{x,x-6,x+6},new int[]{y-7,y+5,y+5},3));
			default -> graphics.fillPolygon(new Polygon(new int[]{x,x-6,x,x+6},new int[]{y-7,y,y+7,y},4));
		}
	}
}
