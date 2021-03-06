package com.rockwellcollins.atc.agree.analysis.views;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;

import jkind.api.results.AnalysisResult;
import jkind.api.results.PropertyResult;
import jkind.api.ui.results.AnalysisResultTree;
import jkind.interval.NumericInterval;
import jkind.lustre.Program;
import jkind.lustre.values.Value;
import jkind.results.Counterexample;
import jkind.results.InvalidProperty;
import jkind.results.Property;
import jkind.results.Signal;
import jkind.results.UnknownProperty;
import jkind.results.layout.Layout;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.xtext.ui.editor.GlobalURIEditorOpener;
import org.osate.aadl2.ComponentImplementation;
import org.osate.ui.dialogs.Dialog;

import com.rockwellcollins.atc.agree.agree.AgreeSubclause;
import com.rockwellcollins.atc.agree.agree.GuaranteeStatement;
import com.rockwellcollins.atc.agree.analysis.Util;

public class AgreeMenuListener implements IMenuListener {
    private static final GlobalURIEditorOpener globalURIEditorOpener = Util
            .getGlobalURIEditorOpener();
    private final IWorkbenchWindow window;
    private final AnalysisResultTree tree;
    private AgreeResultsLinker linker;

    public AgreeMenuListener(IWorkbenchWindow window, AnalysisResultTree tree) {
        this.window = window;
        this.tree = tree;
    }

    public void setLinker(AgreeResultsLinker linker) {
        this.linker = linker;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager) {
        IStructuredSelection selection = (IStructuredSelection) tree.getViewer().getSelection();
        if (!selection.isEmpty()) {
            AnalysisResult result = (AnalysisResult) selection.getFirstElement();
            addLinkedMenus(manager, result);
        }
    }

    private void addLinkedMenus(IMenuManager manager, AnalysisResult result) {
        addOpenComponentMenu(manager, result);
        addOpenContractMenu(manager, result);
        addViewLogMenu(manager, result);
        addViewCounterexampleMenu(manager, result);
        addViewLustreMenu(manager, result);
        addOpenGuaranteeMenu(manager, result);
    }

    private void addOpenComponentMenu(IMenuManager manager, AnalysisResult result) {
        ComponentImplementation ci = linker.getComponent(result);
        if (ci != null) {
            manager.add(createHyperlinkAction("Open " + ci.getName(), ci));
        }
    }

    private void addOpenContractMenu(IMenuManager manager, AnalysisResult result) {
        AgreeSubclause contract = linker.getContract(result);
        if (contract != null) {
            manager.add(createHyperlinkAction("Open Contract", contract));
        }
    }

    private void addViewLogMenu(IMenuManager manager, AnalysisResult result) {
        String log = linker.getLog(result);
        if (log == null && result instanceof PropertyResult) {
            log = linker.getLog(result.getParent());
        }
        if (log != null && !log.isEmpty()) {
            manager.add(createWriteConsoleAction("View Log", "Log", log));
        }
    }

    private void addViewCounterexampleMenu(IMenuManager manager, AnalysisResult result) {
        final Counterexample cex = getCounterexample(result);
        if (cex != null) {
            final String cexType = getCounterexampleType(result);
            final Layout layout = linker.getLayout(result.getParent());
            final Map<String, EObject> refMap = linker.getReferenceMap(result.getParent());

            manager.add(new Action("View " + cexType + "Counterexample in Console") {
                @Override
                public void run() {
                    viewCexConsole(cex, layout, refMap);
                }
            });

            manager.add(new Action("View " + cexType + "Counterexample in Eclipse") {
                @Override
                public void run() {
                    viewCexEclipse(cex, layout, refMap);
                }
            });

            manager.add(new Action("View " + cexType + "Counterexample in Spreadsheet") {
                @Override
                public void run() {
                    viewCexSpreadsheet(cex, layout);
                }
            });
        }
    }

    private void addViewLustreMenu(IMenuManager manager, AnalysisResult result) {
        Program program = linker.getProgram(result);
        if (program == null && result instanceof PropertyResult) {
            program = linker.getProgram(result.getParent());
        }
        if (program != null) {
            manager.add(createWriteConsoleAction("View Lustre", "Lustre", program));
        }
    }

    private void addOpenGuaranteeMenu(IMenuManager manager, AnalysisResult result) {
        if (result instanceof PropertyResult) {
            PropertyResult pr = (PropertyResult) result;
            Map<String, EObject> refMap = linker.getReferenceMap(pr.getParent());
            EObject guarantee = refMap.get(pr.getName());
            if (guarantee instanceof GuaranteeStatement) {
                manager.add(createHyperlinkAction("Open Guarantee", guarantee));
            }
        }
    }

    private static Counterexample getCounterexample(AnalysisResult result) {
        if (result instanceof PropertyResult) {
            Property prop = ((PropertyResult) result).getProperty();
            if (prop instanceof InvalidProperty) {
                return ((InvalidProperty) prop).getCounterexample();
            } else if (prop instanceof UnknownProperty) {
                return ((UnknownProperty) prop).getInductiveCounterexample();
            }
        }

        return null;
    }

