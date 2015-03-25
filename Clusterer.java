import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

/**
 * Clusterer divides groups of data points into clusters of arbitrary size.
 * @author Andrew Elenbogen, Quang Tran
 * @version February 23, 2015
 *
 */

public class Clusterer {
	private static final String DATAFILE_LOCATION = "/tmp/wp_namespace.txt";
	private ArrayList<DataPoint> data = new ArrayList<DataPoint>();
	private Random rand;
	private boolean normalize;
	public Clusterer(boolean normalize){
		rand = new Random();
		this.normalize = normalize;
		readFile();		
	}

	/**
	 * Reads the intial data file
	 */
	public void readFile(){
		try(Scanner scanner=new Scanner(new File(DATAFILE_LOCATION)))
		{
			scanner.nextLine();
			while (scanner.hasNextLine())
			{
				String[] split=scanner.nextLine().split("\t");
				//System.out.println(split.length);

				DataPoint dataPoint = new DataPoint(Integer.parseInt(split[split.length-4]),Integer.parseInt(split[split.length-3]),Integer.parseInt(split[split.length-2]), Integer.parseInt(split[split.length-1]));
				if (normalize){
					dataPoint.normalize();
				}
				else{
					dataPoint.convertToLog();
				}
				data.add(dataPoint);				
			}
		}
		catch (IOException e){
			System.out.println(e);
			System.exit(0);
		}
	}
	
	/**
	 * Generates starting cluster centers by selecting random point and iteratively finding the point furthest
	 * away from all current centers until it obtains k centers.
	 */
	private ArrayList<DataPoint> startClusterFancy(int k){
		ArrayList<DataPoint> centers = new ArrayList<DataPoint>();
		centers.add(data.get(rand.nextInt(data.size())));
		k--;
		for (int i = k; i > 0; i--){
			DataPoint largest = null;
			float largestDistance = 0;
			for (DataPoint dataPoint: data){
				if (centers.contains(dataPoint)){
					continue;
				}
				float total = 0;
				for (DataPoint center: centers){
					total += dataPoint.getSquaredEuclideanDistance(center);
				}
				if (largest == null || total > largestDistance){
					largestDistance = total;
					largest = dataPoint;
				}
			}
			centers.add(largest.copy());
		}
		return centers;
	}

	/**
	 * Generates k clusters by repeatedly adding data points to their nearest centers and then 
	 * moving the center of each cluster to the average of all points within it.
	 */
	public HashMap<DataPoint,ArrayList<DataPoint>> cluster(int k, boolean fancy){
		HashMap<DataPoint,ArrayList<DataPoint>> clusters = new HashMap<DataPoint,ArrayList<DataPoint>>();
		HashMap<DataPoint,ArrayList<DataPoint>> newClusters = new HashMap<DataPoint,ArrayList<DataPoint>>();

		if(!fancy)
		{
			while(clusters.keySet().size()<k)
			{
				clusters.put(data.get(rand.nextInt(data.size())).copy(),new ArrayList<DataPoint>());	
			}
		}
		else{
			for (DataPoint point: startClusterFancy( k))
				clusters.put(point, new ArrayList<DataPoint>());
		}
			

		boolean changes = true;


		while (changes)
		{
			newClusters=assignPointsToCluster(clusters);
			removeEmptyClusters(clusters, newClusters);

			clusters = newClusters;
			newClusters = new HashMap<DataPoint,ArrayList<DataPoint>>();


			if(!changes)
				break;
			//Assigns centers to be the average of all points that are relevant to them
			for (DataPoint center: clusters.keySet()){
				DataPoint newCenter = new DataPoint(0,0,0,0);
				for (DataPoint current: clusters.get(center)){
					for (String key: current.getMap().keySet()){
						newCenter.put(key, newCenter.get(key) + current.get(key));
					}
				}
				newCenter.divideAll(clusters.get(center).size());
				newClusters.put(newCenter, clusters.get(center));
			}
			changes=!clusters.equals(newClusters);
			clusters = newClusters;	
			System.out.println("SSE: " + calcSSE(clusters));
		}
		if (!normalize){
			newClusters = new HashMap<DataPoint,ArrayList<DataPoint>>();
			for (DataPoint center: clusters.keySet())
			{
				for (DataPoint dataPoint: clusters.get(center)){
					dataPoint.deLogify();
				}
				ArrayList<DataPoint> clusterPoints=clusters.get(center);
				//center.deLogify();
				newClusters.put(center, clusterPoints);	
			}
			clusters=newClusters;
		}
		return clusters;
	}

