package prev26lang.phase;

import prev26lang.*;
import prev26lang.common.logger.*;

/**
 * The abstract compiler phase.
 * 
 * All compiler phases should be implemented as a subclass of this class in
 * order to ensure they can be properly logged.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public abstract class Phase implements AutoCloseable {

	/** The logger used to produce the log of this phase. */
	public final XMLLogger xmlLogger;

	/**
	 * Constructs a new phase of a compiler. If logging of this phase has been
	 * requested, it prepares a logger using the phase name for naming the XML and
	 * XSL files as well as for the topmost XML element within the XML file.
	 * 
	 * @param phaseName The phase name.
	 */
	protected Phase(final String phaseName) {
		final String loggedPhase = Compiler.cmdLineOptValue("--logged-phase");
		if (loggedPhase.matches(phaseName + "|all")) {
			// Prepare the name of the xml file.
			String xmlFileName = Compiler.cmdLineOptValue("--xml") + "-" + phaseName + ".xml";

			// Prepare the name of the supporting xsl file.
			String xslDirName = Compiler.cmdLineOptValue("--xsl");
			if (xslDirName == null) {
				xslDirName = "";
			}

			xmlLogger = new XMLLogger(phaseName, xmlFileName, xslDirName + phaseName + ".xsl");
		} else {
			xmlLogger = null;
		}
	}

	@Override
	public void close() {
		if (xmlLogger != null)
			xmlLogger.close();
	}

}
