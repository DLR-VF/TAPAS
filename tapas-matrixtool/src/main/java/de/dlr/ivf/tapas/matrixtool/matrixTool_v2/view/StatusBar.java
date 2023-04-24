package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.BorderLayout;
import java.awt.Image;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.ImageIcon;

import de.dlr.ivf.tapas.matrixtool.common.events.IOEvent;
import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.AbstractCheckingController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.AnalyseStatisticAggrController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleOpsController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleStructureController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.OuterControl;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.AbstractOperationEvent;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.BinaryOperationEvent;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.UnaryOperationEvent;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.UserInputEvent;

public class StatusBar extends JPanel implements Observer {

	private JLabel message;
	private JLabel sign;
	private JProgressBar bar;
	private boolean isEventInProgress;
	/*
	 * waehrend langwieriegen operationen wie lesen,schreiben,rechnen sollen
	 * keine kleinen anmerkungen von feldern wie "eingabe ok" angezeigt werden.
	 */

	public StatusBar() {

		setLayout(new BorderLayout(20,0));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
 
		sign = new JLabel(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.informationIcon")).
				getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
		add(sign, BorderLayout.LINE_START);

		message = new JLabel(Localisation.getLocaleGuiTerm("OK"));
		add(message, BorderLayout.CENTER);

		isEventInProgress = false;

		bar = new JProgressBar();
		bar.setSize(100,20);
		add(bar, BorderLayout.LINE_END);
	}

	public void update(Observable o, Object arg) {

		if (o instanceof OuterControl){
			if (arg instanceof IOEvent){
				if (((IOEvent) arg).getType() == IOEvent.Type.ERROR_READING ||
						((IOEvent) arg).getType() == IOEvent.Type.ERROR_WRITING){

					message.setText(((IOEvent) arg).getMessage()+
							(((IOEvent) arg).getFile() != null ? " :   "+((IOEvent) arg).getFile() : ""));
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.errorIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = false;
					bar.setIndeterminate(false);
				}
				if (((IOEvent) arg).getType() == IOEvent.Type.READING){

					message.setText(Localisation.getLocaleGuiTerm("READING")+
							(((IOEvent) arg).getFile() != null ? " :   "+((IOEvent) arg).getFile() : ""));
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.informationIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = true;
					bar.setIndeterminate(true);
				}
				if (((IOEvent) arg).getType() == IOEvent.Type.WRITING){

					message.setText(Localisation.getLocaleGuiTerm("WRITING")+
							(((IOEvent) arg).getFile() != null ? " :   "+((IOEvent) arg).getFile() : ""));
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.informationIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = true;
					bar.setIndeterminate(true);
				}
				if (((IOEvent) arg).getType() == IOEvent.Type.FINISHED_READING){

					message.setText(Localisation.getLocaleGuiTerm("READ")+
							(((IOEvent) arg).getFile() != null ? " :   "+((IOEvent) arg).getFile() : ""));
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.informationIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = false;
					bar.setIndeterminate(false);
				}
				if (((IOEvent) arg).getType() == IOEvent.Type.FINISHED_WRITING){

					message.setText(Localisation.getLocaleGuiTerm("WRITTEN")+
							(((IOEvent) arg).getFile() != null ? " :   "+((IOEvent) arg).getFile() : ""));
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.informationIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = false;
					bar.setIndeterminate(false);
				}
			}
		} else if (o instanceof ManipModuleOpsController){
			if (arg instanceof BinaryOperationEvent){

				if (((BinaryOperationEvent)arg).getType() == AbstractOperationEvent.Type.OP_START){
					message.setText(Localisation.getLocaleGuiTerm("EXECUTING")+
							" :   "+((BinaryOperationEvent)arg).getSinkOperand() +" - "+
							((BinaryOperationEvent)arg).getOperation() + " - "+
							((BinaryOperationEvent)arg).getSourceOperand());
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.informationIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = true;
					bar.setIndeterminate(true);
				}
				if (((BinaryOperationEvent)arg).getType() == AbstractOperationEvent.Type.OP_FNSHD){
					message.setText(Localisation.getLocaleGuiTerm("EXECUTED")+
							" :   "+((BinaryOperationEvent)arg).getSinkOperand() +" - "+
							((BinaryOperationEvent)arg).getOperation() + " - "+
							((BinaryOperationEvent)arg).getSourceOperand());
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.informationIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = false;
					bar.setIndeterminate(false);
				}
				if (((BinaryOperationEvent)arg).getType() == AbstractOperationEvent.Type.ERROR){
					message.setText(((BinaryOperationEvent)arg).getMessage() +
							" :   "+((BinaryOperationEvent)arg).getSinkOperand() +" - "+
							((BinaryOperationEvent)arg).getOperation() + " - "+
							((BinaryOperationEvent)arg).getSourceOperand());
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.errorIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = false;
					bar.setIndeterminate(false);
				}
			}
		} else if (o instanceof AnalyseStatisticAggrController){
			if (arg instanceof UnaryOperationEvent){

				if (((UnaryOperationEvent)arg).getType() == AbstractOperationEvent.Type.OP_START){
					message.setText(Localisation.getLocaleGuiTerm("EXECUTING")+
							" :   "+((UnaryOperationEvent)arg).getOperand() +" - "+
							((UnaryOperationEvent)arg).getOperation());
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.informationIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = true;
					bar.setIndeterminate(true);
				}
				if (((UnaryOperationEvent)arg).getType() == AbstractOperationEvent.Type.OP_FNSHD){
					message.setText(Localisation.getLocaleGuiTerm("EXECUTED")+
							" :   "+((UnaryOperationEvent)arg).getOperand() +" - "+
							((UnaryOperationEvent)arg).getOperation()+"");
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.informationIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = false;
					bar.setIndeterminate(false);
				}
				if (((UnaryOperationEvent)arg).getType() == AbstractOperationEvent.Type.ERROR){
					message.setText(((UnaryOperationEvent)arg).getMessage() +
							" :   "+((UnaryOperationEvent)arg).getOperand() +" - "+
							((UnaryOperationEvent)arg).getOperation());
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.errorIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = false;
					bar.setIndeterminate(false);
				}
			}

		} else if (o instanceof ManipModuleStructureController){

			if (!isEventInProgress){

				if (arg instanceof String){
					message.setText(arg.toString());
					sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.errorIcon")).
							getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
					isEventInProgress = false;
					bar.setIndeterminate(false);

				} else if (arg instanceof UserInputEvent){
					
					String userInput = (((UserInputEvent)arg).getInput() == null ||
							((UserInputEvent)arg).getInput().length() == 0) ? 
									Localisation.getLocaleMessageTerm("EMPTYSTR") :
										((UserInputEvent)arg).getInput();

					if (((UserInputEvent)arg).getMessage() == UserInputEvent.Type.WRONG){
						message.setText(((UserInputEvent)arg).getProblem()+
								" :   "+userInput);
						sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.errorIcon")).
								getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
						isEventInProgress = false;
						bar.setIndeterminate(false);
					}

					if (((UserInputEvent)arg).getMessage() == UserInputEvent.Type.PROBLEM){
						message.setText(((UserInputEvent)arg).getProblem()+
								" :   "+userInput);
						sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.warningIcon")).
								getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
						isEventInProgress = false;
						bar.setIndeterminate(false);
					}

					if (((UserInputEvent)arg).getMessage() == UserInputEvent.Type.OK){
						message.setText(Localisation.getLocaleGuiTerm("OK_INPUT")+
								" :   "+userInput);
						sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.informationIcon")).
								getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
						isEventInProgress = false;
						bar.setIndeterminate(false);
					}
				}
			}
			/*
			 * TODO
			 * gehackt hier
			 * TODO
			 */

		} else if (o instanceof AbstractCheckingController){

			if (arg instanceof UserInputEvent){

				if (!isEventInProgress){
					
					String userInput = (((UserInputEvent)arg).getInput() == null ||
								((UserInputEvent)arg).getInput().length() == 0) ? 
										Localisation.getLocaleMessageTerm("EMPTYSTR") :
											((UserInputEvent)arg).getInput();

					if (((UserInputEvent)arg).getMessage() == UserInputEvent.Type.WRONG){
						message.setText(((UserInputEvent)arg).getProblem()+
								" :   "+userInput);
						sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.errorIcon")).
								getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
						isEventInProgress = false;
						bar.setIndeterminate(false);
					}

					if (((UserInputEvent)arg).getMessage() == UserInputEvent.Type.PROBLEM){
						message.setText(((UserInputEvent)arg).getProblem()+
								" :   "+userInput);
						sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.warningIcon")).
								getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
						isEventInProgress = false;
						bar.setIndeterminate(false);
					}

					if (((UserInputEvent)arg).getMessage() == UserInputEvent.Type.OK){
						message.setText(Localisation.getLocaleGuiTerm("OK_INPUT")+
								" :   "+userInput);
						sign.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane.informationIcon")).
								getImage().getScaledInstance(20,20,Image.SCALE_DEFAULT)));
						isEventInProgress = false;
						bar.setIndeterminate(false);
					}
				}
			}
		}
	}
}
