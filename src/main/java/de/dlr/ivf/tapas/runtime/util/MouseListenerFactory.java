package de.dlr.ivf.tapas.runtime.util;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

/**
 * This factory class enables one to use MouseListeners with lambda expressions.
 * 
 * @author sche_ai
 *
 */
public class MouseListenerFactory {
	
	public static MouseListener onClick(Consumer<MouseEvent> me) {
		
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {me.accept(e);}
		};
	}
	
	public static MouseListener onDrag(Consumer<MouseEvent> me) {
		
		return new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {me.accept(e);}
		};
	}

	public static MouseListener onEnter(Consumer<MouseEvent> me) {
		
		return new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {me.accept(e);}
		};
	}
	
	public static MouseListener onExit(Consumer<MouseEvent> me) {
		
		return new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {me.accept(e);}
		};
	}
	
	public static MouseListener onMove(Consumer<MouseEvent> me) {
		
		return new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {me.accept(e);}
		};
	}
	
	public static MouseListener onPress(Consumer<MouseEvent> me) {
		
		return new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {me.accept(e);}
		};
	}
	
	public static MouseListener onRelease(Consumer<MouseEvent> me) {
		
		return new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {me.accept(e);}
		};
	}
}
