package luan.modules;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import luan.Luan;
import luan.LuanState;
import luan.LuanFunction;
import luan.LuanTable;
import luan.LuanException;
import luan.DeepCloner;


public final class ThreadLuan {
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
