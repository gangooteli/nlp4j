/**
 * Copyright 2015, Emory University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.emory.mathcs.nlp.component.template.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import edu.emory.mathcs.nlp.common.collection.arc.AbstractArc;
import edu.emory.mathcs.nlp.common.collection.list.SortedArrayList;
import edu.emory.mathcs.nlp.common.collection.tuple.Pair;
import edu.emory.mathcs.nlp.common.constant.StringConst;
import edu.emory.mathcs.nlp.common.util.DSUtils;
import edu.emory.mathcs.nlp.common.util.Joiner;
import edu.emory.mathcs.nlp.common.util.StringUtils;
import edu.emory.mathcs.nlp.component.dep.DEPArc;
import edu.emory.mathcs.nlp.component.template.feature.Direction;
import edu.emory.mathcs.nlp.component.template.feature.Field;
import edu.emory.mathcs.nlp.component.template.reader.TSVReader;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * @author Jinho D. Choi ({@code jinho.choi@emory.edu})
 */
public abstract class AbstractNLPNode<N extends AbstractNLPNode<N>> implements Serializable, Comparable<N>
{
	private static final long serialVersionUID = -6890831718184647451L;
	static final String ROOT_TAG = "@#r$%";
	
	// core fields
	protected int             id;
	protected String          word_form;
	protected String          lemma;
	protected String          pos_tag;
	protected String          nament_tag;
	protected FeatMap         feat_map;
	protected String          dependency_label;
	protected N               dependency_head;
	protected List<DEPArc<N>> semantic_heads;
	protected List<DEPArc<N>> secondary_heads;
    
	// offsets
	protected int start_offset;
	protected int end_offset;
    
	// inferred fields
	public int dependent_id;
	protected String word_form_lowercase;
	protected String word_form_simplified;
	protected String word_form_undigitalized;
	protected String word_form_simplified_lowercase;
	protected SortedArrayList<N> dependent_list;
	
	// lexica
	protected Set<String>  named_entity_gazetteers;
	protected List<String> ambiguity_classes;
	protected Set<String>  word_clusters;
	protected float[]      word_embedding;
	protected boolean      stop_word;
	
	public abstract N self();
	
	public AbstractNLPNode()
	{
		toRoot();
	}
	
	public AbstractNLPNode(int id, String form)
	{
		this(id, form, null);
	}
	
	public AbstractNLPNode(int id, String form, String posTag)
	{
		this(id, form, posTag, new FeatMap());
	}
	
	public AbstractNLPNode(int id, String form, String posTag, FeatMap feats)
	{
		this(id, form, null, posTag, feats);
	}
	
	public AbstractNLPNode(int id, String form, String lemma, String posTag, FeatMap feats)
	{
		this(id, form, lemma, posTag, null, feats);
	}
	
	public AbstractNLPNode(int id, String form, String lemma, String posTag, String namentTag, FeatMap feats)
	{
		set(id, form, lemma, posTag, namentTag, feats, null, null);
	}
	
	public AbstractNLPNode(int id, String form, String lemma, String posTag, FeatMap feats, N dhead, String deprel)
	{
		set(id, form, lemma, posTag, null, feats, dhead, deprel);
	}
	
	public AbstractNLPNode(int id, String form, String lemma, String posTag, String namentTag, FeatMap feats, N dhead, String deprel)
	{
		set(id, form, lemma, posTag, namentTag, feats, dhead, deprel);
	}
	
	public void set(int id, String form, String lemma, String posTag, String namentTag, FeatMap feats, N dhead, String deprel)
	{
		setID(id);
		setWordForm(form);
		setLemma(lemma);
		setPartOfSpeechTag(posTag);
		setNamedEntityTag(namentTag);
		setFeatMap(feats);
		setDependencyHead(dhead);
		setDependencyLabel(deprel);
		
		dependent_list = new SortedArrayList<>();
		semantic_heads = new ArrayList<>();
	}
	
	public N toRoot()
	{
		set(0, ROOT_TAG, ROOT_TAG, ROOT_TAG, ROOT_TAG, new FeatMap(), null, null);
		return self();
	}
	
//	============================== GETTERS ==============================
	
	public int getID()
	{
		return id;
	}
	
	public String getWordForm()
	{
		return word_form;
	}
	
	public String getWordFormLowercase()
	{
		return word_form_lowercase;
	}
	
	/** @see StringUtils#toSimplifiedForm(String). */
	public String getWordFormSimplified()
	{
		return word_form_simplified;
	}
	
	public String getWordFormSimplifiedLowercase()
	{
		return word_form_simplified_lowercase;
	}
	
	public String getWordFormUndigitalized()
	{
		return word_form_undigitalized;
	}
	
	public String getWordShape()
	{
		return StringUtils.getShape(word_form_simplified, 2);
	}
	
	public String getWordShapeLowercase()
	{
		return StringUtils.getShape(word_form_simplified_lowercase, 2);
	}
	
	public String getLemma()
	{
		return lemma;
	}
	
	public String getPartOfSpeechTag()
	{
		return pos_tag;
	}
	
	public String getNamedEntityTag()
	{
		return nament_tag;
	}
	
	public FeatMap getFeatMap()
	{
		return feat_map;
	}
	
	public String getFeat(String key)
	{
		return feat_map.get(key);
	}
	
