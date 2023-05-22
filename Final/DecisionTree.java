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
    static boolean pruning = true;
    static boolean splitinfo = true;
    Tree tree;
    HashMap<HashMap<String, ArrayList<String>>, ArrayList<String>> ruleset;
    ArrayList<HashMap<String, ArrayList<String>>> rules;

    public DecisionTree(ArrayList<ArrayList<String>> trainset, ArrayList<String> attrs) {
	// Making the tree.
	tree = new Tree(trainset, attrs, null, new HashMap<ArrayList<String>, Float>(), 0);
    }

    public void prune(ArrayList<ArrayList<String>> validation) {
	/**
	 * Turn the tree into rules and loop through the elements of each rule to determine which are necessary
	 */
	ruleset = new HashMap<HashMap<String, ArrayList<String>>, ArrayList<String>>();
	tree.toRules(new HashMap<String, ArrayList<String>>());
	
	ArrayList<HashMap<String, ArrayList<String>>> temprules = new ArrayList<HashMap<String, ArrayList<String>>>(ruleset.keySet());

	for(HashMap<String, ArrayList<String>> rule : temprules) {
	    String label = ruleset.get(rule).get(0);
	    HashMap<String, ArrayList<String>> bestRule = rule;
	    double accuracy = ruleAccCalc(validation, rule, label);
	    ruleset.get(rule).add(accuracy + "");
	    boolean done = false;
	    while(!done) {
		done = true;
		for(String attr : rule.keySet()) {
		    HashMap<String, ArrayList<String>> newRule = new HashMap<String, ArrayList<String>>(rule);
		    newRule.remove(attr);
		    double newAcc = ruleAccCalc(validation, newRule, label);
		    if(newAcc >= accuracy) {
			done = false;
			accuracy = newAcc;
			bestRule = newRule;
		    }
		}
		if(!done) {
		    ArrayList<String> temp = new ArrayList<String>(ruleset.remove(rule));
		    temp.set(1, accuracy + "");
		    ruleset.put(bestRule, temp);
		    rule = bestRule;
		}
	    }
	}
	// Sort rules by accuracy into a list.
	rules = new ArrayList<HashMap<String, ArrayList<String>>>();
	for(HashMap<String, ArrayList<String>> rule : ruleset.keySet()) {
	    if(rules.isEmpty()) {
		rules.add(rule);
	    } else {
		int i = 0;
		boolean done = false;
		while(!done && i < rules.size()) {
		    float oldAcc = Float.parseFloat(ruleset.get(rules.get(i)).get(1));
		    float newAcc = Float.parseFloat(ruleset.get(rule).get(1));
		    if(newAcc > oldAcc) {
			rules.add(i, rule);
			done = true;
		    }
		    i++;
		}
		if(!done) {
		    rules.add(rule);
		}
	    }
	}
    }

    private double ruleAccCalc (ArrayList<ArrayList<String>> validation, HashMap<String, ArrayList<String>> rule, String label) {
	/**
	 * Calculate the overall accuracy of a rule based on the given validation set
	 */
	double correct = 0;
	for(ArrayList<String> instance : validation) {
	    String actual = instance.get(0);
	    double match = 1;
	    for(String attr : rule.keySet()) {
		String val = rule.get(attr).get(0);
		String value = instance.get(attributes.indexOf(attr));
		if(value.equals("?")) {
		    // Handle missing values
		    match *= Float.parseFloat(rule.get(attr).get(1));
		} try {
		    float threshold = Float.parseFloat(val.substring(1));
		    float fvalue = Float.parseFloat(value);
		    if(fvalue > threshold && val.charAt(0) == '<') {
			match = 0;
		    } else if (fvalue <= threshold && val.charAt(0) == '>') {
			match = 0;
		    }
		} catch (NumberFormatException e) {
		    if(!value.equals(val)){
			match = 0;
		    }
		}
	    }
	    if(label.equals(actual)) {
		    correct += match;
	    } else if(match == 0){
		correct += 1;
	    }
	}
	return correct/validation.size();
    }

    public void test(ArrayList<ArrayList<String>> testset, String file, String seed, String alg) {
	// Goes through each instance in testset, predicts its label, and output a confusion matrix.
	String label;
	String actual;
	ArrayList<String> labels = new ArrayList<String>();
	String matrix = "";
	HashMap<String, HashMap<String, Integer>> confusion = new HashMap<String, HashMap<String, Integer>>();
	for(ArrayList<String> instance : testset) {
	    actual = instance.get(0);
	    label = null;
	    if(algorithm.equals("C4.5")) {
		if(!pruning) {
		    ruleset = new HashMap<HashMap<String, ArrayList<String>>, ArrayList<String>>();
		    tree.toRules(new HashMap<String, ArrayList<String>>());
		    rules = new ArrayList<HashMap<String, ArrayList<String>>>();
		    for(HashMap<String, ArrayList<String>> rule : ruleset.keySet()) {
			rules.add(rule);
		    }
		 }
		label = predictRule(instance);
	    } else {
		label = tree.predict(instance);
	    }
	    if(label != null) {
		HashMap<String, Integer> accuracy;
		if(!confusion.containsKey(actual)) {
		    accuracy = new HashMap<String, Integer>();
		    if(!labels.contains(actual)) {
			labels.add(actual);
			matrix += actual + ",";
		    }
		} else {
		    accuracy = confusion.get(actual);
		}
		if(!accuracy.containsKey(label)) {
		    accuracy.put(label, 1);
		    if(!labels.contains(label)) {
			labels.add(label);
			matrix += label + ",";
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
	    FileWriter output = new FileWriter(alg + file + seed + ".csv");
	    output.append(matrix);
	    output.close();
	} catch(IOException e) {
	    System.out.println("This really shouldn't be throwing this exception, it's supposed to create a new file, not find one.");
	}
    }

    public String predictRule(ArrayList<String> instance) {
	/**
	 * Goes through rules, determines if one matches instance and returns the corresponding label, returns null otherwise.
	 */
	HashMap<String, Double> partialCounts = new HashMap<String, Double>();
	for(HashMap<String, ArrayList<String>> rule : rules) {
	    double match = 1;
	    for(String attr : rule.keySet()) {
		String val = rule.get(attr).get(0);
		String value = instance.get(attributes.indexOf(attr));
		if(value.equals("?")) {
		    match *= Float.parseFloat(rule.get(attr).get(1));
		}else if(val.charAt(0) == '>' || val.charAt(0) == '<') {
		    float threshold = Float.parseFloat(val.substring(1));
		    float fvalue = Float.parseFloat(value);
		    if(fvalue > threshold && val.charAt(0) == '<') {
			match = 0;
		    } else if(fvalue <= threshold && val.charAt(0) == '>') {
			match = 0;
		    }
		}else if(!value.equals(val)) {
		    match = 0;
		    break;
		}
	    }
	    if(match == 1) {
		return ruleset.get(rule).get(0);
	    } else if(match > 0) {
		// If the rule is a partial match, add the weight to the partial count for that label
		String label = ruleset.get(rule).get(0);
		if(!partialCounts.containsKey(label)) {
		    partialCounts.put(label, match);
		} else {
		    double temp = partialCounts.get(label) + match;
		    partialCounts.put(label, temp);
		}
	    }
	}
	double best = 0;
	String bestl = null;
	for(String label : partialCounts.keySet()) {
	    // If there hasn't been a perfect match, return the label with the best vote
	    if(partialCounts.get(label) > best) {
		best = partialCounts.get(label);
		bestl = label;
	    }
	}
	return bestl;
    }

    public HashMap<String, Integer> valuecounter(ArrayList<ArrayList<String>> trainset, int attribute){
	// Counts up how often each value of a given attribute occurs.
	HashMap<String, Integer> labelcount = new HashMap<String, Integer>();
	if(trainset != null) {
	    for(ArrayList<String> instance : trainset) {
		if(!labelcount.containsKey(instance.get(attribute))) {
		    labelcount.put(instance.get(attribute), 1);
		} else {
		    int prev = labelcount.get(instance.get(attribute));
		    labelcount.put(instance.get(attribute), prev+1);
		}
	    }
	}
	return labelcount;
    }

    private class Tree{
	/*
	 * The actual tree structure.
	 */
	boolean leaf;
	String attr;
	int atr;
	boolean continuous;
	boolean loop;
	double threshold;
	String label;
	HashMap<String, Tree> children;
	HashMap<String, Float> partialcounts = new HashMap<String, Float>();

	private Tree(ArrayList<ArrayList<String>> trainset, ArrayList<String> attrs, Tree last, HashMap<ArrayList<String>, Float> partialinst, int depth) {
	    // Counting up how often each label occurs (useful for multiple different cases).
	    HashMap<String, Integer> labelcount = valuecounter(trainset, 0);
	    ArrayList<ArrayList<String>> partinst = new ArrayList<ArrayList<String>>(partialinst.keySet());
	    HashMap<String, Integer> partlabelcount = valuecounter(partinst, 0);
	    ArrayList<String> labels = new ArrayList<String>(labelcount.keySet());
	    labels.addAll(partlabelcount.keySet());
	    if(labels.size() == 2) {
		if(labels.get(0).equals(labels.get(1))) {
		    labels.remove(1);
		}
	    }
	    if(attrs.size() == 1) {
		// If we've used up all the possible attributes, just find the most common label and choose it.
		// Finding the most common label.
		int best = 0;
		String bestkey = null;
		int count;
		for(String l : labels) {
		    count = 0;
		    if(labelcount.containsKey(l)) {
			count += labelcount.get(l);
		    }
		    if(partlabelcount.containsKey(l)) {
			count += partlabelcount.get(l);
		    }
		    if(count > best) {
			best = count;
			bestkey = l;
		    }
		}
		// Turn this into a leaf predicting the most common label.
		leaf = true;
		attr = attrs.get(0);
		label = bestkey;
		//System.out.println(label);
	    } else if(labels.size() == 1) {
		// If all of the remaining instances have the same label, turn this into a leaf predicting that label.
		leaf = true;
		attr = attrs.get(0);
		label = labels.get(0);
		//System.out.println(label);
	    } else {
		if(trainset == null) {
		    trainset = new ArrayList<ArrayList<String>>();
		}
		leaf = false;
		// Otherwise, calculate entropy for each attribute and turn this into a node for the least entropic
		double best = 100;
		int bestattr = 1;
		double theta = -1;
		continuous = false;
		HashMap<String, ArrayList<ArrayList<String>>> split = new HashMap<String, ArrayList<ArrayList<String>>>();
		HashMap<String, HashMap<ArrayList<String>, Float>> partialSplit = new HashMap<String, HashMap<ArrayList<String>, Float>>();
		HashMap<ArrayList<String>, Float> modifiedPartials = new HashMap<ArrayList<String>, Float>();
		ArrayList<ArrayList<String>> newPartials = new ArrayList<ArrayList<String>>();
		for(int i = 1; i < attrs.size(); i++) {
		    double t = 0;
		    boolean cont = false;
		    ArrayList<ArrayList<String>> oldpartials = new ArrayList<ArrayList<String>>(partialinst.keySet());
		    if(algorithm.equals("C4.5")) {
			try {
			    InstanceComparator comp = new InstanceComparator(i);
			    trainset.sort(comp);
			    oldpartials.sort(comp);
			    cont = true;
			} catch(NumberFormatException e) {
			}
		    }
		    ArrayList<ArrayList<String>> partials = new ArrayList<ArrayList<String>>();
		    HashMap<String, HashMap<ArrayList<String>, Float>> partSplitset = new HashMap<String, HashMap<ArrayList<String>, Float>>();
		    HashMap<String, ArrayList<ArrayList<String>>> splitset = new HashMap<String, ArrayList<ArrayList<String>>>();
		    if(cont) {
			splitset.put("less", new ArrayList<ArrayList<String>>());
			splitset.put("greater", new ArrayList<ArrayList<String>>(trainset));
			partSplitset.put("less", new HashMap<ArrayList<String>, Float>());
			partSplitset.put("greater", new HashMap<ArrayList<String>, Float>(partialinst));
			String l = null;
			ArrayList<String> instance = null;
			int nextPartial = 0;
			for(int j = 0; j < trainset.size(); j++) {
			    // For each instance add it to the first split until the label changes
			    instance = trainset.get(j);
			    boolean done = false;
			    while(!done && instance.get(i).equals("?")) {
				// If the value of the given attribute is missing, remove the instance and add it to partials
				partials.add(trainset.get(j));
				splitset.get("greater").remove(instance);
				j++;
				if(j < trainset.size()) {
				    instance = trainset.get(j);
				} else {
				    done = true;
				}
			    }
			    if(nextPartial < oldpartials.size()) {
				boolean done2 = false;
				// If there is a partial instance with an attribute value between the full instances add it to the first split and see if the label changed
				while(!done2) {
				    ArrayList<String> nextPart = oldpartials.get(nextPartial);
				    String partVal = nextPart.get(i);
				    while(!done2 && partVal.equals("?")) {
					modifiedPartials.put(nextPart, partialinst.get(nextPart));
					partSplitset.get("greater").remove(nextPart);
					nextPartial++;
					if(nextPartial < oldpartials.size()) {
					    nextPart = oldpartials.get(nextPartial);
					    partVal = nextPart.get(i);
					} else {
					    done2 = true;
					}
				    }
				    if(!done2) {
					try {
					    Float partialVal = Float.parseFloat(partVal);
					    Float val = (float) 0;
					    if(!done) {
						val = Float.parseFloat(instance.get(i));
					    }
					    if(done || partialVal > val) {
						instance = oldpartials.get(nextPartial);
						if(l == null) {
						    l = instance.get(0);
						}
						partSplitset.get("less").put(instance, partialinst.get(instance));
						partSplitset.get("greater").remove(instance);
						if(!instance.get(0).equals(l)) {
						    // when the label changes, add everything else to "greater" and calculate entropy
						    l = instance.get(0);
						    t = Integer.parseInt(instance.get(i));
						    double totalH = gainCalc(splitset, partSplitset, partials, modifiedPartials, trainset.size());
						    if(totalH < best) {
							best = totalH;
							bestattr = i;
							split = new HashMap<String, ArrayList<ArrayList<String>>>();
							split.put("less", new ArrayList<ArrayList<String>>(splitset.get("less")));
							split.put("greater", new ArrayList<ArrayList<String>>(splitset.get("greater")));
							partialSplit = new HashMap<String, HashMap<ArrayList<String>, Float>>();
							partialSplit.put("less", new HashMap<ArrayList<String>, Float>(partSplitset.get("less")));
							partialSplit.put("greater", new HashMap<ArrayList<String>, Float>(partSplitset.get("greater")));
							theta = t;
							continuous = true;
							newPartials = partials;

						    }
						}
						nextPartial++;
						if(nextPartial < oldpartials.size()) {
						    partVal = oldpartials.get(nextPartial).get(i);
						} else {
						    done2 = true;
						}
					    } else {
						done2 = true;
					    }
					} catch (NumberFormatException e){
					    nextPartial++;
					    if(nextPartial < oldpartials.size()) {
						partVal = oldpartials.get(nextPartial).get(i);
					    } else {
						done2 = true;
					    }
					}
				    }
				}
			    }
			    if(l == null) {
				l = instance.get(0);
			    }
			    splitset.get("less").add(instance);
			    splitset.get("greater").remove(instance);
			    if(!done && !instance.get(0).equals(l)) {
				// when the label changes, calculate entropy
				l = instance.get(0);
				t = Float.parseFloat(instance.get(i));
				double totalH = gainCalc(splitset, partSplitset, partials, modifiedPartials, trainset.size());
				if(totalH < best) {
				    best = totalH;
				    bestattr = i;
				    split = new HashMap<String, ArrayList<ArrayList<String>>>();
				    split.put("less", new ArrayList<ArrayList<String>>(splitset.get("less")));
				    split.put("greater", new ArrayList<ArrayList<String>>(splitset.get("greater")));
				    partialSplit = new HashMap<String, HashMap<ArrayList<String>, Float>>();
				    partialSplit.put("less", new HashMap<ArrayList<String>, Float>(partSplitset.get("less")));
				    partialSplit.put("greater", new HashMap<ArrayList<String>, Float>(partSplitset.get("greater")));
				    theta = t;
				    continuous = true;
				    newPartials = partials;

				}
			    }
			}
			if(theta == -1) {
			    // if the label didn't change, calculate entropy anyway
			    double totalH = gainCalc(splitset, partSplitset, partials, modifiedPartials, trainset.size());
			    if(totalH < best) {
				best = totalH;
				bestattr = i;
				split = new HashMap<String, ArrayList<ArrayList<String>>>();
				split.put("less", new ArrayList<ArrayList<String>>(splitset.get("less")));
				split.remove("greater");
				partialSplit.put("less", new HashMap<ArrayList<String>, Float>(partSplitset.get("less")));
				partialSplit.remove("greater");
				theta = t;
				continuous = true;
				newPartials = partials;
			    }
			}
		    } else {
			for(int j = 0; j < trainset.size(); j++) {
			    // For each instance, add it to splitset according to the value of the attribute designated by i
			    ArrayList<String> inst = trainset.get(j);
			    String attrval = inst.get(i);
			    if(algorithm.equals("C4.5") && attrval.equals("?")) {
				partials.add(inst);
			    }else {
				if(!splitset.containsKey(attrval)) {
				    splitset.put(attrval, new ArrayList<ArrayList<String>>());
				}
				splitset.get(inst.get(i)).add(inst);
			    }
			}
			for(ArrayList<String> partInst : partialinst.keySet()) {
			    // For each partial instance, add it to partSplitset according to the value of attribute i
			    String attrval = partInst.get(i);
			    if(attrval.equals("?")) {
				modifiedPartials.put(partInst, partialinst.get(partInst));
			    }else{
				if(!partSplitset.containsKey(attrval)) {
				    partSplitset.put(attrval, new HashMap<ArrayList<String>, Float>());
				}
				partSplitset.get(attrval).put(partInst, partialinst.get(partInst));
			    }
			}
			double totalH = 0;
			totalH = gainCalc(splitset, partSplitset, partials, modifiedPartials, trainset.size());
			if(totalH < best) {
			    best = totalH;
			    bestattr = i;
			    split = splitset;
			    partialSplit = partSplitset;
			    continuous = false;
			    newPartials = partials;
			}
		    }
		}
		
		// Designate this node as bestattr
		attr = attrs.get(bestattr);
		atr = bestattr;
		loop = false;
		if(continuous) {
		    threshold = theta;
		}
		if(continuous && last != null) {
		    if(attr.equals(last.attr) && threshold == last.threshold) {
			loop = true;
		    }
		}
		if(!continuous || loop) {
		    // If the attribute isn't continuous remove bestattr from attrs.
		    attrs.remove(atr);
		}
		// Remove bestattr from each instance (if not continuous), add each value of the chosen attribute to children and recurse.
		children = new HashMap<String, Tree>();
		ArrayList<ArrayList<String>> subset;
		ArrayList<String> vals = new ArrayList<String>(split.keySet());
		vals.addAll(partialSplit.keySet());
		boolean first = true;
		for(String val : vals) {
		    subset = split.get(val);
		    if(!continuous || loop) {
			ArrayList<ArrayList<String>> s = new ArrayList<ArrayList<String>>();
			if(subset != null) {
			    for(ArrayList<String> inst : subset) {
				ArrayList<String> modinst = new ArrayList<String>(inst);
				if(modinst.size() > attrs.size()) {
				    modinst.remove(atr);
				}
				s.add(modinst);
			    }
			}
			subset = s;
		    }
		    // Take care of partial instances
		    HashMap<ArrayList<String>, Float> tempPartialInst = new HashMap<ArrayList<String>, Float>();
		    if(algorithm.equals("C4.5")) {
			float subsize;
			if(subset == null) {
			    subsize = 0;
			} else {
			    subsize = subset.size();
			}
			float pSub = subsize/trainset.size();
			partialcounts.put(val, pSub);
			if(partialSplit.containsKey(val)) {
			    if(first && !continuous || loop) {
				HashMap<ArrayList<String>, Float> partSplit = new HashMap<ArrayList<String>, Float>(partialSplit.get(val));
				for(ArrayList<String> inst : partSplit.keySet()) {
				    ArrayList<String> modinst = new ArrayList<String>(inst);
				    if(modinst.size() > attrs.size()) {
					modinst.remove(atr);
				    }
				    tempPartialInst.put(modinst, partSplit.get(inst));
				}
			    } else {
				tempPartialInst = new HashMap<ArrayList<String>, Float>(partialSplit.get(val));
			    }

			}
			for(ArrayList<String> inst : newPartials) {
			    ArrayList<String> modinst = new ArrayList<String>(inst);
			    if(modinst.size() > attrs.size() && first && !continuous || loop) {
				modinst.remove(atr);
			    }
			    tempPartialInst.put(modinst, (Float) pSub);
			}
			for(ArrayList<String> inst : modifiedPartials.keySet()) {
			    ArrayList<String> modinst = new ArrayList<String>(inst);
			    if(modinst.size() > attrs.size() && first && !continuous || loop) {
				modinst.remove(atr);
			    }
			    tempPartialInst.put(modinst, (Float) pSub*modifiedPartials.get(inst));    
			}
		    }
		    first = false;
		    children.put(val, new Tree(subset, new ArrayList<String>(attrs), this, tempPartialInst, depth+1));
		}
	    }
	}

	private double gainCalc(HashMap<String, ArrayList<ArrayList<String>>> splitset, HashMap<String, HashMap<ArrayList<String>, Float>> partSplitset, ArrayList<ArrayList<String>> partials, HashMap<ArrayList<String>, Float> modPartials, int totalinst) {
	    double totalH = 0;
	    double splitInfo = 0;
	    HashMap<String, Integer> labelcounts = new HashMap<String, Integer>();
	    HashMap<String, Float> weightedLabels = new HashMap<String, Float>();
	    HashMap<String, Float> modWeightedLabels = new HashMap<String, Float>();
	    HashMap<String, Integer> newWeightedLabels = new HashMap<String, Integer>();
	    ArrayList<String> values = new ArrayList<String>(splitset.keySet());
	    if(algorithm.equals("C4.5")) {
		if(!partSplitset.isEmpty()) {
		    values.addAll(partSplitset.keySet());
		}
	    }
	    for(String value : values) {
		// For each value of the designated attribute, calculate weighted entropy and add to totalH
		double attrcount = 0;
		double newWeight = 0;
		if(splitset.containsKey(value)) {
		    attrcount = splitset.get(value).size();
		    labelcounts = valuecounter(splitset.get(value), 0);
		}
		ArrayList<String> labels = new ArrayList<String>(labelcounts.keySet());
		if(algorithm.equals("C4.5") && splitinfo) {
		    // Count partial instances per label and add to labels
		    newWeightedLabels = valuecounter(partials, 0);
		    if(partSplitset.containsKey(value)) {
			weightedLabels = weightedValCounter(partSplitset.get(value), 0);
		    }
		    modWeightedLabels = weightedValCounter(modPartials, 0);
		    labels.addAll(newWeightedLabels.keySet());
		    labels.addAll(weightedLabels.keySet());
		    // Count up partial instances
		    for(String label : weightedLabels.keySet()) {
			attrcount += weightedLabels.get(label);
		    }
		    newWeight = attrcount/totalinst;
		    // Count up modified partial instances
		    for(String label : modWeightedLabels.keySet()) {
			attrcount += modWeightedLabels.get(label)*newWeight;
		    }
		    attrcount += partials.size()*attrcount/totalinst;
		    // Calculate split info
		    double pattr = attrcount/totalinst;
		    splitInfo += pattr*Math.log(pattr)/Math.log(2);
		} else {
		    splitInfo = -1;
		}
		double entropy = 0;
		for(String label : labels) {
		    // For each label, calculate p(label)log2p(label)
		    double labelCount = 0;
		    if(labelcounts.containsKey(label)) {
			labelCount = labelcounts.get(label);
		    }
		    if(algorithm.equals("C4.5")) {
			if(weightedLabels.containsKey(label)) {
			    labelCount += weightedLabels.get(label);
			}
			if(newWeightedLabels.containsKey(label)) {
			    labelCount += newWeightedLabels.get(label)*newWeight;
			}
			if(modWeightedLabels.containsKey(label)) {
			    labelCount += modWeightedLabels.get(label)*newWeight;
			}
		    }
		    double plabel = labelCount/attrcount;
		    entropy += plabel*Math.log(plabel)/Math.log(2);
		}
		totalH += entropy*attrcount/totalinst;
	    }
	    return totalH/splitInfo;
	}
	
	private HashMap<String, Float> weightedValCounter(HashMap<ArrayList<String>, Float> data, int index){
	    HashMap<String, Float> labelcount = new HashMap<String, Float>();
		for(ArrayList<String> instance : data.keySet()) {
		    if(!labelcount.containsKey(instance.get(index))) {
			labelcount.put(instance.get(index), data.get(instance));
		    } else {
			float prev = labelcount.get(instance.get(index));
			labelcount.put(instance.get(index), prev+data.get(instance));
		    }
		}
		return labelcount;
	}

	private void toRules(HashMap<String, ArrayList<String>> rule){
	    if(leaf) {
		ArrayList<String> temp = new ArrayList<String>();
		temp.add(label);
		ruleset.put(rule, temp);
	    } else {
		for(String val : children.keySet()) {
		    HashMap<String, ArrayList<String>> temp = new HashMap<String, ArrayList<String>>(rule);
		    ArrayList<String> list = new ArrayList<String>();
		    if(continuous) {
			if(val.equals("less")) {
			    list.add("<" + threshold);
			} else if(val.equals("greater")) {
			    list.add(">"+threshold);
			}
		    } else {
			list.add(val);
		    }
		    list.add("" + partialcounts.get(val));
		    temp.put(attr, list);
		    children.get(val).toRules(temp);
		}
	    }
	}

	private String predict(ArrayList<String> instance) {
	    // Predicts the label of instance as label if leaf, or by recursing on children otherwise
	    if(leaf) {
		return label;
	    } else if(continuous){
		double val = Float.parseFloat(instance.get(atr));
		if(loop) {
		    instance.remove(atr);
		}
		if(val > threshold) {
		    return children.get("greater").predict(instance);
		} else {
		    return children.get("less").predict(instance);
		}
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

    static class InstanceComparator implements Comparator<ArrayList<String>>{
	/**
	 * Comparator class to sort instances by the value of a given continuous attribute
	 */
	int attrdex;

	public InstanceComparator(int index) {
	    attrdex = index;
	}

	public int compare(ArrayList<String> inst1, ArrayList<String> inst2){
	    String val1 = inst1.get(attrdex);
	    String val2 = inst2.get(attrdex);
	    Float v1;
	    Float v2;
	    if(val1.equals("?")) {
		v1 = (float) -10000;
	    }else {
		v1 = Float.parseFloat(val1);
	    }
	    if(val2.equals("?")) {
		v2 = (float) -10000;
	    } else {
		v2 = Float.parseFloat(val2);
	    }
	    return v1.compareTo(v2);
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
	} else if(args[1].equals("ID3")) {
	    // Test to make sure the second argument is a valid algorithm.
	    algorithm = args[1];
	} else if (args[1].substring(0, 4).equals("C4.5")) {
	    algorithm = "C4.5";
	    if(args[1].substring(4).equals("NP")) {
		pruning = false;
	    } else if(args[1].substring(4).equals("NSI")) {
		splitinfo = false;
	    }
	}else {
	    System.out.println("The second argument must designate an algorithm. Your choices are ID3, C4.5, C4.5NP, and C4.5NSI.");
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

	// Split dataset into trainset and testset (and a validation set if using C4.5) based on split (proportion in testset) using seed.
	int split = (int) (.2*dataset.size());
	Collections.shuffle(dataset, new Random(seed));
	ArrayList<ArrayList<String>> testset = new ArrayList<ArrayList<String>>(dataset.subList(0, split));
	ArrayList<ArrayList<String>> validset = null;
	if(algorithm.equals("C4.5") && pruning) {
	    validset = new ArrayList<ArrayList<String>>(dataset.subList(split, 2*split));
	    split *= 2;
	}
	ArrayList<ArrayList<String>> trainset = new ArrayList<ArrayList<String>>(dataset.subList(split, dataset.size()));

	// Actually make and train the decision tree.
	DecisionTree learner = new DecisionTree(trainset, new ArrayList<String>(attributes));
	// Pruning if using C4.5
	if(algorithm.equals("C4.5") && pruning) {
	    learner.prune(validset);
	}
	// Testing the tree
	String file = args[0].substring(0, args[0].length()-4);
	learner.test(testset, file, args[2], args[1]);
    }
}