/*
 * ModalDialog.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2005 Operational Dynamics
 */
package xseq.ui;

import org.gnu.gtk.ButtonsType;
import org.gnu.gtk.DialogFlags;
import org.gnu.gtk.MessageDialog;
import org.gnu.gtk.MessageType;
import org.gnu.gtk.Window;
import org.gnu.gtk.WindowPosition;

/**
 * A simple wrapper, as the MessageDialog class (which is supposedly a
 * convenience) is a pain to use. [Note this class partially exists because the
 * subtext routines are missing from gtk2.4].
 * <P>
 * Instantiate the dialog then call run() to show & block, or present() just to
 * show it. It will center on Screen.
 * <P>
 * Example of use:
 * 
 * <PRE>
 * 
 * ModalDialog error = new ModalDialog("File not found", e.getMessage() + "\n" +
 * "Try again?", MessageType.WARNING); error.run();
 * 
 * </PRE>
 * 
 * @author Andrew Cowie
 */
public class ModalDialog
{
	MessageDialog	_dialog	= null;

	/**
	 * Pop a MessageDialog. Parameters message and subtext are Pango markup
	 * enabled.
	 * 
	 * @param message
	 *            Will be rendered in a larger font as the main [error] message
	 *            text.
	 * @param subtext
	 *            Rendered in normal font, can amplify the message.
	 * @param type
	 *            Will control the icon used, and the text of the dismiss
	 *            button.
	 */
	public ModalDialog(String message, String subtext, MessageType type) {
		ButtonsType buttons;

		if (type == MessageType.INFO) {
			buttons = ButtonsType.OK;
		} else {
			buttons = ButtonsType.CLOSE;
		}

		Window[] windows = Window.listToplevelWindows();

		_dialog = new MessageDialog(windows[0], DialogFlags.DESTROY_WITH_PARENT, type, buttons, "<big><b>" + message
				+ "</b></big>\n" + subtext, true);
		_dialog.hide();
		_dialog.setPosition(WindowPosition.CENTER);
	}

	/**
	 * Modally display the dialog. Blocks, as is the behaviour with
	 * Dialog.run(), then destroys after action by the user causes it to return.
	 */
	public void run() {
		_dialog.showAll();
		_dialog.run();
		_dialog.destroy();
	}

	/**
	 * Display the dialog.
	 */
	public void present() {
		_dialog.showAll();
		_dialog.present();
	}
}