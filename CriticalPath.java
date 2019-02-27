import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * This class takes in an a file of an adjacency matrix as an argument and
 * returns the Earliest Completion, Latest Completion, and Slack times. This is
 * the parent class.
 * 
 * @author conor cook
 *
 */
public class CriticalPath {
	private static ArrayList<String> nodeNames = new ArrayList<String>();
	private static ArrayList<Integer> allMatrixValues = new ArrayList<Integer>();
	private static List<String>[] dependentNodes;
	private static int[] mainVal;
	private static int maxCost;
	//Format for the final string output
	private static String format = "%1$-20s %2$-6s %3$-9s %4$-5s\n";

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Please input activity-node graph.");
			System.exit(0);
		}

		File input = new File(args[0]);
		parseFile(input);
		HashSet<Node> allTasks = new HashSet<Node>();
		Node[] nodes = new Node[nodeNames.size()];
		
		for (int i = 0; i < nodeNames.size(); i++) {
			nodes[i] = new Node(nodeNames.get(i), mainVal[i]);
		}
		for (int i = 0; i < nodeNames.size(); i++) {
			Node[] nodeList = new Node[dependentNodes[i].size()];
			for (int j = 0; j < dependentNodes[i].size(); j++) {
				for (int k = 0; k < nodeNames.size(); k++) {
					if (dependentNodes[i].get(j) == nodes[k].getName()) {
						nodeList[j] = nodes[k];
					}
				}
			}
			nodes[i].setDependsOn(nodeList);
			allTasks.add(nodes[i]);
		}
		Node[] result = criticalPath(allTasks);
		print(result);
		System.out.println("\n(Printed in Alphabetical order.)");
	}
	
	/**
	 * This is the main method that does most of the work. This calculates the
	 * values of running through the path finding the critical path
	 * 
	 * @param nodes
	 * @return sol
	 */
	private static Node[] criticalPath(Set<Node> nodes) {
		HashSet<Node> done = new HashSet<Node>();
		HashSet<Node> remaining = new HashSet<Node>(nodes);
		boolean progress = false;

		while (!remaining.isEmpty()) {
			progress = false;
			for (Iterator<Node> iter = remaining.iterator(); iter.hasNext();) {
				Node node = iter.next();
				if (done.containsAll(node.dependsOn)) {
					int crit = 0;
					for (Node n : node.dependsOn) {
						if (n.critCost > crit) {
							crit = n.critCost;
						}
					}
					node.critCost = crit + node.cost;
					done.add(node);
					iter.remove();
					progress = true;
				}
			}
			if (!progress) {
				throw new RuntimeException("Negative Cycle, program has been stopped.");
			}
		}

		maxCost(nodes);
		HashSet<Node> initNodes = initials(nodes);
		calcEarly(initNodes);
		Node[] sol = done.toArray(new Node[0]);
		
		//////////////////////////////////////////////////////////
		//If you want to sort alphabetically you can use this sort
		//@conor cook
		//////////////////////////////////////////////////////////
		 Arrays.sort(sol, new Comparator<Node>() {
		 public int compare(Node x, Node y) {
		 return x.name.compareTo(y.name);
		 }
		 });
		//////////////////////////////////////////////////////////
		
		return sol;
	}

	/**
	 * Simple helper method that parses the file containing the adjacency matrix and
	 * calls another method to parse the arrays that are created.
	 * 
	 * @param file
	 */
	private static void parseFile(File file) {
		try {
			Scanner scan = new Scanner(file);
			Scanner nameScan = new Scanner(scan.nextLine());
			while (nameScan.hasNext()) {
				if (!nodeNames.contains(nameScan.hasNext())) {
					nodeNames.add(nameScan.next());
				}
			}
			while (scan.hasNextLine()) {
				for (int i = 0; i < nodeNames.size(); i++) {
					Scanner valueScan = new Scanner(scan.nextLine());
					valueScan.next();
					while (valueScan.hasNext()) {
						allMatrixValues.add(Integer.parseInt(valueScan.next()));
					}
				}
			}
			ArrayList<Integer> temp = new ArrayList<Integer>(allMatrixValues);
			temp = allMatrixValues;
			parseArray(temp);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("File Not Found.");
			System.exit(0);
		}
	}

	/**
	 * This is the helper method that parses the arrays created in the parseFile
	 * method
	 * 
	 * @param input
	 */
	private static void parseArray(ArrayList input) {
		int index = 0;
		int mod = nodeNames.size();
		dependentNodes = new List[nodeNames.size()];
		mainVal = new int[nodeNames.size()];

		for (int i = 0; i < nodeNames.size(); i++) {
			dependentNodes[i] = new ArrayList<>();

			for (int j = 0; j < nodeNames.size(); j++) {
				if (allMatrixValues.get(index) != -1) {
					if (i != 0) {
						dependentNodes[i].add(nodeNames.get(j));
					}
					mainVal[index % mod] = allMatrixValues.get(index);
				}
				index++;
			}
		}
	}

	/**
	 * Helper method that calculates the early costs for the list of nodes entered
	 * in
	 * 
	 * @param inits
	 */
	private static void calcEarly(HashSet<Node> initialNodes) {
		for (Node inits : initialNodes) {
			inits.earlyStart = 0;
			inits.earlyFinish = inits.cost;
			setEarly(inits);
		}
	}

	/**
	 * Helper method to set the early times for the node
	 * 
	 * @param init
	 */
	private static void setEarly(Node initialNode) {
		int compTime = initialNode.earlyFinish;
		for (Node n : initialNode.dependsOn) {
			if (compTime >= n.earlyStart) {
				n.earlyStart = compTime;
				n.earlyFinish = compTime + n.cost;
			}
			setEarly(n);
		}
	}

	/**
	 * Method for setting the initial array of Nodes to walk the critical path
	 * 
	 * @param nodes
	 * @return remaining
	 */
	private static HashSet<Node> initials(Set<Node> nodes) {
		HashSet<Node> remaining = new HashSet<Node>(nodes);
		for (Node n : nodes) {
			for (Node nd : n.dependsOn) {
				remaining.remove(nd);
			}
		}
		return remaining;
	}

	/**
	 * Helper method that calculations the maximum cost for each node
	 * 
	 * @param tasks
	 */
	private static void maxCost(Set<Node> nodes) {
		int max = -1;
		for (Node n : nodes) {
			if (n.critCost > max) {
				max = n.critCost;
			}
		}
		maxCost = max;
		
		//////////////////////////////////////////////////////////
		//If you want the program to print critical path length
		// use this print call.   @conorcook
		//////////////////////////////////////////////////////////
		// System.out.println("Critical Path Length: " + maxCost);
		//////////////////////////////////////////////////////////
		
		for (Node n : nodes) {
			n.setLatest();
			if (n.name == nodeNames.get(0)) {
				n.setLatestStart();
			}
		}
	}

	/**
	 * Simple helper method to print all of the information about the nodes
	 * 
	 * @param nodes
	 */
	private static void print(Node[] nodes) {
		System.out.println("Activity Node        EC     LC     SlackTime");
		System.out.println("---------------------------------------------");
		for (Node n : nodes) {
			System.out.format(format, (Object[]) n.toStringArray());
		}
	}
	
	/**
	 * This is the child class creating Node objects to represent the activity node
	 * graph interpreted from the adjacency matrix
	 * 
	 * @author conor cook
	 *
	 */
	private static class Node {
		private int cost, critCost, earlyStart, earlyFinish, lateStart, lateFinish;
		private String name;
		private HashSet<Node> dependsOn = new HashSet<Node>();

		/**
		 * Constructor for Node class
		 * 
		 * @param name
		 * @param cost
		 */
		private Node(String name, int cost) {
			this.name = name;
			this.cost = cost;
			this.earlyFinish = -1;
		}

		/**
		 * Simple helper method to get name of the node
		 * 
		 * @return this.name
		 */
		private String getName() {
			return this.name;
		}
		
		/**
		 * Setter Method to set the dependencies for each node
		 * 
		 * @param dependsOn
		 */
		private void setDependsOn(Node... dependsOn) {
			for (Node n : dependsOn) {
				this.dependsOn.add(n);
			}
		}
		
		/**
		 * Helper method that returns if the node called is dependent upon another node
		 * 
		 * @param n
		 * @return true/false
		 */
		private boolean isDependent(Node n) {
			if (dependsOn.contains(n)) {
				return true;
			}
			for (Node depNodes : dependsOn) {
				if (depNodes.isDependent(n)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Helper method to set LateStart and LateFinish values
		 */
		private void setLatest() {
			lateStart = maxCost - critCost;
			lateFinish = lateStart + cost;
		}

		/**
		 * Helper method to set Late values on starting Node
		 */
		private void setLatestStart() {
			lateFinish = 0;
			lateStart = 0;

		}

		/**
		 * Method that creates a new toString for the node
		 * 
		 * @return toString
		 */
		private String[] toStringArray() {
			String[] toString = { name, earlyFinish + "", lateFinish + "", lateStart - earlyStart + "" };
			return toString;
		}
	}
}