	/** @return the value of the specific field. */
	public String getValue(Field field)
	{
		switch (field)
		{
		case word_form: return getWordForm();
		case word_form_lowercase: return getWordFormLowercase();
		case word_form_simplified: return getWordFormSimplified();
		case word_form_undigitalized: return getWordFormUndigitalized();
		case word_form_simplified_lowercase: return getWordFormSimplifiedLowercase();
		case word_shape: return getWordShape();
		case word_shape_lowercase: return getWordShapeLowercase();
		case lemma: return getLemma();
		case part_of_speech_tag: return getPartOfSpeechTag();
		case named_entity_tag: return getNamedEntityTag();
		case dependency_label: return getDependencyLabel();
		case ambiguity_classes: return getAmbiguityClasses();
		case named_entity_gazetteers: return getNamedEntityGazetteers();
		default: return null;
		}
	}
	
	public Set<String> getWordClusters()
	{
		return word_clusters;
	}
	
	public float[] getWordEmbedding()
	{
		return word_embedding;
	}
	
	public String getAmbiguityClass(int index)
	{
		return ambiguity_classes != null && DSUtils.isRange(ambiguity_classes, index) ? ambiguity_classes.get(index) : null;
	}
	
	public String getAmbiguityClasses()
	{
		return getCollectionValue(ambiguity_classes);
	}

	@Deprecated
	public List<String> getAmbiguityClasseList()
	{
		return ambiguity_classes;
	}

	public List<String> getAmbiguityClassList()
	{
		return ambiguity_classes;
	}
	
	public String getNamedEntityGazetteers()
	{
		return getCollectionValue(named_entity_gazetteers);
	}
	
	public Set<String> getNamedEntityGazetteerSet()
	{
		return named_entity_gazetteers;
	}
	
	protected String getCollectionValue(Collection<String> col)
	{
		return col == null || col.isEmpty() ? null : Joiner.join(col, StringConst.UNDERSCORE);
	}
	
	public int getStartOffset()
    {
        return start_offset;
    }
	
	public int getEndOffset()
    {
        return end_offset;
    }
	
//	============================== SETTERS ==============================
	
	public void setID(int id)
	{
		this.id = id;
	}
	
	public void setWordForm(String form)
	{
		word_form                      = form;
		word_form_lowercase            = StringUtils.toLowerCase(word_form);
		word_form_simplified           = StringUtils.toSimplifiedForm(form);
		word_form_undigitalized        = StringUtils.toUndigitalizedForm(form);
		word_form_simplified_lowercase = StringUtils.toLowerCase(word_form_simplified);
	}
	
	public void setLemma(String lemma)
	{
		this.lemma = lemma;
	}
	
	public void setPartOfSpeechTag(String tag)
	{
		pos_tag = tag;
	}
	
    public void setStartOffset(int offset)
    {
        this.start_offset = offset;
    }

    public void setEndOffset(int offset)
    {
        end_offset = offset;
    }

	public void setFeatMap(FeatMap map)
	{
		feat_map = map;
	}
	
	public String putFeat(String key, String value)
	{
		return feat_map.put(key, value);
	}
	
	public String removeFeat(String key)
	{
		return feat_map.remove(key);
	}
	
	public void setNamedEntityTag(String tag)
	{
		nament_tag = tag;
	}
	
	public void setAmbiguityClasses(List<String> classes)
	{
		ambiguity_classes = classes;
	}
	
	public void setWordClusters(Set<String> clusters)
	{
		word_clusters = clusters;
	}
	
	public void setWordEmbedding(float[] embedding)
	{
		word_embedding = embedding;
	}
	
	public void setNamedEntityGazetteers(Set<String> gazetteers)
	{
		named_entity_gazetteers = gazetteers;
	}
	
	public void addNamedEntityGazetteer(String gazetteer)
	{
		if (named_entity_gazetteers == null)
			named_entity_gazetteers = new TreeSet<>();
		
		named_entity_gazetteers.add(gazetteer);
	}
	
	public void setStopWord(boolean stopword)
	{
		stop_word = stopword;
	}
	
//	============================== BOOLEANS ==============================
	
	public boolean isID(int id)
	{
		return this.id == id;
	}
	
	public boolean isWordForm(String form)
	{
		return form.equals(word_form);
	}
	
	public boolean isSimplifiedWordForm(String form)
	{
		return form.equals(word_form_simplified);
	}
	
	public boolean isLemma(String lemma)
	{
		return lemma.equals(this.lemma);
	}
	
	public boolean isPartOfSpeechTag(String tag)
	{
		return tag.equals(pos_tag);
	}
	
	public boolean isPartOfSpeechTag(Pattern pattern)
	{
		return pattern.matcher(pos_tag).find();
	}
	
	public boolean isNamedEntityTag(String tag)
	{
		return tag.equals(nament_tag);
	}
	
	public boolean isStopWord()
	{
		return stop_word;
	}
	
	public boolean hasWordClusters()
	{
		return word_clusters != null;
	}
	
	public boolean hasWordEmbedding()
	{
		return word_embedding != null;
	}
	
//	============================== DEPENDENCY GETTERS ==============================
	
	/** @return the dependency label of this node if exists; otherwise, null. */
	public String getDependencyLabel()
	{
		return dependency_label;
	}
	
	/** @return the dependency head of this node if exists; otherwise, null. */
	public N getDependencyHead()
	{
		return dependency_head;
	}
	
