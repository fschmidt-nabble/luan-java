package luan.interp;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.Var;
import org.parboiled.support.StringVar;
import org.parboiled.support.StringBuilderVar;
import org.parboiled.support.ValueStack;
import org.parboiled.errors.ErrorUtils;
import luan.Lua;
import luan.LuaNumber;
import luan.LuaState;


public class LuaParser extends BaseParser<Object> {

	static final class Frame {
		final Frame parent;
		final List<String> symbols = new ArrayList<String>();
		int stackSize = 0;
		int loops = 0;
		boolean isVarArg = false;

		Frame(Frame parent) {
			this.parent = parent;
		}
	}

	int nEquals;
	int parens = 0;
	Frame frame = new Frame(null);

	boolean nEquals(int n) {
		nEquals = n;
		return true;
	}

	boolean incParens() {
		parens++;
		return true;
	}

	boolean decParens() {
		parens--;
		return true;
	}

	List<String> symbols() {
		return frame.symbols;
	}

	int symbolsSize() {
		return frame.symbols.size();
	}

	boolean addSymbol(String name) {
		frame.symbols.add(name);
		if( frame.stackSize < symbolsSize() )
			frame.stackSize = symbolsSize();
		return true;
	}

	int index(String name) {
		List<String> symbols = frame.symbols;
		int i = symbols.size();
		while( --i >= 0 ) {
			if( symbols.get(i).equals(name) )
				return i;
		}
		return -1;
	}

	boolean popSymbols(int n) {
		List<String> symbols = frame.symbols;
		while( n-- > 0 ) {
			symbols.remove(symbols.size()-1);
		}
		return true;
	}

	boolean incLoops() {
		frame.loops++;
		return true;
	}

	boolean decLoops() {
		frame.loops--;
		return true;
	}


	public Rule Target() {
		return Sequence(
			Spaces(),
			FirstOf(
				Sequence( ExpList(), EOI ),
				Sequence(
					Block(),
					EOI,
					push( new Chunk( (Stmt)pop(), frame.stackSize, 0, false ) )
				)
			)
		);
	}

	Rule Block() {
		Var<List<Stmt>> stmts = new Var<List<Stmt>>(new ArrayList<Stmt>());
		Var<Integer> stackStart = new Var<Integer>(symbolsSize());
		return Sequence(
			Optional( Stmt(stmts) ),
			ZeroOrMore(
				StmtSep(),
				Optional( Stmt(stmts) )
			),
			push( newBlock(stmts.get(),stackStart.get()) )
		);
	}

	Stmt newBlock(List<Stmt> stmts,int stackStart) {
		int stackEnd = symbolsSize();
		popSymbols( stackEnd - stackStart );
		if( stmts.isEmpty() )
			return Stmt.EMPTY;
		if( stmts.size()==1 && stackStart==stackEnd )
			return stmts.get(0);
		return new Block( stmts.toArray(new Stmt[0]), stackStart, stackEnd );
	}

	Rule StmtSep() {
		return Sequence(
			FirstOf(
				';',
				Sequence(
					Optional( "--", ZeroOrMore(NoneOf("\r\n")) ),
					EndOfLine()
				)
			),
			Spaces()
		);
	}

	Rule EndOfLine() {
		return FirstOf("\r\n", '\r', '\n');
	}

	Rule Stmt(Var<List<Stmt>> stmts) {
		return FirstOf(
			LocalStmt(stmts),
			Sequence(
				FirstOf(
					ReturnStmt(),
					FunctionStmt(),
					LocalFunctionStmt(),
					BreakStmt(),
					GenericForStmt(),
					NumericForStmt(),
					DoStmt(),
					WhileStmt(),
					RepeatStmt(),
					IfStmt(),
					SetStmt(),
					ExpressionsStmt()
				),
				stmts.get().add( (Stmt)pop() )
			)
		);
	}

	Rule ReturnStmt() {
		return Sequence(
			Keyword("return"), Expressions(),
			push( new ReturnStmt( (Expressions)pop() ) )
		);
	}

	Rule FunctionStmt() {
		return Sequence(
			Keyword("function"), FnName(), Function(),
			push( new SetStmt( (Settable)pop(1), expr(pop()) ) )
		);
	}

