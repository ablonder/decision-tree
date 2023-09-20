## Decision Tree Classifier

One of the more complex machine learning algorithms: a recursive tree structure where each branch corresponds to some "decision" based on the value of an attribute (e.g. if red go left, otherwise go right) until it reaches a conclusion about how to classify it (i.e. one of the leaves of the tree).

DecisionTree.java takes in a csv file of test cases, and creates a decision tree using the either the ID3 or C4.5 algorithm - the latter with or without pruning and split information - tests it, and outputs a csv file containing a confusion matrix detailing the results of the test.

Update: Fixed several minor problems with the previous version, enabling all algorithms to run, though C4.5NP runs very slowly on both hypothyroid and opticalDigits, and C4.5 runs very slowly on hypothyroid.