    private static String getCounterexampleType(AnalysisResult result) {
        if (result instanceof PropertyResult) {
            Property prop = ((PropertyResult) result).getProperty();
            if (prop instanceof UnknownProperty) {
                return "Inductive ";
            }
        }

        return " ";
    }

    private IAction createHyperlinkAction(String text, final EObject eObject) {
        return new Action(text) {
            @Override
            public void run() {
                globalURIEditorOpener.open(EcoreUtil.getURI(eObject), true);
            }
        };
    }

    private Action createWriteConsoleAction(String actionName, final String consoleName,
            final Object content) {
        return new Action(actionName) {
            @Override
            public void run() {
                final MessageConsole console = findConsole(consoleName);
                showConsole(console);
                console.clearConsole();

                /*
                 * From the Eclipse API: "Clients should avoid writing large amounts of output to
                 * this stream in the UI thread. The console needs to process the output in the UI
                 * thread and if the client hogs the UI thread writing output to the console, the
                 * console will not be able to process the output."
                 */
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        console.newMessageStream().println(content.toString());
                    }
                }).start();
            }
        };
    }

    private void viewCexConsole(final Counterexample cex, final Layout layout,
            Map<String, EObject> refMap) {
        final MessageConsole console = findConsole("Counterexample");
        showConsole(console);
        console.clearConsole();
        console.addPatternMatchListener(new AgreePatternListener(refMap));

        /*
         * From the Eclipse API: "Clients should avoid writing large amounts of output to this
         * stream in the UI thread. The console needs to process the output in the UI thread and if
         * the client hogs the UI thread writing output to the console, the console will not be able
         * to process the output."
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (MessageConsoleStream out = console.newMessageStream()) {
                    for (String category : layout.getCategories()) {
                        if (isEmpty(category, cex, layout)) {
                            continue;
                        }

                        printHLine(out, cex.getLength());
                        out.println("Variables for " + category);
                        printHLine(out, cex.getLength());

                        out.print(String.format("%-60s", "Variable Name"));
                        for (int k = 0; k < cex.getLength(); k++) {
                            out.print(String.format("%-15s", k));
                        }
                        out.println();
                        printHLine(out, cex.getLength());

                        for (Signal<Value> signal : cex.getCategorySignals(layout, category)) {
                            out.print(String.format("%-60s", "{" + signal.getName() + "}"));
                            for (int k = 0; k < cex.getLength(); k++) {
                                Value val = signal.getValue(k);
                                if (jkind.util.Util.isArbitrary(val)) {
                                    out.print(String.format("%-15s", "-"));
                                } else if (val instanceof NumericInterval) {
                                    out.print(String.format("%-15s",
                                            formatInterval((NumericInterval) val)));
                                } else {
                                    out.print(String.format("%-15s", val.toString()));
                                }
                            }
                            out.println();
                        }
                        out.println();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String formatInterval(NumericInterval ni) {
        if (ni.isExact()) {
            return formatDouble(ni.getLow().toDouble());
        }

        String low;
        if (ni.getLow().isFinite()) {
            low = formatDouble(ni.getLow().toDouble());
        } else {
            low = "-inf";
        }

        String high;
        if (ni.getHigh().isFinite()) {
            high = formatDouble(ni.getHigh().toDouble());
        } else {
            high = "inf";
        }

        return "[" + low + ", " + high + "]";
    }

    private static final DecimalFormat format = new DecimalFormat("#.##");

    private String formatDouble(double x) {
        return format.format(x);
    }

    private boolean isEmpty(String category, Counterexample cex, Layout layout) {
        return cex.getCategorySignals(layout, category).isEmpty();
    }

    private void printHLine(MessageConsoleStream out, int nVars) {
        out.print("--------------------------------------------------");
        for (int k = 0; k < nVars; k++) {
            out.print("---------------------");
        }
        out.println();
    }

    private static MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++) {
            if (name.equals(existing[i].getName())) {
                return (MessageConsole) existing[i];
            }
        }
        // no console found, so create a new one
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] { myConsole });
        return myConsole;
    }

    private void showConsole(IConsole console) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
            view.display(console);
            view.setScrollLock(true);
        } catch (PartInitException e) {
        }
    }

    private void viewCexEclipse(Counterexample cex, Layout layout, Map<String, EObject> refMap) {
        try {
            AgreeCounterexampleView cexView = (AgreeCounterexampleView) window.getActivePage()
                    .showView(AgreeCounterexampleView.ID);
            cexView.setInput(cex, layout, refMap);
            cexView.setFocus();
        } catch (PartInitException e) {
            e.printStackTrace();
        }
    }

    private void viewCexSpreadsheet(Counterexample cex, Layout layout) {
        try {
            File file = File.createTempFile("cex", ".xls");
            cex.toExcel(file, layout);
            org.eclipse.swt.program.Program.launch(file.toString());
        } catch (IOException e) {
            Dialog.showError("Unable to open spreadsheet", e.getMessage());
            e.printStackTrace();
        }
    }
}