	/** @return the dependency grand-head of the node if exists; otherwise, null. */
	public N getDependencyGrandHead()
	{
		N head = getDependencyHead();
		return (head == null) ? null : head.getDependencyHead();
	}

	public N getGrandDependencyHead()
	{
		N head = getDependencyHead();
		return (head == null) ? null : head.getDependencyHead();
	}
	
	/** 
	 * Get the left nearest dependency node.
	 * Calls {@link #getLeftNearestDependent(int)}, where {@code order=0}.
	 * @return the left nearest dependency node
	 */
	public N getLeftNearestDependent()
	{
		return getLeftNearestDependent(0);
	}
	
	/**
	 * Get the left nearest dependency node with input displacement (0 - left-nearest, 1 - second left-nearest, etc.).
	 * The left nearest dependent must be on the left-hand side of this node.
	 * @param order left displacement
	 * @return the left-nearest dependent of this node if exists; otherwise, {@code null}
	 */
	public N getLeftNearestDependent(int order)
	{
		int index = dependent_list.getInsertIndex(self()) - order - 1;
		return (index >= 0) ? getDependent(index) : null;
	}
	
	/**
	 * Get the right nearest dependency node.
	 * Calls {@link #getRightNearestDependent(int)}, where {@code order=0}. 
	 * @return the right nearest dependency node
	 */
	public N getRightNearestDependent()
	{
		return getRightNearestDependent(0);
	}
	
	/**
	 * Get the right nearest dependency node with input displacement (0 - right-nearest, 1 - second right-nearest, etc.).
	 * The right-nearest dependent must be on the right-hand side of this node.
	 * @param order right displacement
	 * @return the right-nearest dependent of this node if exists; otherwise, {@code null}
	 */
	public N getRightNearestDependent(int order)
	{
		int index = dependent_list.getInsertIndex(self()) + order;
		return (index < getDependentSize()) ? getDependent(index) : null;
	}
	
	/**
	 * Get the left most dependency node of the node.
	 * Calls {@link #getLeftMostDependent(int)}, where {@code order=0}
	 * @return the left most dependency node of the node
	 */
	public N getLeftMostDependent()
	{
		return getLeftMostDependent(0);
	}
	
	/**
	 * Get the left dependency node with input displacement (0 - leftmost, 1 - second leftmost, etc.).
	 * The leftmost dependent must be on the left-hand side of this node.
	 * @param order left displacement
	 * @return the leftmost dependent of this node if exists; otherwise, {@code null}
	 */
	public N getLeftMostDependent(int order)
	{
		if (DSUtils.isRange(dependent_list, order))
		{
			N dep = getDependent(order);
			if (dep.id < id) return dep;
		}

		return null;
	}
	
	/** 
	 * Get the right most dependency node of the node.
	 * Calls {@link #getRightMostDependent(int)}, where {@code order=0}. 
	 * @return the right most dependency node of the node
	 */
	public N getRightMostDependent()
	{
		return getRightMostDependent(0);
	}
	
	/**
	 * Get the right dependency node with input displacement (0 - rightmost, 1 - second rightmost, etc.).
	 * The rightmost dependent must be on the right-hand side of this node.
	 * @param order right displacement
	 * @return the rightmost dependent of this node if exists; otherwise, {@code null}
	 */
	public N getRightMostDependent(int order)
	{
		order = getDependentSize() - 1 - order;
		
		if (DSUtils.isRange(dependent_list, order))
		{
			N dep = getDependent(order);
			if (dep.id > id) return dep;
		}

		return null;
	}
	
	/** Calls {@link #getLeftNearestSibling(int)}, where {@code order=0}. */
	public N getLeftNearestSibling()
	{
		return getLeftNearestSibling(0);
	}
	
	/**
	 * @return the left sibling node with input displacement.
	 * @param order left displacement (0 - left-nearest, 1 - second left-nearest, etc.).
	 */
	public N getLeftNearestSibling(int order)
	{
		if (dependency_head != null)
		{
			order = dependent_id - order - 1;
			if (order >= 0) return dependency_head.getDependent(order);
		}
		
		return null;
	}
	
	public N getLeftNearestSibling(String label)
	{
		if (dependency_head != null)
		{
			N node;
			
			for (int i=dependent_id-1; i>=0; i--)
			{	
				node = dependency_head.getDependent(i);
				if (node.isDependencyLabel(label)) return node;
			}
		}
		
		return null;
	}

	/**
	 * Get the right nearest sibling node of the node.
	 * Calls {@link #getRightNearestSibling(int)}, where {@code order=0}.
	 * @return the right nearest sibling node
	 */
	public N getRightNearestSibling()
	{
		return getRightNearestSibling(0);
	}
	
	/**
	 * Get the right sibling node with input displacement (0 - right-nearest, 1 - second right-nearest, etc.).
	 * @param order right displacement
	 * @return the right sibling node with input displacement
	 */
	public N getRightNearestSibling(int order)
	{
		if (dependency_head != null)
		{
			order = dependent_id + order + 1;
			if (order < dependency_head.getDependentSize()) return dependency_head.getDependent(order);
		}
		
		return null;
	}
	
	public N getRightNearestSibling(String label)
	{
		if (dependency_head != null)
		{
			int i, size = dependency_head.getDependentSize();
			N node;
			
			for (i=dependent_id+1; i<size; i++)
			{	
				node = dependency_head.getDependent(i);
				if (node.isDependencyLabel(label)) return node;
			}
		}
		
		return null;
	}

