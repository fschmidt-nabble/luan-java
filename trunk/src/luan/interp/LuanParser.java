package luan.interp;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import luan.Luan;
import luan.LuanState;
import luan.LuanSource;
import luan.parser.Parser;
import luan.parser.ParseException;
import luan.lib.PackageLib;


final class LuanParser {

	private static final class Frame {
		final Frame parent;
		final List<String> symbols = new ArrayList<String>();
		int stackSize = 0;
		int loops = 0;
		boolean isVarArg = false;
		final List<String> upValueSymbols = new ArrayList<String>();
		final List<UpValue.Getter> upValueGetters = new ArrayList<UpValue.Getter>();

		Frame(UpValue.Getter envGetter) {
			this.parent = null;
			upValueSymbols.add(_ENV);
			upValueGetters.add(envGetter);
		}

		Frame(Frame parent) {
			this.parent = parent;
			if( upValueIndex(_ENV) != 0 )
				throw new RuntimeException();
		}

		int stackIndex(String name) {
			int i = symbols.size();
			while( --i >= 0 ) {
				if( symbols.get(i).equals(name) )
					return i;
			}
			return -1;
		}

		int upValueIndex(String name) {
			int i = upValueSymbols.size();
			while( --i >= 0 ) {
				if( upValueSymbols.get(i).equals(name) )
					return i;
			}
			if( parent==null )
				return -1;
			i = parent.stackIndex(name);
			if( i != -1 ) {
				upValueGetters.add(new UpValue.StackGetter(i));
			} else {
				i = parent.upValueIndex(name);
				if( i == -1 )
					return -1;
				upValueGetters.add(new UpValue.NestedGetter(i));
			}
			upValueSymbols.add(name);
			return upValueSymbols.size() - 1;
		}

		void addUpValueGetter(String name,UpValue.Getter upValueGetter) {
			upValueSymbols.add(name);
			upValueGetters.add(upValueGetter);
		}
	}

	private static final String _ENV = "_ENV";
	private static final UpValue.Getter[] NO_UP_VALUE_GETTERS = new UpValue.Getter[0];

	private final LuanSource source;
	private Frame frame;
	private final Parser parser;
	private final boolean interactive;

	LuanParser(LuanSource source,UpValue.Getter envGetter) {
		this.source = source;
		this.frame = new Frame(envGetter);
		this.parser = new Parser(source);
		this.interactive = envGetter==UpValue.globalGetter;
	}

	void addVar(String name,Object value) {
		frame.addUpValueGetter(name,new UpValue.ValueGetter(value));
	}

	private LuanSource.Element se(int start) {
		return new LuanSource.Element(source,start,parser.currentIndex());
	}

	private List<String> symbols() {
		return frame.symbols;
	}

	private int symbolsSize() {
		return frame.symbols.size();
	}

	private void addSymbol(String name) {
		frame.symbols.add(name);
		if( frame.stackSize < symbolsSize() )
			frame.stackSize = symbolsSize();
	}

	private void addSymbols(List<String> names) {
		frame.symbols.addAll(names);
		if( frame.stackSize < symbolsSize() )
			frame.stackSize = symbolsSize();
	}

	private int stackIndex(String name) {
		return frame.stackIndex(name);
	}

	private void popSymbols(int n) {
		List<String> symbols = frame.symbols;
		while( n-- > 0 ) {
			symbols.remove(symbols.size()-1);
		}
	}

	private int upValueIndex(String name) {
		return frame.upValueIndex(name);
	}

	private void incLoops() {
		frame.loops++;
	}

	private void decLoops() {
		frame.loops--;
	}

	private <T> T required(T t) throws ParseException {
		if( t==null )
			throw parser.exception();
		return t;
	}

	private <T> T required(T t,String msg) throws ParseException {
		if( t==null )
			throw parser.exception(msg);
		return t;
	}

	private static Expr expr(Code code) {
		if( code instanceof Expressions )
			return new ExpressionsExpr((Expressions)code);
		return (Expr)code;
	}

	private FnDef newFnDef(int start,Stmt stmt) {
		return new FnDef( se(start), stmt, frame.stackSize, symbolsSize(), frame.isVarArg, frame.upValueGetters.toArray(NO_UP_VALUE_GETTERS) );
	}

	FnDef Expressions() throws ParseException {
		Spaces();
		int start = parser.begin();
		Expressions exprs = ExpList();
		if( exprs != null && parser.endOfInput() ) {
			Stmt stmt = new ReturnStmt( se(start), exprs );
			return parser.success(newFnDef(start,stmt));
		}
		return parser.failure(null);
	}

	FnDef RequiredModule() throws ParseException {
		Spaces();
		int start = parser.begin();
		frame.isVarArg = true;
		Stmt stmt = RequiredBlock();
		if( parser.endOfInput() )
			return parser.success(newFnDef(start,stmt));
		throw parser.exception();
	}

