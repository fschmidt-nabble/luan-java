package luan.modules;

import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;


public final class MathLuan {

	public static double abs(double x) {
		return Math.abs(x);
	}

	public static double acos(double x) {
		return Math.acos(x);
	}

	public static double asin(double x) {
		return Math.asin(x);
	}

	public static double atan(double x) {
		return Math.atan(x);
	}

	public static double atan2(double y,double x) {
		return Math.atan2(y,x);
	}

	public static double ceil(double x) {
		return Math.ceil(x);
	}

	public static double cos(double x) {
		return Math.cos(x);
	}

	public static double cosh(double x) {
		return Math.cosh(x);
	}

	public static double deg(double x) {
		return Math.toDegrees(x);
	}

	public static double exp(double x) {
		return Math.exp(x);
	}

	public static double floor(double x) {
		return Math.floor(x);
	}

	public static double log(double x) {
		return Math.log(x);
	}

	public static double min(double x,double... a) {
		for( double d : a ) {
			if( x > d )
				x = d;
		}
		return x;
	}

	public static double max(double x,double... a) {
		for( double d : a ) {
			if( x < d )
				x = d;
		}
		return x;
	}

	public static double[] modf(double x) {
		double i = (int)x;
		return new double[]{i,x-i};
	}

	public static double pow(double x,double y) {
		return Math.pow(x,y);
	}

	public static double rad(double x) {
		return Math.toRadians(x);
	}

	public static double random(Integer m,Integer n) {
		if( m==null )
			return Math.random();
		if( n==null )
			return Math.floor(m*Math.random()) + 1;
		return Math.floor((n-m+1)*Math.random()) + m;
	}

	public static double sin(double x) {
		return Math.sin(x);
	}

	public static double sinh(double x) {
		return Math.sinh(x);
	}

	public static double sqrt(double x) {
		return Math.sqrt(x);
	}

	public static double tan(double x) {
		return Math.tan(x);
	}

	public static double tanh(double x) {
		return Math.tanh(x);
	}

}
