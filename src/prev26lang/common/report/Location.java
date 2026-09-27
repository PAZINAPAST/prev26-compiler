package prev26lang.common.report;

import prev26lang.common.logger.*;

/**
 * The description of a location within a source file.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class Location implements Locatable, Loggable {

	/**
	 * The line number of the first character of the specified part of the source
	 * file.
	 */
	private final int begLine;

	/**
	 * The column number of the first character of the specified part of the source
	 * file.
	 */
	private final int begColumn;

	/**
	 * The line number of the last character of the specified part of the source
	 * file.
	 */
	private final int endLine;

	/**
	 * The column number of the last character of the specified part of the source
	 * file.
	 */
	private final int endColumn;

	/**
	 * Constructs a new location if the position of the first and the last
	 * characters are given.
	 * 
	 * @param begLine   The line number of the first character of the specified part
	 *                  of the source file.
	 * @param begColumn The column number of the first character of the specified
	 *                  part of the source file.
	 * @param endLine   The line number of the last character of the specified part
	 *                  of the source file.
	 * @param endColumn The column number of the last character of the specified
	 *                  part of the source file.
	 */
	public Location(final int begLine, final int begColumn, final int endLine, final int endColumn) {
		this.begLine = begLine;
		this.begColumn = begColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
	}

	/**
	 * Constructs a new location if the position of a single character is given.
	 * 
	 * @param line   The line number of the character of the specified part of the
	 *               source file.
	 * @param column The column number of the character of the specified part of the
	 *               source file.
	 */
	public Location(final int line, final int column) {
		this(line, column, line, column);
	}

	/**
	 * Constructs a new location given an object relating to a part of a source
	 * file.
	 * 
	 * @param that An object relating to a part of a source file.
	 */
	public Location(final Locatable that) {
		this(that.location().begLine, that.location().begColumn, that.location().endLine, that.location().endColumn);
	}

	/**
	 * Constructs a new location given two objects relating to parts of a source
	 * file.
	 * 
	 * @param beg An object relating to the beginning of part of a source file.
	 * @param end An object relating to the end of part of a source file.
	 */
	public Location(final Locatable beg, final Locatable end) {
		this(beg.location().begLine, beg.location().begColumn, end.location().endLine, end.location().endColumn);
	}

	@Override
	public Location location() {
		return this;
	}

	@Override
	public void log(final XMLLogger xmlLogger) {
		if (xmlLogger == null)
			return;
		xmlLogger.begElement("location");
		xmlLogger.addAttribute("begline", Integer.toString(begLine));
		xmlLogger.addAttribute("begcolumn", Integer.toString(begColumn));
		xmlLogger.addAttribute("endline", Integer.toString(endLine));
		xmlLogger.addAttribute("endcolumn", Integer.toString(endColumn));
		xmlLogger.endElement();
	}

	@Override
	public String toString() {
		return begLine + "." + begColumn + " - " + endLine + "." + endColumn;
	}

}