	private Stmt RequiredBlock() throws ParseException {
		List<Stmt> stmts = new ArrayList<Stmt>();
		int stackStart = symbolsSize();
		Stmt(stmts);
		while( StmtSep(stmts) ) {
			Spaces();
			Stmt(stmts);
		}
		int stackEnd = symbolsSize();
		popSymbols( stackEnd - stackStart );
		if( stmts.isEmpty() )
			return Stmt.EMPTY;
		if( stmts.size()==1 && stackStart==stackEnd )
			return stmts.get(0);
		return new Block( stmts.toArray(new Stmt[0]), stackStart, stackEnd );
	}

	private boolean StmtSep(List<Stmt> stmts) throws ParseException {
		parser.begin();
		if( parser.match( ';' ) )
			return parser.success();
		if( parser.match( "--" ) ) {
			while( parser.noneOf("\r\n") );
		}
		return EndOfLine() ? parser.success() : parser.failure();
	}

	private boolean EndOfLine() {
		return parser.match( "\r\n" ) || parser.match( '\r' ) || parser.match( '\n' );
	}

	private void Stmt(List<Stmt> stmts) throws ParseException {
		if( LocalStmt(stmts) )
			return;
		Stmt stmt;
		if( (stmt=ReturnStmt()) != null
			|| (stmt=FunctionStmt()) != null
			|| (stmt=LocalFunctionStmt()) != null
			|| (stmt=ImportStmt()) != null
			|| (stmt=BreakStmt()) != null
			|| (stmt=GenericForStmt()) != null
			|| (stmt=TryStmt()) != null
			|| (stmt=DoStmt()) != null
			|| (stmt=WhileStmt()) != null
			|| (stmt=FunctionStmt()) != null
			|| (stmt=RepeatStmt()) != null
			|| (stmt=IfStmt()) != null
			|| (stmt=SetStmt()) != null
			|| (stmt=ExpressionsStmt()) != null
		) {
			stmts.add(stmt);
		}
	}

	private Expressions JspExpressions() throws ParseException {
		int start = parser.begin();
		if( !parser.match( "%>" ) )
			return parser.failure(null);
		EndOfLine();
		ExpList.Builder builder = new ExpList.Builder();
		while(true) {
			if( parser.match( "<%=" ) ) {
				Spaces();
				builder.add( RequiredExpr() );
				RequiredMatch( "%>" );
			} else if( parser.match( "<%" ) ) {
				Spaces();
				return parser.success(builder.build());
			} else {
				int i = parser.currentIndex();
				do {
					if( parser.match( "%>" ) )
						throw parser.exception("'%>' unexpected");
					if( !parser.anyChar() )
						throw parser.exception("Unclosed JSP expressions");
				} while( !parser.test( "<%" ) );
				String match = parser.textFrom(i);
				builder.add( new ConstExpr(match) );
			}
		}
	}

	private Stmt ReturnStmt() throws ParseException {
		int start = parser.begin();
		if( !Keyword("return") )
			return parser.failure(null);
		Expressions exprs = ExpList();
		if( exprs==null )
			exprs = ExpList.emptyExpList;
		return parser.success( new ReturnStmt(se(start),exprs) );
	}

	private Stmt FunctionStmt() throws ParseException {
		parser.begin();
		if( !Keyword("function") )
			return parser.failure(null);

		int start = parser.currentIndex();
		Var var = nameVar(start,RequiredName());
		while( parser.match( '.' ) ) {
			Spaces();
			var = indexVar( start, expr(var.expr()), NameExpr(false) );
		}
		Settable fnName = var.settable();

		FnDef fnDef = RequiredFunction(false);
		return parser.success( new SetStmt(fnName,fnDef) );
	}

	private Stmt LocalFunctionStmt() throws ParseException {
		parser.begin();
		if( !(Keyword("local") && Keyword("function")) )
			return parser.failure(null);
		String name = RequiredName();
		addSymbol( name );
		FnDef fnDef = RequiredFunction(false);
		return parser.success( new SetStmt( new SetLocalVar(symbolsSize()-1), fnDef ) );
	}

	private Stmt ImportStmt() throws ParseException {
		int start = parser.begin();
		if( !Keyword("import") )
			return parser.failure(null);
		Expr importExpr = new ConstExpr(se(start),PackageLib.require);
		String modName = StringLiteral(false);
		if( modName==null )
			return parser.failure(null);
		String varName = modName.substring(modName.lastIndexOf('.')+1);
		LuanSource.Element se = se(start);
		FnCall require = new FnCall( se, importExpr, new ExpList.SingleExpList(new ConstExpr(modName)) );
		Settable settable;
		if( interactive ) {
			settable = new SetTableEntry( se(start), env(), new ConstExpr(varName) );
		} else {
			addSymbol( varName );
			settable = new SetLocalVar(symbolsSize()-1);
		}
		return parser.success( new SetStmt( settable, expr(require) ) );
	}

