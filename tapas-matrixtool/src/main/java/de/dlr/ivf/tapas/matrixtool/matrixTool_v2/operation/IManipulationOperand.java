package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

public interface IManipulationOperand<Number> {

	public int getMinLineIndex();
	
	public int getNumberOfLines();
	
	public int getMinColumnIndex();
	
	public int getNumberOfColumns();
	
	/*
	 * pro zeile hoechstens 1 werte -> kann auf spalte projeziert werden.
	 */
	public boolean isProjectableToColumn();
	
	/*
	 * fuer einfaches iterieren bei spalten-operationen
	 */
	public Number getFirstValueFromLine(int x);

	/*
	 * fuer den fall, dass dieser operand manipuliert werden soll, soll dadurch
	 * bestimmbar sein, ob in dem groben raum (rows x columns) der wert bei [x][y]
	 * ueberhaupt gesetzt werden darf. dies ist also eine verfeinerung, die ueber
	 * die kriterien rows und columns hinausgeht (z.b. durch value-ranges).
	 */
	public boolean shouldBeConsidered(int x, int y);
	
	/*
	 * bei spalten-operationen muss ja auch geprueft werden, ob die zeile betrachtet
	 * werden muss
	 */
	public boolean shouldBeConsidered(int i);
	
	public Number getValue(int x, int y);
	
	public void setValue(int x, int y, Number n);

	/*
	 * z.b. zum veranlassen von model.doNotify() oder kopiere matrix in die
	 * zwischenablage
	 */
	public void commitOperation();
	
	/*
	 * beschreibung in der gui
	 */
	public String toString();
}
