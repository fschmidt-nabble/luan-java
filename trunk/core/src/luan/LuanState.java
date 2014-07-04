package luan;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import luan.impl.LuanCompiler;
import luan.modules.BasicLuan;
import luan.modules.PackageLuan;


public abstract class LuanState implements DeepCloneable<LuanState> {

	final List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();

	private LuanTable registry;
	private LuanTable global;

	protected LuanState() {
		registry = new LuanTable();
		global = new LuanTable();
		global.put("_G",global);
	}

	protected LuanState(LuanState luan) {}

	@Override public void deepenClone(LuanState clone,DeepCloner cloner) {
		clone.registry = cloner.deepClone(registry);
		clone.global = cloner.deepClone(global);
	}

	public abstract LuanTable currentEnvironment();

	public final LuanTable registry() {
		return registry;
	}

	public final LuanTable registryTable(Object key) {
		LuanTable tbl = (LuanTable)registry.get(key);
		if( tbl == null ) {
			tbl = new LuanTable();
			registry.put(key,tbl);
		}
		return tbl;
	}

	public final LuanTable global() {
		return global;
	}

	public static LuanState newStandard() {
		try {
			LuanState luan = LuanCompiler.newLuanState();
			BasicLuan.do_file(luan,"java:luan/init.luan");
			return luan;
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
	}

	public final Object eval(String cmd) {
		return eval(cmd,new LuanTable());
	}

	public final Object eval(String cmd,LuanTable env) {
		try {
			LuanFunction fn = BasicLuan.load(this,cmd,"eval",env,true);
			return call(fn);
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
	}

	public final LuanTable getMetatable(Object obj) {
		return getMetatable(obj,null);
	}

	public abstract LuanTable getMetatable(Object obj,MetatableGetter beforeThis);

	public abstract void addMetatableGetter(MetatableGetter mg);

	public final LuanBit bit(LuanElement el) {
		return new LuanBit(this,el);
	}

	public final Object getHandler(String op,Object obj) {
		LuanTable t = getMetatable(obj);
		return t==null ? null : t.get(op);
	}


	// convenience methods

	private final LuanBit JAVA = bit(LuanElement.JAVA);

	public LuanException exception(Object msg) {
		return JAVA.exception(msg);
	}

	public Object call(LuanFunction fn) throws LuanException {
		return call(fn,null,LuanFunction.NOTHING);
	}

	public Object call(LuanFunction fn,String fnName) throws LuanException {
		return call(fn,fnName,LuanFunction.NOTHING);
	}

	public Object call(LuanFunction fn,Object[] args) throws LuanException {
		return call(fn,null,args);
	}

	public Object call(LuanFunction fn,String fnName,Object[] args) throws LuanException {
		return JAVA.call(fn,fnName,args);
	}

	public LuanFunction checkFunction(Object obj) throws LuanException {
		return JAVA.checkFunction(obj);
	}

	public String toString(Object obj) throws LuanException {
		return JAVA.toString(obj);
	}

	public String repr(Object obj) throws LuanException {
		return JAVA.repr(obj);
	}

	public boolean isLessThan(Object o1,Object o2) throws LuanException {
		return JAVA.isLessThan(o1,o2);
	}
}