	/**
	 * @param predicate takes a dependency node and compares the specific tag with the referenced function.
	 * @return the first-dependent with the specific label.
	 */
	public N getFirstDependent(String label, BiPredicate<N,String> predicate)
	{
		for (N node : dependent_list)
		{
			if (predicate.test(node, label))
				return node;
		}
		
		return null;
	}
	
	/**
	 * Get the first dependency node of the node by label.
	 * @param pattern pattern label of the first-dependency node
	 * @return the first-dependency node of the specific label
	 */
	public N getFirstDependentByLabel(Pattern pattern)
	{
		for (N node : dependent_list)
		{
			if (node.isDependencyLabel(pattern))
				return node;
		}
		
		return null;
	}
	
	/**
	 * Get the list of all the dependency nodes of the node.
	 * @return list of all the dependency nodes of the node
	 */
	public List<N> getDependentList()
	{
		return dependent_list;
	}
	
	/**
	 * Get the list of all the dependency nodes of the node by label.
	 * @param label string label
	 * @return list of all the dependency nodes of the node by label
	 */
	public List<N> getDependentListByLabel(String label)
	{
		List<N> list = new ArrayList<>();
		
		for (N node : dependent_list)
		{
			if (node.isDependencyLabel(label))
				list.add(node);
		}
		
		return list;
	}
	
	/**
	 * Get the list of all the dependency nodes of the node by labels set.
	 * @param y labels set
	 * @return list of all the dependency nodes of the node by labels set
	 */
	public List<N> getDependentListByLabel(Set<String> labels)
	{
		List<N> list = new ArrayList<>();
		
		for (N node : dependent_list)
		{
			if (labels.contains(node.getDependencyLabel()))
				list.add(node);
		}
		
		return list;
	}
	
	/**
	 * Get the list of all the dependency nodes of the node by label pattern.
	 * @param y label pattern
	 * @return list of all the dependency nodes of the node by label pattern
	 */
	public List<N> getDependentListByLabel(Pattern pattern)
	{
		List<N> list = new ArrayList<>();
		
		for (N node : dependent_list)
		{
			if (node.isDependencyLabel(pattern))
				list.add(node);
		}
		
		return list;
	}
	
	/**
	 * Get the list of all the left dependency nodes of the node.
	 * @return list of all the left dependency nodes of the node
	 */
	public List<N> getLeftDependentList()
	{
		List<N> list = new ArrayList<>();
		
		for (N node : dependent_list)
		{
			if (node.id > id) break;
			list.add(node);
		}
		
		return list;
	}
	
	/**
	 * Get the list of all the left dependency nodes of the node by label pattern.
	 * @param y label pattern
	 * @return list of all the left dependency nodes of the node by label pattern
	 */
	public List<N> getLeftDependentListByLabel(Pattern pattern)
	{
		List<N> list = new ArrayList<>();
		
		for (N node : dependent_list)
		{
			if (node.id > id) break;
			if (node.isDependencyLabel(pattern)) list.add(node);
		}
		
		return list;
	}
	
	/**
	 * Get the list of all the right dependency nodes of the node.
	 * @return list of all the right dependency nodes of the node
	 */
	public List<N> getRightDependentList()
	{
		List<N> list = new ArrayList<>();
		
		for (N node : dependent_list)
		{
			if (node.id < id) continue;
			list.add(node);
		}
		
		return list;
	}
	
	/**
	 * Get the list of all the right dependency nodes of the node by label pattern.
	 * @param y label pattern
	 * @return list of all the right dependency nodes of the node by label pattern
	 */
	public List<N> getRightDependentListByLabel(Pattern pattern)
	{
		List<N> list = new ArrayList<>();
		
		for (N node : dependent_list)
		{
			if (node.id < id) continue;
			if (node.isDependencyLabel(pattern)) list.add(node);
		}
		
		return list;
	}
	
	/**
	 * Get the list of all grand-dependents of the node. 
	 * @return an unsorted list of grand-dependents of the node
	 */
	public List<N> getGrandDependentList()
	{
		List<N> list = new ArrayList<>();
		
		for (N node : dependent_list)
			list.addAll(node.getDependentList());
	
		return list;
	}
	
	/**
	 * Get the list of all descendant nodes of the node with specified height.
	 * If {@code height == 1}, return {@link #getDependentList()}.
	 * If {@code height > 1} , return all descendants within the depth.
	 * If {@code height < 1} , return an empty list.
	 * @param height height level of the descendant nodes
	 * @return an unsorted list of descendants.
	 */
	public List<N> getDescendantList(int height)
	{
		List<N> list = new ArrayList<>();
	
		if (height > 0)
			getDescendantListAux(self(), list, height-1);
		
		return list;
	}
	
	private void getDescendantListAux(N node, List<N> list, int height)
	{
		list.addAll(node.getDependentList());
		
		if (height > 0)
		{
			for (N dep : node.getDependentList())
				getDescendantListAux(dep, list, height-1);
		}
	}
	
	/**
	 * Get any descendant node with POS tag.
	 * @param tag POS tag
	 * @return s descendant node with the POS tag
	 */
	public N getAnyDescendantByPartOfSpeechTag(String tag)
	{
		return getAnyDescendantByPartOfSpeechTagAux(self(), tag);
	}
	
