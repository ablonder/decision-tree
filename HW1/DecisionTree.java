import java.io.*;
import java.util.*;

/*
 * Creates and tests a decision tree based on a given data set.
 * @author Aviva Blonder
 */

public class DecisionTree {
    static ArrayList<ArrayList<String>> dataset = new ArrayList<ArrayList<String>>();
    static String algorithm;
    static ArrayList<String> attributes;
    Tree tree;

    public DecisionTree(ArrayList<ArrayList<String>> trainset, ArrayList<String> attrs) {
	// Making the tree.
	tree = new Tree(trainset, attrs);
    }

    public void test(ArrayList<ArrayList<String>> testset) {
	// Goes through each instance in testset, predicts its label, and output a confusion matrix.
	String label;
	String actual;
	ArrayList<String> labels = new ArrayList<String>();
	String matrix = "";
	HashMap<String, HashMap<String, Integer>> confusion = new HashMap<String, HashMap<String, Integer>>();
	for(ArrayList<String> instance : testset) {
	    actual = instance.get(0);
	    label = tree.predict(instance);
	    if(label != null) {
		HashMap<String, Integer> accuracy;
		if(!confusion.containsKey(actual)) {
		    accuracy = new HashMap<String, Integer>();
		    labels.add(actual);
		    matrix += actual + ",";
		} else {
		    accuracy = confusion.get(actual);
		}
		if(!accuracy.containsKey(label)) {
		    accuracy.put(label, 1);
		    if(!labels.contains(label)) {
			labels.add(label);
			matrix += actual + ",";
		    }
		} else {
		    accuracy.put(label, accuracy.get(label)+1);
		}
		confusion.put(actual, accuracy);
	    }
	}
	// Formats confusion matrix as a string.
	for(String actl : labels) {
	    matrix += "\n";
	    boolean cont = confusion.containsKey(actl);
	    for(String predl : labels) {
		if(!cont || !confusion.get(actl).containsKey(predl)) {
		    matrix += "0,";
		} else {
		    matrix += confusion.get(actl).get(predl) + ",";
		}
	    }
	    matrix += actl;
	}
	try {
	    FileWriter output = new FileWriter("ConfusionMatrix.csv");
	    output.append(matrix);
	    output.close();
	} catch(IOException e) {
	    System.out.println("This really shouldn't be throwing this exception, it's supposed to create a new file, not find one.");
	}
    }

    public HashMap<String, Integer> valuecounter(ArrayList<ArrayList<String>> trainset, int attribute){
	// Counts up how often each value of a given attribute occurs.
	HashMap<String, Integer> labelcount = new HashMap<String, Integer>();
	for(ArrayList<String> instance : trainset) {
	    if(!labelcount.containsKey(instance.get(attribute))) {
		labelcount.put(instance.get(attribute), 1);
	    } else {
		int prev = labelcount.get(instance.get(attribute));
		labelcount.put(instance.get(attribute), prev+1);
	    }
	}
	return labelcount;
    }

    private class Tree{
	/*
	 * The actual tree structure.
	 */
	Boolean leaf;
	String attr;
	int atr;
	String label;
	HashMap<String, Tree> children;

