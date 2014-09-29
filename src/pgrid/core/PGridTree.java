/**
 *
 * Copyright (c) 2002-2008 The P-Grid Team, All Rights Reserved.
 *
 * This file is part of the P-Grid package.
 * P-Grid homepage: http://www.p-grid.org/
 *  
 * The P-Grid package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The P-Grid package is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the P-Grid package.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package pgrid.core;

import pgrid.Constants;
import pgrid.Properties;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.util.Tokenizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;

import com.google.uzaygezen.core.BitVector;
import com.google.uzaygezen.core.HilbertIndexMasks;

import mgrid.core.MGridUtils;
import mgrid.core.Point;

/**
 * This class is responsible for building the indexing structure (the PGridP2P
 * tree) to map ASCII string to binary strings.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class PGridTree {

	/**
	 * The standard maximum number of data items per leaf.
	 */
	private static int MAX_LEAF_SIZE = 8;

	/**
	 * The standard length of data strings.
	 */
	private static int STRING_LEN = 4;

	/**
	 * The database of traced search strings.
	 */
	private String[] mDatabase = null;

	/**
	 * The maximum number of data items mapped to a leaf.
	 */
	private int mMaxLeafSize = MAX_LEAF_SIZE;

	/**
	 * The length of the PGridServer data strings (negative if variable).
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * The length of the PGridServer data strings (negative if variable).
	 */
	private int mStringLen = STRING_LEN;

	/**
	 * The root tree node.
	 */
	private TreeNode mTreeRoot = null;

	/**
	 * Creates a new PGridP2P tree.
	 */
	public PGridTree() {
	}

	/**
	 * Returns the binary represantation for the given query string.
	 *
	 * @param query the query string
	 * @return the binary represantation.
	 */
	public String findKey(String query) {
		if (mTreeRoot == null)
			return null;
		String key = findKey(query.toLowerCase(), mTreeRoot);
	
		return query;
	
	}
	/**
	 * Returns the binary represantation for the given point.
	 *
	 * @param point 
	 * @return the binary represantation.
	 */
	public String findKey(Point point) {
		BitVector row = MGridUtils.HilbertConvertor(point.x,point.y);
		return row.toString();
	
	}
	/**
	 * Returns the binary represantation for the given query string, starting at
	 * the given entry node of the PGridP2P tree and recursivly parses throw down
	 * the tree.
	 *
	 * @param query the query string
	 * @param node  the entry node of the PGridP2P tree.
	 * @return the binary represantation.
	 */
	private String findKey(String query, TreeNode node) {
		if ((node.getLeftChild() == null) && (node.getRightChild() == null))
			return "";
		if (node.getPrefix().compareTo(query) > 0)
			return ("0" + findKey(query, node.getLeftChild()));
		else
			return ("1" + findKey(query, node.getRightChild()));
	}

	/**
	 * Finds the position to split the range.
	 * The first different character at position <code>common</code> in the
	 * neighbouring data strings is searched, starting at the middle of the range
	 * and step apart.
	 *
	 * @param start  the starting index for the subtree.
	 * @param end    the ending index for the subtree.
	 * @param common the length of the common prefix of all the PGridServer data words.
	 * @return the found pivot position to split the range.
	 */
	private int findPivotPos(int start, int end, int common) {
		if (start == end)
			return start;
		while (true) {
			int left = (start + end) / 2;
			int right = (start + end + 1) / 2;
			boolean leftFlag = false;
			boolean rightFlag = false;
			while ((left >= start) && (right <= end)) {
				leftFlag = false;
				rightFlag = false;
				if ((mDatabase[right].length() >= (common + 1)) && (mDatabase[right - 1].length() >= (common + 1)))
					if (mDatabase[right].charAt(common) == mDatabase[right - 1].charAt(common))
						rightFlag = true;
				if ((mDatabase[left].length() >= (common + 1)) && (mDatabase[left + 1].length() >= (common + 1)))
					if (mDatabase[left].charAt(common) == mDatabase[left + 1].charAt(common))
						leftFlag = true;
				if ((!rightFlag) || (!leftFlag))
					break;
				left--;
				right++;
			}
			if (!rightFlag)
				return right;
			if (!leftFlag)
				return left + 1;
			common++;
		}
	}

	/**
	 * Finds the common prefix for two strings.
	 *
	 * @param left   the first string.
	 * @param right  the second string.
	 * @param common the length of prefix guaranteed to be common.
	 * @return the found common prefix.
	 */
	private String findPrefix(String left, String right, int common) {
		int len = Math.min(left.length(), right.length());
		int i;
		for (i = common; i < len; i++)
			if (left.charAt(i) != right.charAt(i))
				break;
		return right.substring(0, i + 1); // i+1
	}

	/**
	 * Initializes the PGridP2P tree.
	 */
	synchronized public void init() {
		mPGridP2P = PGridP2P.sharedInstance();
		InputStream in = getClass().getResourceAsStream("/" + mPGridP2P.propertyString(Properties.TREE_INI_FILE));
		if (in == null)
			initWithDbFile();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			mTreeRoot = loadTreeFromFile(reader, mPGridP2P.propertyString(Properties.TREE_INI_FILE));
		} catch (NullPointerException e) {
			pgrid.Constants.LOGGER.log(Level.SEVERE, "Could not read/write PGrid Tree initialization file '" + mPGridP2P.propertyString(Properties.TREE_INI_FILE) + "'!", e);
			System.exit(-1);
		}
	}

	/**
	 * Initializes the PGridP2P tree with a file of traced search queries.
	 */
	synchronized private void initWithDbFile() {
		InputStream in = getClass().getResourceAsStream("/" + mPGridP2P.propertyString(Properties.TREE_DB_FILE));
		if (in == null) {
			Constants.LOGGER.severe("Database file '" + mPGridP2P.propertyString(Properties.TREE_DB_FILE) + "' for the PGrid Tree does not exist!");
			System.exit(-1);
		}

		Vector database = new Vector();
		// read the file
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] words = Tokenizer.tokenize(line, " \n\r\t");
				for (int i = 0; i < words.length; i++) {
					if (mStringLen < 0)
						database.add(words[i]);
					else if (words[i].length() >= mStringLen)
						database.add(words[i].substring(0, mStringLen));

				}
			}
		} catch (IOException e) {
			Constants.LOGGER.log(Level.SEVERE, "Could not read database file '" + mPGridP2P.propertyString(Properties.TREE_DB_FILE) + "'!", e);
			System.exit(-1);
		}

		// eliminate duplicate entries
		int index = 0;
		while (index < (database.size() - 1)) {
			if (((String)database.get(index)).toLowerCase().equals(((String)database.get(index + 1)).toLowerCase()))
				database.remove(index + 1);
			else
				index++;
		}

		// copy to Array
		mDatabase = new String[database.size()];
		for (int i = 0; i < database.size(); i++) {
			mDatabase[i] = ((String)database.get(i)).toLowerCase();
		}

		// sort data
		Arrays.sort(mDatabase);

		if (mDatabase.length == 0) {
			Constants.LOGGER.warning("Database '" +  mPGridP2P.propertyString(Properties.TREE_DB_FILE) + "' has no words with required length!");
		}

		// create the tree
		mTreeRoot = makeTreeNode(0, mDatabase.length - 1, 0);

		// saveDataTable the tree to a file
		File file = new File(mPGridP2P.propertyString(Properties.TREE_INI_FILE));
		try {
			FileWriter fWriter = new FileWriter(file);
			saveTreeToFile(fWriter, mTreeRoot, file.getName());
			fWriter.close();
		} catch (IOException e) {
			Constants.LOGGER.log(Level.SEVERE, "Could not create PGrid Tree initialization file '" + file.getName() + "'!", e);
			System.exit(-1);
		}
	}

	/**
	 * Initializes the PGridP2P Tree with PGridP2P Tree initialization file.
	 *
	 * @param reader   the PGridP2P Tree initialization file.
	 * @param filename the initialization filename.
	 * @return the created PGridP2P Tree node.
	 */
	private TreeNode loadTreeFromFile(BufferedReader reader, String filename) {
		String prefix;
		try {
			prefix = reader.readLine();
		} catch (IOException e) {
			Constants.LOGGER.log(Level.WARNING, "Could not read/write PGrid Tree initialization file '" + filename + "'!", e);
			return null;
		}
		if (prefix.equals(""))
			return null;
		TreeNode leftChild = loadTreeFromFile(reader, filename);
		TreeNode rightChild = loadTreeFromFile(reader, filename);

		int leaves = (leftChild == null ? 0 : 1 + leftChild.getLeavesCount());
		leaves += (rightChild == null ? 0 : 1 + rightChild.getLeavesCount());
		int leftDepth = (leftChild == null ? 0 : 1 + leftChild.getDepth());
		int rightDepth = (rightChild == null ? 0 : 1 + rightChild.getDepth());
		int depth = Math.max(leftDepth, rightDepth);
		int nodes = 1;
		nodes += (leftChild == null ? 0 : leftChild.getNodesCount());
		nodes += (rightChild == null ? 0 : rightChild.getNodesCount());

		return new TreeNode(prefix, leftChild, rightChild, leaves, depth, nodes);
	}

	/**
	 * Creates a PGridP2P tree node and recursivly calls this function to create the
	 * child nodes for this node.
	 *
	 * @param start  the starting index for the subtree.
	 * @param end    the ending index for the subtree.
	 * @param common the length of the common prefix of all the PGridServer data words.
	 * @return the created PGridP2P tree node.
	 */
	private TreeNode makeTreeNode(int start, int end, int common) {
		if (((end - start + 1) <= mMaxLeafSize) || (start == end)) {
			return new TreeNode(mDatabase[(start + end) / 2], null, null, 1, 1, 1);
		} else {
			int pivotPos = findPivotPos(start, end, common);
			String prefix = findPrefix(mDatabase[pivotPos - 1], mDatabase[pivotPos], common);

			common = prefix.length() - 1;
			TreeNode leftChild = makeTreeNode(start, pivotPos - 1, common);
			TreeNode rightChild = makeTreeNode(pivotPos, end, common);
			int leaves = leftChild.getLeavesCount() + rightChild.getLeavesCount();
			int depth = Math.max(leftChild.getDepth(), rightChild.getDepth());
			int nodes = leftChild.getNodesCount() + rightChild.getNodesCount() + 1;

			return new TreeNode(prefix, leftChild, rightChild, leaves, depth, nodes);
		}
	}

	/**
	 * Returns the number of leaves in the PGridP2P tree.
	 *
	 * @return the number of leaves in the PGridP2P tree.
	 */
	int getLeavesCount() {
		return mTreeRoot.getLeavesCount();
	}

	/**
	 * Returns the used database.
	 *
	 * @return the used database.
	 */
	String[] getDatabase() {
		return mDatabase;
	}

	/**
	 * Returns the size of the used database.
	 *
	 * @return the size of the used database.
	 */
	int getDatabaseSize() {
		return mDatabase.length;
	}

	/**
	 * Returns the depth of the PGridP2P tree.
	 *
	 * @return the depth of the PGridP2P tree.
	 */
	int getDepth() {
		return mTreeRoot.getDepth();
	}

	/**
	 * Returns the number of nodes in the PGridP2P tree.
	 *
	 * @return the number of nodes in the PGridP2P tree.
	 */
	int getNodesCount() {
		return mTreeRoot.getNodesCount();
	}

	public void createKeys(String filename) {
		File file = new File(filename);
		try {
			FileWriter fWriter = new FileWriter(file);
			createKeys(fWriter, mTreeRoot, filename, "");
			fWriter.close();
		} catch (IOException e) {
			Constants.LOGGER.log(Level.WARNING, "Could not write to file '" + filename + "'!", e);
		}
	}

	void createKeys(FileWriter writer, TreeNode node, String filename, String key) {
		try {
			TreeNode left = node.getLeftChild();
			TreeNode right = node.getRightChild();
			if (left != null)
				createKeys(writer, left, filename, key + "0");
			if (right != null)
				createKeys(writer, right, filename, key + "1");
			if ((left == null) && (right == null))
				writer.write(node.getPrefix() + Constants.LINE_SEPERATOR);
		} catch (IOException e) {
			Constants.LOGGER.log(Level.WARNING, "Could not write to file '" + filename + "'!", e);
		}
	}

	/**
	 * Saves the PGridP2P Tree to a file.
	 * This information can be used to reconstruct the tree.
	 *
	 * @param writer   the file writer to store the tree.
	 * @param node     the node to store.
	 * @param filename the filename.
	 */
	private void saveTreeToFile(FileWriter writer, TreeNode node, String filename) {
		try {
			writer.write(node.getPrefix() + Constants.LINE_SEPERATOR);
			TreeNode left = node.getLeftChild();
			TreeNode right = node.getRightChild();
			if (left != null)
				saveTreeToFile(writer, left, filename);
			else
				writer.write(Constants.LINE_SEPERATOR);
			if (right != null)
				saveTreeToFile(writer, right, filename);
			else
				writer.write(Constants.LINE_SEPERATOR);
		} catch (IOException e) {
			Constants.LOGGER.log(Level.SEVERE, "Could not read/write PGrid Tree initialization file '" + filename +"'!", e);
			System.exit(-1);
		}
	}

	/**
	 * This inner class represents a node of the PGridP2P tree.
	 */
	private class TreeNode {

		/**
		 * The greater depth of the subtrees.
		 */
		private int mDepth = 0;

		/**
		 * The number of leaves in the subtrees.
		 */
		private int mLeavesCount = 0;

		/**
		 * The left subtree.
		 */
		private TreeNode mLeftChild = null;

		/**
		 * The number of nodes in the subtrees.
		 */
		private int mNodesCount = 0;

		/**
		 * The prefix of this node.
		 */
		private String mPrefix = new String();

		/**
		 * The right subtree.
		 */
		private TreeNode mRightChild = null;

		/**
		 * Creates a new tree node with the given properties.
		 *
		 * @param prefix      the prefix for this node.
		 * @param leftChild   the left subtree
		 * @param rightChild  the right subtree
		 * @param leavesCount the number of leaves in the subtrees.
		 * @param depth       the greater depth of the subtrees.
		 * @param nodesCount  the number of nodes in the subtrees.
		 */
		private TreeNode(String prefix, TreeNode leftChild, TreeNode rightChild, int leavesCount, int depth, int nodesCount) {
			mPrefix = prefix;
			mLeftChild = leftChild;
			mRightChild = rightChild;
			mLeavesCount = leavesCount;
			mDepth = depth;
			mNodesCount = nodesCount;
		}

		/**
		 * Returns the greater of depth of the subtrees.
		 *
		 * @return the greater of depth of the subtrees.
		 */
		private int getDepth() {
			return mDepth;
		}

		/**
		 * Returns the number of leaves in the subtrees.
		 *
		 * @return the number of leaves in the subtrees.
		 */
		private int getLeavesCount() {
			return mLeavesCount;
		}

		/**
		 * Returns the left subtree.
		 *
		 * @return the left subtree.
		 */
		private TreeNode getLeftChild() {
			return mLeftChild;
		}

		/**
		 * Returns the number of nodes in the subtrees.
		 *
		 * @return the number of nodes in the subtrees.
		 */
		private int getNodesCount() {
			return mNodesCount;
		}

		/**
		 * Returns the prefix for this node.
		 *
		 * @return the prefix for this node.
		 */
		private String getPrefix() {
			return mPrefix;
		}

		/**
		 * Returns the right subtree.
		 *
		 * @return the right subtree.
		 */
		private TreeNode getRightChild() {
			return mRightChild;
		}

	}

}