	private N getAnyDescendantByPartOfSpeechTagAux(N node, String tag)
	{
		for (N dep : node.getDependentList())
		{
			if (dep.isPartOfSpeechTag(tag)) return dep;
			
			dep = getAnyDescendantByPartOfSpeechTagAux(dep, tag);
			if (dep != null) return dep;
		}
		
		return null;
	}

	/**
	 * Get the sorted list of all the nodes in the subtree of the node.
	 * @return a sorted list of nodes in the subtree of this node (inclusive)
	  */
	public List<N> getSubNodeList()
	{
		List<N> list = new ArrayList<>();
		getSubNodeCollectionAux(list, self());
		Collections.sort(list);
		return list;
	}
	
	/**
	 * Get a set of all the nodes is the subtree of the node.
	 * @return a set of nodes in the subtree of this node (inclusive)
	 */
	public Set<N> getSubNodeSet()
	{
		Set<N> set = new HashSet<>();
		getSubNodeCollectionAux(set, self());
		return set;
	}
	
	private void getSubNodeCollectionAux(Collection<N> col, N node)
	{
		col.add(node);
		
		for (N dep : node.getDependentList())
			getSubNodeCollectionAux(col, dep);
	}
	
	/**
	 * Get the IntHashSet of all the nodes in the subtree (Node ID -> NLPNode).
	 * @return the ntHashSet of all the nodes in the subtree (inclusive)
	 */
	public IntSet getSubNodeIDSet()
	{
		IntSet set = new IntOpenHashSet();
		getSubNodeIDSetAux(set, self());
		return set;
	}

	private void getSubNodeIDSetAux(IntSet set, N node)
	{
		set.add(node.id);
		
		for (N dep : node.getDependentList())
			getSubNodeIDSetAux(set, dep);
	}
	
	/** 
	 * Get a sorted array of IDs of all the nodes in the subtree of the node.
	 * @return a sorted array of IDs from the subtree of the node (inclusive) 
	 */
	public int[] getSubNodeIDSortedArray()
	{
		IntSet set = getSubNodeIDSet();
		int[] list = set.toIntArray();
		Arrays.sort(list);
		return list;
	}
	
	/**
	 * Get the dependency node with specific index.
	 * @return the dependency node of the node with the specific index if exists; otherwise, {@code null}.
	 * @throws IndexOutOfBoundsException
	 */
	public N getDependent(int index)
	{
		return dependent_list.get(index);
	}
	
	/**
	 * Get the index of the dependency node of a specified NLPNode.
	 * If the specific node is not a dependent of this node, returns a negative number.
	 * @return the index of the dependent node among other siblings (starting with 0).
	 */
	public int getDependentIndex(N node)
	{
		return dependent_list.indexOf(node);
	}
	
	/**
	 * Get the size of the dependents of the node.
	 * @return the number of dependents of the node 
	 */
	public int getDependentSize()
	{
		return dependent_list.size();
	}
	
	/**
	 * Get the the valency of the node.
	 * @param direction DirectionType of l, r, a 
	 * @return "0" - no dependents, "<" - left dependents, ">" - right dependents, "<>" - left and right dependents. 
	 */
	public String getValency(Direction direction)
	{
		switch (direction)
		{
		case  left: return getLeftValency();
		case  right: return getRightValency();
		case  all: return getLeftValency()+"-"+getRightValency();
		default: return null;
		}
	}
	
	/**
	 * Get the left valency of the node.
	 * @return "<" - left dependents
	 */
	public String getLeftValency()
	{
		StringBuilder build = new StringBuilder();
		
		if (getLeftMostDependent() != null)
		{
			build.append(StringConst.LESS_THAN);
			
			if (getLeftMostDependent(1) != null)
				build.append(StringConst.LESS_THAN);
		}
		
		return build.toString();
	}
	
	/**
	 * Get the right valency of the node.
	 * @return ">" - right dependents
	 */
	public String getRightValency()
	{
		StringBuilder build = new StringBuilder();
		
		if (getRightMostDependent() != null)
		{
			build.append(StringConst.GREATER_THAN);
			
			if (getRightMostDependent(1) != null)
				build.append(StringConst.GREATER_THAN);
		}
		
		return build.toString();
	}

	public Set<String> getDependentValueSet(Field field)
	{
		Set<String> s = new HashSet<>();
		
		for (N dep : getDependentList())
			s.add(dep.getValue(field));
		
		return s;
	}
	
	/**
	 * Get sub-categorization of the node.
	 * @param direction direction DirectionType of l, r, a
	 * @param field Field of tag feature
	 * @return "< {@code TagFeature}" for left sub-categorization, "> {@code TagFeature}" for right-categorization, and {@code null} if not exist
	 */
	public String getSubcategorization(Direction direction, Field field)
	{
		switch (direction)
		{
		case left: return getLeftSubcategorization (field);
		case right: return getRightSubcategorization(field);
		case all:
			String left = getLeftSubcategorization(field);
			if (left == null) return getRightSubcategorization(field);
			String right = getRightSubcategorization(field);
			return  (right == null) ? left : left+right;
		default: return null; 
		}
	}
	