	Rule FnName() {
		return Sequence(
			push(null),  // marker
			Name(),
			ZeroOrMore(
				'.', Spaces(),
				makeVarExp(),
				NameExpr()
			),
			makeSettableVar()
		);
	}

	Rule LocalFunctionStmt() {
		return Sequence(
			Keyword("local"), Keyword("function"), LocalName(), Function(),
			push( new SetStmt( new SetLocalVar(symbolsSize()-1), expr(pop()) ) )
		);
	}

	Rule BreakStmt() {
		return Sequence(
			Keyword("break"),
			frame.loops > 0,
			push( new BreakStmt() )
		);
	}

	Rule GenericForStmt() {
		Var<Integer> stackStart = new Var<Integer>(symbolsSize());
		return Sequence(
			Keyword("for"), NameList(), Keyword("in"), Expr(), Keyword("do"), LoopBlock(), Keyword("end"),
			push( new GenericForStmt( stackStart.get(), symbolsSize() - stackStart.get(), expr(pop(1)), (Stmt)pop() ) ),
			popSymbols( symbolsSize() - stackStart.get() )
		);
	}

	Rule NumericForStmt() {
		return Sequence(
			Keyword("for"), Name(), '=', Spaces(), Expr(), ',', Spaces(), Expr(),
			push( new ConstExpr(new LuaNumber(1)) ),  // default step
			Optional(
				',', Spaces(),
				drop(),
				Expr()
			),
			addSymbol( (String)pop(3) ),  // add "for" var to symbols
			Keyword("do"), LoopBlock(), Keyword("end"),
			push( new NumericForStmt( symbolsSize()-1, expr(pop(3)), expr(pop(2)), expr(pop(1)), (Stmt)pop() ) ),
			popSymbols(1)
		);
	}

	Rule DoStmt() {
		return Sequence(
			Keyword("do"), Block(), Keyword("end")
		);
	}

	Rule LocalStmt(Var<List<Stmt>> stmts) {
		Var<Integer> stackStart = new Var<Integer>(symbolsSize());
		return Sequence(
			Keyword("local"), NameList(),
			Optional(
				'=', Spaces(), ExpList(),
				stmts.get().add( newSetLocalStmt(stackStart.get()) )
			)
		);
	}

	Rule NameList() {
		return Sequence(
			LocalName(),
			ZeroOrMore(
				',', Spaces(), LocalName()
			)
		);
	}

	Rule LocalName() {
		return Sequence(
			Name(),
			addSymbol( (String)pop() )
		);
	}

	SetStmt newSetLocalStmt(int stackStart) {
		Expressions values = (Expressions)pop();
		SetLocalVar[] vars = new SetLocalVar[symbolsSize()-stackStart];
		for( int i=0; i<vars.length; i++ ) {
			vars[i] = new SetLocalVar(stackStart+i);
		}
		return new SetStmt( vars, values );
	}

	Rule WhileStmt() {
		return Sequence(
			Keyword("while"), Expr(), Keyword("do"), LoopBlock(), Keyword("end"),
			push( new WhileStmt( expr(pop(1)), (Stmt)pop() ) )
		);
	}

	Rule RepeatStmt() {
		return Sequence(
			Keyword("repeat"), LoopBlock(), Keyword("until"), Expr(),
			push( new RepeatStmt( (Stmt)pop(1), expr(pop()) ) )
		);
	}

	Rule LoopBlock() {
		return Sequence( incLoops(), Block(), decLoops() );
	}

	Rule IfStmt() {
		Var<Integer> n = new Var<Integer>(1);
		return Sequence(
			Keyword("if"), Expr(), Keyword("then"), Block(),
			push(Stmt.EMPTY),
			ZeroOrMore(
				Keyword("elseif"), drop(), Expr(), Keyword("then"), Block(),
				push(Stmt.EMPTY),
				n.set(n.get()+1)
			),
			Optional(
				Keyword("else"), drop(), Block()
			),
			Keyword("end"),
			buildIfStmt(n.get())
		);
	}

