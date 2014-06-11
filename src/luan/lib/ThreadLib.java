package luan.lib;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import luan.LuanState;
import luan.LuanFunction;
import luan.LuanTable;
import luan.LuanJavaFunction;
import luan.LuanException;
import luan.DeepCloner;


public final class ThreadLib {

	public static final String NAME = "Thread";

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			LuanTable module = new LuanTable();
			try {
				add( module, "fork", LuanState.class, LuanFunction.class, new Object[0].getClass() );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return module;
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(ThreadLib.class.getMethod(method,parameterTypes),null) );
	}

	private static final Executor exec = Executors.newCachedThreadPool();

	public static void fork(LuanState luan,LuanFunction fn,Object... args) {
		DeepCloner cloner = new DeepCloner();
		final LuanState newLuan = cloner.deepClone(luan);
		final LuanFunction newFn = cloner.get(fn);
		final Object[] newArgs = cloner.deepClone(args);
		exec.execute(new Runnable(){public void run() {
			try {
				newLuan.call(newFn,"<forked>",newArgs);
			} catch(LuanException e) {
				e.printStackTrace();
			}
		}});
	}

}