	private Stmt BreakStmt() throws ParseException {
		parser.begin();
		if( !Keyword("break") )
			return parser.failure(null);
		if( frame.loops <= 0 )
			throw parser.exception("'break' outside of loop");
		return parser.success( new BreakStmt() );
	}

	private Stmt GenericForStmt() throws ParseException {
		int start = parser.begin();
		int stackStart = symbolsSize();
		if( !Keyword("for") )
			return parser.failure(null);
		List<String> names = RequiredNameList(false);
		if( !Keyword("in") )
			return parser.failure(null);
		Expr expr = expr(RequiredExpr());
		RequiredKeyword("do");
		addSymbols(names);
		Stmt loop = RequiredLoopBlock();
		RequiredKeyword("end");
		Stmt stmt = new GenericForStmt( se(start), stackStart, symbolsSize() - stackStart, expr, loop );
		popSymbols( symbolsSize() - stackStart );
		return parser.success(stmt);
	}

	private Stmt TryStmt() throws ParseException {
		parser.begin();
		if( !Keyword("try") )
			return parser.failure(null);
		Stmt tryBlock = RequiredBlock();
		RequiredKeyword("catch");
		String name = RequiredName();
		addSymbol(name);
		RequiredKeyword("do");
		Stmt catchBlock = RequiredBlock();
		RequiredKeyword("end");
		Stmt stmt = new TryStmt( tryBlock, symbolsSize()-1, catchBlock );
		popSymbols(1);
		return parser.success(stmt);
	}

	private Stmt DoStmt() throws ParseException {
		parser.begin();
		if( !Keyword("do") )
			return parser.failure(null);
		Stmt stmt = RequiredBlock();
		RequiredKeyword("end");
		return parser.success(stmt);
	}

	private boolean LocalStmt(List<Stmt> stmts) throws ParseException {
		parser.begin();
		if( !Keyword("local") )
			return parser.failure();
		List<String> names = NameList(false);
		if( names==null )
			return parser.failure();
		if( parser.match( '=' ) ) {
			Spaces();
			Expressions values = ExpList();
			if( values==null )
				throw parser.exception("Expressions expected");
			SetLocalVar[] vars = new SetLocalVar[names.size()];
			int stackStart = symbolsSize();
			for( int i=0; i<vars.length; i++ ) {
				vars[i] = new SetLocalVar(stackStart+i);
			}
			stmts.add( new SetStmt( vars, values ) );
		}
		addSymbols(names);
		return parser.success();
	}

	private List<String> RequiredNameList(boolean inParens) throws ParseException {
		parser.begin();
		List<String> names = NameList(inParens);
		if( names==null )
			parser.exception("Name expected");
		return parser.success(names);
	}

	private List<String> NameList(boolean inParens) throws ParseException {
		String name = Name(inParens);
		if( name==null )
			return null;
		List<String> names = new ArrayList<String>();
		names.add(name);
		while( parser.match( ',' ) ) {
			Spaces(inParens);
			names.add( RequiredName() );
		}
		return names;
	}

	private Stmt WhileStmt() throws ParseException {
		parser.begin();
		if( !Keyword("while") )
			return parser.failure(null);
		Expr cnd = expr(RequiredExpr());
		RequiredKeyword("do");
		Stmt loop = RequiredLoopBlock();
		RequiredKeyword("end");
		return parser.success( new WhileStmt(cnd,loop) );
	}

	private Stmt RepeatStmt() throws ParseException {
		parser.begin();
		if( !Keyword("repeat") )
			return parser.failure(null);
		Stmt loop = RequiredLoopBlock();
		RequiredKeyword("until");
		Expr cnd = expr(RequiredExpr());
		return parser.success( new RepeatStmt(loop,cnd) );
	}

	private Stmt RequiredLoopBlock() throws ParseException {
		incLoops();
		Stmt stmt = RequiredBlock();
		decLoops();
		return stmt;
	}

	private Stmt IfStmt() throws ParseException {
		parser.begin();
		if( !Keyword("if") )
			return parser.failure(null);
		return parser.success( IfStmt2() );
	}

	private Stmt IfStmt2() throws ParseException {
		Expr cnd = expr(RequiredExpr());
		RequiredKeyword("then");
		Stmt thenBlock = RequiredBlock();
		Stmt elseBlock;
		if( Keyword("elseif") ) {
			elseBlock = IfStmt2();
		} else {
			elseBlock = Keyword("else") ? RequiredBlock() : Stmt.EMPTY;
			RequiredKeyword("end");
		}
		return new IfStmt(cnd,thenBlock,elseBlock);
	}

