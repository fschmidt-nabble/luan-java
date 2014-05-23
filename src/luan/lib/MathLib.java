package luan.lib;

import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;


public final class MathLib {

	public static final String NAME = "Math";

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object[] call(LuanState luan,Object[] args) {
			LuanTable module = new LuanTable();
			try {
				add( module, "abs", Double.TYPE );
				add( module, "acos", Double.TYPE );
				add( module, "asin", Double.TYPE );
				add( module, "atan", Double.TYPE );
				add( module, "atan2", Double.TYPE, Double.TYPE );
				add( module, "ceil", Double.TYPE );
				add( module, "cos", Double.TYPE );
				add( module, "cosh", Double.TYPE );
				add( module, "deg", Double.TYPE );
				add( module, "exp", Double.TYPE );
				add( module, "floor", Double.TYPE );
				add( module, "log", Double.TYPE );
				add( module, "min", Double.TYPE, new double[0].getClass() );
				add( module, "max", Double.TYPE, new double[0].getClass() );
				add( module, "modf", Double.TYPE );
				module.put("pi",Math.PI);
				add( module, "pow", Double.TYPE, Double.TYPE );
				add( module, "rad", Double.TYPE );
				add( module, "random" );
				add( module, "sin", Double.TYPE );
				add( module, "sinh", Double.TYPE );
				add( module, "sqrt", Double.TYPE );
				add( module, "tan", Double.TYPE );
				add( module, "tanh", Double.TYPE );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return new Object[]{module};
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(MathLib.class.getMethod(method,parameterTypes),null) );
	}

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

	public static double random() {
		return Math.random();
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
