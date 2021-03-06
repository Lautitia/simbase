package com.guokr.simbase;

import gnu.trove.iterator.TFloatIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.guokr.simbase.util.Sorter;

public class SimTable implements KryoSerializable {

	private Map<String, Integer> dimensions = new HashMap<String, Integer>();
	private String[] current = new String[0];

	private TFloatList probs = new TFloatArrayList();
	private TIntIntMap indexer = new TIntIntHashMap();
	private TIntObjectHashMap<TIntList> reverseIndexer = new TIntObjectHashMap<TIntList>();
	private TIntFloatMap waterLine = new TIntFloatHashMap();
	private TIntObjectHashMap<Sorter> scores = new TIntObjectHashMap<Sorter>();

	private Map<String, Object> context;
	private double loadfactor;
	private int maxlimits;

	public SimTable() {
		loadfactor = 0.75;
		maxlimits = 20;
	}

	public SimTable(Map<String, Object> context) {
		this.context = context;
		loadfactor = (Double) context.get("loadfactor");
		maxlimits = (Integer) context.get("maxlimits");
	}

	private void score(int src, int tgt, float value) {
		Sorter sorter = scores.get(src);
		if (sorter == null) {
			sorter = new Sorter(maxlimits);
			synchronized (scores) {
				scores.put(src, sorter);
			}
		}

		TIntList reverseRange = reverseIndexer.get(tgt);
		if (reverseRange == null) {
			reverseRange = new TIntArrayList();
			reverseIndexer.put(tgt, reverseRange);
		}

		if (src != tgt) {
			if (waterLine.containsKey(src)) {
				if (waterLine.get(src) <= value) {// 先前的添加不改变水位线
					sorter.add(tgt, value);
					reverseRange.add(src);// 添加反向索引
				}
			} else {
				waterLine.put(src, 0f);
				sorter.add(tgt, value);
				reverseRange.add(src);// 添加反向索引
			}
		}

		if (sorter.size() > maxlimits) {
			float lastScore = sorter.removeLast();
			if (lastScore > waterLine.get(src))
				waterLine.put(src, lastScore);// 放置水位线
		}
	}

	private float[] mapping(float[] input) {
		int size = dimensions.size();
		float[] ret = new float[size];

		for (int i = 0; i < size; i++) {
			ret[i] = 0;
		}
		for (int i = 0; i < input.length; i++) {
			String dim = current[i];
			int pos = dimensions.get(dim);
			ret[pos] = input[i];
		}

		return ret;
	}

	public void revise(String[] schema) {
		current = schema;
		for (String dim : schema) {
			if (!dimensions.containsKey(dim)) {
				dimensions.put(dim, dimensions.size());
			}
		}
	}

	public String[] schema() {
		return current;
	}

	public void add(int docid, float[] distr) {
		if (current != null && current.length != dimensions.size()) {
			distr = mapping(distr);
		}

		float length = 0;
		int start;
		if (indexer.containsKey(docid)) {
			start = indexer.get(docid);
			int cursor = start;
			for (float val : distr) {
				probs.set(cursor, val);
				length += val * val;
				cursor++;
			}
			probs.set(cursor++, (float) (docid + 1));
			probs.set(cursor, length);
		} else {
			start = probs.size();
			indexer.put(docid, start);
			for (float val : distr) {
				probs.add(val);
				length += val * val;
			}
			probs.add((float) (docid + 1));
			probs.add(length);
		}
		int end = probs.size();

		float scoring = 0;
		int base = 0;
		for (int offset = 0; offset < end; offset++) {
			float val = probs.get(offset);
			if (val >= 0) {
				if (val < 1) {
					int idx = offset - base;
					if (idx < end - start - 1) {
						float another = distr[idx];// ArrayIndexOutOfBoundsException
						scoring += another * val;
					}
				} else {
					float cosine = scoring * scoring / length
							/ probs.get(offset + 1);
					score(docid, (int) val - 1, cosine);
					score((int) val - 1, docid, cosine);
					scoring = 0;
					offset = offset + 1;
					base = offset + 1;
				}
			} else {
				base = offset + 1;
			}
		}
	}

	public void append(int docid, Object[] pairs) {
		float[] distr = new float[dimensions.size()];
		for (int i = 0; i < pairs.length;) {
			String key = (String)pairs[i++];
			Float val = (Float)pairs[i++];
			if (dimensions.containsKey(key)) {
				distr[dimensions.get(key)] = val;
			}
		}
		add(docid, distr);
	}

	public void put(int docid, float[] distr) {
		add(docid, distr);
	}

	public void update(int docid, Object[] pairs) {
		append(docid, pairs);
	}

