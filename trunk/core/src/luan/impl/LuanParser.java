package luan.impl;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import luan.Luan;
import luan.LuanState;
import luan.LuanSource;
import luan.modules.PackageLuan;


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

	private static class In {
		static final In NOTHING = new In(false,false);

		final boolean parens;
		final boolean template;

		private In(boolean parens,boolean template) {
			this.parens = parens;
			this.template = template;
		}

		In parens() {
			return parens ? this : new In(true,template);
		}

		In template() {
			return template ? this : new In(parens,true);
		}
	}

	private static final String _ENV = "_ENV";
	private static final UpValue.Getter[] NO_UP_VALUE_GETTERS = new UpValue.Getter[0];

	final LuanSource source;
	private Frame frame;
	private final Parser parser;
	private final boolean interactive;

	LuanParser(LuanSource source,UpValue.Getter envGetter) {
		this.source = source;
		this.frame = new Frame(envGetter);
		this.parser = new Parser(source);
		this.interactive = envGetter instanceof UpValue.ValueGetter;
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

	private static Expr expr(Expressions exprs) {
		if( exprs instanceof Expr )
			return (Expr)exprs; 
		return new ExpressionsExpr(exprs);
	}

	private FnDef newFnDef(int start,Stmt stmt) {
		return new FnDef( se(start), stmt, frame.stackSize, symbolsSize(), frame.isVarArg, frame.upValueGetters.toArray(NO_UP_VALUE_GETTERS) );
	}

	FnDef Expression() throws ParseException {
		Spaces(In.NOTHING);
		int start = parser.begin();
		Expressions expr = Expr(In.NOTHING);
		if( expr != null && parser.endOfInput() ) {
			Stmt stmt = new ReturnStmt( se(start), expr );
			return parser.success(newFnDef(start,stmt));
		}
		return parser.failure(null);
	}

	FnDef RequiredModule() throws ParseException {
		Spaces(In.NOTHING);
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
			Spaces(In.NOTHING);
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
		if( EndOfLine() )
			return parser.success();
		parser.rollback();
		Stmt stmt = TemplateStmt();
		if( stmt != null ) {
			stmts.add(stmt);
			return parser.success();
		}
		return parser.failure();
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
			|| (stmt=ForStmt()) != null
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

	private Stmt TemplateStmt() throws ParseException {
		int start = parser.currentIndex();
		Expressions exp = TemplateExpressions(In.NOTHING);
		if( exp == null )
			return null;
		Expr fnExp = (Expr)nameVar(start,"Io").expr();
		fnExp = new IndexExpr( se(start), fnExp, new ConstExpr("stdout") );
		fnExp = new IndexExpr( se(start), fnExp, new ConstExpr("write") );
		FnCall fnCall = new FnCall( se(start), fnExp, exp );
		return new ExpressionsStmt(fnCall);
	}

	private Expressions TemplateExpressions(In in) throws ParseException {
		if( in.template )
			return null;
		int start = parser.begin();
		if( !parser.match( "%>" ) )
			return parser.failure(null);
		EndOfLine();
		In inTemplate = in.template();
		List<Expressions> builder = new ArrayList<Expressions>();
		while(true) {
			if( parser.match( "<%=" ) ) {
				Spaces(inTemplate);
				builder.add( RequiredExpr(inTemplate) );
				RequiredMatch( "%>" );
			} else if( parser.match( "<%" ) ) {
				Spaces(inTemplate);
				return parser.success(ExpList.build(builder));
			} else {
				int i = parser.currentIndex();
				do {
					if( parser.match( "%>" ) )
						throw parser.exception("'%>' unexpected");
					if( !parser.anyChar() )
						throw parser.exception("Unclosed template expression");
				} while( !parser.test( "<%" ) );
				String match = parser.textFrom(i);
				builder.add( new ConstExpr(match) );
			}
		}
	}

	private Stmt ReturnStmt() throws ParseException {
		int start = parser.begin();
		if( !Keyword("return",In.NOTHING) )
			return parser.failure(null);
		Expressions exprs = ExpList(In.NOTHING);
		if( exprs==null )
			exprs = ExpList.emptyExpList;
		return parser.success( new ReturnStmt(se(start),exprs) );
	}

	private Stmt FunctionStmt() throws ParseException {
		parser.begin();
		if( !Keyword("function",In.NOTHING) )
			return parser.failure(null);

		int start = parser.currentIndex();
		Var var = nameVar(start,RequiredName(In.NOTHING));
		while( parser.match( '.' ) ) {
			Spaces(In.NOTHING);
			var = indexVar( start, expr(var.expr()), NameExpr(In.NOTHING) );
		}
		Settable fnName = var.settable();

		FnDef fnDef = RequiredFunction(In.NOTHING);
		return parser.success( new SetStmt(fnName,fnDef) );
	}

	private Stmt LocalFunctionStmt() throws ParseException {
		parser.begin();
		if( !(Keyword("local",In.NOTHING) && Keyword("function",In.NOTHING)) )
			return parser.failure(null);
		String name = RequiredName(In.NOTHING);
		addSymbol( name );
		FnDef fnDef = RequiredFunction(In.NOTHING);
		return parser.success( new SetStmt( new SetLocalVar(symbolsSize()-1), fnDef ) );
	}

	private Stmt ImportStmt() throws ParseException {
		int start = parser.begin();
		if( !Keyword("import",In.NOTHING) )
			return parser.failure(null);
		String modName = StringLiteral(In.NOTHING);
		if( modName==null )
			return parser.failure(null);
		int i = modName.lastIndexOf('/');
		if( i == -1 )
			i = modName.lastIndexOf('.');
		String varName = modName.substring(i+1);
		if( !isValidName(varName) )
			throw parser.exception("invalid variable name '"+varName+"' in import");
		LuanSource.Element se = se(start);
		FnCall require = new FnCall( se, new ConstExpr(se,PackageLuan.requireFn), new ConstExpr(modName) );
		Settable settable;
		if( interactive ) {
			settable = nameVar(se,varName).settable();
		} else {
			addSymbol( varName );
			settable = new SetLocalVar(symbolsSize()-1);
		}
		return parser.success( new SetStmt( settable, expr(require) ) );
	}

	private boolean isValidName(String s) {
		if( s.length() == 0 )
			return false;
		char c = s.charAt(0);
		if( !('a'<=c && c<='z' || 'A'<=c && c<='Z' || c=='_') )
			return false;
		for( int i=1; i<s.length() ; i++ ) {
			if( !('a'<=c && c<='z' || 'A'<=c && c<='Z' || c=='_' || '0'<=c && c<='9') )
				return false;
		}
		return true;
	}

	private Stmt BreakStmt() throws ParseException {
		parser.begin();
		if( !Keyword("break",In.NOTHING) )
			return parser.failure(null);
		if( frame.loops <= 0 )
			throw parser.exception("'break' outside of loop");
		return parser.success( new BreakStmt() );
	}

	private Stmt ForStmt() throws ParseException {
		int start = parser.begin();
		int stackStart = symbolsSize();
		if( !Keyword("for",In.NOTHING) )
			return parser.failure(null);
		List<String> names = RequiredNameList(In.NOTHING);
		if( !Keyword("in",In.NOTHING) )
			return parser.failure(null);
		Expr expr = expr(RequiredExpr(In.NOTHING));
		RequiredKeyword("do",In.NOTHING);
		addSymbols(names);
		Stmt loop = RequiredLoopBlock();
		RequiredKeyword("end",In.NOTHING);
		Stmt stmt = new ForStmt( se(start), stackStart, symbolsSize() - stackStart, expr, loop );
		popSymbols( symbolsSize() - stackStart );
		return parser.success(stmt);
	}

	private Stmt TryStmt() throws ParseException {
		parser.begin();
		if( !Keyword("try",In.NOTHING) )
			return parser.failure(null);
		Stmt tryBlock = RequiredBlock();
		RequiredKeyword("catch",In.NOTHING);
		String name = RequiredName(In.NOTHING);
		addSymbol(name);
		RequiredKeyword("do",In.NOTHING);
		Stmt catchBlock = RequiredBlock();
		RequiredKeyword("end",In.NOTHING);
		Stmt stmt = new TryStmt( tryBlock, symbolsSize()-1, catchBlock );
		popSymbols(1);
		return parser.success(stmt);
	}

	private Stmt DoStmt() throws ParseException {
		parser.begin();
		if( !Keyword("do",In.NOTHING) )
			return parser.failure(null);
		Stmt stmt = RequiredBlock();
		RequiredKeyword("end",In.NOTHING);
		return parser.success(stmt);
	}

	private boolean LocalStmt(List<Stmt> stmts) throws ParseException {
		parser.begin();
		if( !Keyword("local",In.NOTHING) )
			return parser.failure();
		List<String> names = NameList(In.NOTHING);
		if( names==null )
			return parser.failure();
		if( parser.match( '=' ) ) {
			Spaces(In.NOTHING);
			Expressions values = ExpList(In.NOTHING);
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

	private List<String> RequiredNameList(In in) throws ParseException {
		parser.begin();
		List<String> names = NameList(in);
		if( names==null )
			throw parser.exception("Name expected");
		return parser.success(names);
	}

	private List<String> NameList(In in) throws ParseException {
		String name = Name(in);
		if( name==null )
			return null;
		List<String> names = new ArrayList<String>();
		names.add(name);
		while( (name=anotherName(in)) != null ) {
			names.add(name);
		}
		return names;
	}

	private String anotherName(In in) throws ParseException {
		parser.begin();
		if( !parser.match( ',' ) )
			return parser.failure(null);
		Spaces(in);
		String name = Name(in);
		if( name==null )
			return parser.failure(null);
		return parser.success(name);
	}

	private Stmt WhileStmt() throws ParseException {
		int start = parser.begin();
		if( !Keyword("while",In.NOTHING) )
			return parser.failure(null);
		Expr cnd = expr(RequiredExpr(In.NOTHING));
		RequiredKeyword("do",In.NOTHING);
		Stmt loop = RequiredLoopBlock();
		RequiredKeyword("end",In.NOTHING);
		return parser.success( new WhileStmt(se(start),cnd,loop) );
	}

	private Stmt RepeatStmt() throws ParseException {
		int start = parser.begin();
		if( !Keyword("repeat",In.NOTHING) )
			return parser.failure(null);
		Stmt loop = RequiredLoopBlock();
		RequiredKeyword("until",In.NOTHING);
		Expr cnd = expr(RequiredExpr(In.NOTHING));
		return parser.success( new RepeatStmt(se(start),loop,cnd) );
	}

	private Stmt RequiredLoopBlock() throws ParseException {
		incLoops();
		Stmt stmt = RequiredBlock();
		decLoops();
		return stmt;
	}

	private Stmt IfStmt() throws ParseException {
		parser.begin();
		if( !Keyword("if",In.NOTHING) )
			return parser.failure(null);
		return parser.success( IfStmt2() );
	}

	private Stmt IfStmt2() throws ParseException {
		int start = parser.currentIndex();
		Expr cnd = expr(RequiredExpr(In.NOTHING));
		RequiredKeyword("then",In.NOTHING);
		Stmt thenBlock = RequiredBlock();
		Stmt elseBlock;
		if( Keyword("elseif",In.NOTHING) ) {
			elseBlock = IfStmt2();
		} else {
			elseBlock = Keyword("else",In.NOTHING) ? RequiredBlock() : Stmt.EMPTY;
			RequiredKeyword("end",In.NOTHING);
		}
		return new IfStmt(se(start),cnd,thenBlock,elseBlock);
	}

	private Stmt SetStmt() throws ParseException {
		parser.begin();
		List<Settable> vars = new ArrayList<Settable>();
		Settable s = SettableVar();
		if( s == null )
			return parser.failure(null);
		vars.add(s);
		while( parser.match( ',' ) ) {
			Spaces(In.NOTHING);
			s = SettableVar();
			if( s == null )
				return parser.failure(null);
			vars.add(s);
		}
		if( !parser.match( '=' ) )
			return parser.failure(null);
		Spaces(In.NOTHING);
		Expressions values = ExpList(In.NOTHING);
		if( values==null )
			throw parser.exception("Expressions expected");
		return parser.success( new SetStmt( vars.toArray(new Settable[0]), values ) );
	}

	private Stmt ExpressionsStmt() throws ParseException {
		parser.begin();
		Expressions exp = Expr(In.NOTHING);
		if( exp instanceof FnCall || exp instanceof AndExpr || exp instanceof OrExpr )
			return parser.success( new ExpressionsStmt(exp) );
		return parser.failure(null);
	}

	private Settable SettableVar() throws ParseException {
		int start = parser.begin();
		Var var = VarZ(In.NOTHING);
		if( var==null )
			return parser.failure(null);
		return parser.success( var.settable() );
	}

	private Expressions RequiredExpr(In in) throws ParseException {
		parser.begin();
		return parser.success(required(Expr(in),"Bad expression"));
	}

	private Expressions Expr(In in) throws ParseException {
		parser.begin();
		Expressions exp;
		return (exp = VarArgs(in)) != null
			|| (exp = OrExpr(in)) != null
			? parser.success(exp)
			: parser.failure((Expressions)null)
		;
	}

	private Expressions OrExpr(In in) throws ParseException {
		int start = parser.begin();
		Expressions exp = AndExpr(in);
		if( exp==null )
			return parser.failure(null);
		while( Keyword("or",in) ) {
			exp = new OrExpr( se(start), expr(exp), required(expr(AndExpr(in))) );
		}
		return parser.success(exp);
	}

	private Expressions AndExpr(In in) throws ParseException {
		int start = parser.begin();
		Expressions exp = RelExpr(in);
		if( exp==null )
			return parser.failure(null);
		while( Keyword("and",in) ) {
			exp = new AndExpr( se(start), expr(exp), required(expr(RelExpr(in))) );
		}
		return parser.success(exp);
	}

	private Expressions RelExpr(In in) throws ParseException {
		int start = parser.begin();
		Expressions exp = ConcatExpr(in);
		if( exp==null )
			return parser.failure(null);
		while(true) {
			if( parser.match("==") ) {
				Spaces(in);
				exp = new EqExpr( se(start), expr(exp), required(expr(ConcatExpr(in))) );
			} else if( parser.match("~=") ) {
				Spaces(in);
				exp = new NotExpr( se(start), new EqExpr( se(start), expr(exp), required(expr(ConcatExpr(in))) ) );
			} else if( parser.match("<=") ) {
				Spaces(in);
				exp = new LeExpr( se(start), expr(exp), required(expr(ConcatExpr(in))) );
			} else if( parser.match(">=") ) {
				Spaces(in);
				exp = new LeExpr( se(start), required(expr(ConcatExpr(in))), expr(exp) );
			} else if( parser.match("<") ) {
				Spaces(in);
				exp = new LtExpr( se(start), expr(exp), required(expr(ConcatExpr(in))) );
			} else if( parser.match(">") ) {
				Spaces(in);
				exp = new LtExpr( se(start), required(expr(ConcatExpr(in))), expr(exp) );
			} else
				break;
		}
		return parser.success(exp);
	}

	private Expressions ConcatExpr(In in) throws ParseException {
		int start = parser.begin();
		Expressions exp = SumExpr(in);
		if( exp==null )
			return parser.failure(null);
		if( parser.match("..") ) {
			Spaces(in);
			exp = new ConcatExpr( se(start), expr(exp), required(expr(ConcatExpr(in))) );
		}
		return parser.success(exp);
	}

	private Expressions SumExpr(In in) throws ParseException {
		int start = parser.begin();
		Expressions exp = TermExpr(in);
		if( exp==null )
			return parser.failure(null);
		while(true) {
			if( parser.match('+') ) {
				Spaces(in);
				exp = new AddExpr( se(start), expr(exp), required(expr(TermExpr(in))) );
			} else if( Minus() ) {
				Spaces(in);
				exp = new SubExpr( se(start), expr(exp), required(expr(TermExpr(in))) );
			} else
				break;
		}
		return parser.success(exp);
	}

	private boolean Minus() {
		parser.begin();
		return parser.match('-') && !parser.match('-') ? parser.success() : parser.failure();
	}

	private Expressions TermExpr(In in) throws ParseException {
		int start = parser.begin();
		Expressions exp = UnaryExpr(in);
		if( exp==null )
			return parser.failure(null);
		while(true) {
			if( parser.match('*') ) {
				Spaces(in);
				exp = new MulExpr( se(start), expr(exp), required(expr(UnaryExpr(in))) );
			} else if( parser.match('/') ) {
				Spaces(in);
				exp = new DivExpr( se(start), expr(exp), required(expr(UnaryExpr(in))) );
			} else if( Mod() ) {
				Spaces(in);
				exp = new ModExpr( se(start), expr(exp), required(expr(UnaryExpr(in))) );
			} else
				break;
		}
		return parser.success(exp);
	}

	private boolean Mod() {
		parser.begin();
		return parser.match('%') && !parser.match('>') ? parser.success() : parser.failure();
	}

	private Expressions UnaryExpr(In in) throws ParseException {
		int start = parser.begin();
		if( parser.match('#') ) {
			Spaces(in);
			return parser.success( new LenExpr( se(start), required(expr(UnaryExpr(in))) ) );
		}
		if( Minus() ) {
			Spaces(in);
			return parser.success( new UnmExpr( se(start), required(expr(UnaryExpr(in))) ) );
		}
		if( Keyword("not",in) ) {
			Spaces(in);
			return parser.success( new NotExpr( se(start), required(expr(UnaryExpr(in))) ) );
		}
		Expressions exp = PowExpr(in);
		if( exp==null )
			return parser.failure(null);
		return parser.success(exp);
	}

	private Expressions PowExpr(In in) throws ParseException {
		int start = parser.begin();
		Expressions exp = SingleExpr(in);
		if( exp==null )
			return parser.failure(null);
		if( parser.match('^') ) {
			Spaces(in);
			exp = new ConcatExpr( se(start), expr(exp), required(expr(PowExpr(in))) );
		}
		return parser.success(exp);
	}

	private Expressions SingleExpr(In in) throws ParseException {
		parser.begin();
		Expressions exp;
		exp = FunctionExpr(in);
		if( exp != null )
			return parser.success(exp);
		exp = TableExpr(in);
		if( exp != null )
			return parser.success(exp);
		exp = VarExp(in);
		if( exp != null )
			return parser.success(exp);
		exp = Literal(in);
		if( exp != null )
			return parser.success(exp);
		return parser.failure(null);
	}

	private Expr FunctionExpr(In in) throws ParseException {
		if( !Keyword("function",in) )
			return null;
		return RequiredFunction(in);
	}

	private FnDef RequiredFunction(In in) throws ParseException {
		int start = parser.begin();
		RequiredMatch('(');
		In inParens = in.parens();
		Spaces(inParens);
		frame = new Frame(frame);
		List<String> names = NameList(in);
		if( names != null ) {
			addSymbols(names);
			if( parser.match(',') ) {
				Spaces(inParens);
				if( !parser.match("...") )
					throw parser.exception();
				frame.isVarArg = true;
			}
		} else if( parser.match("...") ) {
			Spaces(inParens);
			frame.isVarArg = true;
		}
		RequiredMatch(')');
		Spaces(in);
		Stmt block = RequiredBlock();
		RequiredKeyword("end",in);
		FnDef fnDef = newFnDef(start,block);
		frame = frame.parent;
		return parser.success(fnDef);
	}

	private VarArgs VarArgs(In in) throws ParseException {
		int start = parser.begin();
		if( !frame.isVarArg || !parser.match("...") )
			return parser.failure(null);
		Spaces(in);
		return parser.success( new VarArgs(se(start)) );
	}

	private Expr TableExpr(In in) throws ParseException {
		int start = parser.begin();
		if( !parser.match('{') )
			return parser.failure(null);
		In inParens = in.parens();
		Spaces(inParens);
		List<TableExpr.Field> fields = new ArrayList<TableExpr.Field>();
		List<Expressions> builder = new ArrayList<Expressions>();
		while( Field(fields,builder,in) && FieldSep(inParens) );
		Spaces(inParens);
		if( !parser.match('}') )
			throw parser.exception("Expected table element or '}'");
		return parser.success( new TableExpr( se(start), fields.toArray(new TableExpr.Field[0]), ExpList.build(builder) ) );
	}

	private boolean FieldSep(In in) throws ParseException {
		if( !parser.anyOf(",;") )
			return false;
		Spaces(in);
		return true;
	}

	private boolean Field(List<TableExpr.Field> fields,List<Expressions> builder,In in) throws ParseException {
		parser.begin();
		Expr exp = SubExpr(in);
		if( exp==null )
			exp = NameExpr(in);
		if( exp!=null && parser.match('=') ) {
			Spaces(in);
			fields.add( new TableExpr.Field( exp, required(expr(Expr(in))) ) );
			return parser.success();
		}
		parser.rollback();
		Expressions exprs = Expr(in);
		if( exprs != null ) {
			builder.add(exprs);
			return parser.success();
		}
		return parser.failure();
	}

	private Expressions VarExp(In in) throws ParseException {
		Var var = VarZ(in);
		return var==null ? null : var.expr();
	}

	private Var VarZ(In in) throws ParseException {
		int start = parser.begin();
		Var var = VarStart(in);
		if( var==null )
			return parser.failure(null);
		Var var2;
		while( (var2=Var2(in,start,var.expr())) != null ) {
			var = var2;
		}
		return parser.success(var);
	}

	private Var Var2(In in,int start,Expressions exp1) throws ParseException {
		parser.begin();
		Var var = VarExt(in,start,exp1);
		if( var != null )
			return parser.success(var);
		if( parser.match("->") ) {
			Spaces(in);
			List<Expressions> builder = new ArrayList<Expressions>();
			builder.add(exp1);
			Expr exp2 = expr(RequiredVarExpB(in));
			FnCall fnCall = required(Args( in, start, exp2, builder ));
			return parser.success(exprVar(fnCall));
		}
		FnCall fnCall = Args( in, start, expr(exp1), new ArrayList<Expressions>() );
		if( fnCall != null )
			return parser.success(exprVar(fnCall));
		return parser.failure(null);
	}

	private Expressions RequiredVarExpB(In in) throws ParseException {
		int start = parser.begin();
		Var var = required(VarStart(in));
		Var var2;
		while( (var2=VarExt(in,start,var.expr())) != null ) {
			var = var2;
		}
		return parser.success(var.expr());
	}

	private Var VarExt(In in,int start,Expressions exp1) throws ParseException {
		parser.begin();
		Expr exp2 = SubExpr(in);
		if( exp2 != null )
			return parser.success(indexVar(start,expr(exp1),exp2));
		if( parser.match('.') ) {
			Spaces(in);
			exp2 = NameExpr(in);
			if( exp2!=null )
				return parser.success(indexVar(start,expr(exp1),exp2));
		}
		return parser.failure(null);
	}

	private Var VarStart(In in) throws ParseException {
		int start = parser.begin();
		if( parser.match('(') ) {
			In inParens = in.parens();
			Spaces(inParens);
			Expr exp = expr(RequiredExpr(inParens));
			RequiredMatch(')');
			Spaces(in);
			return parser.success(exprVar(exp));
		}
		String name = Name(in);
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
		public Expressions expr();
		public Settable settable();
	}

	private Var nameVar(final int start,final String name) {
		return nameVar(se(start),name);
	}

	private Var nameVar(final LuanSource.Element se,final String name) {
		return new Var() {

			public Expr expr() {
				int index = stackIndex(name);
				if( index != -1 )
					return new GetLocalVar(se,index);
				index = upValueIndex(name);
				if( index != -1 )
					return new GetUpVar(se,index);
				return new IndexExpr( se, env(), new ConstExpr(name) );
			}

			public Settable settable() {
				int index = stackIndex(name);
				if( index != -1 )
					return new SetLocalVar(index);
				index = upValueIndex(name);
				if( index != -1 )
					return new SetUpVar(index);
				return new SetTableEntry( se, env(), new ConstExpr(name) );
			}
		};
	}

	private Var exprVar(final Expressions expr) {
		return new Var() {

			public Expressions expr() {
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

	private FnCall Args(In in,int start,Expr fn,List<Expressions> builder) throws ParseException {
		parser.begin();
		return args(in,builder)
			? parser.success( new FnCall( se(start), fn, ExpList.build(builder) ) )
			: parser.failure((FnCall)null);
	}

	private boolean args(In in,List<Expressions> builder) throws ParseException {
		if( parser.match('(') ) {
			In inParens = in.parens();
			Spaces(inParens);
			ExpList(inParens,builder);  // optional
			if( !parser.match(')') )
				throw parser.exception("Expression or ')' expected");
			Spaces(in);
			return true;
		}
		Expr exp = TableExpr(in);
		if( exp != null ) {
			builder.add(exp);
			return true;
		}
		String s = StringLiteral(in);
		if( s != null ) {
			builder.add( new ConstExpr(s) );
			return true;
		}
/*
		Expressions exps = TemplateExpressions(in);
		if( exps != null ) {
			builder.add(exps);
			return true;
		}
*/
		return false;
	}

	private Expressions ExpList(In in) throws ParseException {
		List<Expressions> builder = new ArrayList<Expressions>();
		return ExpList(in,builder) ? ExpList.build(builder) : null;
	}

	private boolean ExpList(In in,List<Expressions> builder) throws ParseException {
		parser.begin();
		Expressions exp = TemplateExpressions(in);
		if( exp != null ) {
			builder.add(exp);
			return parser.success();
		}
		exp = Expr(in);
		if( exp==null )
			return parser.failure();
		builder.add(exp);
		while( parser.match(',') ) {
			Spaces(in);
			exp = TemplateExpressions(in);
			if( exp != null ) {
				builder.add(exp);
				return parser.success();
			}
			builder.add( RequiredExpr(in) );
		}
		return parser.success();
	}

	private Expr SubExpr(In in) throws ParseException {
		parser.begin();
		if( !parser.match('[') )
			return parser.failure(null);
		In inParens = in.parens();
		Spaces(inParens);
		Expr exp = expr(RequiredExpr(inParens));
		RequiredMatch(']');
		Spaces(in);
		return parser.success(exp);
	}

	private Expr NameExpr(In in) throws ParseException {
		String name = Name(in);
		return name==null ? null : new ConstExpr(name);
	}

	private String RequiredName(In in) throws ParseException {
		parser.begin();
		String name = Name(in);
		if( name==null )
			throw parser.exception("Name expected");
		return parser.success(name);
	}

	private String Name(In in) throws ParseException {
		int start = parser.begin();
		if( !NameFirstChar() )
			return parser.failure(null);
		while( NameChar() );
		String match = parser.textFrom(start);
		if( keywords.contains(match) )
			return parser.failure(null);
		Spaces(in);
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

	private void RequiredKeyword(String keyword,In in) throws ParseException {
		if( !Keyword(keyword,in) )
			throw parser.exception("'"+keyword+"' expected");
	}

	private boolean Keyword(String keyword,In in) throws ParseException {
		parser.begin();
		if( !parser.match(keyword) || NameChar() )
			return parser.failure();
		Spaces(in);
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

	private Expr Literal(In in) throws ParseException {
		if( NilLiteral(in) )
			return new ConstExpr(null);
		Boolean b = BooleanLiteral(in);
		if( b != null )
			return new ConstExpr(b);
		Number n = NumberLiteral(in);
		if( n != null )
			return new ConstExpr(n);
		String s = StringLiteral(in);
		if( s != null )
			return new ConstExpr(s);
		return null;
	}

	private boolean NilLiteral(In in) throws ParseException {
		return Keyword("nil",in);
	}

	private Boolean BooleanLiteral(In in) throws ParseException {
		if( Keyword("true",in) )
			return true;
		if( Keyword("false",in) )
			return false;
		return null;
	}

	private Number NumberLiteral(In in) throws ParseException {
		parser.begin();
		Number n;
		if( parser.matchIgnoreCase("0x") ) {
			n = HexNumber();
		} else {
			n = DecNumber();
		}
		if( n==null || NameChar() )
			return parser.failure(null);
		Spaces(in);
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

	private String StringLiteral(In in) throws ParseException {
		String s;
		if( (s=QuotedString('"'))==null
			&& (s=QuotedString('\''))==null
			&& (s=LongString())==null
		)
			return null;
		Spaces(in);
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
		if( parser.match('a') )  return parser.success('\u0007');
		if( parser.match('b') )  return parser.success('\b');
		if( parser.match('f') )  return parser.success('\f');
		if( parser.match('n') )  return parser.success('\n');
		if( parser.match('r') )  return parser.success('\r');
		if( parser.match('t') )  return parser.success('\t');
		if( parser.match('v') )  return parser.success('\u000b');
		if( parser.match('\\') )  return parser.success('\\');
		if( parser.match('"') )  return parser.success('"');
		if( parser.match('\'') )  return parser.success('\'');
		int start = parser.currentIndex();
		if( parser.match('x') && HexDigit() && HexDigit() )
			return parser.success((char)Integer.parseInt(parser.textFrom(start+1),16));
		if( Digit() ) {
			if( Digit() ) Digit();  // optional
			return parser.success((char)Integer.parseInt(parser.textFrom(start)));
		}
		return parser.failure(null);
	}

	private void Spaces(In in) throws ParseException {
		while( parser.anyOf(" \t") || Comment() || ContinueOnNextLine() || in.parens && NewLine() );
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