	/**
	 * Get left sub-categorization of the node.
	 * @param field Field of tag feature 
	 * @return "< {@code TagFeature}" for left sub-categorization, {@code null} if not exist. 
	 */
	public String getLeftSubcategorization(Field field)
	{
		StringBuilder build = new StringBuilder();
		int i, size = getDependentSize();
		N node;
		
		for (i=0; i<size; i++)
		{
			node = getDependent(i);
			if (node.getID() > id) break;
			build.append(StringConst.LESS_THAN);
			build.append(node.getValue(field));
		}
		
		return build.length() > 0 ? build.toString() : null;
	}
	
	/**
	 * Get right sub-categorization of the node.
	 * @param field Field of tag feature 
	 * @return "> {@code TagFeature}" for right sub-categorization, {@code null} if not exist. 
	 */
	public String getRightSubcategorization(Field field)
	{
		StringBuilder build = new StringBuilder();
		int i, size = getDependentSize();
		N node;
		
		for (i=size-1; i>=0; i--)
		{
			node = getDependent(i);
			if (node.getID() < id) break;
			build.append(StringConst.GREATER_THAN);
			build.append(node.getValue(field));
		}
		
		return build.length() > 0 ? build.toString() : null;
	}
	
	
	/**
	 * Find the path of between this nodes and the input NLPNode.
	 * @param node the node that you want to find the path from this node
	 * @param field Field of the the node for search
	 * @return the path between the two nodes
	 */
	public String getPath(N node, Field field)
	{
		N lca = getLowestCommonAncestor(node);
		return (lca != null) ? getPath(node, lca, field) : null;
	}
	
	/**
	 * Find the path of between this nodes and the input NLPNode with the lowest common ancestor specified.
	 * @param node the node that you want to find the path from this node
	 * @param lca the lowest common ancestor NLPNode that you specified for the path
	 * @param field Field of the the node for search
	 * @return the path between the two nodes
	 */
	public String getPath(N node, N lca, Field field)
	{
		if (node == lca)
			return getPathAux(lca, self(), field, "^", true);
		
		if (this == lca)
			return getPathAux(lca, node, field, "|", true);
		
		return getPathAux(lca, self(), field, "^", true) + getPathAux(lca, node, field, "|", false);
	}
	
	private String getPathAux(N top, N bottom, Field field, String delim, boolean includeTop)
	{
		StringBuilder build = new StringBuilder();
		N node = bottom;
		int dist = 0;
		String s;
		
		do
		{
			s = node.getValue(field);
			
			if (s != null)
			{
				build.append(delim);
				build.append(s);
			}
			else
			{
				dist++;
			}
		
			node = node.getDependencyHead();
		}
		while (node != top && node != null);
		
		if (field == Field.distance)
		{
			build.append(delim);
			build.append(dist);
		}
		else if (field != Field.dependency_label && includeTop)
		{
			build.append(delim);
			build.append(top.getValue(field));
		}
		
		return build.length() == 0 ? null : build.toString();
	}
	
	/**
	 * Get a set of all the ancestor nodes of the node (ie. Parent node, Grandparent node, etc.).
	 * @return set of all the ancestor nodes
	 */
	public Set<N> getAncestorSet()
	{
		Set<N> set = new HashSet<>();
		N node = getDependencyHead();
		
		while (node != null)
		{
			set.add(node);
			node = node.getDependencyHead();
		}
		
		return set;
	}
	
	/**
	 * Get the first/lowest common ancestor of the two given nodes (this node and the input NLPNode).
	 * @param node the node that you want to find the lowest common ancestor with the node with
	 * @return the lowest common ancestor of the node and the specified node
	 */
	public N getLowestCommonAncestor(N node)
	{
		Set<N> set = getAncestorSet();
		set.add(self());
		
		while (node != null)
		{
			if (set.contains(node)) return node;
			node = node.getDependencyHead();
		}
		
		return null;
	}
	
//	============================== DEPENDENCY SETTERS ==============================
	
	/** Sets the dependency label. */
	public void setDependencyLabel(String label)
	{
		dependency_label = label;
	}
	
	/** Sets the dependency head. */
	public void setDependencyHead(N node)
	{
		if (hasDependencyHead())
		{
			dependency_head.dependent_list.remove(self());
			dependency_head.resetDependentIDs();
		}
		
		if (node != null)
		{
			node.dependent_list.addItem(self());
			node.resetDependentIDs();
		}
		
		dependency_head = node;
	}
	
	/** Sets the dependency head of this node with the specific label. */
	public void setDependencyHead(N node, String label)
	{
		setDependencyHead (node);
		setDependencyLabel(label);
	}
	
	/** Add the specific node as a dependent. */
	public void addDependent(N node)
	{
		node.setDependencyHead(self());
	}
	
	/** Add the specific node as a dependent with the specific label. */
	public void addDependent(N node, String label)
	{
		node.setDependencyHead(self(), label);
	}
	
	/**
	 * Clear out all dependencies (head, label, and sibling relations) of the node.
	 * @param the previous head information.
	 */
	public DEPArc<N> clearDependencies()
	{
		DEPArc<N> arc = new DEPArc<>(dependency_head, dependency_label);
		dependency_head  = null;
		dependency_label = null;
		dependent_list.clear();
		return arc;
	}
	
	protected void resetDependentIDs()
	{
		for (int i=0; i<dependent_list.size(); i++)
			dependent_list.get(i).dependent_id = i;
	}
	
//	============================== DEPENDENCY BOOLEANS ==============================

	/** @return true if this node has the dependency head; otherwise, false. */
	public boolean hasDependencyHead()
	{
		return dependency_head != null;
	}
	
