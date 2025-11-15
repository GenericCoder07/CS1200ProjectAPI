package java_sql_lib_raymond;
import java.util.HashMap;
import java.util.function.Consumer;

public class Table
{
	private String name;
	private HashMap<String, TableVar> tableVarNameMap;
	private TableVar[] tableVars;
	public Table(String name, TableVar... tableVars)
	{
		this.name = name;
		this.tableVars = tableVars;

		tableVarNameMap = new HashMap<>();

		for(TableVar tableVar : tableVars)
			tableVarNameMap.put(tableVar.getName(), tableVar);
	}

	public String getName()
	{
		return name;
	}

	public TableVar getTableVar(int index)
	{
		return tableVars[index];
	}

	public TableVar getTableVar(String name)
	{
		return tableVarNameMap.get(name);
	}

	public void forEach(Consumer<TableVar> func)
	{
		for(TableVar tableVar : tableVars)
			func.accept(tableVar);
	}

	public TableVar[] getTableVars()
	{
		return tableVars;
	}
}