	boolean buildIfStmt(int n) {
		while( n-- > 0 ) {
			Stmt elseStmt = (Stmt)pop();
			Stmt thenStmt = (Stmt)pop();
			Expr cnd = expr(pop());
			push( new IfStmt(cnd,thenStmt,elseStmt) );
		}
		return true;
	}

	Rule SetStmt() {
		return Sequence(
			VarList(),
			'=', Spaces(),
			ExpList(),
			push( newSetStmt() )
		);
	}

	Rule ExpressionsStmt() {
		return Sequence(
			ExpList(),
			push( new ExpressionsStmt((Expressions)pop()) )
		);
	}

	SetStmt newSetStmt() {
		Expressions values = (Expressions)pop();
		@SuppressWarnings("unchecked")
		List<Settable> vars = (List<Settable>)pop();
		return new SetStmt( vars.toArray(new Settable[0]), values );
	}

	Rule VarList() {
		Var<List<Settable>> vars = new Var<List<Settable>>(new ArrayList<Settable>());
		return Sequence(
			SettableVar(),
			vars.get().add( (Settable)pop() ),
			ZeroOrMore(
				',', Spaces(), SettableVar(),
				vars.get().add( (Settable)pop() )
			),
			push(vars.get())
		);
	}

	Rule SettableVar() {
		return Sequence( Var(), makeSettableVar() );
	}

	boolean makeSettableVar() {
		Object obj2 = pop();
		if( obj2==null )
			return false;
		Object obj1 = pop();
		if( obj1==null ) {
			String name = (String)obj2;
			int index = index(name);
			if( index == -1 ) {
				push( new SetTableEntry( EnvExpr.INSTANCE, new ConstExpr(name) ) );
			} else {
				push( new SetLocalVar(index) );
			}
		} else {
			Expr key = expr(obj2);
			Expr table = expr(obj1);
			push( new SetTableEntry(table,key) );
		}
		return true;
	}

	Rule Expr() {
		return FirstOf(
			VarArgs(),
			OrExpr()
		);
	}

