package edu.umn.cs.crisys.smaccm.aadl2rtos.gluecode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.osate.aadl2.DirectionType;
import org.osate.aadl2.PortCategory;

import edu.umn.cs.crisys.smaccm.aadl2rtos.Aadl2RtosException;
import edu.umn.cs.crisys.smaccm.aadl2rtos.AstHelper;
import edu.umn.cs.crisys.smaccm.aadl2rtos.Model;
import edu.umn.cs.crisys.smaccm.aadl2rtos.ast.ArrayType;
import edu.umn.cs.crisys.smaccm.aadl2rtos.ast.IdType;
import edu.umn.cs.crisys.smaccm.aadl2rtos.ast.PointerType;
import edu.umn.cs.crisys.smaccm.aadl2rtos.ast.Type;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.Dispatcher;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.Dispatcher.DispatcherType;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.ExternalHandler;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.MyPort;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.SharedDataAccessor;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.SharedDataAccessor.AccessType;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.ThreadImplementation;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.ThreadInstance;
import edu.umn.cs.crisys.smaccm.aadl2rtos.util.Util;
import edu.umn.cs.crisys.smaccm.topsort.CyclicException;
import edu.umn.cs.crisys.smaccm.topsort.TopologicalSort;

public class HeaderWriter extends AbstractCodeWriter {
	public HeaderWriter(BufferedWriter out, File CFile, File HFile,
			Model model, AstHelper astHelper) {
		super(out, CFile, HFile, model, astHelper);
	}

	public String getHeaderDefStr() {
	  return "__" + Util.normalizeAadlName(this.HFile.getName()) + "__";
	}
	
	public void writeHeader() throws IOException {
	  writeBeginningWrapper(); 
	  
	  // Write license data
		writeLicense();
		out.append("\n\n");

		// Write file metadata (date, filename, etc.)
		writeFileMetadata(HFile);
		out.append("\n\n");

		// Write file description
		writeComment(
				"   This header file contains the datatypes used for communications between \n"
						+ "   AADL components as defined in the system implementation "
						+ sysInstanceName
						+ ".\n"
						+ "   It also contains the function declarations for: \n"
						+ "      1.) The task-level entrypoint functions for each task, \n"
						+ "      2.) The user-callable writer functions for each task, and \n"
						+ "      3.) The expected interface for each of the user-defined sub-entrypoints\n\n");
		out.append("\n\n");

		if (model.getThreadCalendar().hasDispatchers()) {
		  writeComment("  DECLARATION FOR INITIALIZATION OF SCHEDULER \n");
		  out.append("void smaccm_initialize_px4_systick_interrupt();\n\n"); 
		}
		
		// Write AADL type declarations
		writeComment("   AADL TYPE DECLARATIONS USED BY AT LEAST ONE PORT \n");
		writeTypes(astTypesEntrySet);
		out.append("\n\n");

		// Write task-level entrypoint functions
		writeComment("   TASK-LEVEL ENTRYPOINT FUNCTIONS \n");
		writeEntrypoints();
		out.append("\n\n");

    // Write reader functions for user-level entrypoints
    writeComment("   READER/WRITER FUNCTIONS FOR USER-LEVEL ENTRYPOINTS \n");
    writeReaderWriterDecls();
    out.append("\n\n");


    // Write interface for user-defined entrypoints
		writeComment("   INTERFACE FOR USER-DEFINED ENTRYPOINTS \n");
		writeUdeDecls(allThreads);
		out.append("\n\n");

		writeEndingWrapper() ; 
		// End of file
		writeComment("   End of autogenerated file: " + HFile.toString() + "\n");
	}

	private void writeBeginningWrapper() throws IOException {
	  String name = this.getHeaderDefStr();
	  out.append("#ifndef " + name + "\n");
	  out.append("#define " + name + "\n");
	  out.append("#ifdef __cplusplus \n");
	  out.append("  extern \"C\" { \n");
	  out.append("#endif\n"); 
	}
	
	private void writeEndingWrapper() throws IOException {
    out.append("#ifdef __cplusplus \n");
    out.append("  } \n");
    out.append("#endif\n");
    out.append("#endif /*" + this.getHeaderDefStr() + "*/\n");
	}
	
	private void writeEntrypoints() throws IOException {
		for (ThreadImplementation tw : taskThreads) {
			out.append("	void " + tw.getGeneratedEntrypoint() + "(int threadID); \n\n");
			
			List<ThreadInstance> instanceList = tw.getThreadInstanceList();
			for (ThreadInstance instance: instanceList) {
				out.append("	void " + tw.getGeneratedEntrypoint() + "_Instance_" + instance.getThreadId() + "();\n\n");
			} 
		}
	}