	/**
	 * Assigns all points using the given cluster centers to their respective clusters.
	 */
	private HashMap<DataPoint,ArrayList<DataPoint>> assignPointsToCluster(HashMap<DataPoint,ArrayList<DataPoint>> clusters)
	{
		HashMap<DataPoint,ArrayList<DataPoint>> newClusters= new HashMap<DataPoint,ArrayList<DataPoint>>();
		for (DataPoint dataPoint: data)
		{
			float minDistance = 0;
			DataPoint closest = null;
			for (DataPoint center: clusters.keySet()){
				if (closest == null || dataPoint.getSquaredEuclideanDistance(center) < minDistance){
					minDistance = dataPoint.getSquaredEuclideanDistance(center);
					closest = center;
				}				
			}
			if(newClusters.get(closest)==null)
				newClusters.put(closest, new ArrayList<DataPoint>());
			newClusters.get(closest).add(dataPoint);				
		}

		return newClusters;
	}

	
	/**
	 * Replaces empty cluster centers with the points which are furthest away from their respective centers, 
	 * assigns points to centers. 
	 * Repeats this process until no empty clusters exists.
	 */
	private void removeEmptyClusters(HashMap<DataPoint,ArrayList<DataPoint>> clusters, HashMap<DataPoint,ArrayList<DataPoint>> newClusters)
	{
		int count=clusters.keySet().size()-newClusters.keySet().size();

		if(count==0)
			return;


		HashMap<Float, DataPoint> distanceMap=new HashMap<Float, DataPoint>();

		for(DataPoint center: newClusters.keySet())
		{
			for(DataPoint point: newClusters.get(center))
			{
				if(!newClusters.keySet().contains(point))
				{
					distanceMap.put(point.getSquaredEuclideanDistance(center), point);
				}
			}
		}
		ArrayList<Float> keys=new ArrayList<Float>(distanceMap.keySet());
		Collections.sort(keys);

		for(int i=0; i<count; i++)
		{
			newClusters.put(distanceMap.get(keys.get(i)).copy(), new ArrayList<DataPoint>());
		}

		HashMap<DataPoint,ArrayList<DataPoint>> newerClusters=assignPointsToCluster(newClusters);
		System.out.println(newerClusters.get(new DataPoint(1, 0, 0, 1)));
		removeEmptyClusters(newClusters, newerClusters);
		newClusters=newerClusters;
	}

	/**
	 * Calculates and returns the SSE of the given clusters
	 */
	private float calcSSE(HashMap<DataPoint, ArrayList<DataPoint>> clusters){
		float sse = 0;
		for (DataPoint currentCenter: clusters.keySet()){
			for (DataPoint data: clusters.get(currentCenter)){
				sse += data.getSquaredEuclideanDistance(currentCenter);
			}
		}
		return sse;

	}

	public static void main(String[] args)
	{
		/*DataPoint point= new DataPoint(100f, 20f, 7f, .4f);
		point.normalize();
		System.out.println(point);
		System.out.println(point.getSquaredEuclideanDistance(new DataPoint(0f, 0f, 0f, 0f)));*/
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Input k: ");
		
		int k = scanner.nextInt();
		System.out.println("Fancy intialization? (y/n): ");
		boolean fancy=(scanner.next().trim().equals("y"));
		System.out.println("Normalize? (y/n): ");
		Clusterer clusterer = new Clusterer((scanner.next().trim().equals("y")));
		HashMap<DataPoint,ArrayList<DataPoint>> result=clusterer.cluster(k, fancy);
		
		//Sorts the centers so they appear in order of their main values
		ArrayList<DataPoint> sortedKeySet= new ArrayList<DataPoint>(result.keySet());
		Collections.sort(sortedKeySet, new Comparator<DataPoint>() 
		{
			@Override
			public int compare(DataPoint one, DataPoint two)
			{
				return (int) (Math.round(one.get("main")-two.get("main")));
			}
		});
		for(DataPoint center: sortedKeySet)
		{
			System.out.print(center+"\t");
		}
		scanner.close();
	}
}