	private Stmt SetStmt() throws ParseException {
		parser.begin();
		List<Settable> vars = new ArrayList<Settable>();
		Settable s = SettableVar();
		if( s == null )
			return parser.failure(null);
		vars.add(s);
		while( parser.match( ',' ) ) {
			Spaces();
			s = SettableVar();
			if( s == null )
				return parser.failure(null);
			vars.add(s);
		}
		if( !parser.match( '=' ) )
			return parser.failure(null);
		Spaces();
		Expressions values = ExpList();
		if( values==null )
			throw parser.exception("Expressions expected");
		return parser.success( new SetStmt( vars.toArray(new Settable[0]), values ) );
	}

	private Stmt ExpressionsStmt() throws ParseException {
		parser.begin();
		Expressions exprs = ExpList();
		if( exprs==null )
			return parser.failure(null);
		return parser.success( new ExpressionsStmt(exprs) );
	}

	private Settable SettableVar() throws ParseException {
		int start = parser.begin();
		Var var = VarZ(false);
		if( var==null )
			return null;
		return var.settable();
	}

	private Code RequiredExpr() throws ParseException {
		return RequiredExpr(false);
	}

	private Code Expr() throws ParseException {
		return Expr(false);
	}

	private Code RequiredExpr(boolean inParens) throws ParseException {
		parser.begin();
		return parser.success(required(Expr(inParens),"Bad expression"));
	}

	private Code Expr(boolean inParens) throws ParseException {
		parser.begin();
		Code exp;
		return (exp = VarArgs(inParens)) != null
			|| (exp = JspExpressions()) != null
			|| (exp = OrExpr(inParens)) != null
			? parser.success(exp)
			: parser.failure((Code)null)
		;
	}

	private Code OrExpr(boolean inParens) throws ParseException {
		int start = parser.begin();
		Code exp = AndExpr(inParens);
		if( exp==null )
			return parser.failure(null);
		while( Keyword("or",inParens) ) {
			exp = new OrExpr( se(start), expr(exp), required(expr(AndExpr(inParens))) );
		}
		return parser.success(exp);
	}

	private Code AndExpr(boolean inParens) throws ParseException {
		int start = parser.begin();
		Code exp = RelExpr(inParens);
		if( exp==null )
			return parser.failure(null);
		while( Keyword("and",inParens) ) {
			exp = new AndExpr( se(start), expr(exp), required(expr(RelExpr(inParens))) );
		}
		return parser.success(exp);
	}

	private Code RelExpr(boolean inParens) throws ParseException {
		int start = parser.begin();
		Code exp = ConcatExpr(inParens);
		if( exp==null )
			return parser.failure(null);
		while(true) {
			if( parser.match("==") ) {
				Spaces(inParens);
				exp = new EqExpr( se(start), expr(exp), required(expr(ConcatExpr(inParens))) );
			} else if( parser.match("~=") ) {
				Spaces(inParens);
				exp = new NotExpr( se(start), new EqExpr( se(start), expr(exp), required(expr(ConcatExpr(inParens))) ) );
			} else if( parser.match("<=") ) {
				Spaces(inParens);
				exp = new LeExpr( se(start), expr(exp), required(expr(ConcatExpr(inParens))) );
			} else if( parser.match(">=") ) {
				Spaces(inParens);
				exp = new LeExpr( se(start), required(expr(ConcatExpr(inParens))), expr(exp) );
			} else if( parser.match("<") ) {
				Spaces(inParens);
				exp = new LtExpr( se(start), expr(exp), required(expr(ConcatExpr(inParens))) );
			} else if( parser.match(">") ) {
				Spaces(inParens);
				exp = new LtExpr( se(start), required(expr(ConcatExpr(inParens))), expr(exp) );
			} else
				break;
		}
		return parser.success(exp);
	}

	private Code ConcatExpr(boolean inParens) throws ParseException {
		int start = parser.begin();
		Code exp = SumExpr(inParens);
		if( exp==null )
			return parser.failure(null);
		if( parser.match("..") ) {
			Spaces(inParens);
			exp = new ConcatExpr( se(start), expr(exp), required(expr(ConcatExpr(inParens))) );
		}
		return parser.success(exp);
	}

	private Code SumExpr(boolean inParens) throws ParseException {
		int start = parser.begin();
		Code exp = TermExpr(inParens);
		if( exp==null )
			return parser.failure(null);
		while(true) {
			if( parser.match('+') ) {
				Spaces(inParens);
				exp = new AddExpr( se(start), expr(exp), required(expr(TermExpr(inParens))) );
			} else if( Minus() ) {
				Spaces(inParens);
				exp = new SubExpr( se(start), expr(exp), required(expr(TermExpr(inParens))) );
			} else
				break;
		}
		return parser.success(exp);
	}

	private boolean Minus() {
		parser.begin();
		return parser.match('-') && !parser.match('-') ? parser.success() : parser.failure();
	}

