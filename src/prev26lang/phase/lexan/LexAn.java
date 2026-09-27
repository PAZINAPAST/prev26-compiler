package prev26lang.phase.lexan;

import java.io.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;

import prev26lang.*;
import prev26lang.common.logger.*;
import prev26lang.common.report.*;
import prev26lang.phase.*;

/**
 * Lexical analysis phase.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class LexAn extends Phase {

	/** The ANTLR lexer that actually performs lexical analysis. */
	public final Prev26Lexer lexer;

	/**
	 * Phase construction: sets up logging and the ANTLR lexer.
	 */
	public LexAn() {
		super("lexan");

		final String srcFileName = Compiler.cmdLineOptValue("--src-file-name");
		try {
			lexer = new Prev26Lexer(CharStreams.fromFileName(srcFileName));
			lexer.setTokenFactory(new LocLogTokenFactory());
		} catch (IOException __) {
			throw new Report.Error( //
					"Cannot open file '" + srcFileName + "'.");
		}
	}

	/**
	 * A customized token that is locatable (see {@link Locatable}) and loggable
	 * (see {@link Loggable}).
	 */
	@SuppressWarnings("serial")
	public class LocLogToken extends CommonToken implements Locatable, Loggable {

		/** The location of this token. */
		private final Location location;

		/**
		 * Never used outside {@link Prev26Lexer} (see
		 * <a href="https://www.antlr.org/index.html">ANTLR</a>).
		 */
		@SuppressWarnings("doclint:missing")
		public LocLogToken(final int type, final String text) {
			super(type, text);
			setLine(0);
			setCharPositionInLine(0);
			location = new Location(getLine(), getCharPositionInLine(), getLine(),
					getCharPositionInLine() + getText().length() - 1);
		}

		/**
		 * Never used outside {@link Prev26Lexer} (see
		 * <a href="https://www.antlr.org/index.html">ANTLR</a>).
		 */
		@SuppressWarnings("doclint:missing")
		public LocLogToken(final Pair<TokenSource, CharStream> source, final int type, final int channel,
				final int start, final int stop) {
			super(source, type, channel, start, stop);
			setCharPositionInLine(getCharPositionInLine() - getText().length() + 1);
			location = new Location(getLine(), getCharPositionInLine(), getLine(),
					getCharPositionInLine() + getText().length() - 1);
		}

		@Override
		public Location location() {
			return location;
		}

		@Override
		public void log(final XMLLogger xmlLogger) {
			if (xmlLogger == null)
				return;
			xmlLogger.begElement("token");
			if (getType() == -1) {
				xmlLogger.addAttribute("kind", "EOF");
			} else {
				xmlLogger.addAttribute("kind", Prev26Lexer.VOCABULARY.getSymbolicName(getType()));
				xmlLogger.addAttribute("lexeme", getText());
				location.log(xmlLogger);
			}
			xmlLogger.endElement();
		}

	}

	/**
	 * A customized token factory which logs tokens.
	 */
	private class LocLogTokenFactory implements TokenFactory<LocLogToken> {

		/**
		 * Constructs a new token factory.
		 */
		private LocLogTokenFactory() {
			super();
		}

		@Override
		public LocLogToken create(int type, String text) {
			LocLogToken token = new LocLogToken(type, text);
			token.log(xmlLogger);
			return token;
		}

		@Override
		public LocLogToken create(Pair<TokenSource, CharStream> source, int type, String text, int channel, int start,
				int stop, int line, int charPositionInLine) {
			LocLogToken token = new LocLogToken(source, type, channel, start, stop);
			token.log(xmlLogger);
			return token;
		}
	}

}