	public void delete(int docid) {
		if (indexer.containsKey(docid)) {
			int cursor = indexer.get(docid);
			while (true) {
				float val = probs.get(cursor);
				if (val < 0f) {
					break;
				}
				if (val >= 1f) {// 到达docid的指针部分
					probs.set(cursor, -val);
					cursor++;
					val = probs.get(cursor);
					probs.set(cursor, -val);
					break;
				}

				probs.set(cursor, -val);
				cursor++;
			}
		}

		indexer.remove(docid);// HashMap里没有这个键了也可以用= =
		scores.remove(docid);
		waterLine.remove(docid);// 移除水位线

		// 根据反向索引移除scores
		if (reverseIndexer.contains(docid)) {
			TIntIterator reverseIter = reverseIndexer.get(docid).iterator();
			while (reverseIter.hasNext()) {
				int reverId = reverseIter.next();
				scores.get(reverId).remove(docid);
			}
			reverseIndexer.remove(docid);// 移除反向索引
		}
	}

	public TFloatList get(int docid) {
		TFloatList res = null;
		if (indexer.containsKey(docid)) {
			res = new TFloatArrayList();
			int idx = indexer.get(docid);
			float ftmp = 0;
			while ((ftmp = probs.get(idx++)) > 0 && (ftmp < 1)) {
				res.add(ftmp);
			}
		}
		return res;
	}

	public float similarity(int docid1, int docid2) {
		return scores.get(docid1).get(docid2);
	}

	public String[] retrieve(int docid) {
		if (scores.contains(docid)) {
			return scores.get(docid).pickle();
		} else {
			return new String[0];
		}
	}

	public int[] recommend(int docid) {
		if (scores.contains(docid)) {
			return scores.get(docid).docids();
		} else {
			return new int[0];
		}
	}

	public String[] nearby(float[] distr) {
		return null;// TODO
	}

	public SimTable clone() {
		SimTable peer = new SimTable(context);

		peer.dimensions = dimensions;
		peer.current = current;

		int cursor = 0, start = 0;
		TFloatIterator piter = probs.iterator();
		peer.probs = new TFloatArrayList((int) (probs.size() / loadfactor));
		while (piter.hasNext()) {
			float value = piter.next();
			if (value < 0) {
				start++;
				// continue;
			} else {
				if (value > 1) {
					peer.indexer.put((int) value - 1, start);
					peer.probs.add(value);
					cursor++;
					peer.probs.add(piter.next());
					start = cursor + 1;
				} else {
					peer.probs.add(value);
				}
			}
			cursor++;
		}

		synchronized (scores) {
			TIntIterator siter = scores.keySet().iterator();
			while (siter.hasNext()) {
				Integer docid = siter.next();
				Sorter thissorter = scores.get(docid);
				Sorter peersorter = new Sorter(maxlimits);
				for (int key : thissorter.docids()) {
					peersorter.add(key, thissorter.get(key));
				}
				peer.scores.put(docid, peersorter);
			}
		}

		return peer;
	}

	public void reload(SimTable table) {
		probs = table.probs;
		indexer = table.indexer;
		scores = table.scores;
	}

	@Override
	// 重载序列化代码
	public void read(Kryo kryo, Input input) {
		current = kryo.readObject(input, String[].class);
		int dimsize = kryo.readObject(input, int.class);
		while (dimsize > 0) {
			String key = kryo.readObject(input, String.class);
			int value = kryo.readObject(input, int.class);
			dimensions.put(key, value);
			dimsize--;
		}

		probs = kryo.readObject(input, TFloatArrayList.class);
		int indexsize = kryo.readObject(input, int.class);
		while (indexsize > 0) {
			int key = kryo.readObject(input, int.class);
			int value = kryo.readObject(input, int.class);
			indexer.put(key, value);
			indexsize--;
		}
		int scoresize = kryo.readObject(input, int.class);
		while (scoresize > 0) {
			Integer docid = kryo.readObject(input, Integer.class);
			Sorter sorter = null;
			sorter = new Sorter(maxlimits);
			scores.put(docid, sorter);
			int listsize = kryo.readObject(input, int.class);
			while (listsize > 0) {
				Integer key = kryo.readObject(input, Integer.class);
				Float score = kryo.readObject(input, Float.class);
				sorter.add(key, score);
				listsize--;
			}
			scoresize--;
		}
	}

	@Override
	public void write(Kryo kryo, Output output) {
		kryo.writeObject(output, current);
		kryo.writeObject(output, dimensions.size());
		for (String key : dimensions.keySet()) {
			int pos = dimensions.get(key);
			kryo.writeObject(output, key);
			kryo.writeObject(output, pos);
		}

		kryo.writeObject(output, probs);

		TIntIntMap indexmap = indexer;
		kryo.writeObject(output, indexmap.size());
		for (int key : indexmap.keys()) {
			int indexscore = indexmap.get(key);
			kryo.writeObject(output, key);
			kryo.writeObject(output, indexscore);
		}

		TIntIterator iter = scores.keySet().iterator();
		kryo.writeObject(output, scores.size());
		while (iter.hasNext()) {
			Integer docid = iter.next();
			kryo.writeObject(output, docid);
			Sorter sorter = scores.get(docid);
			kryo.writeObject(output, sorter.size());
			for (int key : sorter.docids()) {
				Float score = sorter.get(key);
				kryo.writeObject(output, key);
				kryo.writeObject(output, score);
			}
		}

	}
}