	private Code TermExpr(boolean inParens) throws ParseException {
		int start = parser.begin();
		Code exp = UnaryExpr(inParens);
		if( exp==null )
			return parser.failure(null);
		while(true) {
			if( parser.match('*') ) {
				Spaces(inParens);
				exp = new MulExpr( se(start), expr(exp), required(expr(UnaryExpr(inParens))) );
			} else if( parser.match('/') ) {
				Spaces(inParens);
				exp = new DivExpr( se(start), expr(exp), required(expr(UnaryExpr(inParens))) );
			} else if( Mod() ) {
				Spaces(inParens);
				exp = new ModExpr( se(start), expr(exp), required(expr(UnaryExpr(inParens))) );
			} else
				break;
		}
		return parser.success(exp);
	}

	private boolean Mod() {
		parser.begin();
		return parser.match('%') && !parser.match('>') ? parser.success() : parser.failure();
	}

	private Code UnaryExpr(boolean inParens) throws ParseException {
		int start = parser.begin();
		if( parser.match('#') ) {
			Spaces(inParens);
			return parser.success( new LenExpr( se(start), required(expr(UnaryExpr(inParens))) ) );
		}
		if( Minus() ) {
			Spaces(inParens);
			return parser.success( new UnmExpr( se(start), required(expr(UnaryExpr(inParens))) ) );
		}
		if( Keyword("not",inParens) ) {
			Spaces(inParens);
			return parser.success( new NotExpr( se(start), required(expr(UnaryExpr(inParens))) ) );
		}
		Code exp = PowExpr(inParens);
		if( exp==null )
			return parser.failure(null);
		return parser.success(exp);
	}

	private Code PowExpr(boolean inParens) throws ParseException {
		int start = parser.begin();
		Code exp = SingleExpr(inParens);
		if( exp==null )
			return parser.failure(null);
		if( parser.match('^') ) {
			Spaces(inParens);
			exp = new ConcatExpr( se(start), expr(exp), required(expr(PowExpr(inParens))) );
		}
		return parser.success(exp);
	}

	private Code SingleExpr(boolean inParens) throws ParseException {
		parser.begin();
		Code exp;
		exp = FunctionExpr(inParens);
		if( exp != null )
			return parser.success(exp);
		exp = TableExpr(inParens);
		if( exp != null )
			return parser.success(exp);
		exp = VarExp(inParens);
		if( exp != null )
			return parser.success(exp);
		exp = Literal(inParens);
		if( exp != null )
			return parser.success(exp);
		return parser.failure(null);
	}

	private Expr FunctionExpr(boolean inParens) throws ParseException {
		if( !Keyword("function",inParens) )
			return null;
		return RequiredFunction(inParens);
	}

	private FnDef RequiredFunction(boolean inParens) throws ParseException {
		int start = parser.begin();
		RequiredMatch('(');
		Spaces(true);
		frame = new Frame(frame);
		List<String> names = NameList(false);
		if( names != null ) {
			addSymbols(names);
			if( parser.match(',') ) {
				Spaces(true);
				if( !parser.match("...") )
					throw parser.exception();
				frame.isVarArg = true;
			}
		} else if( parser.match("...") ) {
			Spaces(true);
			frame.isVarArg = true;
		}
		RequiredMatch(')');
		Spaces(inParens);
		Stmt block = RequiredBlock();
		RequiredKeyword("end",inParens);
		FnDef fnDef = newFnDef(start,block);
		frame = frame.parent;
		return parser.success(fnDef);
	}

	private VarArgs VarArgs(boolean inParens) throws ParseException {
		int start = parser.begin();
		if( !frame.isVarArg || !parser.match("...") )
			return parser.failure(null);
		Spaces(inParens);
		return parser.success( new VarArgs(se(start)) );
	}

	private Expr TableExpr(boolean inParens) throws ParseException {
		int start = parser.begin();
		if( !parser.match('{') )
			return parser.failure(null);
		Spaces(true);
		List<TableExpr.Field> fields = new ArrayList<TableExpr.Field>();
		ExpList.Builder builder = new ExpList.Builder();
		while( Field(fields,builder) && FieldSep() );
		if( !parser.match('}') )
			throw parser.exception("Expected table element or '}'");
		return parser.success( new TableExpr( se(start), fields.toArray(new TableExpr.Field[0]), builder.build() ) );
	}

	private boolean FieldSep() throws ParseException {
		if( !parser.anyOf(",;") )
			return false;
		Spaces(true);
		return true;
	}

	private boolean Field(List<TableExpr.Field> fields,ExpList.Builder builder) throws ParseException {
		parser.begin();
		Expr exp = SubExpr(true);
		if( exp==null )
			exp = NameExpr(true);
		if( exp!=null && parser.match('=') ) {
			fields.add( new TableExpr.Field( exp, required(expr(Expr(true))) ) );
			return parser.success();
		}
		parser.rollback();
		Code code = Expr(true);
		if( code != null ) {
			builder.add(code);
			return parser.success();
		}
		return parser.failure();
	}

	private Code VarExp(boolean inParens) throws ParseException {
		Var var = VarZ(inParens);
		return var==null ? null : var.expr();
	}