	/** @return true if the dependency label of this node equals to the specific label. */
	public boolean isDependencyLabel(String label)
	{
		return label.equals(dependency_label);
	}
	
	/** @return true if the dependency label of this node equals to any of the specific labels. */
	public boolean isDependencyLabelAny(String... labels)
	{
		for (String label : labels)
		{
			if (isDependencyLabel(label))
				return true;
		}
		
		return false;
	}
	
	/** @return true if the dependency label of this node matches the specific pattern. */
	public boolean isDependencyLabel(Pattern pattern)
	{
		return pattern.matcher(dependency_label).find();
	}
	
	/** @return true if this node is a dependent of the specific node. */
	public boolean isDependentOf(N node)
	{
		return dependency_head == node;
	}
	
	/** {@link #isDependentOf(N)} && {@link #isDependencyLabel(String)}. */
	public boolean isDependentOf(N node, String label)
	{
		return isDependentOf(node) && isDependencyLabel(label);
	}
	
	/** @return true if the node has the specific node as a dependent. */
	public boolean containsDependent(N node)
	{
		return dependent_list.contains(node);
	}
	
	/**
	 * @return true if this node has a dependent with the specific label.
	 * @see #getFirstDependent(String, BiPredicate).
	 */
	public boolean containsDependent(String label, BiPredicate<N,String> predicate)
	{
		return getFirstDependent(label, predicate) != null;
	}
	
	/**
	 * @return true if this node has a dependent with the specific pattern.
	 * @see #getFirstDependentByLabel(Pattern).
	 */
	public boolean containsDependentByLabel(Pattern pattern)
	{
		return getFirstDependentByLabel(pattern) != null;
	}
	
	/**
	 * @return true if this node has a dependent with the specific label.
	 * @see #getFirstDependent(String, BiPredicate).
	 */
	public boolean containsDependentByLabel(String label)
	{
		return getFirstDependent(label, (n,l) -> n.isDependencyLabel(l)) != null;
	}
	
	/** @return true if the node is a descendant of the specific node. */
	public boolean isDescendantOf(N node)
	{
		N head = getDependencyHead();
		
		while (head != null)
		{
			if (head == node) return true;
			head = head.getDependencyHead();
		}
		
		return false;
	}
	
	/** @return true if this node is a sibling of the specific node. */
	public boolean isSiblingOf(N node)
	{
		return hasDependencyHead() && node.isDependentOf(dependency_head);
	}
	
//	============================== SEMANTICS ==============================

	/** @return a list of all semantic head arc of the node. */
	public List<DEPArc<N>> getSemanticHeadList()
	{
		return semantic_heads;
	}
	
	/** @return a list of all semantic head arc of the node with the given label. */
	public List<DEPArc<N>> getSemanticHeadList(String label)
	{
		List<DEPArc<N>> list = new ArrayList<>();
		
		for (DEPArc<N> arc : semantic_heads)
		{
			if (arc.isLabel(label))
				list.add(arc);
		}
		
		return list;
	}
	
	/** @return semantic arc relationship between the node and another given node. */
	public DEPArc<N> getSemanticHeadArc(N node)
	{
		for (DEPArc<N> arc : semantic_heads)
		{
			if (arc.isNode(node))
				return arc;
		}
		
		return null;
	}
	
	/** @return the semantic arc relationship between the node and another given node with a given label. */
	public DEPArc<N> getSemanticHeadArc(N node, String label)
	{
		for (DEPArc<N> arc : semantic_heads)
		{
			if (arc.equals(node, label))
				return arc;
		}
		
		return null;
	}
	
	/** @return the semantic arc relationship between the node and another given node with a given pattern. */
	public DEPArc<N> getSemanticHeadArc(N node, Pattern pattern)
	{
		for (DEPArc<N> arc : semantic_heads)
		{
			if (arc.equals(node, pattern))
				return arc;
		}
		
		return null;
	}
	
	/** @return the semantic label of the given in relation to the node. */
	public String getSemanticLabel(N node)
	{
		for (DEPArc<N> arc : semantic_heads)
		{
			if (arc.isNode(node))
				return arc.getLabel();
		}
		
		return null;
	}
	
	/** @return the first node that is found to have the semantic head of the given label from the node. */
	public N getFirstSemanticHead(String label)
	{
		for (DEPArc<N> arc : semantic_heads)
		{
			if (arc.isLabel(label))
				return arc.getNode();
		}
		
		return null;
	}
	
	/** @return the first node that is found to have the semantic head of the given pattern from the node. */
	public N getFirstSemanticHead(Pattern pattern)
	{
		for (DEPArc<N> arc : semantic_heads)
		{
			if (arc.isLabel(pattern))
				return arc.getNode();
		}
		
		return null;
	}
	
	/** @param arcs {@code Collection<DEPArc>} of the semantic heads. */
	public void addSemanticHeads(Collection<DEPArc<N>> arcs)
	{
		semantic_heads.addAll(arcs);
	}
	
	/** Adds a node a give the given semantic label to the node. */
	public void addSemanticHead(N head, String label)
	{
		addSemanticHead(new DEPArc<>(head, label));
	}
	
	/** Adds a semantic arc to the node. */
	public void addSemanticHead(DEPArc<N> arc)
	{
		semantic_heads.add(arc);
	}
	
	/** Sets semantic heads of the node. */
	public void setSemanticHeads(List<DEPArc<N>> arcs)
	{
		semantic_heads = arcs;
	}
	