	// TODO: mention that we do not currently support IN/OUT ports
	private void writeReaderWriterDecls() throws IOException {

		for (ThreadImplementation tw : allThreads) {
			for (MyPort dpiw : tw.getPortList()) {
			  String fnName = Names.getReaderWriterFnName(dpiw);
				String argString = ""; 
				if (dpiw.getCategory() == PortCategory.DATA || 
				    dpiw.getCategory() == PortCategory.EVENT_DATA) {
          argString = Names.createRefParameter(dpiw.getDataType(), "arg");
          if (dpiw.getDirection() == DirectionType.OUT) {
            argString = "const " + argString;
          }
				}
        out.append("   bool " + fnName + "(" + argString + "); \n\n");
			}
			
			for (SharedDataAccessor sda: tw.getSharedDataAccessors()) {
			  if (sda.getAccessType() == AccessType.READ || 
			      sda.getAccessType() == AccessType.READ_WRITE) {
			    String fnName = Names.getThreadImplReaderFnName(sda);
			    String argString = Names.createRefParameter(sda.getSharedData().getDataType(), "arg");
			    out.append("   bool " + fnName + "(" + argString + "); \n\n");
			  }
			  if (sda.getAccessType() == AccessType.WRITE || 
			      sda.getAccessType() == AccessType.READ_WRITE) {
          String fnName = Names.getThreadImplWriterFnName(sda);
          String argString = Names.createRefParameter(sda.getSharedData().getDataType(), "arg");
          out.append("   bool " + fnName + "(const " + argString + "); \n\n");
			  }
			}
		}
	}

	
	private void writeType(IdType ty) throws IOException {
		// get the underlying type for the id. If it is a structured type,
		// (which I expect) then emit a 'typedef struct'. Else emit a typedef.
		StringBuffer typeName = new StringBuffer();
		typeName.append("typedef ");
		typeName.append(ty.getTypeRef().getCType().varString(ty.getTypeId()) + "; \n");
		out.append(typeName.toString());
	}

	private void writeTypes(Set<Entry<String, Type>> entrySet) throws IOException {
		try {
			// First we need to get all of the "top-level" types described as
			// ID-types, then
			// we can sort them topologically, then emit them.
			List<Type> idTypes = new ArrayList<Type>();
			for (Entry<String, Type> e : entrySet) {
				idTypes.add(new IdType(e.getKey(), e.getValue()));
			}
			List<Type> sortedTypes = TopologicalSort.performTopologicalSort(idTypes);

			for (Type t : sortedTypes) {
				if (t instanceof IdType) {
					writeType((IdType) t);
					out.append("\n");
				}
			}
		} catch (CyclicException e) {
			throw new Aadl2RtosException(
					"Error writing datatypes to C header: cyclic reference between types.");
		}
	}

	// TODO: well-formedness check on model for INOUT ports.
	private void writeThreadUdeDecls(ThreadImplementation tw) throws IOException {
		// List<MyPort> dataList = tw.getInputDataPorts();

		// user-level initializer for thread.
		ExternalHandler initHandler = tw.getInitializeEntrypointOpt();
		if ((initHandler != null)) {
			out.append("   void " + initHandler.getHandlerName() + "();\n\n");
		}

		// compute entrypoints for thread.
		List<Dispatcher> dl = tw.getDispatcherList();
		for (Dispatcher d: dl) {
		  List<ExternalHandler> handlers = d.getExternalHandlerList();
		  for (ExternalHandler eh: handlers) {
		    out.append(udeEntrypointDecl(d, eh.getHandlerName()));
		  }
		}
	}

	// For each thread;
	// record the list of data ports
	// TODO: For each event port, how do we want the interface?
	// Perhaps it does not really exist.
	private void writeUdeDecls(List<ThreadImplementation> allThreads) throws IOException {
		for (ThreadImplementation tw : allThreads) {
			writeThreadUdeDecls(tw);
		}
	}

	private String udeEntrypointDecl(Dispatcher d, String fnName) {
	  if (d.getDispatcherType() == DispatcherType.INPUT_PORT_DISPATCHER &&
	      d.getEventPort().isInputEventDataPort()) {
	    return "   void " + fnName
	        + "(const " + Names.createRefParameter(d.getEventPort().getDataType(), "elem") + " ); \n\n";
	  }
	  else {
	    return "   void " + fnName
				+ "(  ); \n\n";
	  }
	}
	
/*
	private String dataportArgs(List<MyPort> dpl) {
		StringBuilder out = new StringBuilder();
		Iterator<MyPort> iterator = dpl.iterator();
		while (iterator.hasNext()) {
			MyPort arg = iterator.next();
			out.append(arg.getDataType().toString());
			out.append(" ");
			out.append(arg.getName());
			if (iterator.hasNext()) {
				out.append(", ");
			}
		}
		return out.toString();
	}
*/  
	
}