	private Var VarZ(boolean inParens) throws ParseException {
		int start = parser.begin();
		Var var = VarStart(inParens);
		if( var==null )
			return parser.failure(null);
		Var var2;
		while( (var2=Var2(inParens,start,var.expr())) != null ) {
			var = var2;
		}
		return parser.success(var);
	}

	private Var Var2(boolean inParens,int start,Code exp1) throws ParseException {
		Var var = VarExt(inParens,start,exp1);
		if( var != null )
			return var;
		if( parser.match("->") ) {
			Spaces(inParens);
			ExpList.Builder builder = new ExpList.Builder();
			builder.add(exp1);
			Expr exp2 = expr(RequiredVarExpB(inParens));
			FnCall fnCall = required(Args( inParens, start, exp2, builder ));
			return exprVar(fnCall);
		}
		FnCall fnCall = Args( inParens, start, expr(exp1), new ExpList.Builder() );
		if( fnCall != null )
			return exprVar(fnCall);
		return null;
	}

	private Code RequiredVarExpB(boolean inParens) throws ParseException {
		int start = parser.begin();
		Var var = required(VarStart(inParens));
		Var var2;
		while( (var2=VarExt(inParens,start,var.expr())) != null ) {
			var = var2;
		}
		return parser.success(var.expr());
	}

	private Var VarExt(boolean inParens,int start,Code exp1) throws ParseException {
		parser.begin();
		Expr exp2 = SubExpr(inParens);
		if( exp2 != null )
			return parser.success(indexVar(start,expr(exp1),exp2));
		if( parser.match('.') ) {
			Spaces(inParens);
			exp2 = NameExpr(inParens);
			if( exp2!=null )
				return parser.success(indexVar(start,expr(exp1),exp2));
		}
		return parser.failure(null);
	}

	private Var VarStart(boolean inParens) throws ParseException {
		int start = parser.begin();
		if( parser.match('(') ) {
			Spaces(true);
			Expr exp = expr(Expr(true));
			RequiredMatch(')');
			Spaces(inParens);
			return parser.success(exprVar(exp));
		}
		String name = Name(inParens);
		if( name != null )
			return parser.success(nameVar(start,name));
		return parser.failure(null);
	}

	private Expr env() {
		int index = stackIndex(_ENV);
		if( index != -1 )
			return new GetLocalVar(null,index);
		index = upValueIndex(_ENV);
		if( index != -1 )
			return new GetUpVar(null,index);
		throw new RuntimeException("_ENV not found");
	}

	private interface Var {
		public Code expr();
		public Settable settable();
	}

	private Var nameVar(final int start,final String name) {
		return new Var() {

			public Expr expr() {
				int index = stackIndex(name);
				if( index != -1 )
					return new GetLocalVar(se(start),index);
				index = upValueIndex(name);
				if( index != -1 )
					return new GetUpVar(se(start),index);
				return new IndexExpr( se(start), env(), new ConstExpr(name) );
			}

			public Settable settable() {
				int index = stackIndex(name);
				if( index != -1 )
					return new SetLocalVar(index);
				index = upValueIndex(name);
				if( index != -1 )
					return new SetUpVar(index);
				return new SetTableEntry( se(start), env(), new ConstExpr(name) );
			}
		};
	}

	private Var exprVar(final Code expr) {
		return new Var() {

			public Code expr() {
				return expr;
			}

			public Settable settable() {
				return null;
			}
		};
	}

	private Var indexVar(final int start,final Expr table,final Expr key) {
		return new Var() {

			public Expr expr() {
				return new IndexExpr( se(start), table, key );
			}

			public Settable settable() {
				return new SetTableEntry(se(start),table,key);
			}
		};
	}

	private FnCall Args(boolean inParens,int start,Expr fn,ExpList.Builder builder) throws ParseException {
		parser.begin();
		return args(inParens,builder)
			? parser.success( new FnCall( se(start), fn, builder.build() ) )
			: parser.failure((FnCall)null);
	}

	private boolean args(boolean inParens,ExpList.Builder builder) throws ParseException {
		if( parser.match('(') ) {
			Spaces(true);
			ExpList(true,builder);  // optional
			if( !parser.match(')') )
				throw parser.exception("Expression or ')' expected");
			Spaces(inParens);
			return true;
		}
		Expr exp = TableExpr(inParens);
		if( exp != null ) {
			builder.add(exp);
			return true;
		}
		String s = StringLiteral(inParens);
		if( s != null ) {
			builder.add( new ConstExpr(s) );
			return true;
		}
		Expressions exps = JspExpressions();
		if( exps != null ) {
			builder.add(exps);
			return true;
		}
		return false;
	}

	private Expressions ExpList() throws ParseException {
		return ExpList(false);
	}

	private Expressions ExpList(boolean inParens) throws ParseException {
		ExpList.Builder builder = new ExpList.Builder();
		return ExpList(inParens,builder) ? builder.build() : null;
	}

