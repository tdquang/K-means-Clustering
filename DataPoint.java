import java.util.HashMap;

/**
 * This class represents a single 4-dimensional datapoint
 *@author Andrew Elenbogen, Quang Tran
 * @version February 23, 2015
 *
 */
public class DataPoint 
{
	
	private HashMap<String, Float> fields;
	
	public DataPoint(float main, float talk, float user, float usertalk) 
	{
		fields=new HashMap<String, Float>();
		fields.put("main", main);
		fields.put("talk", talk);
		fields.put("user", user);
		fields.put("usertalk", usertalk);
	}
	
	/**
	 * @return A deep copy of this DataPoint
	 */
	public DataPoint copy()
	{
		return new DataPoint(fields.get("main"), fields.get("talk"), fields.get("user"), fields.get("usertalk") );
	}

	/**
	 * Returns the field of the DataPoint associated with the given String
	 */
	public Float get(String name)
	{
		return fields.get(name);
	}
	
	/**
	 * Sets each field of the point, x, to be log(x+1) with a base of 2.
	 */
	public void convertToLog() 
	{
		for(String key: fields.keySet())
		{
			fields.put(key, (float) (Math.log((double) (fields.get(key) + 1.0f) )/Math.log(2)));
		}		
	}
	
	/**
	 * Gets the squared Euclidean distance of this point to the other point.
	 */
	public float getSquaredEuclideanDistance(DataPoint otherPoint) 
	{
		float total=0;
		for(String key: fields.keySet())
		{
			total+=(Math.pow((this.get(key) - otherPoint.get(key)), 2));
		}
		return total;
	}
	
	/**
	 * Normalizes the point
	 */
	public void normalize(){
		float total = 0;
		for(float value: fields.values()){
			total += Math.pow(value, 2);
		}
		total = (float) Math.sqrt(total);
		for(String key: fields.keySet()){
			fields.put(key, (float) fields.get(key)/total);
		}
	}
	/**
	 * Returns whether or not this point is the same as the other Object.
	 */
	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof DataPoint)){
			return false;
		}
		DataPoint otherPoint = (DataPoint) other;
		for(String key: fields.keySet())
		{
			if(!this.get(key).equals(otherPoint.get(key)))
				return false;
		}
		return true;
	}
	/**
	 * @return This points map of field names to values.
	 */
	public HashMap<String, Float> getMap(){
		return fields;
	}
	
	/**
	 * Adds the given field to this point with the given name
	 */
	public void put(String name, Float value)
	{
		fields.put(name, value);
	}
	
	/**
	 * Divides all fields by the given number.
	 */
	public void divideAll(float num){
		for (String key: fields.keySet()){
			fields.put(key, fields.get(key)/num);
		}
	}
	
	/**
	 * Used for hashing DataPoints, required to store them well in a HashMap
	 */
	@Override
	public int hashCode()
	{
		return (int) (fields.get("main")+10*fields.get("talk")+100*fields.get("user")+1000*fields.get("usertalk"));
	}
	
	/**
	 * Undoes the converToLog procedure.
	 */
	public void deLogify(){
		for(String key: fields.keySet())
		{
			fields.put(key, (float) (Math.pow(2, fields.get(key))-1));
		}
	}
	/**
	 * Returns a nicely formatted summary of the DataPoint
	 */
	public String toString()
	{
		String summary="";
		
		for(String key: fields.keySet())
		{
			summary+=key+":"+fields.get(key)+"\t";
		}
		return summary;
	}
	
	/**
	 * Returns a nicely formatted summary of the DataPoint ready-made for pasting into Excel.
	 */
	public String toStringForTable()
	{
		{
			String summary="";
			String[] ordered={"main", "talk", "user", "usertalk"};
			
			for(String key: ordered)
			{
				summary+=fields.get(key)+"\t";
			}
			return summary;
		}
	}
}