	Rule OrExpr() {
		return Sequence(
			AndExpr(),
			ZeroOrMore( "or", Spaces(), AndExpr(), push( new OrExpr(expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule AndExpr() {
		return Sequence(
			RelExpr(),
			ZeroOrMore( "and", Spaces(), RelExpr(), push( new AndExpr(expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule RelExpr() {
		return Sequence(
			ConcatExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( "==", Spaces(), ConcatExpr(), push( new EqExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( "~=", Spaces(), ConcatExpr(), push( new NotExpr(new EqExpr(expr(pop(1)),expr(pop()))) ) ),
					Sequence( "<=", Spaces(), ConcatExpr(), push( new LeExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( ">=", Spaces(), ConcatExpr(), push( new LeExpr(expr(pop()),expr(pop())) ) ),
					Sequence( "<", Spaces(), ConcatExpr(), push( new LtExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( ">", Spaces(), ConcatExpr(), push( new LtExpr(expr(pop()),expr(pop())) ) )
				)
			)
		);
	}

	Rule ConcatExpr() {
		return Sequence(
			SumExpr(),
			Optional( "..", Spaces(), ConcatExpr(), push( new ConcatExpr(expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule SumExpr() {
		return Sequence(
			TermExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( '+', Spaces(), TermExpr(), push( new AddExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( '-', TestNot('-'), Spaces(), TermExpr(), push( new SubExpr(expr(pop(1)),expr(pop())) ) )
				)
			)
		);
	}

	Rule TermExpr() {
		return Sequence(
			UnaryExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( '*', Spaces(), UnaryExpr(), push( new MulExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( '/', Spaces(), UnaryExpr(), push( new DivExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( '%', Spaces(), UnaryExpr(), push( new ModExpr(expr(pop(1)),expr(pop())) ) )
				)
			)
		);
	}

	Rule UnaryExpr() {
		return FirstOf(
			Sequence( '#', Spaces(), PowExpr(), push( new LenExpr(expr(pop())) ) ),
			Sequence( '-', TestNot('-'), Spaces(), PowExpr(), push( new UnmExpr(expr(pop())) ) ),
			Sequence( "not", Spaces(), PowExpr(), push( new NotExpr(expr(pop())) ) ),
			PowExpr()
		);
	}

	Rule PowExpr() {
		return Sequence(
			SingleExpr(),
			Optional( '^', Spaces(), PowExpr(), push( new PowExpr(expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule SingleExpr() {
		return FirstOf(
			FunctionExpr(),
			TableExpr(),
			VarExp(),
			LiteralExpr()
		);
	}

	Rule FunctionExpr() {
		return Sequence( "function", Spaces(), Function() );
	}

	Rule Function() {
		return Sequence(
			action( frame = new Frame(frame) ),
			'(', incParens(), Spaces(),
			Optional(
				FirstOf(
					Sequence( NameList(), Optional( ',', Spaces(), VarArgName() ) ),
					VarArgName()
				)
			),
			')', decParens(), Spaces(), Block(), Keyword("end"),
			push( new Chunk( (Stmt)pop(), frame.stackSize, symbolsSize(), frame.isVarArg ) ),
			action( frame = frame.parent )
		);
	}

	Rule VarArgName() {
		return Sequence(
			"...", Spaces(),
			action( frame.isVarArg = true )
		);
	}

	Rule VarArgs() {
		return Sequence(
			"...", Spaces(),
			frame.isVarArg,
			push( VarArgs.INSTANCE )
		);
	}

	Rule TableExpr() {
		return Sequence(
			'{', incParens(), Spaces(),
			push( new ArrayList<TableExpr.Field>() ),
			push( 1.0 ),  // counter
			Optional(
				Field(),
				ZeroOrMore(
					FieldSep(),
					Field()
				),
				Optional( FieldSep() )
			),
			'}', decParens(),
			push( newTableExpr() ),
			Spaces()
		);
	}

	TableExpr newTableExpr() {
		pop();  // counter
		@SuppressWarnings("unchecked")
		List<TableExpr.Field> list = (List<TableExpr.Field>)pop();
		return new TableExpr(list.toArray(new TableExpr.Field[0]));
	}

	Rule FieldSep() {
		return Sequence( AnyOf(",;"), Spaces() );
	}

	Rule Field() {
		return FirstOf(
			Sequence(
				FirstOf( SubExpr(), NameExpr() ),
				'=', Spaces(), Expr(),
				addField()
			),
			Sequence(
				Expr(),
				addIndexedField()
			)
		);
	}

	boolean addField() {
		TableExpr.Field field = new TableExpr.Field( expr(pop(1)), expr(pop()) );
		@SuppressWarnings("unchecked")
		List<TableExpr.Field> list = (List<TableExpr.Field>)peek(1);
		list.add(field);
		return true;
	}

	boolean addIndexedField() {
		Expr val = expr(pop());
		double i = (Double)pop();
		TableExpr.Field field = new TableExpr.Field( new ConstExpr(new LuaNumber(i)), val );
		push( i + 1 );
		@SuppressWarnings("unchecked")
		List<TableExpr.Field> list = (List<TableExpr.Field>)peek(1);
		list.add(field);
		return true;
	}

	static Expr expr(Object obj) {
		if( obj instanceof Expressions )
			return new ExpressionsExpr((Expressions)obj);
		return (Expr)obj;
	}

	Rule VarExp() {
		return Sequence(
			Var(),
			makeVarExp()
		);
	}

	Rule Var() {
		return Sequence(
			FirstOf(
				Sequence(
					'(', incParens(), Spaces(), Expr(), ')', decParens(), Spaces(),
					push(expr(pop())),
					push(null)  // marker
				),
				Sequence(
					push(null),  // marker
					Name()
				)
			),
			ZeroOrMore(
				makeVarExp(),
				FirstOf(
					SubExpr(),
					Sequence( '.', Spaces(), NameExpr() ),
					Sequence(
						Args(),
						push(null)  // marker
					)
				)
			)
		);
	}

	boolean makeVarExp() {
		Object obj2 = pop();
		if( obj2==null )
			return true;
		Object obj1 = pop();
		if( obj1==null ) {
			String name = (String)obj2;
			int index = index(name);
			if( index == -1 ) {
				return push( new GetExpr( EnvExpr.INSTANCE, new ConstExpr(name) ) );
			} else {
				return push( new GetLocalVar(index) );
			}
		}
		return push( new GetExpr( expr(obj1), expr(obj2) ) );
	}

	// function should be on top of the stack
	Rule Args() {
		return Sequence(
			FirstOf(
				Sequence(
					'(', incParens(), Spaces(), Expressions(), ')', decParens(), Spaces()
				),
				Sequence(
					TableExpr(),
					push( new ExpList.SingleExpList(expr(pop())) )
				),
				Sequence(
					StringLiteral(), Spaces(),
					push( new ExpList.SingleExpList(new ConstExpr(pop())) )
				)
			),
			push( new FnCall( expr(pop(1)), (Expressions)pop() ) )
		);
	}

	Rule Expressions() {
		return FirstOf(
			ExpList(),
			push( ExpList.emptyExpList )
		);
	}

	Rule ExpList() {
		Var<ExpList.Builder> builder = new Var<ExpList.Builder>(new ExpList.Builder());
		return Sequence(
			Expr(),
			addToExpList(builder.get()),
			ZeroOrMore(
				',', Spaces(), Expr(),
				addToExpList(builder.get())
			),
			push( builder.get().build() )
		);
	}

	boolean addToExpList(ExpList.Builder bld) {
		Object obj = pop();
		if( obj instanceof Expressions ) {
			bld.add( (Expressions)obj );
		} else {
			bld.add( (Expr)obj );
		}
		return true;
	}


	Rule SubExpr() {
		return Sequence( '[', incParens(), Spaces(), Expr(), ']', decParens(), Spaces() );
	}

	Rule NameExpr() {
		return Sequence(
			Name(),
			push( new ConstExpr((String)pop()) )
		);
	}

	Rule Name() {
		return Sequence(
			Sequence(
				NameFirstChar(),
				ZeroOrMore( NameChar() )
			),
			!keywords.contains(match()),
			push(match()),
			Spaces()
		);
	}

	Rule NameChar() {
		return FirstOf( NameFirstChar(), Digit() );
	}

	Rule NameFirstChar() {
		return FirstOf(
			CharRange('a', 'z'),
			CharRange('A', 'Z'),
			'_'
		);
	}

	Rule Keyword(String keyword) {
		return Sequence(
			keyword,
			TestNot( NameChar() ),
			Spaces()
		);
	}

	static final Set<String> keywords = new HashSet<String>(Arrays.asList(
		"and",
		"break",
		"do",
		"else",
		"elseif",
		"end",
		"false",
		"for",
		"function",
		"goto",
		"if",
		"in",
		"local",
		"nil",
		"not",
		"or",
		"repeat",
		"return",
		"then",
		"true",
		"until",
		"while"
	));

	Rule LiteralExpr() {
		return Sequence(
			Literal(), Spaces(),
			push(new ConstExpr(pop()))
		);
	}

	Rule Literal() {
		return FirstOf(
			NilLiteral(),
			BooleanLiteral(),
			NumberLiteral(),
			StringLiteral()
		);
	}

	Rule NilLiteral() {
		return Sequence( "nil", push(null) );
	}

	Rule BooleanLiteral() {
		return FirstOf(
			Sequence( "true", push(true) ),
			Sequence( "false", push(false) )
		);
	}

	Rule NumberLiteral() {
		return Sequence(
			Number(),
			push(new LuaNumber((Double)pop()))
		);
	}

	Rule Number() {
		return FirstOf(
			Sequence(
				IgnoreCase("0x"),
				HexNumber()
			),
			Sequence(
				DecNumber(),
				push(Double.parseDouble(match()))
			)
		);
	}

	Rule DecNumber() {
		return FirstOf(
			Sequence(
				Int(),
				Optional( '.', Optional(Int()) ),
				Exponent()
			),
			Sequence( '.', Int(), Exponent() )
		);
	}

	Rule Exponent() {
		return Optional(
			IgnoreCase('e'),
			Optional(AnyOf("+-")),
			Int()
		);
	}

	Rule Int() {
		return OneOrMore(Digit());
	}

	Rule Digit() {
		return CharRange('0', '9');
	}

	Rule HexNumber() {
		return FirstOf(
			Sequence(
				HexInt(),
				push( (double)Long.parseLong(match(),16) ),
				Optional( '.', Optional(HexDec()) ),
				HexExponent()
			),
			Sequence( push(0.0), '.', HexDec(), HexExponent() )
		);
	}

	Rule HexDec() {
		return Sequence(
			HexInt(),
			push( (Double)pop() + (double)Long.parseLong(match(),16) / Math.pow(16,matchLength()) )
		);
	}

	Rule HexExponent() {
		return Optional(
			IgnoreCase('p'),
			Sequence(
				Optional(AnyOf("+-")),
				HexInt()
			),
			push( (Double)pop() * Math.pow(2,(double)Long.parseLong(match())) )
		);
	}

	Rule HexInt() {
		return OneOrMore(Digit());
	}


	Rule HexDigit() {
		return FirstOf(
			Digit(),
			AnyOf("abcdefABCDEF")
		);
	}

	Rule StringLiteral() {
		return FirstOf(
			QuotedString('"'),
			QuotedString('\''),
			LongString()
		);
	}

	Rule LongString() {
		return Sequence(
			'[',
			ZeroOrMore('='),
			nEquals(matchLength()),
			'[',
			ZeroOrMore(
				TestNot(LongBracketsEnd()),
				ANY
			),
			push( match() ),
			LongBracketsEnd()
		);
	}

	Rule QuotedString(char quote) {
		StringBuilderVar buf = new StringBuilderVar();
		return Sequence(
			quote,
			ZeroOrMore(
				FirstOf(
					Sequence(
						NoneOf("\\\n"+quote),
						buf.append(matchedChar())
					),
					EscSeq(buf)
				)
			),
			quote,
			push( buf.getString() )
		);
	}

	Rule EscSeq(StringBuilderVar buf) {
		return Sequence(
			'\\',
			FirstOf(
				Sequence( 'a', buf.append('\u0007') ),
				Sequence( 'b', buf.append('\b') ),
				Sequence( 'f', buf.append('\f') ),
				Sequence( 'n', buf.append('\n') ),
				Sequence( 'r', buf.append('\r') ),
				Sequence( 't', buf.append('\t') ),
				Sequence( 'v', buf.append('\u000b') ),
				Sequence( '\\', buf.append('\\') ),
				Sequence( '"', buf.append('"') ),
				Sequence( '\'', buf.append('\'') ),
				Sequence(
					'x',
					Sequence( HexDigit(), HexDigit() ),
					buf.append( (char)Integer.parseInt(match(),16) )
				),
				Sequence(
					Sequence(
						Digit(),
						Optional(
							Digit(),
							Optional(
								Digit()
							)
						)
					),
					buf.append( (char)Integer.parseInt(match()) )
				)
			)
		);
	}

	Rule Spaces() {
		return ZeroOrMore(
			FirstOf(
				AnyOf(" \t"),
				Comment(),
				Sequence( '\\', EndOfLine() ),
				Sequence( AnyOf("\r\n"), parens > 0 )
			)
		);
	}

	Rule Comment() {
		return Sequence(
			"--[",
			ZeroOrMore('='),
			nEquals(matchLength()),
			'[',
			ZeroOrMore(
				TestNot(LongBracketsEnd()),
				ANY
			),
			LongBracketsEnd()
		);
	}

	Rule LongBracketsEnd() {
		return Sequence( ']', ZeroOrMore('='), nEquals==matchLength(), ']' );
	}

	static boolean action(Object obj) {
		return true;
	}

	// for debugging
	boolean print(Object o) {
		System.out.println(o);
		return true;
	}

	// for testing
	public static void main(String[] args) throws Exception {
		LuaParser parser = Parboiled.createParser(LuaParser.class);
		while( true ) {
			String input = new Scanner(System.in).nextLine();
			ParsingResult<?> result = new ReportingParseRunner(parser.Target()).run(input);
			if( result.hasErrors() ) {
				System.out.println("Parse Errors:\n" + ErrorUtils.printParseErrors(result));
			} else {
				Expr expr = expr(result.resultValue);
				LuaState lua = new LuaState();
				Object val = expr.eval(lua);
				System.out.println("Result: "+Lua.toString(val));
			}
		}
	}
}