	private boolean ExpList(boolean inParens,ExpList.Builder builder) throws ParseException {
		parser.begin();
		Code exp = Expr(inParens);
		if( exp==null )
			return parser.failure();
		builder.add(exp);
		while( parser.match(',') ) {
			Spaces(inParens);
			builder.add( RequiredExpr(inParens) );
		}
		return parser.success();
	}

	private Expr SubExpr(boolean inParens) throws ParseException {
		parser.begin();
		if( !parser.match('[') )
			return parser.failure(null);
		Spaces(true);
		Expr exp = expr(RequiredExpr(true));
		RequiredMatch(']');
		Spaces(inParens);
		return parser.success(exp);
	}

	private Expr NameExpr(boolean inParens) throws ParseException {
		String name = Name(inParens);
		return name==null ? null : new ConstExpr(name);
	}

	private String RequiredName() throws ParseException {
		parser.begin();
		String name = Name();
		if( name==null )
			parser.exception("Name expected");
		return parser.success(name);
	}

	private String Name() throws ParseException {
		return Name(false);
	}

	private String Name(boolean inParens) throws ParseException {
		int start = parser.begin();
		if( !NameFirstChar() )
			return parser.failure(null);
		while( NameChar() );
		String match = parser.textFrom(start);
		if( keywords.contains(match) )
			return parser.failure(null);
		Spaces(inParens);
		return parser.success(match);
	}

	private boolean NameChar() {
		return NameFirstChar() || Digit();
	}

	private boolean NameFirstChar() {
		return parser.inCharRange('a', 'z') || parser.inCharRange('A', 'Z') || parser.match('_');
	}

	private void RequiredMatch(char c) throws ParseException {
		if( !parser.match(c) )
			throw parser.exception("'"+c+"' expected");
	}

	private void RequiredMatch(String s) throws ParseException {
		if( !parser.match(s) )
			throw parser.exception("'"+s+"' expected");
	}

	private void RequiredKeyword(String keyword) throws ParseException {
		RequiredKeyword(keyword,false);
	}

	private boolean Keyword(String keyword) throws ParseException {
		return Keyword(keyword,false);
	}

	private void RequiredKeyword(String keyword,boolean inParens) throws ParseException {
		if( !Keyword(keyword,inParens) )
			throw parser.exception("'"+keyword+"' expected");
	}

	private boolean Keyword(String keyword,boolean inParens) throws ParseException {
		parser.begin();
		if( !parser.match(keyword) || NameChar() )
			return parser.failure();
		Spaces(inParens);
		return parser.success();
	}

	private static final Set<String> keywords = new HashSet<String>(Arrays.asList(
		"and",
		"break",
		"catch",
		"do",
		"else",
		"elseif",
		"end",
		"false",
		"for",
		"function",
		"goto",
		"if",
		"import",
		"in",
		"local",
		"nil",
		"not",
		"or",
		"repeat",
		"return",
		"then",
		"true",
		"try",
		"until",
		"while"
	));

	private Expr Literal(boolean inParens) throws ParseException {
		if( NilLiteral(inParens) )
			return new ConstExpr(null);
		Boolean b = BooleanLiteral(inParens);
		if( b != null )
			return new ConstExpr(b);
		Number n = NumberLiteral(inParens);
		if( n != null )
			return new ConstExpr(n);
		String s = StringLiteral(inParens);
		if( s != null )
			return new ConstExpr(s);
		return null;
	}

	private boolean NilLiteral(boolean inParens) throws ParseException {
		return Keyword("nil",inParens);
	}

	private Boolean BooleanLiteral(boolean inParens) throws ParseException {
		if( Keyword("true",inParens) )
			return true;
		if( Keyword("false",inParens) )
			return false;
		return null;
	}

	private Number NumberLiteral(boolean inParens) throws ParseException {
		parser.begin();
		Number n;
		if( parser.matchIgnoreCase("0x") ) {
			n = HexNumber();
		} else {
			n = DecNumber();
		}
		if( n==null || NameChar() )
			return parser.failure(null);
		Spaces(inParens);
		return parser.success(n);
	}

	private Number DecNumber() {
		int start = parser.begin();
		if( Int() ) {
			if( parser.match('.') )
				Int();  // optional
		} else if( parser.match('.') && Int() ) {
			// ok
		} else
			return parser.failure(null);
		Exponent();  // optional
		return parser.success(Double.valueOf(parser.textFrom(start)));
	}

	private boolean Exponent() {
		parser.begin();
		if( !parser.matchIgnoreCase("e") )
			return parser.failure();
		parser.anyOf("+-");  // optional
		if( !Int() )
			return parser.failure();
		return parser.success();
	}

	private boolean Int() {
		if( !Digit() )
			return false;
		while( Digit() );
		return true;
	}

	private boolean Digit() {
		return parser.inCharRange('0', '9');
	}

