package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class ClipBoardOperand implements IManipulationOperand<Number>, ClipboardOwner {

	private int minLineIdx;
	private int minColIdx;
	private int lineNumber;
	private int colNumber;
	private Double[][] values;
	private String delim;

	public ClipBoardOperand(String delim){
		this.delim = delim;
	}

	public void initAsSink(int numberRows, int numberCols){

		values = new Double[numberRows][numberCols];
		minLineIdx = 0;
		minColIdx = 0;
		lineNumber = numberRows;
		colNumber = numberCols;
		
		System.out.println("clipboard matrix has numberRows "+numberRows);
		System.out.println("clipboard matrix has numberCols "+numberCols);
	}

	public void initAsSource() throws OperandException,NumberFormatException{

		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable clipData = clipboard.getContents(clipboard);
		
		minLineIdx = 0;
		minColIdx = 0;

		try {
			String s = (String) clipData.getTransferData(DataFlavor.stringFlavor);
			String[] lines = s.split("\n");
			
			//1.0;; hat als .split(delim) die laenge eins, deshalb vorverarbeitung
			for (int i = 0; i < lines.length; i++){
				lines[i] = lines[i].replaceAll(delim, delim + " ");	
			}

			if (lines.length > 0){
				colNumber = lines[0].split(delim).length;
				lineNumber = lines.length;
				
				System.out.println("clipboard matrix has #cols = "+colNumber);
				System.out.println("clipboard matrix has #lines = "+lineNumber);
				
				if (colNumber > 0){
					values = new Double[lineNumber][colNumber];
					
					for (int i = 0; i < lines.length; i++){
						String[] line = lines[i].split(delim);
						System.out.println(lines[i]+" has #tokens "+line.length);

						for (int j = 0; j < line.length; j++){
							System.out.println("line["+j+"] : '"+line[j]+"'");
							if (!line[j].trim().isEmpty()){
								values[i][j] = Double.parseDouble(line[j]);
//								System.out.println("setting "+values[i][j].doubleValue()+" at "+i+","+j);
							}
						}
					}
				} else {
//					throw new OperandException("Zwischenablage hat keine Zeilen");
					throw new OperandException(toString());
				}
			} else {
//				throw new OperandException("Zwischenablage-Zeilen lassen sich nicht splitten");
				throw new OperandException(toString());
			}

		} catch (UnsupportedFlavorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Number getValue(int x, int y) {
		return (Number)values[x][y];
	}

	public String toString(){
		return Localisation.getLocaleGuiTerm("OPERAND_CLIPBOARD")+" " +
				"('"+(delim.equals("\t") ? "tab" : delim)+"')";
	}

	public boolean shouldBeConsidered(int x, int y) {
		return true;
	}

	public void commitOperation() {
		
//		if (delim.equals("\t"))
//			delim = " ";

		StringBuffer s = new StringBuffer();
		for (int i = 0; i < values.length; i++){
			for (int j = 0; j < values[0].length; j++){

				//null-werte (also nicht uebertragene) -> nichts (";;") 
				if (values[i][j] != null)
					s.append(values[i][j] +"");

				if (j < values[0].length - 1)
					s.append(delim);
			}
			s.append("\r\n");
		}
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(s.toString()),this);

	}

	public void setValue(int x, int y, Number n) {

		values[x][y] = Double.parseDouble(n.toString());
//		System.out.println("set value "+values[x][y].doubleValue()+" at "+x+","+y);
	}

	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		// TODO Auto-generated method stub
	}

	public int getMinColumnIndex() {
		return minColIdx;
	}

	public int getMinLineIndex() {
		return minLineIdx;
	}

	public int getNumberOfColumns() {
		return colNumber;
	}

	public int getNumberOfLines() {
		return lineNumber;
	}

	public boolean isProjectableToColumn() {
		return colNumber == 1  ||  atMostOneValuePerLine();
	}

	private boolean atMostOneValuePerLine() {
		
		for (int i = minLineIdx; i < minLineIdx + lineNumber; i++){
			int valuesPerLine = 0;
			for (int j = minColIdx; j < minColIdx + colNumber; j++){
				if (values[i][j] != null)
					valuesPerLine++;
			}
			if (valuesPerLine > 1)
				return false;
		}
		
		return true;
	}

	public Number getFirstValueFromLine(int x) {
		
		for (int i = minColIdx; i < minColIdx + colNumber; i++){
			if (values[x][i] != null){
				return (Number)values[x][i];
			}
		}
		
		return null;
	}

	public boolean shouldBeConsidered(int i) {
		return true;
	}
}