	/** Removes all semantic heads of the node in relation to a given node.
	 * @return {@code true}, else {@code false} if nothing gets removed. 
	 */
	public boolean removeSemanticHead(N node)
	{
		for (DEPArc<N> arc : semantic_heads)
		{
			if (arc.isNode(node))
				return semantic_heads.remove(arc);
		}
		
		return false;
	}
	
	/** Removes a specific semantic head of the node. */
	public boolean removeSemanticHead(DEPArc<N> arc)
	{
		return semantic_heads.remove(arc);
	}
	
	/** Removes a collection of specific semantic heads of the node. */
	public void removeSemanticHeads(Collection<DEPArc<N>> arcs)
	{
		semantic_heads.removeAll(arcs);
	}
	
	/** Removes all semantic heads of the node that have the given label. */
	public void removeSemanticHeads(String label)
	{
		semantic_heads.removeAll(getSemanticHeadList(label));
	}
	
	/** Removes all semantic heads of the node. */
	public List<DEPArc<N>> clearSemanticHeads()
	{
		List<DEPArc<N>> backup = semantic_heads.subList(0, semantic_heads.size());
		semantic_heads.clear();
		return backup;
	}
	
	/** @return {@code true}, else {@code false} if there is no DEPArc between the two nodes. */
	public boolean isArgumentOf(N node)
	{
		return getSemanticHeadArc(node) != null;
	}
	
	/** @return {@code true}, else {@code false} if there is no DEPArc with the given label. */
	public boolean isArgumentOf(String label)
	{
		return getFirstSemanticHead(label) != null;
	}
	
	/** @return {@code true}, else {@code false} if there is no DEPArc with the given pattern. */
	public boolean isArgumentOf(Pattern pattern)
	{
		return getFirstSemanticHead(pattern) != null;
	}
	
	/** @return {@code true}, else {@code false} if there is no DEPArc with the given label between the two node. */
	public boolean isArgumentOf(N node, String label)
	{
		return getSemanticHeadArc(node, label) != null;
	}
	
	/** @return {@code true}, else {@code false} if there is no DEPArc with the given pattern between the two node. */
	public boolean isArgumentOf(N node, Pattern pattern)
	{
		return getSemanticHeadArc(node, pattern) != null;
	}

	/**
	 * Consider this node as a predicate.
	 * @param maxDepth  > 0.
	 * @param maxHeight > 0.
	 * @return list of (argument, lowest common ancestor) pairs.
	 */
	public List<Pair<N,N>> getArgumentCandidateList(int maxDepth, int maxHeight)
	{
		List<Pair<N,N>> list = new ArrayList<>();
		int i, j, beginIndex, endIndex = 0;
		N lca = self(), prev;
		
		// descendents
		for (N node : lca.getDependentList())
			list.add(new Pair<>(node, lca));
		
		for (i=1; i<maxDepth; i++)
		{
			if (endIndex == list.size()) break;
			beginIndex = endIndex;
			endIndex   = list.size();
			
			for (j=beginIndex; j<endIndex; j++)
			{
				for (N node : list.get(j).o1.getDependentList())
					list.add(new Pair<>(node, lca));
			}
		}
		
		// ancestors
		for (i=0; i<maxHeight; i++)
		{
			prev = lca;
			lca  = lca.getDependencyHead();
			if (lca == null || !lca.hasDependencyHead()) break;
			list.add(new Pair<>(lca, lca));
			
			for (N node : lca.getDependentList())
				if (node != prev) list.add(new Pair<>(node, lca));
		}
		
		return list;
	}
	
//	============================== HELPERS ==============================
	
	@Override
	public int compareTo(N node)
	{
		return id - node.id;
	}

	@Override
	public String toString()
	{
		StringJoiner join = new StringJoiner(StringConst.TAB);
		
		join.add(Integer.toString(id));
		join.add(toString(word_form));
		join.add(toString(lemma));
		join.add(toString(pos_tag));
		join.add(feat_map.toString());
		toStringDependency(join);
		join.add(toStringSemantics(semantic_heads));
		join.add(toString(nament_tag));
		
		return join.toString();
	}
	
	private String toString(String s)
	{
		return (s == null) ? TSVReader.BLANK : s;
	}
	
	private void toStringDependency(StringJoiner join)
	{
		if (hasDependencyHead())
		{
			join.add(Integer.toString(dependency_head.id));
			join.add(toString(dependency_label));
		}
		else
		{
			join.add(TSVReader.BLANK);
			join.add(TSVReader.BLANK);
		}
	}
	
	private <T extends AbstractArc<N>>String toStringSemantics(List<T> arcs)
	{
		if (arcs == null || arcs.isEmpty())
			return TSVReader.BLANK;
		
		Collections.sort(arcs);
		return Joiner.join(arcs, AbstractArc.ARC_DELIM);
	}

//	============================== HELPERS ==============================
	
	public List<DEPArc<N>> getSecondaryHeadList()
	{
		return secondary_heads;
	}
	
	public void setSecondaryHeads(List<DEPArc<N>> heads)
	{
		secondary_heads = heads;
	}
	
	public void addSecondaryHead(DEPArc<N> head)
	{
		secondary_heads.add(head);
	}
	
	public void addSecondaryHead(N head, String label)
	{
		addSecondaryHead(new DEPArc<>(head, label));
	}
}