	private Tree(ArrayList<ArrayList<String>> trainset, ArrayList<String> attrs) {
	    // Counting up how often each label occurs (useful for multiple different cases).
	    HashMap<String, Integer> labelcount = valuecounter(trainset, 0);
	    if(attrs.size() == 1) {
		// If we've used up all the possible attributes, just find the most common label and choose it.
		// Finding the most common label.
		int best = 0;
		String bestkey = null;
		int count;
		for(String key : labelcount.keySet()) {
		    count = labelcount.get(key);
		    if(count > best) {
			best = count;
			bestkey = key;
		    }
		}
		// Turn this into a leaf predicting the most common label.
		leaf = true;
		attr = attrs.get(0);
		label = bestkey;
		//System.out.println(label);
	    } else if(labelcount.keySet().size() == 1) {
		// If all of the remaining instances have the same label, turn this into a leaf predicting that label.
		leaf = true;
		attr = attrs.get(0);
		label = (String) (labelcount.keySet().toArray()[0]);
		//System.out.println(label);
	    } else {
		leaf = false;
		// Otherwise, calculate entropy for each attribute and turn this into a node for the least entropic
		double best = 1;
		int bestattr = 0;
		HashMap<String, ArrayList<ArrayList<String>>> split = new HashMap<String, ArrayList<ArrayList<String>>>();
		for(int i = 1; i < attrs.size(); i++) {
		    HashMap<String, ArrayList<ArrayList<String>>> splitset = new HashMap<String, ArrayList<ArrayList<String>>>();
		    for(ArrayList<String> instance : trainset) {
			// For each instance, add it to splitset according to the value of the attribute designated by i
			if(!splitset.containsKey(instance.get(i))) {
			    splitset.put(instance.get(i), new ArrayList<ArrayList<String>>());
			}
			splitset.get(instance.get(i)).add(instance);
		    }
		    double totalH = 0;
		    HashMap<String, Integer> labels = new HashMap<String, Integer>();
		    for(String value : splitset.keySet()) {
			// For each value of the designated attribute, calculate weighted entropy and add to totalH
			int attrcount = splitset.get(value).size();
			labels = valuecounter(splitset.get(value), 0);
			double entropy = 0;
			for(String label : labels.keySet()) {
			    // For each label, calculate p(label)log2p(label)
			    double plabel = (double) (labels.get(label))/attrcount;
			    entropy += plabel*Math.log(plabel)/Math.log(2);
			}
			totalH += -1*entropy*attrcount/trainset.size();
		    }
		    if(totalH < best) {
			best = totalH;
			bestattr = i;
			split = splitset;
		    }
		}
		// Designate this node as bestattr
		attr = attrs.get(bestattr);
		atr = bestattr;
		// Remove bestattr from attrs.
		attrs.remove(bestattr);
		// Remove bestattr from each instance, add each value of the chosen attribute to children and recurse.
		children = new HashMap<String, Tree>();
		ArrayList<ArrayList<String>> subset;
		for(String val : split.keySet()) {
		    subset = split.get(val);
		    for(ArrayList<String> inst : subset) {
			inst.remove(bestattr);
		    }
		    //System.out.print(attr + " - " + val);
		    children.put(val, new Tree(subset, new ArrayList<String>(attrs)));
		}
	    }
	}

	private String predict(ArrayList<String> instance) {
	    // Predicts the label of instance as label if leaf, or by recursing on children otherwise
	    if(leaf) {
		return label;
	    } else {
		String val = instance.remove(atr);
		if(children.containsKey(val)) {
		    return children.get(val).predict(instance);
		} else {
		    return null;
		}
	    }
	}
    }

    public static void main(String[] args){
	/*
	 * Reads in provided data set, splits it into test and training sets according to a given seed,
	 * and creates a decision tree using the designated algorithm.
	 */

	// Test to make sure all the arguments are present.
	if(args.length < 3) {
	    System.out.println("Needs a file, algorithm, and seed.");
	    System.exit(1);
	} else if(args[1].equals("ID3") || args[1].equals("C4.5") || args[1].equals("CART")) {
	    // Test to make sure the second argument is a valid algorithm.
	    algorithm = args[1];
	} else {
	    System.out.println("The second argument must designate an algorithm. Your choices are ID3, C4.5, and CART.");
	    System.exit(1);
	}

	int seed = 0;
	try {
	    // Test to make sure the third argument is a number for the seed.
	    seed = Integer.parseInt(args[2]);
	} catch(NumberFormatException e) {
	    System.out.println("The third argument must be an integer to designate the random seed.");
	    System.exit(1);
	}

	Scanner readSet;
	// Make sure the first argument is a file.
	try{
	    // Scan through the provided file and add each line (instance) to dataset.
	    readSet = new Scanner(new File(args[0]));
	    String line = readSet.nextLine();
	    attributes = new ArrayList<String>(Arrays.asList(line.split(",")));
	    while(readSet.hasNextLine()) {
		line = readSet.nextLine();
		dataset.add(new ArrayList<String>(Arrays.asList(line.split(","))));
	    }
	} catch(FileNotFoundException e){
	    System.out.println("File '" + args[0] + "' not found! Try again.");
	    System.exit(1);
	}

	// Split dataset into trainset and testset based on split (proportion in testset) using seed.
	double split = .2;
	int testsize = (int) (dataset.size()*split);
	int trainsize = dataset.size() - testsize;
	ArrayList<ArrayList<String>> testset = new ArrayList<ArrayList<String>>(testsize);
	ArrayList<ArrayList<String>> trainset = new ArrayList<ArrayList<String>>(trainsize);
	Random rndgen = new Random(seed);
	while(testset.size() < testsize) {
	    // Fill testset with randomly generated indices.
	    int next = rndgen.nextInt(dataset.size());
	    if(!testset.contains(dataset.get(next))) {
		testset.add(dataset.get(next));
	    }
	}
	for(int i = 0; i < dataset.size(); i++) {
	    // Fill trainset with the rest.
	    if(!testset.contains(dataset.get(i))) {
		trainset.add(dataset.get(i));
	    }
	}

	// Actually make and train the decision tree.
	DecisionTree learner = new DecisionTree(trainset, new ArrayList<String>(attributes));
	// Testing the tree
	learner.test(testset);
    }
}