package java_sql_lib_raymond;
public class TableVar
{
	private String name, type, modifiers[];

	public TableVar(String name, String type, String... modifiers)
	{
		this.name = name;
		this.type = type;
		this.modifiers = modifiers.clone();
	}

	public String getName()
	{
		return name;
	}

	public String toString()
	{
		StringBuilder result = new StringBuilder();

		result.append(name);
		result.append(" ");
		result.append(type);
		result.append(" ");

		for(String modifier : modifiers)
		{
			result.append(modifier);
			result.append(" ");
		}

		return result.toString().trim();
	}
}