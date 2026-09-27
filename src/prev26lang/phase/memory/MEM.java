package prev26lang.phase.memory;

/**
 * Internal representation of stack frames and variable accesses.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class MEM {

	/**
	 * A stack frame.
	 * 
	 * @author bostjan.slivnik@fri.uni-lj.si
	 */
	static public class Frame {

		/** The function's entry label. */
		public final Label label;

		/** The function's static depth (global functions are at depth 0). */
		public final long depth;

		/** The size of the frame. */
		public final long size;

		/** The size of the block of local variables within a frame. */
		public final long locsSize;

		/** The size of the block of (call, i.e., outgoing) arguments within a frame. */
		public final long argsSize;

		/** The register to hold the frame pointer. */
		public final Temp FP;

		/** The register to hold the return value. */
		public final Temp RV;

		/**
		 * Constructs a new frame with no temporary variables and no saved registers.
		 * 
		 * @param label    The function's entry label.
		 * @param depth    The function's static depth.
		 * @param locsSize The size of the block of local variables within a frame.
		 * @param argsSize The size of the block of (call, i.e., outgoing) arguments
		 *                 within a frame.
		 * @param size     The size of the frame.
		 */
		public Frame(Label label, long depth, long locsSize, long argsSize, long size) {
			this.label = label;
			this.depth = depth;
			this.locsSize = locsSize;
			this.argsSize = argsSize;
			this.size = size;
			this.FP = new Temp();
			this.RV = new Temp();
		}

		@Override
		public String toString() {
			return String.format(
				"Label=%s, Depth=%d, LocsSize=%d, ArgsSize=%d, Size=%d",
				label, depth, locsSize, argsSize, size
			);
		}
	}

	/**
	 * Access to a variable.
	 * 
	 * @author bostjan.slivnik@fri.uni-lj.si
	 */
	static public abstract class Access {

		/** The size of the variable. */
		public final long size;

		/**
		 * Creates a new access to a variable.
		 * 
		 * @param size The size of the variable.
		 */
		public Access(long size) {
			this.size = size;
		}

	}

	/**
	 * An access to a variable at a fixed address.
	 * 
	 * (Also used for string constants.)
	 * 
	 * @author bostjan.slivnik@fri.uni-lj.si
	 */
	static public class AbsAccess extends Access {

		/** Label denoting a fixed address. */
		public final Label label;

		/** Initial value. */
		public final String init;

		/**
		 * Constructs a new absolute access.
		 * 
		 * @param size  The size of a variable.
		 * @param label Offset of a variable at an absolute address.
		 * @param init  Initial value (or {@code null}).
		 */
		public AbsAccess(long size, Label label, String init) {
			super(size);
			this.label = label;
			this.init = init;
		}

		/**
		 * Constructs a new absolute access.
		 * 
		 * @param size  The size of a variable.
		 * @param label Offset of a variable at an absolute address.
		 */
		public AbsAccess(long size, Label label) {
			super(size);
			this.label = label;
			this.init = null;
		}

	}

	/**
	 * An access to a variable relative to an (unspecified) base address.
	 * 
	 * @author bostjan.slivnik@fri.uni-lj.si
	 */
	static public class RelAccess extends Access {

		/**
		 * Offset of a variable relative to a base address, i.e., positive for
		 * parameters and record components, negative for local variables.
		 */
		public final long offset;

		/**
		 * The variable's static depth (-1 for record components, otherwise the static
		 * depth of the function within which the variable is defined).
		 */
		public final long depth;

		/**
		 * Constructs a new relative access.
		 * 
		 * @param size   The size of the variable.
		 * @param offset Offset of a variable relative to a base address.
		 * @param depth  The variable's static depth (-1 for record components).
		 */
		public RelAccess(long size, long offset, long depth) {
			super(size);
			this.offset = offset;
			this.depth = depth;
		}

	}

	/**
	 * A temporary variable.
	 *
	 * @author bostjan.slivnik@fri.uni-lj.si
	 */
	static public class Temp {

		/** The name of a temporary variable. */
		public final long temp;

		/** Counter of temporary variables. */
		private static long count = 0;

		/** Creates a new temporary variable. */
		public Temp() {
			this.temp = count;
			count++;
		}

		@Override
		public String toString() {
			return "T" + temp;
		}

	}

	/**
	 * A label.
	 * 
	 * @author bostjan.slivnik@fri.uni-lj.si
	 */
	static public class Label {

		/** The name of a label. */
		public final String name;

		/** Counter of anonymous labels. */
		private static long count = 0;

		/** Creates a new anonymous label. */
		public Label() {
			this.name = "L" + count;
			count++;
		}

		/**
		 * Creates a new named label.
		 * 
		 * @param name The name of a label.
		 */
		public Label(String name) {
			this.name = "_" + name;
		}

	}

}