	private Number HexNumber() {
		int start = parser.begin();
		double n;
		if( HexInt() ) {
			n = (double)Long.parseLong(parser.textFrom(start),16);
			if( parser.match('.') ) {
				start = parser.currentIndex();
				if( HexInt() ) {
					String dec = parser.textFrom(start);
					n += (double)Long.parseLong(dec,16) / Math.pow(16,dec.length());
				}
			}
		} else if( parser.match('.') && HexInt() ) {
			String dec = parser.textFrom(start+1);
			n = (double)Long.parseLong(dec,16) / Math.pow(16,dec.length());
		} else {
			return parser.failure(null);
		}
		if( parser.matchIgnoreCase("p") ) {
			parser.anyOf("+-");  // optional
			start = parser.currentIndex();
			if( !HexInt() )
				return parser.failure(null);
			n *= Math.pow(2,(double)Long.parseLong(parser.textFrom(start)));
		}
		return parser.success(Double.valueOf(n));
	}

	private boolean HexInt() {
		if( !HexDigit() )
			return false;
		while( HexDigit() );
		return true;
	}


	private boolean HexDigit() {
		return Digit() || parser.anyOf("abcdefABCDEF");
	}

	private String StringLiteral(boolean inParens) throws ParseException {
		String s;
		if( (s=QuotedString('"'))==null
			&& (s=QuotedString('\''))==null
			&& (s=LongString())==null
		)
			return null;
		Spaces(inParens);
		return s;
	}

	private String LongString() throws ParseException {
		parser.begin();
		if( !parser.match('[') )
			return parser.failure(null);
		int start = parser.currentIndex();
		while( parser.match('=') );
		int nEquals = parser.currentIndex() - start;
		if( !parser.match('[') )
			return parser.failure(null);
		EndOfLine();
		start = parser.currentIndex();
		while( !LongBracketsEnd(nEquals) ) {
			if( !parser.anyChar() )
				throw parser.exception("Unclosed long string");
		}
		String s = parser.text.substring( start, parser.currentIndex() - nEquals - 2 );
		return parser.success(s);
	}

	private String QuotedString(char quote) throws ParseException {
		parser.begin();
		if( !parser.match(quote) )
			return parser.failure(null);
		StringBuilder buf = new StringBuilder();
		while( !parser.match(quote) ) {
			Character c = EscSeq();
			if( c != null ) {
				buf.append(c);
			} else {
				if( !parser.anyChar() )
					throw parser.exception("Unclosed string");
				buf.append(parser.lastChar());
			}
		}
		return parser.success(buf.toString());
	}

	private Character EscSeq() {
		parser.begin();
		if( !parser.match('\\') )
			return parser.failure(null);
		if( parser.match('a') )  return '\u0007';
		if( parser.match('b') )  return '\b';
		if( parser.match('f') )  return '\f';
		if( parser.match('n') )  return '\n';
		if( parser.match('r') )  return '\r';
		if( parser.match('t') )  return '\t';
		if( parser.match('v') )  return '\u000b';
		if( parser.match('\\') )  return '\\';
		if( parser.match('"') )  return '"';
		if( parser.match('\'') )  return '\'';
		int start = parser.currentIndex();
		if( parser.match('x') && HexDigit() && HexDigit() )
			return (char)Integer.parseInt(parser.textFrom(start+1),16);
		if( Digit() ) {
			if( Digit() ) Digit();  // optional
			return (char)Integer.parseInt(parser.textFrom(start));
		}
		return parser.failure(null);
	}

	private void Spaces() throws ParseException {
		Spaces(false);
	}

	private void Spaces(boolean inParens) throws ParseException {
		while( parser.anyOf(" \t") || Comment() || ContinueOnNextLine() || inParens && NewLine() );
	}

	private boolean ContinueOnNextLine() {
		parser.begin();
		return parser.match('\\') &&  EndOfLine() ? parser.success() : parser.failure();
	}

	private boolean NewLine() {
		if( !EndOfLine() )
			return false;
		if( parser.match("--") ) {
			while( parser.noneOf("\r\n") );
		}
		return true;
	}

	private boolean Comment() throws ParseException {
		parser.begin();
		if( !parser.match("--[") )
			return parser.failure();
		int start = parser.currentIndex();
		while( parser.match('=') );
		int nEquals = parser.currentIndex() - start;
		if( !parser.match('[') )
			return parser.failure();
		while( !LongBracketsEnd(nEquals) ) {
			if( !parser.anyChar() )
				throw parser.exception("Unclosed comment");
		}
		return parser.success();
	}

	private boolean LongBracketsEnd(int nEquals) {
		parser.begin();
		if( !parser.match(']') )
			return parser.failure();
		while( nEquals-- > 0 ) {
			if( !parser.match('=') )
				return parser.failure();
		}
		if( !parser.match(']') )
			return parser.failure();
		return parser.success();
	}

}
