/**
 * author: Marcel Genzmehr
 * 27.07.2011
 */
package org.freeplane.plugin.workspace.dnd;

import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.io.StringOutputStream;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;
import org.freeplane.plugin.workspace.view.WorkspaceNodeRenderer;
/**
 * 
 */
public class WorkspaceTransferHandler extends TransferHandler implements DropTargetListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Insets DEFAULT_INSETS = new Insets(20, 20, 20, 20);

	private JTree tree;
	private final IWorkspaceDragnDropController controller;
	private final IDropTargetDispatcher dispatcher;

	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/

	public WorkspaceTransferHandler(JTree tree, IWorkspaceDragnDropController controller, int action, boolean drawIcon) {
		this.controller = controller;
		this.dispatcher = new DefaultWorkspaceDroptTargetDispatcher();
		
		this.tree = tree;
		this.tree.setTransferHandler(this);
		this.tree.setDragEnabled(true);
		this.tree.setAutoscrolls(true);
		new DropTarget(tree, COPY_OR_MOVE, this);
	}

	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/

	public static WorkspaceTransferHandler configureDragAndDrop(JTree tree, IWorkspaceDragnDropController controller) {
		return new WorkspaceTransferHandler(tree, controller, DnDConstants.ACTION_COPY_OR_MOVE, true);
	}

	private void autoscroll(JTree tree, Point cursorLocation) {
		Insets insets = DEFAULT_INSETS;
		Rectangle outer = tree.getVisibleRect();
		Rectangle inner = new Rectangle(outer.x + insets.left, outer.y + insets.top, outer.width - (insets.left + insets.right),
				outer.height - (insets.top + insets.bottom));
		if (!inner.contains(cursorLocation)) {
			Rectangle scrollRect = new Rectangle(cursorLocation.x - insets.left, cursorLocation.y - insets.top, insets.left
					+ insets.right, insets.top + insets.bottom);
			tree.scrollRectToVisible(scrollRect);
		}
	}

	public Transferable createTransferable(JComponent comp) {		
		StringOutputStream writer = new StringOutputStream();
		List<AWorkspaceTreeNode> objectList = new ArrayList<AWorkspaceTreeNode>();
		if (comp instanceof JTree) {
			JTree t = (JTree) comp;
			//FIXME: prepare for multiple node selection
			for (TreePath p : t.getSelectionPaths()) {
				AWorkspaceTreeNode node = (AWorkspaceTreeNode) p.getLastPathComponent();
				if (node instanceof IWorkspaceTransferableCreator) {
					return ((IWorkspaceTransferableCreator)node).getTransferable();
				}
				try {
					ObjectOutputStream oos = new ObjectOutputStream(writer);
					oos.writeObject(node);
					objectList.add(node);
				}
				catch (IOException e) {
				}
			}
		}	
		
		WorkspaceTransferable transferable = new WorkspaceTransferable(WorkspaceTransferable.WORKSPACE_SERIALIZED_FLAVOR, writer.getString());
		transferable.addData(WorkspaceTransferable.WORKSPACE_NODE_FLAVOR, objectList);
		return transferable;

	}

	public boolean importData(JComponent comp, Transferable t) {
		System.out.println("importData: " + comp);
		return super.importData(comp, t);
	}

	public int getSourceActions(JComponent comp) {
		return COPY_OR_MOVE;
	}

	public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
		System.out.println("exportToClipboard");
		super.exportToClipboard(comp, clip, action);
	}

	// Causes the Swing drag support to be initiated.
	public void exportAsDrag(JComponent comp, java.awt.event.InputEvent e, int action) {
		System.out.println("exportAsDrag");
		super.exportAsDrag(comp, e, action);
	}

	// Invoked after data has been exported.
	public void exportDone(JComponent source, Transferable data, int action) {
		System.out.println("exportDone");
		super.exportDone(source, data, action);
	}

	/***********************************************************************************
	 * REQUIRED METHODS FOR INTERFACES
	 **********************************************************************************/
	/* DropTarget Methods */

	public final void drop(DropTargetDropEvent event) {
		if(WorkspaceController.getController().getWorkspaceViewTree().getPathForLocation(event.getLocation().x, event.getLocation().y) == null) {
			return;
		}
		// new method to handle drop events
		if(this.dispatcher.dispatchDropEvent(event)) {
			return;
		}
		//FIXME: remove/comment out the following code if new drop handling works, maybe leave some fall back routines
		System.out.println("drop: " + event.getSource());
		if(controller.canPerformAction(event)) {
			if(controller.executeDrop(event)) {
				return;
			}
		}
		try {
			Transferable transferable = event.getTransferable();
			if (transferable.isDataFlavorSupported(WorkspaceTransferable.WORKSPACE_FILE_LIST_FLAVOR)) {
				event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				List<?> files = (List<?>) transferable.getTransferData(WorkspaceTransferable.WORKSPACE_FILE_LIST_FLAVOR);
				for (Object item : files) {
					System.out.println(item.toString());
				}
				
				event.getDropTargetContext().dropComplete(true);
			}
			else if (transferable.isDataFlavorSupported(WorkspaceTransferable.WORKSPACE_NODE_FLAVOR)) {
				event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				List<?> path = (List<?>) transferable.getTransferData(WorkspaceTransferable.WORKSPACE_NODE_FLAVOR);
				for (Object item : path) {
					System.out.println(item.toString());
				}
				event.getDropTargetContext().dropComplete(true);

			}
			else if (transferable.isDataFlavorSupported(WorkspaceTransferable.WORKSPACE_FREEPLANE_NODE_FLAVOR)) {
				event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				Object object = transferable.getTransferData(WorkspaceTransferable.WORKSPACE_FREEPLANE_NODE_FLAVOR);
				System.out.println(object.toString());
				event.getDropTargetContext().dropComplete(true);

			}
			else if (transferable.isDataFlavorSupported(WorkspaceTransferable.WORKSPACE_SERIALIZED_FLAVOR)) {
				event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				Object object = transferable.getTransferData(WorkspaceTransferable.WORKSPACE_SERIALIZED_FLAVOR);
				System.out.println(object.toString());
				event.getDropTargetContext().dropComplete(true);

			}
			else {
				event.rejectDrop();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			event.rejectDrop();
		}
	}

	public final void dragEnter(DropTargetDragEvent dtde) {
		// System.out.println("dragEnter: " + dtde);
	}

	public final void dragExit(DropTargetEvent dte) {
		 System.out.println("dragExit: " + dte);
	}

	private TreePath lastPathLocation = null;
	public final void dragOver(DropTargetDragEvent dtde) {
		autoscroll(this.tree, dtde.getLocation());		
		TreePath path = tree.getPathForLocation(dtde.getLocation().x, dtde.getLocation().y);
		if(path == lastPathLocation) {
			return;
		}
		WorkspaceNodeRenderer renderer = (WorkspaceNodeRenderer) tree.getCellRenderer();
		if(path != null && path != lastPathLocation) {								
			lastPathLocation = path;			
			renderer.highlightRow(tree.getRowForLocation(dtde.getLocation().x, dtde.getLocation().y));
			tree.repaint();
		} 
		else if(lastPathLocation != null) {			
			lastPathLocation = null;
			renderer.highlightRow(-1);
			tree.repaint();
		}
	}

	public final void dropActionChanged(DropTargetDragEvent dtde) {
		System.out.println("dropActionChanged: " + dtde);
	}

	/***********************************************************************************
	 * INTERNAL CLASSES
	 **********************************************************************************/
	
	

}